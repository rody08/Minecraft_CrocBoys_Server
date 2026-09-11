BigOscieGF v0.2.2
==================
Built for RXSpicy's Purpur 26.2 server. Requires Citizens 2.0.43+.

WHAT CHANGED IN v0.2.2
----------------------
LOCAL OLLAMA
- Added explicit `openai` and `ollama` providers.
- Ollama uses the compatible non-stateful `/v1/responses` request shape.
- Supports a generic `BIGOSCIE_AI_API_KEY` environment variable for a protected relay.
- Use `scripts/Setup-OllamaBridge.ps1` and `scripts/Start-OllamaBridge.ps1` from the server repository.
- The recommended local model is `qwen2.5-light`; it was measured at about 0.7-0.9 seconds warm on the RX 6900 XT.
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
