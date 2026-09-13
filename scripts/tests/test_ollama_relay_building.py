"""Synthetic tests only: do not load .env, start a relay or contact Ollama."""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from ollama_relay import prepare_forward


class BuildRoutingTest(unittest.TestCase):
    def test_chat_cannot_load_a_caller_selected_model(self):
        source = {"model": "attacker-model", "stream": True}
        forwarded, timeout = prepare_forward(source, "chat", "nyx-dialogue", "builder")
        self.assertEqual(forwarded["model"], "nyx-dialogue")
        self.assertFalse(forwarded["stream"])
        self.assertEqual(timeout, 45)
        self.assertTrue(source["stream"])

    def test_blueprint_routes_only_to_configured_builder(self):
        forwarded, timeout = prepare_forward({"model": "anything", "max_output_tokens": 4096},
                                             "blueprint", "nyx-dialogue", "builder")
        self.assertEqual(forwarded["model"], "builder")
        self.assertEqual(timeout, 120)

    def test_rejects_unbounded_or_invalid_design_budgets(self):
        for tokens in (-1, 0, 4097, True, "4096", None):
            with self.subTest(tokens=tokens), self.assertRaises(ValueError):
                prepare_forward({"max_output_tokens": tokens}, "blueprint", "chat", "builder")

    def test_unknown_tasks_and_disabled_builders_fail_closed(self):
        with self.assertRaises(ValueError):
            prepare_forward({}, "shell", "chat", "builder")
        with self.assertRaises(ValueError):
            prepare_forward({}, "blueprint", "chat", "")


if __name__ == "__main__":
    unittest.main()
