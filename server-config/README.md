# Reviewed server configuration

This directory is for non-secret configuration that is useful for reproducing or
understanding the server. Mirror the live relative paths where practical, for
example:

```text
server-config/
  server.properties.example
  paper-global.yml
  plugins/
    CrocBoysCore/
      config.yml
```

Use `.example` files when values must be redacted. Never commit credentials,
tokens, private connection details, player lists or identifiers, databases,
worlds, logs, crash reports, caches, or backups.
