# Custom plugins

Create one self-contained Maven or Gradle project per plugin. Use a clear folder
name matching the plugin name, include the build wrapper when practical, and add a
README covering:

- purpose and player-visible behavior;
- supported Minecraft, server API, and Java versions;
- commands, permissions, dependencies, and soft dependencies;
- configuration and data-storage format;
- build, test, installation, upgrade, and rollback instructions;
- any migrations or compatibility caveats.

Do not put third-party plugin JARs here. Record them in `docs/PLUGIN_CATALOG.md`.
