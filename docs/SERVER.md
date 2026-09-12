# Server profile

Keep this page current. It is the first compatibility reference for plugin work.
Do not include passwords, tokens, private connection URLs, or player personal data.

## Identity

- Server name: CrocBoys
- Public address/domain: _To be filled in_
- Environment: Production
- Hosting provider: PebbleHost
- PebbleHost plan/tier: _To be filled in_
- Server timezone: UTC (confirmed from live log timestamps)

## Runtime compatibility

- Minecraft edition: Java with Geyser/Floodgate present (confirm Bedrock support status)
- Minecraft version: 26.2
- Server software: Purpur
- Server software build: 2622
- Java version: 25 (Eclipse Adoptium Temurin 25+36 LTS)
- Proxy: None / Velocity / BungeeCord / Other: _To be filled in_
- Build tool preference: Maven / Gradle: _To be filled in_
- Plugin language: Java / Kotlin / Both: _To be filled in_

## Gameplay and architecture

- Server style: _Survival, minigames, network, etc._
- Worlds and their purposes: _To be filled in_
- Database services: _None, SQLite, MySQL/MariaDB, etc._
- Cross-play setup: Geyser-Spigot 2.11.2-SNAPSHOT and floodgate 2.2.5-SNAPSHOT (build 140); both enabled successfully
- Permissions system: LuckPerms 5.5.71
- Economy provider: _Vault provider or none_
- Placeholder provider: _PlaceholderAPI or none_
- Other shared APIs: _ProtocolLib, WorldEdit, etc._

## Local test server

- Test server path: _To be filled in if one exists_
- Test server version/build: _To be filled in_
- Test data policy: Use synthetic data; do not copy private player data.

## PebbleHost file access

The host, port, and username are stored locally in the Git-ignored
`.pebblehost/connection.psd1`. Never add the password or private key to any file
in this repository.

- Local connection profile: Configured
- Remote server path shown by panel: `/home/container`
- Remote plugin path shown by panel: `/home/container/plugins`
- SFTP-exposed root: `/`
- SFTP-exposed plugin directory: `/plugins`
- SFTP capabilities verified: Read, upload, and delete
- Live-write policy: Use only for an explicitly requested deployment or server-file change

## Change history

| Date | Change | Notes |
| --- | --- | --- |
| 2026-09-11 | Workspace created | Initial profile; runtime facts still needed |
| 2026-09-11 | Hosting profile added | Purpur 26.2 build 2632 and local SFTP profile recorded |
| 2026-09-11 | SFTP verified | Read-only authentication and `/plugins` listing succeeded |
| 2026-09-11 | SFTP write verified | Temporary marker uploaded and removed; no test files remain |
| 2026-09-11 | Nyx 0.3.0 deployed | Magnum v4 12B enabled; validated player item gifts and persistent NPC home chunk added |
| 2026-09-11 | Nyx 0.3.1 deployed | Long replies now split at sentence or word boundaries with compact continuation lines |
| 2026-09-11 | Nyx 0.3.2 deployed | Removed the old 180-character cutoff and reduced display chunks to fit narrow chat layouts |
| 2026-09-11 | Nyx 0.4.0 deployed | Added persistent player trust plus trust-gated enchanted, unsafe, and allowlisted EliteMobs item gifts; owner 100/100 and live AI verified |
| 2026-09-11 | Nyx 0.4.1 deployed | Reworked trust into immediate 0/50/100 emotional states and added trust-100 custom-enchantment forging; owner and AI verified live |
| 2026-09-11 | Nyx 0.5.0 deployed | Cumulative release includes 0.4.2 compact complete chat and confirmation-gated generated house schematics; fresh backup `Sep-11-2026-22`, version, NPC status, and live AI verified |
| 2026-09-11 | Nyx 0.5.1 deployed | Removed leading transcript echoes from chat; fresh backup `Sep-11-2026-23`, uploaded JAR hash, plugin version, NPC status, and live AI verified at 7989 ms |
| 2026-09-11 | Startup profile verified | Live startup reports Purpur build 2622, Java 25, and UTC log timestamps |
| 2026-09-11 | Startup maintenance | Created full backup `Sep-11-2026-15`; updated AxGraves 1.31.0, FreeMinecraftModels 2.12.0, and ResourcePackManager 2.4.0; deployed BetterStructures world exclusions; verified a successful 28.592-second startup |
