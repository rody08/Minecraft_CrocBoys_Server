# BigOscieGF 0.5.2

- Send shared chat as real Responses API user/assistant turns instead of transcript-shaped prompt text.
- Let Magnum write normal replies, resolve follow-ups, and decide when to request permitted actions; retain only narrow safety cleanup and server-side validation.
- Use a shorter, more natural persona prompt and a 260-character final safety cap without a hard word-count rewrite.
- Add configurable Ollama temperature with a stable default of 0.35.
- Added pre-action prompt-leak rejection so leaked control text cannot trigger trust, item, or build actions.
- Accept the two-field build marker Magnum emits naturally while keeping legacy three-field markers compatible.
- Simplify item/build tool instructions and add examples verified against the local Magnum model.
- Strip bracketed model labels and marker-adjacent punctuation without rewriting ordinary model prose.
- Lowered the default build trust requirement from 100 to 50 while retaining the OP-default permission, bounded schematic, clear-volume check, preview, confirmation, and WorldEdit undo protections.
- Added a repeatable local-GPU evaluation covering greetings, build follow-ups, unsupported designs, food follow-ups, and ordinary conversation.
