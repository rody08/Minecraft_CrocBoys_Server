# BigOscieGF 0.4.0

## Trusted item abilities

- Adds persistent UUID-keyed trust scores in the live plugin data directory.
- Locks the configured owner at maximum trust.
- Rate-limits automatic trust growth to prevent chat spam farming.
- Adds trust-gated enchantments, custom item names, unsafe materials, and enchantment levels up to 255.
- Adds a strict EliteMobs adapter for allowlisted custom-item filenames using the installed `em loot give` command.
- Adds admin commands to inspect, set, and adjust player trust.
- Continues to reject model-authored targets, selectors, NBT/components, and arbitrary commands.

## Default tiers

| Trust | Ability |
| ---: | --- |
| 0 | Ordinary vanilla items |
| 15 | Vanilla-compatible enchantments and custom names |
| 50 | Configured unsafe items and overlevel enchantments up to 255 |
| 75 | Allowlisted EliteMobs custom items |
| 100 | Maximum; the configured owner is always here |
