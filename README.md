# CrocBoys Minecraft Server

This repository is the working home for the CrocBoys server, its custom plugins,
and the configuration and documentation needed to maintain it.

## Repository map

| Path | Purpose |
| --- | --- |
| `plugins/` | One source-code folder per custom plugin |
| `server-config/` | Reviewed, non-secret configuration copied from the live server |
| `artifacts/` | Local plugin builds ready to test or deploy (ignored by Git) |
| `docs/SERVER.md` | Server version, software, hosting, and gameplay facts |
| `docs/PLUGIN_CATALOG.md` | Inventory and status of custom and third-party plugins |
| `docs/OPERATIONS.md` | Safe build, test, backup, and deployment workflow |
| `docs/OLLAMA.md` | Local Ollama bridge for BigOscieGF |
| `docs/DECISIONS.md` | Short record of important technical decisions |
| `scripts/Deploy-Plugin.ps1` | Upload one built plugin JAR over SFTP/SCP |
| `AGENTS.md` | Instructions that help Codex work safely and gather context |

## Start here

1. Fill in the blanks in `docs/SERVER.md`.
2. Put each custom plugin in its own folder under `plugins/`.
3. Add every installed plugin to `docs/PLUGIN_CATALOG.md`.
4. Copy only useful, reviewed configuration into `server-config/`. Do not copy
   passwords, tokens, player data, worlds, logs, or backups into Git.
5. Commit meaningful checkpoints before deploying changes.

## Custom plugin layout

Each plugin should be a complete Gradle or Maven project:

```text
plugins/
  ExamplePlugin/
    README.md
    pom.xml                 # or build.gradle(.kts)
    src/
      main/
        java/
        resources/
          plugin.yml
```

The plugin README should explain what it does, its commands and permissions,
dependencies, configuration files, and how to build and test it.

## PebbleHost connection

PebbleHost exposes server files through its panel and SFTP. This repository can
use a local `.env` file for the SFTP settings. Copy `.env.example` to `.env`, add
the password only to `.env`, and never commit that local file.

Set up the local SFTP helper once:

```powershell
.\scripts\Setup-SftpTools.ps1
```

List the live plugin directory without changing it:

```powershell
.\.pebblehost\python\Scripts\python.exe .\scripts\pebblehost_sftp.py list
```

The original interactive upload helper is also available:

```powershell
.\scripts\Deploy-Plugin.ps1 `
  -JarPath .\plugins\ExamplePlugin\target\ExamplePlugin.jar `
  -HostName YOUR_SFTP_HOST `
  -Port YOUR_SFTP_PORT `
  -UserName YOUR_SFTP_USERNAME `
  -DryRun
```

Remove `-DryRun` when the displayed source and destination are correct. The
OpenSSH client will ask for the password interactively. Stop the Minecraft
server or use its documented plugin reload procedure before replacing a live
JAR, and keep a PebbleHost backup available.

Keep the PebbleHost password only in the ignored `.env` file. Never put a panel
token, password, or private SSH key in a tracked file or Git commit.
