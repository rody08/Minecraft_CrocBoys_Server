"""Authenticated localhost relay for BigOscieGF -> Ollama Responses API."""

from __future__ import annotations

import argparse
import hmac
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
from pathlib import Path
import urllib.error
import urllib.request


REPOSITORY_ROOT = Path(__file__).resolve().parent.parent
MAX_REQUEST_BYTES = 1_048_576


def prepare_forward(payload: dict, task: str, chat_model: str, builder_model: str) -> tuple[dict, int]:
    """Only locally configured model names can be selected by authenticated callers."""
    if task not in ("chat", "blueprint"):
        raise ValueError("unknown_task")
    forwarded = dict(payload)
    if task == "blueprint":
        tokens = forwarded.get("max_output_tokens", 4096)
        if type(tokens) is not int or not 1 <= tokens <= 4096:
            raise ValueError("invalid_blueprint_budget")
        if not builder_model:
            raise ValueError("builder_model_not_configured")
        forwarded["model"] = builder_model
        forwarded["max_output_tokens"] = tokens
    else:
        forwarded["model"] = chat_model
    forwarded["stream"] = False
    return forwarded, 120 if task == "blueprint" else 45


def load_env(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip().strip('"').strip("'")
    return values


class RelayHandler(BaseHTTPRequestHandler):
    server_version = "CrocBoysOllamaRelay/1.0"

    def log_message(self, format_string: str, *args) -> None:
        # Never log authorization headers or request bodies.
        print(f"{self.address_string()} - {format_string % args}", flush=True)

    def send_json(self, status: int, payload: dict) -> None:
        encoded = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(encoded)))
        self.end_headers()
        self.wfile.write(encoded)

    def authorized(self) -> bool:
        supplied = self.headers.get("Authorization", "")
        expected = "Bearer " + self.server.relay_token
        return hmac.compare_digest(supplied, expected)

    def do_GET(self) -> None:
        if self.path != "/health" or not self.authorized():
            self.send_json(404, {"error": "not_found"})
            return
        self.send_json(200, {"status": "ok", "model": self.server.model, "builder_model": self.server.builder_model})

    def do_POST(self) -> None:
        if self.path != "/v1/responses":
            self.send_json(404, {"error": "not_found"})
            return
        if not self.authorized():
            self.send_json(401, {"error": "unauthorized"})
            return

        try:
            content_length = int(self.headers.get("Content-Length", "0"))
        except ValueError:
            self.send_json(400, {"error": "invalid_content_length"})
            return
        if content_length < 1 or content_length > MAX_REQUEST_BYTES:
            self.send_json(413, {"error": "request_too_large"})
            return

        try:
            payload = json.loads(self.rfile.read(content_length))
        except (UnicodeDecodeError, json.JSONDecodeError):
            self.send_json(400, {"error": "invalid_json"})
            return
        if not isinstance(payload, dict):
            self.send_json(400, {"error": "invalid_payload"})
            return

        # The relay owns model selection so a leaked token cannot load arbitrary
        # local models. Streaming is disabled because the plugin expects one body.
        try:
            payload, inference_timeout = prepare_forward(payload, self.headers.get("X-Nyx-Task", "chat"),
                                                         self.server.model, self.server.builder_model)
        except ValueError as error:
            self.send_json(400, {"error": str(error)})
            return
        forwarded = json.dumps(payload).encode("utf-8")
        request = urllib.request.Request(
            "http://127.0.0.1:11434/v1/responses",
            data=forwarded,
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        try:
            with urllib.request.urlopen(request, timeout=inference_timeout) as response:
                body = response.read()
                status = response.status
                content_type = response.headers.get("Content-Type", "application/json")
        except urllib.error.HTTPError as error:
            body = error.read()
            status = error.code
            content_type = error.headers.get("Content-Type", "application/json")
        except (urllib.error.URLError, TimeoutError) as error:
            self.send_json(502, {"error": "ollama_unavailable", "detail": str(getattr(error, "reason", error))})
            return

        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


def main() -> int:
    parser = argparse.ArgumentParser(description="Run the BigOscieGF Ollama relay")
    parser.add_argument("--env-file", type=Path, default=REPOSITORY_ROOT / ".env")
    args = parser.parse_args()
    settings = load_env(args.env_file.resolve())
    token = settings.get("BIGOSCIE_OLLAMA_RELAY_TOKEN", "")
    model = settings.get("NYX_OLLAMA_MODEL") or settings.get(
        "BIGOSCIE_OLLAMA_MODEL", "hf.co/bartowski/magnum-v4-12b-GGUF:Q5_K_M"
    )
    port = int(settings.get("BIGOSCIE_OLLAMA_RELAY_PORT", "11435"))
    if len(token) < 32:
        raise SystemExit("BIGOSCIE_OLLAMA_RELAY_TOKEN must contain at least 32 characters")

    server = ThreadingHTTPServer(("127.0.0.1", port), RelayHandler)
    server.relay_token = token
    server.model = model
    server.builder_model = settings.get("NYX_BUILDER_MODEL", "qwen3-coder:30b")
    print(f"Relay listening on http://127.0.0.1:{port} for model {model}", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
