# BigOscieGF local Ollama bridge

## Architecture

```text
BigOscieGF on PebbleHost
  -> HTTPS Cloudflare Tunnel URL
  -> authenticated relay on 127.0.0.1:11435
  -> Ollama on 127.0.0.1:11434
  -> Magnum v4 12B Q5_K_M on the RX 6900 XT
```

Ollama itself stays bound to localhost. The relay exposes only `/v1/responses`,
requires a random bearer token, limits request size, disables streaming, and pins
chat requests to the configured Nyx model. Local Nyx 0.6.0 adds authenticated
`X-Nyx-Task: blueprint` requests pinned to `NYX_BUILDER_MODEL` (default
`qwen3-coder:30b`), separately from `NYX_OLLAMA_MODEL`. Arbitrary caller-supplied
model names are still ignored. The builder model must already be installed;
the relay does not download models. Design requests allow at most 4096 output
tokens and a 120-second upstream timeout; chat keeps its 45-second timeout.
The plugin permits one repair attempt for invalid geometry. Switching models
can add GPU loading delays to builds and subsequent chat. Restart the relay
with the updated source when deploying 0.6.0; no restart is performed by the
local implementation task. Direct endpoints can instead set `ai.building.model`.

## Local commands

Run setup once:

```powershell
.\scripts\Setup-OllamaBridge.ps1
```

Start the relay and temporary Cloudflare Quick Tunnel:

```powershell
.\scripts\Start-OllamaBridge.ps1
```

Stop both bridge processes:

```powershell
.\scripts\Stop-OllamaBridge.ps1
```

The start command prints the endpoint for BigOscieGF. The secret relay token is
stored only in the Git-ignored `.env` file.

`NYX_OLLAMA_MODEL` selects Nyx's model. The scripts temporarily accept the old
`BIGOSCIE_OLLAMA_MODEL` name during migration, but new configuration should use
the Nyx-specific name.

Nyx uses `hf.co/bartowski/magnum-v4-12b-GGUF:Q5_K_M`, a Q5 quantization of the
Magnum v4 12B conversational/prose model. The model is selected specifically for
character dialogue; Nyx's exact identity and behavior remain in the plugin's
reviewable `ai.personality` and per-request instructions.

## BigOscieGF configuration

The live plugin configuration needs:

```yaml
ai:
  enabled: true
  provider: "ollama"
  endpoint: "https://GENERATED.trycloudflare.com/v1/responses"
  model: "hf.co/bartowski/magnum-v4-12b-GGUF:Q5_K_M"
  api-key: "LOCAL_RELAY_TOKEN"
```

The relay overrides the requested model as an extra safeguard.

## Availability

Nyx's AI is available only while this Windows PC, Ollama, the relay, and the
tunnel are running. Plugin fallback lines continue to work when the bridge is
offline.

Cloudflare Quick Tunnels are suitable for testing and use a new random hostname
when restarted. For dependable long-term operation, replace the Quick Tunnel
with a named Cloudflare Tunnel and stable hostname.

## Resumable local model benchmark

The saved plan and next-session handoff are in [NYX_ROADMAP.md](NYX_ROADMAP.md).
`scripts/Start-NyxBenchmark.ps1` compares local models with 40 synthetic cases;
the short `Screen` suite uses 16 of them. It checks concise dialogue, follow-ups,
speaker attribution, trust boundaries, vanilla gifts, supported house previews,
and prompt leakage. Profile and mood cases supply synthetic future context only;
they do not implement or validate persistent memory or mood features.

With Ollama already running, start a baseline screen from the repository root:

```powershell
.\scripts\Start-NyxBenchmark.ps1 -Suite Screen -RunDir artifacts/nyx-benchmark/magnum-screen-idle -Models 'hf.co/bartowski/magnum-v4-12b-GGUF:Q5_K_M' -Sampling Stable -Context 4096 -Workload Idle -Background
```

The wrapper uses `.pebblehost/python/Scripts/python.exe` when present, otherwise
`python` (3.10+). It prints a background PID and the `runner.log` path. Omit
`-Background` to watch the run in the terminal and interrupt with Ctrl+C.

Repeat the exact command with the same `-RunDir` to resume. Each reply is saved
atomically. Completed replies, including replies that fail automatic checks,
are skipped; transport errors and missing cases are retried. Settings, fixture,
runner, runtime, or pending model-digest changes require a new run directory.
An interrupted run folder must be preserved to retain that progress.

Omit `-Models` to compare all four tags listed in the roadmap. Missing models
are skipped and recorded, with no implicit download. Use `-PullMissing` only
when you intend to download them. `Screen` defaults to two repetitions and
`Full` to three; `-Cases` and `-Repetitions` can narrow a run. Use a new directory
for each model/sampling/context/workload combination you change. `-Workload
Gaming` only labels a run: launch the representative game yourself and save its
settings, frame-time observations, and GPU headroom in `workload-notes.txt` in
the run folder. Run the same comparison with the PC idle.

Open `report.html` or `report.md` for aggregate results, `summary.json` for
structured metrics, and `blind-review.html` for replies without model labels.
Per-reply records are under `results/`; `manifest.json` retains model digests,
settings, warm-up timing, and run notes. To regenerate reports without inference:

```powershell
.\scripts\Start-NyxBenchmark.ps1 -RunDir artifacts/nyx-benchmark/magnum-screen-idle -ReportOnly
```

The runner returns exit code 2 while measurements are incomplete; this is
separate from completed replies failing a quality check. It never selects a
winner automatically. Automatic checks are a first pass; review the dialogue
and keep real plugin validation, permissions, confirmation, and gameplay tests
as separate requirements. Latency is warm local inference, excluding the relay,
tunnel, server queue, and chat delivery. Reported VRAM is Ollama model residency,
not all GPU allocations or a measurement of game smoothness.

Runs use temporary `nyx-benchmark-*` Ollama aliases to apply context and sampling
settings, then unload and remove their own aliases. They do not change the relay
model, `.env`, or production configuration. Inference still shares the GPU with
Nyx and any running game. Benchmark artifacts are Git-ignored.

Run the harness's offline tests without using Ollama or a GPU:

```powershell
.\.pebblehost\python\Scripts\python.exe -m unittest discover -s scripts/tests -p test_nyx_benchmark.py -v
```

`scripts/Test-NyxAi.ps1` remains the older live-model regression check; it is
separate from the resumable comparison and these offline runner tests.
