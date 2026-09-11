# Server operations

## Normal plugin workflow

1. Confirm the runtime versions in `SERVER.md`.
2. Make changes inside one plugin project.
3. Build the plugin with its Maven or Gradle wrapper.
4. Run automated tests and, for gameplay behavior, test on a local non-production
   server using synthetic player data.
5. Place or copy the release JAR into `artifacts/` if useful.
6. Commit the source and documentation checkpoint.
7. Back up the live server before a risky deployment.
8. Stop the server when the plugin cannot be safely hot-swapped.
9. Upload only the intended JAR to `/plugins/` and start/restart as appropriate.
10. Check the console and relevant log lines for load errors, then perform a short
    gameplay smoke test.

Avoid plugin-manager hot reloads unless the plugin explicitly documents support;
full restarts are more predictable for dependency and classloader changes.

## Configuration workflow

- Store reviewed, shareable configuration beneath `server-config/` using the same
  relative path as the server where practical.
- Redact credentials, tokens, webhook URLs, IP allowlists, player identifiers, and
  database connection strings before committing.
- Pull the current live file before editing so a stale local copy does not replace
  newer server-side changes.
- Review a diff before upload and deploy only the named configuration file.

## Recovery checklist

1. Stop the server if it is repeatedly crashing or writing bad data.
2. Save the failing console/log excerpt outside Git if it contains player data.
3. Restore the previous JAR or configuration from the latest known-good backup.
4. Start the server and verify plugin load, commands, permissions, and data.
5. Record the cause and prevention in the plugin README or `DECISIONS.md`.

## Secrets

Use the PebbleHost panel, an interactive SFTP password prompt, Windows Credential
Manager, or a password manager. Do not store secrets in Git, scripts, Markdown,
shell history, screenshots, or task messages.
