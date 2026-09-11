# CrocBoys workspace instructions

## Purpose

This repository contains custom Minecraft plugin source, reviewed server
configuration, and operational notes for the CrocBoys server.

## Context to read first

Before changing plugin code or server configuration, read:

1. `docs/SERVER.md`
2. `docs/PLUGIN_CATALOG.md`
3. The affected plugin's `README.md`, build file, and `plugin.yml`
4. `docs/OPERATIONS.md` for deployment-related work

If required facts are blank, inspect the project for evidence and state any
assumption that affects compatibility. Ask the owner when the Minecraft version,
server software, Java version, or dependency version would materially change the
implementation.

## Working rules

- Keep each custom plugin as a self-contained project under `plugins/`.
- Match the Minecraft API, Java, server software, and dependency versions listed
  in `docs/SERVER.md`.
- Prefer Paper APIs when the server profile says Paper; avoid NMS/version-specific
  internals unless the plugin documents why they are required.
- Update the affected plugin README when commands, permissions, configuration,
  dependencies, setup, or player-visible behavior changes.
- Update `docs/PLUGIN_CATALOG.md` when a plugin is added, removed, renamed, or its
  deployment status changes.
- Record consequential architecture or compatibility choices in
  `docs/DECISIONS.md`.
- Build and test the affected plugin before calling a change complete.
- Treat the live PebbleHost server as production. Do not upload, delete, restart,
  stop, or change live files unless the user explicitly asks for that action.
- Never commit credentials, tokens, private keys, player data, world files, logs,
  crash reports, backups, database contents, or unreviewed live-server dumps.
- Preserve unrelated user changes and do not overwrite live configuration with
  placeholders.

## Deployment boundary

Prepare and verify artifacts locally by default. A deployment request covers only
the named artifact or configuration files. Before a live upload, identify the
exact local source and remote destination and confirm a backup exists when the
change could affect gameplay or stored data.
