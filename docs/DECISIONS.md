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

## 2026-09-11 — Separate Nyx from coding models

- Context: `qwen2.5-light` is a custom Qwen2.5-Coder 14B model used for light
  coding, while Nyx needs natural dialogue and consistent persona behavior.
- Decision: Rename the bridge selector to `NYX_OLLAMA_MODEL` and recommend
  `gemma3:12b` as Nyx's next model. Keep a temporary legacy-name fallback.
- Consequences: This recommendation was later superseded by the Magnum v4 12B
  deployment recorded below. Changing the model still requires restarting the
  bridge and updating BigOscieGF's live model setting.

## 2026-09-11 — Nyx conversational model and item-gift boundary

- Context: Nyx needs stronger character dialogue and the owner wants players to
  be able to ask her for Minecraft items with administrator-level capability.
- Decision: Use Magnum v4 12B Q5_K_M for dialogue. Let the model emit one hidden,
  structured item request, then validate and fulfill it with the Bukkit inventory
  API for the requesting player only. Do not execute model-authored commands.
- Consequences: Normal items are available to all nearby players who address Nyx,
  with a default maximum of 64 and a five-second cooldown. Operator-only materials
  remain blocked by default, all decisions are logged, and configuration can tune
  the limits without granting arbitrary console access.

## 2026-09-11 — BigOscieGF 0.3.0 deployment

- Context: Magnum and the item-action format passed local checks, and the live
  server was available for a controlled restart.
- Decision: Deploy BigOscieGF 0.3.0 and the Magnum bridge configuration with
  server-side backups of the prior JAR and config. Keep Nyx's single home chunk
  loaded while her owner is offline so Citizens does not immediately despawn her.
- Consequences: Local tests and artifact hash verification passed. The live server
  reports Nyx spawned, AI enabled, Magnum v4 12B selected, and a successful
  end-to-end AI check at 7976 ms. Rollback copies remain beside the live files.

## 2026-09-11 — Nyx trust and custom-item authority

- Context: Players want Nyx to create enchanted and installed custom gear, including unsafe items, while earning that privilege through conversation.
- Decision: Persist a UUID-keyed 0-100 trust score. The configured owner is always 100. Ordinary items require 0, normal enchanted/custom-named gear 15, unsafe materials and enchantments up to 255 require 50, and allowlisted EliteMobs items require 75. Automatic growth is one point per ten minutes of qualifying conversation, with OP commands for manual adjustment.
- Consequences: Nyx receives the speaker's current tier in her prompt, but the plugin independently enforces every threshold. Vanilla gear is built through Paper APIs. EliteMobs integration accepts only installed filenames from a configuration allowlist and supplies the requesting player's server-known name to the fixed `em loot give` command. Arbitrary model-authored commands, targets, selectors, and NBT remain impossible.

## 2026-09-11 — BigOscieGF 0.4.0 deployment

- Context: The trusted enchanted/custom-item implementation passed its clean build and automated tests.
- Decision: Deploy 0.4.0 with a clean stop/start and retain the former JAR at `/plugins/BigOscieGF-0.3.2.jar.pre-v0.4.0-20260911.bak`.
- Consequences: The server reports BigOscieGF 0.4.0, Nyx spawned in FOLLOW mode, Magnum configured, `.BigOscie49` at locked trust 100/100, and a successful live AI check in 8027 ms.

## 2026-09-11 — Fast emotional trust states

- Context: Slow trust accumulation would waste players' time; Nyx's trust should react immediately to how players treat her.
- Decision: Replace gradual scoring with exactly three persistent states. New players start at 50. Insults set 0, apologies set 50, and compliments set 100; the configured owner is always 100. Apply a state change before validating any gift requested in the same message.
- Consequences: Trust 0 is food-only, trust 50 permits all vanilla and unsafe/overlevel-enchanted items, and trust 100 additionally permits bespoke names, supported EliteMobs custom enchantments, and allowlisted installed items. The model chooses emotional transitions through a strictly parsed 0/50/100 marker while the plugin independently enforces item permissions.

## 2026-09-11 — BigOscieGF 0.4.1 deployment

- Context: The fast trust-state implementation passed its clean build and automated parser/policy tests.
- Decision: Deploy 0.4.1 with a clean stop/start and retain 0.4.0 at `/plugins/BigOscieGF-0.4.0.jar.pre-v0.4.1-20260911.bak`.
- Consequences: The live server reports BigOscieGF 0.4.1, Nyx spawned in FOLLOW mode, `.BigOscie49` at 100/100 in the everything tier, and Magnum responding successfully in 8046 ms.

## 2026-09-11 — Startup maintenance and legacy content boundary

- Context: The Purpur 26.2 startup log showed outdated integration plugins,
  unmanaged BetterStructures worlds, missing FreeMinecraftModels assets, and
  retired EliteMobs content from an older configuration format.
- Decision: Take full and targeted backups, update AxGraves to 1.31.0,
  FreeMinecraftModels to 2.12.0, and ResourcePackManager to 2.4.0, and deploy
  explicit BetterStructures exclusions for plugin-managed worlds. Preserve the
  retired EliteMobs dungeon and item files rather than deleting gameplay content
  merely to silence warnings.
- Consequences: The server starts successfully, the missing model and managed-world
  warnings are resolved, and ResourcePackManager completed its Geyser extension
  replacement. EliteMobs 10.9.0 still rejects 17 legacy enchantment definitions
  and disables affected items until their authors publish current YAML/Lua
  replacements. EssentialsX 2.22.0 also reports Purpur 26.2 as unsupported; its
  current stable release officially supports 26.1.2.
## 2026-09-11 — Separate Nyx generation headroom from visible reply length

- Context: Using a small output-token limit to keep Nyx concise caused Magnum to stop mid-sentence, while the later 480-character display allowance and 48-character server chunks filled the chat window with continuation arrows.
- Decision: In BigOscieGF 0.4.2, allow 160 generation tokens but instruct Nyx to answer in 12-28 visible words and cap display at 240 characters. Trim an oversized answer at a completed sentence when possible. Send the answer as one message by default so each Minecraft client wraps it for its own chat width under a single nameplate.
- Consequences: Brevity is controlled independently from model completion, the layout adapts to client chat settings, and administrators can restore explicit server-side chunks by setting `visuals.chat-line-characters` above zero. Existing customized 0.4.1 values are preserved; only unchanged defaults migrate automatically.

## 2026-09-11 — Nyx schematic building begins with generated, confirmed templates

- Context: Nyx should be able to build useful structures, but model-authored WorldEdit commands and arbitrary internet schematic downloads could overwrite terrain, bypass intended limits, or introduce unreviewed content.
- Decision: BigOscieGF 0.5.0 accepts only a strict house/style/size marker, creates a bounded Sponge schematic locally, previews its bounds, and requires same-player confirmation before pasting through WorldEdit. Building requires trust 100 and an OP-default permission. The plugin independently chooses the location, palette, dimensions, file path, and paste operation.
- Consequences: Nyx can safely draft and build three small house styles without arbitrary command execution. Each paste is retained in WorldEdit undo history. Online imports remain a future feature requiring an allowlisted source, download limits, format validation, palette inspection, dimension limits, and the same preview/confirmation boundary.

## 2026-09-11 — BigOscieGF 0.5.0 cumulative deployment

- Context: The owner requested deployment of both 0.4.2 and 0.5.0, but Bukkit must load only one JAR for a plugin name and 0.5.0 already contains every 0.4.2 chat change.
- Decision: Create full manual backup `Sep-11-2026-22`, stop the server cleanly, and deploy only cumulative `BigOscieGF-0.5.0.jar`. Retain the former active JAR at `/plugins/BigOscieGF-0.4.1.jar.pre-v0.5.0-20260911.bak` and a hash-verified local rollback copy.
- Consequences: The live server reports BigOscieGF 0.5.0, Nyx spawned in FOLLOW mode, Magnum v4 12B configured, and a successful `aitest` response in 8661 ms. The uploaded JAR SHA-256 is `7C47EE28ADC72231E553AE531E2FDD23F3720ADA9F3387E50285D6E18D133C96`.

## 2026-09-11 — Treat Nyx's conversation transcript as input, never output

- Context: Magnum occasionally continued the supplied `speaker: message` transcript, causing a Nyx chat line to begin by repeating the player's name and newest message before its actual answer.
- Decision: In 0.5.1, delimit recent conversation as reference-only data, explicitly prohibit transcript labels and input repetition, and defensively remove a leading `player: echoed message Nyx: reply` structure before display.
- Consequences: Shared conversational memory remains available, while model-format leakage is suppressed. The cleaner is deliberately narrow so a genuine reply that naturally addresses a player by name remains untouched.

## 2026-09-11 — BigOscieGF 0.5.1 transcript-fix deployment

- Context: The transcript-echo cleaner and prompt boundary passed the clean local test and build suite, and the owner requested production deployment.
- Decision: Create full manual backup `Sep-11-2026-23`, stop the server cleanly, and atomically replace 0.5.0 with `BigOscieGF-0.5.1.jar`. Retain 0.5.0 at `/plugins/BigOscieGF-0.5.0.jar.pre-v0.5.1-20260911.bak`.
- Consequences: The downloaded live artifact matches local SHA-256 `BFAE96D053A04637EC9A1BD4359A0353F0408F6C65D6D80BB610424DD3D1FAD4`. The server reports BigOscieGF 0.5.1, Nyx spawned in FOLLOW mode with AI enabled, and a successful live AI check in 7989 ms.

## 2026-09-12 — Use model-led conversation turns with validated actions

- Context: Magnum was receiving conversation history as transcript-shaped prose, so it sometimes imitated labels or prompt text. Moving build decisions entirely into keyword code made chat less natural and still did not improve the model itself.
- Decision: In 0.5.2, send history as real Responses API user/assistant turns and let Magnum write replies, interpret follow-ups, and choose permitted structured actions. Keep only narrow prompt-leak/label cleanup and independent validation of every action. Accept Magnum's natural two-field build marker, use a concise tool prompt with examples, and default Ollama temperature to 0.35.
- Consequences: The model controls conversation and decisions without being encouraged to continue a transcript. The plugin still constrains items, trust, build palettes, permissions, dimensions, clear space, confirmation, and WorldEdit undo. A repeatable local-GPU evaluation must pass before deployment.
