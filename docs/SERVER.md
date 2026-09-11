# Server profile

Keep this page current. It is the first compatibility reference for plugin work.
Do not include passwords, tokens, private connection URLs, or player personal data.

## Identity

- Server name: CrocBoys
- Public address/domain: _To be filled in_
- Environment: Production
- Hosting provider: PebbleHost
- PebbleHost plan/tier: _To be filled in_
- Server timezone: _To be filled in_

## Runtime compatibility

- Minecraft edition: Java with Geyser/Floodgate present (confirm Bedrock support status)
- Minecraft version: 26.2
- Server software: Purpur
- Server software build: 2632
- Java version: _To be filled in_
- Proxy: None / Velocity / BungeeCord / Other: _To be filled in_
- Build tool preference: Maven / Gradle: _To be filled in_
- Plugin language: Java / Kotlin / Both: _To be filled in_

## Gameplay and architecture

- Server style: _Survival, minigames, network, etc._
- Worlds and their purposes: _To be filled in_
- Database services: _None, SQLite, MySQL/MariaDB, etc._
- Cross-play setup: Geyser/Floodgate plugin folders are present; versions and status to be confirmed
- Permissions system: _LuckPerms or other_
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
