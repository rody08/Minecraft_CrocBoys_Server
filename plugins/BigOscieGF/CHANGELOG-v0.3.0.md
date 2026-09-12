# BigOscieGF 0.3.0

- Switch Nyx's local model to Magnum v4 12B Q5_K_M.
- Add AI-directed, plugin-validated item gifts for the requesting player.
- Limit each request to one material and at most 64 items by default.
- Add a per-player gift cooldown and configurable dangerous-material blocklist.
- Never execute commands, selectors, NBT, components, or model-selected targets.
- Add parser tests covering valid gifts, command injection, and malformed markers.
- Respawn a saved Nyx at her home after a server restart and keep that one home chunk loaded while the owner is offline.
