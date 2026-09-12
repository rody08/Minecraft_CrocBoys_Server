BigOscieGF v0.5.2
==================
Built for RXSpicy's Purpur 26.2 server. Requires Citizens 2.0.43+ and WorldEdit 7.4.5+.

WHAT CHANGED IN v0.5.2
----------------------
MODEL-LED CHAT WITH REAL CONVERSATION TURNS
- Recent chat is sent to the Responses API as actual user/assistant turns instead of a transcript-shaped text block, so Magnum no longer imitates transcript labels and wrappers.
- Magnum writes the reply, resolves follow-ups, decides whether to ask a question, and chooses permitted item/build actions. The plugin only strips labels or clear prompt leakage and enforces server-side safety.
- The prompt is shorter and action-focused. Normal replies are not rewritten or hard-limited by word count; a 260-character final display cap remains as a safety net.
- Ollama temperature defaults to 0.35 for stable action formatting without making ordinary conversation robotic.
- Both two-field and legacy three-field build markers are accepted because Magnum naturally prefers `[[BUILD_SCHEMATIC: house | oak]]`.
- Cooked-food follow-ups now have an explicit valid-item example, and enchantments are requested only when the player actually asks for them.
- Building now requires trust 50 plus `nyx.build`, which still defaults to OP. The schematic preview and explicit confirmation step remain mandatory.
- Clear prompt leaks are rejected before trust, gift, or build markers are parsed, preventing leaked instructions from triggering actions.
- `scripts/Test-NyxAi.ps1` runs repeatable live tests against the local GPU model before deployment.
- Existing 0.5.1 defaults migrate automatically; customized non-default limits and personality text are preserved.

WHAT CHANGED IN v0.5.1
----------------------
NO TRANSCRIPT ECHOES
- Recent group chat is now explicitly marked as reference context, not dialogue for Magnum to continue.
- Nyx is instructed to return only her spoken reply and hidden action markers, without repeating the player's name/message or adding speaker labels.
- A defensive output cleaner strips transcript replays such as `RXSpicy: echoed message Nyx: actual reply` before players see them.
- Ordinary replies that naturally address a player by name are left unchanged.

WHAT CHANGED IN v0.5.0
----------------------
NYX BUILDS FROM SCHEMATICS
- Ask Nyx to build a small oak, spruce, or dark oak house. She creates a real Sponge schematic before changing the world.
- Nyx previews the 9x7x11 build bounds with particles and waits up to two minutes for the same player to say `Nyx, confirm build`.
- Players can instead use `/nyx build confirm`, `/nyx build cancel`, or `/nyx build status`.
- The paste requires trust 100 and `nyx.build`, which defaults to OP. The target volume must be clear both when drafted and when confirmed.
- Every paste is recorded in the confirming player's WorldEdit undo history.
- The AI can choose only the supported structure, size, and palette. It cannot author commands, coordinates, URLs, filenames, or arbitrary schematic contents.
- Generated files are stored under `plugins/BigOscieGF/schematics/generated/`.
- Online schematic downloads are intentionally not enabled in this release; imported designs need an allowlisted source and separate validation first.

WHAT CHANGED IN v0.4.2
----------------------
COMPACT, COMPLETE CHAT
- Nyx now aims for a direct 12-28 word answer in no more than two short sentences.
- Extra generation room prevents the model from being cut off mid-thought; a separate 240-character display cap keeps chat concise.
- Oversized replies end at a completed sentence when possible instead of being cut at an arbitrary character.
- Each answer uses one Nyx nameplate and Minecraft's natural client wrapping by default, removing the stack of continuation arrows.
- Set `visuals.chat-line-characters` above 0 only to restore explicit server-side continuation lines.

WHAT CHANGED IN v0.4.1
----------------------
FAST THREE-STATE TRUST
- Trust is now exactly 0, 50, or 100 instead of a slow progression. New players start at 50.
- Direct insults or abuse toward Nyx set trust to 0; sincere apologies restore 50; genuine compliments set 100.
- A state change applies immediately, including to an item requested in that same message.
- Trust 0 permits food only.
- Trust 50 permits any vanilla material, unsafe/operator items, and vanilla enchantments up to level 255.
- Trust 100 permits everything above plus custom-named weapon crafting, supported EliteMobs custom enchantments, and allowlisted installed EliteMobs items.
- EliteMobs enchantments use a constrained entry such as `elitemobs:critical_strikes=3`; the plugin applies it through the installed EliteMobs item-enchantment system.
- The configured owner remains permanently locked at 100.

WHAT CHANGED IN v0.4.0
----------------------
This original gradual-trust design is retained as release history and is superseded by v0.4.1 above.

TRUSTED CUSTOM GEAR
- Nyx keeps a persistent 0-100 trust score per player UUID in `plugins/BigOscieGF/trust.yml`.
- `.BigOscie49` is permanently treated as trust 100.
- Trust rises by one after a qualifying conversation, at most once every ten minutes.
- Trust 0 grants ordinary items; 15 unlocks normal enchantments and custom names; 50 unlocks configured unsafe materials and enchantments up to level 255; 75 unlocks allowlisted EliteMobs gear.
- Nyx can now create real enchanted vanilla items with a constrained marker such as `[[GIVE_ITEM: minecraft:netherite_sword | 1 | minecraft:sharpness=255,minecraft:unbreaking=3 | Nightfang]]`.
- EliteMobs gifts use its installed `/em loot give <player> <filename>` interface, but the model may choose only exact filenames in `ai.item-gifts.elitemobs.allowlist`.
- The requesting player remains the only possible target. Nyx never executes arbitrary model-authored console text.
- In current v0.4.1, admins inspect trust with `/bigosciegf trust [player]` and set one of the three states with `/bigosciegf trust set <player> <0|50|100>`.

WHAT CHANGED IN v0.3.2
----------------------
NO MORE PREMATURE REPLY CUTOFF
- Raised Nyx's visible reply allowance from the old 180-220 character range to 480 characters.
- Reduced each display chunk from 72 to 48 characters for narrower Minecraft chat layouts.
- Existing v0.3.1 servers migrate these values automatically without changing other settings.

WHAT CHANGED IN v0.3.1
----------------------
READABLE CHAT LINES
- Nyx splits long replies into multiple Minecraft chat messages.
- Sentence endings are preferred; unusually long sentences wrap at word boundaries.
- The first line keeps Nyx's nameplate and later lines use a subtle continuation arrow.
- `visuals.chat-line-characters` and `visuals.chat-continuation-prefix` control the layout.

WHAT CHANGED IN v0.3.0
----------------------
NYX ITEM GIFTS
- Players can ask Nyx for ordinary Minecraft items in nearby chat.
- The AI may emit one hidden structured item action for the player who asked.
- The plugin validates the material and quantity and grants it through the Bukkit inventory API.
- Model-authored console commands, player selectors, NBT/components, and alternate targets are never executed.
- Gifts default to 64 items maximum with a five-second per-player cooldown.
- Dangerous operator-only materials are blocked unless explicitly enabled in config.
- Every successful or rejected gift is recorded in the server log.
- A saved Nyx now respawns at her home after a server restart; one home chunk stays loaded while the owner is offline.

CHAT MODEL
- Nyx now uses the Magnum v4 12B Q5_K_M conversational model through Ollama.
- Her exact persona remains in the plugin configuration so it stays reviewable and portable.

WHAT CHANGED IN v0.2.2
----------------------
LOCAL OLLAMA
- Added explicit `openai` and `ollama` providers.
- Ollama uses the compatible non-stateful `/v1/responses` request shape.
- Supports a generic `BIGOSCIE_AI_API_KEY` environment variable for a protected relay.
- Use `scripts/Setup-OllamaBridge.ps1` and `scripts/Start-OllamaBridge.ps1` from the server repository.
- The current local model is `hf.co/bartowski/magnum-v4-12b-GGUF:Q5_K_M`, selected for character dialogue.
- The bridge model is selected with `NYX_OLLAMA_MODEL`; the old `BIGOSCIE_OLLAMA_MODEL` name is accepted only as a migration fallback.
- The existing OpenAI provider and `OPENAI_API_KEY` fallback remain supported.

WHAT CHANGED IN v0.2.1
----------------------
CHAT / LUNA
- Still uses OpenAI Responses API with gpt-5.6-luna by default.
- One shared ordered conversation window for the whole nearby group, instead of separate per-player chat histories.
- AI requests are serialized so rapid messages cannot make Nyx answer out of order.
- Luna does the conversational interpretation. The plugin supplies recent chat + a small long-term fact list only.
- Removed the Gravity Hammer from Nyx's baseline personality. Hammer reactions are OFF by default.
- Event reactions may return [[SILENT]] so Luna can decide an event is not worth interrupting the chat for.
- More natural group-context prompt for names, pronouns, jokes and follow-ups.
- Integrated Responses API reliability fix: robust output_text parsing, one small retry for transient failures, and a minimal-request retry for optional-field HTTP 400s.
- /bigosciegf aitest tests the configured model and reports latency/errors in game.

MEMORY
- Recent context is shared and intentionally short; Luna decides what matters inside that context.
- Persistent owner facts remain in plugins/BigOscieGF/owner-memory.txt.
- When Luna is managing memory, it may mark an explicitly stated stable BigOscie fact for storage in the SAME API response. The marker is stripped before players see the reply, so no second API call is needed.
- Nyx will not auto-save jokes, temporary events, guesses, or facts claimed by other players.
- /bigosciegf memory list
- /bigosciegf memory clear recent
- /bigosciegf memory clear persistent
- /bigosciegf memory clear all
- Legacy /remember, /memories and /forget commands still work.

VISUAL UPDATE
- Canonical character name defaults to Nyx.
- The Citizens NPC can be renamed to Nyx without losing its saved Citizens ID.
- Chat prefix defaults to a Nyx nameplate.
- Nyx swings her arm when speaking.
- Portal particles appear while she talks; replies to BigOscie can add a heart particle.
- Existing username, MineSkin and direct signed-texture skin modes are retained.

SKIN COMMANDS
-------------
/bigosciegf skin username <MinecraftUsername>
/bigosciegf skin <MinecraftUsername>          (legacy shorthand)
/bigosciegf skin mineskin <MineSkinUUID-or-URL>
/bigosciegf skin texture <texture-value> <signature>
/bigosciegf skin status
/bigosciegf skin refresh
/bigosciegf skin clear

OTHER ADMIN COMMANDS
--------------------
/bigosciegf spawn
/bigosciegf follow
/bigosciegf stay
/bigosciegf come
/bigosciegf name <name>
/bigosciegf trust [player]
/bigosciegf trust set <player> <0|50|100>
/bigosciegf build confirm|cancel|status
/bigosciegf memory ...
/bigosciegf remember <fact>
/bigosciegf memories
/bigosciegf forget <number|all>
/bigosciegf ai on|off
/bigosciegf aitest
/bigosciegf say <text>
/bigosciegf status
/bigosciegf reload

Permission: bigosciegf.admin (default OP)

UPDATE FROM v0.2.0
------------------
1) Stop the server.
2) Replace the old BigOscieGF JAR with BigOscieGF-0.2.1.jar.
3) Keep plugins/BigOscieGF/config.yml. On first start, v0.2.1 migrates the old config and preserves your API key, skin, home, owner and other settings.
4) Start the server normally. Do not use Bukkit /reload.
5) Run /bigosciegf status and /bigosciegf aitest.
6) If you want a clean conversational slate, run /bigosciegf memory clear all.

NOTES
-----
The plugin's bundled config never contains your API key. Prefer the OPENAI_API_KEY environment variable when possible.
