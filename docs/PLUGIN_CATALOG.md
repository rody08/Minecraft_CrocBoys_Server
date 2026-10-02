# Plugin catalog

This inventory was last checked against the live PebbleHost `/plugins` directory on
2026-10-01. "Candidate" means the name suggests a custom CrocBoys plugin, but its
ownership still needs confirmation.

| Plugin | Ownership | Live version | Local source | Config tracked? | Live status |
| --- | --- | --- | --- | --- | --- |
| AuraSkills | Third-party | 2.3.12 | — | No | Installed |
| AxGraves | Third-party | 1.31.0 | — | No | Running on 26.2; 1.31.0 rejects 26.3 protocol 777, while 1.32.1 passed the local 26.3 startup test |
| BetterStructures | Third-party | 2.7.1 | — | Yes | Running; managed-world exclusions deployed and verified |
| BigOscieGF | Custom | 0.6.7 | `plugins/BigOscieGF/` | No | Running on 26.2; 26.3 requires compatible Citizens and WorldEdit |
| BlueMap | Third-party | 5.23 | — | No | Installed |
| CannonRTP | Candidate custom | 1.2.1 | Not imported | No | Running |
| Chunky | Third-party | 1.5.3 | — | No | Installed |
| Citizens | Third-party | 2.0.43-b4246 | — | No | Running on 26.2; disables itself on 26.3, and the 2.0.44 development line adds 26.3 support |
| EliteMobs | Third-party | 10.9.0 | — | No | Running; current plugin, but retired pre-10.9 enchantment/content files disable some legacy items |
| EliteTower | Candidate custom | 0.4.0 | Not imported | No | Installed |
| EssentialsX | Third-party | 2.22.0 | — | No | Installed |
| EssentialsXSpawn | Third-party | 2.22.0 | — | No | Installed |
| floodgate | Third-party | 2.2.5-SNAPSHOT b140 | — | No | Running on 26.2; build 141 passed the local 26.3 startup test |
| FreeMinecraftModels | Third-party | 2.12.0 | — | No | Running; updated and missing 10.9 staff/wand assets resolved |
| Geyser-Spigot | Third-party | 2.11.2-SNAPSHOT | — | No | Running on 26.2; latest 2.11.3 fails on 26.3 and officially lists Java 26.2 as supported |
| GSit | Third-party | 3.5.1 | — | No | Installed |
| Item NBT API | Third-party | 2.16.0 | — | No | Running on 26.2; warns that 26.3 is unsupported |
| LuckPerms | Third-party | 5.5.71 | — | No | Installed |
| MineGames | Candidate custom | 1.0.4 | Not imported | No | Installed |
| Prism | Third-party | 4.4 | — | No | Installed |
| ProtectionStones | Third-party | 2.10.6 | — | No | Installed |
| ResourcePackManager | Third-party | 2.4.0 | — | No | Running; Geyser extension replacement verified after restart |
| TreeFalls | Candidate custom | 1.3.4 | Not imported | No | Installed |
| UJobs | Candidate custom | 1.0.8 | Not imported | No | Installed |
| VaultUnlocked | Third-party | 2.20.2 | — | No | Installed |
| Simple Voice Chat | Third-party | 2.6.21 | — | No | Installed |
| Waystones | Third-party | 7.8 for 26.2.x | — | No | Running on 26.2; maintainer compatibility is limited to 26.2.x |
| WorldEdit | Third-party | 7.4.5 | — | No | Running on 26.2; crashes on 26.3, while 7.4.6 beta 2 passed the local 26.3 startup test |
| WorldGuard | Third-party | 7.0.18 | — | No | Installed |

## Plugin support directories without a matching top-level JAR

Local BigOscieGF source is 0.6.1, with terrain-aware preview placement and
restored Nyx personality. The live directory and disposable startup test report
0.6.7, so the live artifact is newer and must not be replaced with the local build.

The live directory also contains `AxAPI`, `bStats`, `faststats`, `MagmaCore`,
`NBTAPI`, `spark`, `update`, and `Vault`. These may be data directories, embedded
libraries, generated support folders, or remnants. Do not delete them without
identifying their owner and checking live dependencies.

## Planned plugins

| Plugin or idea | Goal | Priority | Notes |
| --- | --- | --- | --- |
| Import remaining custom plugin source | Put each confirmed custom plugin project under `plugins/` | High | BigOscieGF imported; source cannot be reconstructed reliably from deployed JARs |
| Track reviewed configuration | Copy only redacted, useful plugin configuration | Medium | Start with custom plugins |
