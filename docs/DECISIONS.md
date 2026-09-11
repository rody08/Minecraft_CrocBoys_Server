# Technical decisions

Use short entries for decisions that future plugin work should not rediscover.

## Template

### YYYY-MM-DD — Decision title

- Context: What forced the decision?
- Decision: What did we choose?
- Consequences: What should future work know?

## 2026-09-11 — Repository organization

- Context: Server work previously had no shared project structure.
- Decision: Keep plugin source, reviewed server configuration, operational notes,
  and local deployment tooling in one repository. Exclude runtime data and secrets.
- Consequences: `docs/SERVER.md` and `docs/PLUGIN_CATALOG.md` are the primary
  compatibility and inventory references.

## 2026-09-11 — Live server access

- Context: PebbleHost SFTP credentials are available for server maintenance.
- Decision: Keep credentials in the ignored `.env`, pin the server host-key
  fingerprint, and permit the tooling to read, download, upload, and delete files.
- Consequences: Technical read/write access is configured, but live writes still
  require an explicit user request naming the intended deployment or change.

## 2026-09-11 — BigOscieGF local model

- Context: The hosted server needs low-cost conversational AI, while Ollama runs
  on a separate Windows PC with an RX 6900 XT.
- Decision: Use `qwen2.5-light` through Ollama's non-stateful Responses API and
  an authenticated localhost relay. Reach the relay through an HTTPS tunnel only
  after the owner approves that data path.
- Consequences: Warm local inference is under one second in the current test.
  AI availability depends on the PC, Ollama, relay, and tunnel remaining online.

## 2026-09-11 — BigOscieGF 0.2.2 deployment

- Context: Local and public authenticated relay tests passed, and the server was
  stopped for a safe plugin replacement.
- Decision: Deploy BigOscieGF 0.2.2 configured for the approved temporary
  Cloudflare Quick Tunnel and keep timestamped copies of the prior JAR and config.
- Consequences: Uploaded files were hash-verified and version 0.2.2 started cleanly.
  The authenticated public bridge passed a post-deployment response test, followed
  by a successful live `/bigosciegf aitest` response in 1017 ms.
  Restarting the bridge changes its temporary hostname and requires preparing and
  deploying an updated plugin config.
