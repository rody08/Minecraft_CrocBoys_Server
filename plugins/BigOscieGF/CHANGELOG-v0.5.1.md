# BigOscieGF 0.5.1

- Mark the recent conversation as reference data that the model must not copy or continue.
- Require output to contain only Nyx's spoken reply and permitted hidden action markers.
- Defensively remove a leading replay shaped like `player: echoed message Nyx: actual reply` before displaying chat.
- Preserve ordinary uses of the player's name in Nyx's real response.
- Add regression coverage for single-line, multiline, character-label-only, and embedded-`Nyx:` cases.
