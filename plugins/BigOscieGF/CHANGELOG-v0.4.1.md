# BigOscieGF 0.4.1

## Fast emotional trust

- Replaces timed trust grinding with immediate 0/50/100 emotional states.
- New players start at 50; the configured owner remains locked at 100.
- Insults set 0, sincere apologies set 50, and genuine compliments set 100.
- A hidden, strictly parsed trust marker lets Nyx apply the state during the same response while rejecting every value except 0, 50, and 100.

## Item tiers

| Trust | Ability |
| ---: | --- |
| 0 | Food only |
| 50 | All vanilla materials, unsafe/operator items, and vanilla enchantments up to 255 |
| 100 | Custom-named forged gear, supported EliteMobs custom enchantments, and allowlisted installed custom items |

Arbitrary commands, player targets, selectors, and raw NBT/components remain unavailable to the model.
