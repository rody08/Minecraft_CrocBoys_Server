# BigOscieGF 0.5.0

- Give Nyx a constrained schematic-building tool for small oak, spruce, and dark oak houses.
- Generate and save a Sponge `.schem` before any world edit occurs.
- Preview the planned 9x7x11 bounds with particles and require confirmation from the same player within two minutes.
- Require trust 100 and the `nyx.build` permission, which defaults to OP.
- Re-check that the build volume is clear immediately before pasting.
- Paste through the installed WorldEdit API and retain the edit in the confirming player's WorldEdit undo history.
- Reject model-authored commands, coordinates, URLs, filenames, dimensions, arbitrary block palettes, and unsupported building types.
- Add `/nyx build confirm|cancel|status` and conversational “Nyx, confirm build” / “Nyx, cancel build” controls.
