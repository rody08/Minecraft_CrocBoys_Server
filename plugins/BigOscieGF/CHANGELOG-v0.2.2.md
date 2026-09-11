# BigOscieGF 0.2.2

## Added

- Explicit `ai.provider` selection for `openai` or `ollama`.
- Ollama-compatible minimal `/v1/responses` payloads.
- Generic `BIGOSCIE_AI_API_KEY` environment-variable fallback.
- Reproducible Gradle 9.7.1 wrapper compatible with Java 25.

## Compatibility

- Existing OpenAI settings continue to work with `ai.provider: "openai"`.
- Ollama deployments use `ai.provider: "ollama"`, the secured bridge endpoint,
  `qwen2.5-light`, and the local relay token.
- No live-server configuration or plugin data migration is performed by this
  source update.
