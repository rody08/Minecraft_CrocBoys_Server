# BigOscieGF 0.4.2

- Keep Nyx's normal replies to roughly 12-28 words and at most two short sentences.
- Raise generation headroom so the model can finish its thought instead of stopping at the token limit.
- Reduce the visible safety cap to 240 characters and trim oversized replies at a completed sentence or word boundary.
- Send each answer as one chat message by default, letting Minecraft wrap it naturally under a single Nyx nameplate.
- Migrate unchanged 0.4.1 conversation-layout defaults automatically while preserving customized values.
