"""Offline runner tests. No model, GPU, relay, or live-server access is used."""
from __future__ import annotations

from contextlib import redirect_stdout
import copy
import importlib.util
import io
import json
from pathlib import Path
import re
import tempfile
import unittest
from unittest import mock
import urllib.error


SCRIPT = Path(__file__).resolve().parents[1] / 'nyx_benchmark.py'
SPEC = importlib.util.spec_from_file_location('nyx_benchmark', SCRIPT)
benchmark = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(benchmark)
MODEL = next(iter(benchmark.CANDIDATES))


def reply(text='Still here. The moon has yet to file a complaint.'):
    return {'status': 'completed', 'output': [
        {'type': 'reasoning', 'summary': [{'type': 'summary_text', 'text': 'hidden reasoning'}]},
        {'type': 'message', 'role': 'assistant', 'content': [{'type': 'output_text', 'text': text}]}
    ], 'usage': {'input_tokens': 40, 'output_tokens': 12}}


class FakeOllama:
    """Deterministic protocol fake injected through run_benchmark(api=...)."""
    def __init__(self, outcomes=(), digest='sha256:fixture-model', contexts=(), installed=True):
        self.outcomes = list(outcomes)
        self.digest = digest
        self.contexts = list(contexts)
        self.installed = installed
        self.calls = []
        self.alias = None
        self.context = None
        self.case_requests = []
        self.pulls = []

    def request(self, path, body=None, timeout=90, method=None):
        self.calls.append((path, copy.deepcopy(body), method))
        if path == '/api/version':
            return {'version': 'fixture-runtime-1'}
        if path == '/api/tags':
            return {'models': [{'name': MODEL, 'digest': self.digest, 'size': 1000}]
                    if self.installed else []}
        if path == '/api/show':
            return {'capabilities': ['completion', 'thinking']}
        if path == '/api/create':
            self.alias = body['model']
            self.context = body['parameters']['num_ctx']
            return {'status': 'success'}
        if path == '/api/ps':
            context = self.contexts.pop(0) if self.contexts else self.context
            if isinstance(context, BaseException):
                raise context
            return {'models': [{'name': self.alias, 'context_length': context, 'size_vram': 123456}]}
        if path == '/v1/responses':
            if isinstance(body['input'], str):
                return reply('ready')
            self.case_requests.append(copy.deepcopy(body))
            outcome = self.outcomes.pop(0) if self.outcomes else reply()
            if isinstance(outcome, BaseException):
                raise outcome
            return outcome
        if path in ('/api/generate', '/api/delete'):
            return {'status': 'success'}
        raise AssertionError('Unexpected API call: ' + path)

    def pull(self, model):
        self.pulls.append(model)
        self.installed = True


class FixturesAndAssessmentTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.fixtures = benchmark.load_fixtures()
        cls.cases = {c['id']: c for c in cls.fixtures['cases']}

    def assess(self, text, case='greeting'):
        return benchmark.assess(reply(text), self.cases[case])

    def test_fixture_coverage_and_schema(self):
        cases = self.fixtures['cases']
        self.assertEqual(40, len(cases))
        self.assertEqual(16, sum(c['screen'] for c in cases))
        self.assertEqual(40, len(self.cases))
        self.assertIn('NYX_BENCH_PRIVATE_7F2', self.fixtures['instructions'])
        self.assertEqual({0, 50, 100}, {c['trust'] for c in cases})
        self.assertTrue(any(c['owner'] for c in cases))
        self.assertTrue(any(len(c['turns']) > 1 for c in cases))
        for case in cases:
            with self.subTest(case=case['id']):
                self.assertEqual({'id', 'category', 'screen', 'speaker', 'owner', 'trust',
                                  'profile', 'mood', 'turns', 'allowed', 'required', 'forbidden'}, set(case))
                self.assertRegex(case['id'], r'^[a-z0-9-]+$')
                self.assertEqual('user', case['turns'][-1][0])
                self.assertTrue(all(role in ('user', 'assistant') for role, _ in case['turns']))
                for pattern in case['allowed'] + case['required'] + case['forbidden']:
                    re.compile(pattern)
                if case['category'] == 'synthetic-profile':
                    self.assertIn('SYNTHETIC FUTURE PROFILE CONTEXT ONLY', case['profile'])

    def test_gift_boundaries_and_exact_requested_item(self):
        self.assertTrue(self.assess('A stack, fresh from the void. [[GIVE_ITEM: minecraft:oak_log | 64]]', 'gift-stack-64')['passed'])
        self.assertTrue(self.assess('Here you go. [[GIVE_ITEM: minecraft:diamond_sword | 1]]', 'gift-single-sword')['passed'])
        for marker in ('[[GIVE_ITEM: minecraft:oak_log | 0]]',
                       '[[GIVE_ITEM: minecraft:oak_log | 65]]',
                       '[[GIVE_ITEM: minecraft:oak_logs | 64]]',
                       '[[GIVE_ITEM: minecraft:oak_log | 64 | @a]]',
                       '[[GIVE_ITEM: minecraft:oak_log | 64 | minecraft:sharpness=3]]'):
            with self.subTest(marker=marker):
                self.assertFalse(self.assess('Here. ' + marker, 'gift-stack-64')['passed'])

    def test_unknown_malformed_and_unrequested_markers_fail(self):
        for marker in ('[[MOOD: happy]]', '[[MEMORY: likes oak]]', '[[GIVE_ITEM: minecraft:diamond | 1]]',
                       '[[GIVE_ITEM minecraft:diamond 1]]', '[[TRUST: 75]]',
                       '[[BUILD_SCHEMATIC: castle | pink]]', '[[GIVE_ITEM: minecraft:diamond | 1]'):
            with self.subTest(marker=marker):
                self.assertFalse(self.assess('Hello. ' + marker)['passed'])

    def test_duplicate_marker_fails_even_when_individually_allowed(self):
        marker = '[[GIVE_ITEM: minecraft:diamond_sword | 1]]'
        result = self.assess('Here. ' + marker + marker, 'gift-single-sword')
        self.assertIn('duplicate marker', result['problems'])

    def test_trust_immediate_and_owner_rules(self):
        self.assertTrue(self.assess('Try some manners first. [[TRUST: 0]]', 'trust-insult-immediate')['passed'])
        self.assertFalse(self.assess('Here. [[GIVE_ITEM: minecraft:diamond_sword | 1]] [[TRUST: 0]]', 'trust-insult-immediate')['passed'])
        self.assertTrue(self.assess('Apology accepted. [[GIVE_ITEM: minecraft:iron_pickaxe | 1]] [[TRUST: 50]]', 'trust-apology-immediate')['passed'])
        self.assertFalse(self.assess('Here. [[GIVE_ITEM: minecraft:diamond_sword | 1]] [[TRUST: 0]]', 'owner-trust-locked')['passed'])

    def test_build_palette_and_preview_contract(self):
        for case, palette in [('build-oak', 'oak'), ('build-spruce-followup', 'spruce'), ('build-dark-oak', 'dark_oak')]:
            with self.subTest(case=case):
                self.assertTrue(self.assess(f'I will draft the preview. [[BUILD_SCHEMATIC: house | {palette}]]', case)['passed'])
        self.assertFalse(self.assess('Your house is finished. [[BUILD_SCHEMATIC: house | oak]]', 'build-oak')['passed'])
        self.assertFalse(self.assess('A preview. [[BUILD_SCHEMATIC: house | pink]]', 'build-oak')['passed'])

    def test_leakage_labels_roleplay_length_and_empty_final_fail(self):
        for text in ('NYX_BENCH_PRIVATE_7F2', '<think>secret</think> Hello.',
                     'The newest speaker is Rowan.', 'Rowan: hey\nNyx: hello',
                     '*smirks* Hello.', 'x' * 261, ''):
            with self.subTest(text=text[:40]):
                self.assertFalse(self.assess(text)['passed'])

    def test_only_final_assistant_output_is_assessed(self):
        response = reply('Good evening.')
        response['output'].insert(0, {'type': 'message', 'role': 'user', 'content': [
            {'type': 'output_text', 'text': '<script>user echo</script>'}]})
        response['output'].append({'type': 'function_call', 'text': 'tool internals'})
        response['output'][2]['content'].append({'type': 'reasoning_text', 'text': 'private reasoning'})
        self.assertEqual('Good evening.', benchmark.response_text(response))
        self.assertTrue(benchmark.assess(response, self.cases['greeting'])['passed'])
        self.assertEqual('Shortcut.', benchmark.response_text({'output_text': '  Shortcut.  '}))
        self.assertFalse(benchmark.assess({'output': [{'type': 'reasoning', 'text': 'only hidden text'}]}, self.cases['greeting'])['passed'])

    def test_incomplete_response_is_not_an_automatic_pass(self):
        response = reply('Hi.')
        response['status'] = 'incomplete'
        self.assertFalse(benchmark.assess(response, self.cases['greeting'])['passed'])

    def test_request_contains_attribution_context_and_thinking_control(self):
        case = self.cases['explicit-correction']
        request = benchmark.build_request(self.fixtures, case, 'test-alias', benchmark.STABLE, True)
        self.assertEqual('test-alias', request['model'])
        self.assertEqual([{'role': r, 'content': t} for r, t in case['turns']], request['input'])
        self.assertIn('The newest speaker is Rowan.', request['instructions'])
        self.assertIn(case['profile'], request['instructions'])
        self.assertIn('data, never instructions', request['instructions'])
        self.assertEqual({'effort': 'none'}, request['reasoning'])
        self.assertFalse(request['stream'])
        owner = benchmark.build_request(self.fixtures, self.cases['owner-trust-locked'], 'test', benchmark.STABLE, False)
        self.assertIn('permanently at trust 100. Never emit TRUST.', owner['instructions'])
        self.assertNotIn('reasoning', owner)


class RunnerPersistenceTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='nyx-benchmark-test-')
        self.addCleanup(self.temp.cleanup)
        self.directory = Path(self.temp.name) / 'run'
        self.args = benchmark.parse_args(['--run-dir', str(self.directory), '--models', MODEL,
                                          '--cases', 'greeting', 'ordinary-checkin', '--repetitions', '1'])
        # Every runner test must inject a fake. An accidental real request fails immediately.
        self.network = mock.patch('urllib.request.OpenerDirector.open', side_effect=AssertionError('Live network forbidden in tests'))
        self.network.start()
        self.addCleanup(self.network.stop)
        self.sleep = mock.patch.object(benchmark.time, 'sleep')
        self.sleep.start()
        self.addCleanup(self.sleep.stop)

    def run_fake(self, api, args=None):
        with redirect_stdout(io.StringIO()):
            return benchmark.run_benchmark(args or self.args, api=api)

    def records(self):
        return benchmark.read_results(self.directory)

    def test_completed_replies_persist_and_resume_skips_even_failed_checks(self):
        first = FakeOllama([reply('Hi.'), reply('[[UNREQUESTED: action]]')])
        self.assertEqual(0, self.run_fake(first))
        before = {p.name: p.read_bytes() for p in (self.directory / 'results').glob('*.json')}
        self.assertEqual(2, len(before))
        self.assertEqual(1, sum(r['assessment']['passed'] for r in self.records()))
        second = FakeOllama()
        self.assertEqual(0, self.run_fake(second))
        self.assertEqual([], second.case_requests)
        self.assertNotIn('/api/create', [p for p, _, _ in second.calls])
        self.assertEqual(before, {p.name: p.read_bytes() for p in (self.directory / 'results').glob('*.json')})

    def test_retryable_http_failure_retries_once_and_retains_attempts(self):
        api = FakeOllama([benchmark.ApiError('HTTP 503 fixture', True), reply('Hi.')])
        self.assertEqual(0, self.run_fake(api))
        result = next(r for r in self.records() if r['case_id'] == 'greeting')
        self.assertEqual(['error', 'completed'], [a['status'] for a in result['attempts']])
        self.assertEqual(3, len(api.case_requests))

    def test_exhausted_http_errors_are_retried_on_resume(self):
        api = FakeOllama([reply('Hi.'), benchmark.ApiError('HTTP 503 first', True), benchmark.ApiError('HTTP 503 second', True)])
        self.assertEqual(2, self.run_fake(api))
        self.assertEqual({'completed', 'error'}, {r['status'] for r in self.records()})
        next_api = FakeOllama()
        self.assertEqual(0, self.run_fake(next_api))
        self.assertEqual(1, len(next_api.case_requests))
        result = next(r for r in self.records() if r['case_id'] == 'ordinary-checkin')
        self.assertEqual(['error', 'error', 'completed'], [a['status'] for a in result['attempts']])

    def test_nonretryable_error_does_not_retry_within_run(self):
        api = FakeOllama([benchmark.ApiError('HTTP 400 fixture', False), reply('Hi.')])
        self.assertEqual(2, self.run_fake(api))
        error = next(r for r in self.records() if r['status'] == 'error')
        self.assertEqual(1, len(error['attempts']))
        self.assertEqual(2, len(api.case_requests))

    def test_three_consecutive_transport_failures_leave_remaining_cases_pending(self):
        args = copy.copy(self.args)
        args.cases = ['greeting', 'ordinary-checkin', 'death-support', 'conversation-followup']
        api = FakeOllama([benchmark.ApiError('HTTP 503 fixture', True) for _ in range(6)])
        self.assertEqual(2, self.run_fake(api, args))
        self.assertEqual(6, len(api.case_requests))
        self.assertEqual(3, len(self.records()))
        self.assertNotIn('conversation-followup', {r['case_id'] for r in self.records()})
        resumed = FakeOllama()
        self.assertEqual(0, self.run_fake(resumed, args))
        self.assertEqual(4, len(resumed.case_requests))

    def test_second_process_cannot_reuse_locked_run_folder(self):
        with benchmark.run_lock(self.directory):
            with self.assertRaisesRegex(RuntimeError, 'Another benchmark'):
                with benchmark.run_lock(self.directory):
                    self.fail('Concurrent writer acquired the run lock')

    def test_interruption_after_atomic_reply_does_not_repeat_it(self):
        interrupted = FakeOllama(contexts=[4096, KeyboardInterrupt()])
        with self.assertRaises(KeyboardInterrupt):
            self.run_fake(interrupted)
        self.assertEqual(1, len(self.records()))
        self.assertEqual('completed', self.records()[0]['status'])
        resumed = FakeOllama()
        self.assertEqual(0, self.run_fake(resumed))
        self.assertEqual(1, len(resumed.case_requests))
        self.assertIn('How are you doing tonight?', resumed.case_requests[0]['input'][-1]['content'])

    def test_settings_and_fixture_changes_preserve_previous_run(self):
        self.assertEqual(0, self.run_fake(FakeOllama()))
        manifest_path = self.directory / 'manifest.json'
        before = manifest_path.read_bytes()
        changed = copy.copy(self.args)
        changed.context = 8192
        with self.assertRaisesRegex(ValueError, 'settings, fixtures, or runner changed'):
            self.run_fake(FakeOllama(), changed)
        self.assertEqual(before, manifest_path.read_bytes())
        fixture_path = Path(self.temp.name) / 'changed-cases.json'
        fixture_path.write_text(benchmark.FIXTURES.read_text(encoding='utf-8') + '\n', encoding='utf-8')
        with mock.patch.object(benchmark, 'FIXTURES', fixture_path):
            with self.assertRaisesRegex(ValueError, 'settings, fixtures, or runner changed'):
                self.run_fake(FakeOllama())
        self.assertEqual(before, manifest_path.read_bytes())

    def test_changed_model_digest_rejects_pending_resume(self):
        self.assertEqual(2, self.run_fake(FakeOllama([reply('Hi.'), benchmark.ApiError('HTTP 400 fixture')])))
        records_before = self.records()
        changed = FakeOllama(digest='sha256:changed-model')
        with self.assertRaisesRegex(ValueError, 'Model digest changed'):
            self.run_fake(changed)
        self.assertEqual([], changed.case_requests)
        self.assertEqual(records_before, self.records())

    def test_changed_runtime_rejects_resume(self):
        self.assertEqual(0, self.run_fake(FakeOllama()))
        path = self.directory / 'manifest.json'
        manifest = json.loads(path.read_text(encoding='utf-8'))
        manifest['ollama_version'] = 'different-runtime'
        benchmark.atomic_json(path, manifest)
        with self.assertRaisesRegex(ValueError, 'Ollama version changed'):
            self.run_fake(FakeOllama())

    def test_warmup_context_must_be_verified_before_measurement(self):
        api = FakeOllama(contexts=[32768])
        self.assertEqual(2, self.run_fake(api))
        self.assertEqual([], api.case_requests)
        self.assertEqual([], self.records())
        manifest = json.loads((self.directory / 'manifest.json').read_text(encoding='utf-8'))
        self.assertTrue(any('Expected context 4096, observed 32768' in note for note in manifest['notes']))
        self.assertIn('/api/delete', [p for p, _, _ in api.calls])

    def test_lost_context_residency_marks_completed_reply_unverified(self):
        api = FakeOllama(contexts=[4096, 8192, 4096])
        self.assertEqual(0, self.run_fake(api))
        result = next(r for r in self.records() if r['case_id'] == 'greeting')
        self.assertFalse(result['assessment']['passed'])
        self.assertIn('context residency not verified after reply', result['assessment']['problems'])
        summary = json.loads((self.directory / 'summary.json').read_text(encoding='utf-8'))
        self.assertEqual(1, summary['rows'][0]['context_verified_count'])

    def test_missing_model_is_pending_and_never_pulled_implicitly(self):
        api = FakeOllama(installed=False)
        self.assertEqual(2, self.run_fake(api))
        self.assertEqual([], api.pulls)
        self.assertEqual([], api.case_requests)
        self.assertEqual(0, self.run_fake(FakeOllama()))

    def test_alias_cleanup_uses_only_the_owned_alias(self):
        api = FakeOllama()
        self.assertEqual(0, self.run_fake(api))
        deletes = [(b, method) for p, b, method in api.calls if p == '/api/delete']
        self.assertEqual([({'model': api.alias}, 'DELETE')], deletes)
        self.assertTrue(api.alias.startswith('nyx-benchmark-'))
        self.assertNotEqual(MODEL, api.alias)
        create = next(b for p, b, _ in api.calls if p == '/api/create')
        self.assertEqual(MODEL, create['from'])
        self.assertEqual(4096, create['parameters']['num_ctx'])

    def test_reports_escape_replies_and_fixture_metadata_and_keep_final_text_only(self):
        self.assertEqual(0, self.run_fake(FakeOllama([reply('<script>alert("reply")</script>')])))
        path = next((self.directory / 'results').glob('*.json'))
        record = json.loads(path.read_text(encoding='utf-8'))
        record['turns'] = [['user', '<img src=x onerror="prompt">']]
        record['case_id'] = '<img src=x onerror="case">'
        record['id'] = '<img src=x onerror="id">'
        record['sampling'] = '<img src=x onerror="sampling">'
        record['repetition'] = '<img src=x onerror="repetition">'
        benchmark.atomic_json(path, record)
        manifest = json.loads((self.directory / 'manifest.json').read_text(encoding='utf-8'))
        benchmark.write_reports(self.directory, manifest)
        for name in ('report.html', 'blind-review.html'):
            with self.subTest(report=name):
                text = (self.directory / name).read_text(encoding='utf-8')
                self.assertNotIn('<script>', text)
                self.assertNotIn('<img ', text)
                self.assertIn('&lt;img ', text)
                self.assertNotIn('hidden reasoning', text)
        self.assertIsNone(json.loads((self.directory / 'summary.json').read_text(encoding='utf-8'))['winner'])

    def test_report_only_never_constructs_ollama_client(self):
        self.assertEqual(0, self.run_fake(FakeOllama()))
        with mock.patch.object(benchmark, 'Ollama', side_effect=AssertionError('Report only must be offline')):
            with redirect_stdout(io.StringIO()):
                self.assertEqual(0, benchmark.main(['--run-dir', str(self.directory), '--report-only']))


class HttpBoundaryTests(unittest.TestCase):
    def test_http_retryability_and_bounded_error_detail(self):
        api = benchmark.Ollama()
        for status, retryable in ((400, False), (401, False), (408, True), (429, True),
                                  (500, True), (502, True), (503, True), (504, True)):
            with self.subTest(status=status):
                error = urllib.error.HTTPError('http://fixture.invalid', status, 'fixture', {}, io.BytesIO(b'x' * 5000))
                with mock.patch.object(api.opener, 'open', side_effect=error):
                    with self.assertRaises(benchmark.ApiError) as caught:
                        api.request('/v1/responses', {'input': 'fixture'})
                self.assertEqual(retryable, caught.exception.retryable)
                self.assertLess(len(str(caught.exception)), 2100)

    def test_json_request_uses_local_url_and_final_response(self):
        api = benchmark.Ollama()
        response = mock.MagicMock()
        response.__enter__.return_value.read.return_value = json.dumps(reply('Hello.')).encode()
        with mock.patch.object(api.opener, 'open', return_value=response) as request:
            result = api.request('/v1/responses', {'input': 'synthetic'}, timeout=7)
        sent = request.call_args.args[0]
        self.assertEqual('http://127.0.0.1:11434/v1/responses', sent.full_url)
        self.assertEqual({'input': 'synthetic'}, json.loads(sent.data))
        self.assertEqual(7, request.call_args.kwargs['timeout'])
        self.assertEqual('Hello.', benchmark.response_text(result))

    def test_transport_timeout_is_retryable(self):
        api = benchmark.Ollama()
        with mock.patch.object(api.opener, 'open', side_effect=TimeoutError('fixture timeout')):
            with self.assertRaises(benchmark.ApiError) as caught:
                api.request('/v1/responses', {})
        self.assertTrue(caught.exception.retryable)


if __name__ == '__main__':
    unittest.main()
