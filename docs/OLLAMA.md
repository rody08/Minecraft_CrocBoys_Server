# BigOscieGF local Ollama bridge

## Architecture

```text
BigOscieGF on PebbleHost
  -> HTTPS Cloudflare Tunnel URL
  -> authenticated relay on 127.0.0.1:11435
  -> Ollama on 127.0.0.1:11434
  -> Magnum v4 12B Q5_K_M on the RX 6900 XT
```

Ollama itself stays bound to localhost. The relay exposes only `/v1/responses`,
requires a random bearer token, limits request size, disables streaming, and pins
all requests to the configured Nyx model.

## Local commands

Run setup once:

```powershell
.\scripts\Setup-OllamaBridge.ps1
```

Start the relay and temporary Cloudflare Quick Tunnel:

```powershell
.\scripts\Start-OllamaBridge.ps1
```

Stop both bridge processes:

```powershell
.\scripts\Stop-OllamaBridge.ps1
```

The start command prints the endpoint for BigOscieGF. The secret relay token is
stored only in the Git-ignored `.env` file.

`NYX_OLLAMA_MODEL` selects Nyx's model. The scripts temporarily accept the old
`BIGOSCIE_OLLAMA_MODEL` name during migration, but new configuration should use
the Nyx-specific name.

Nyx uses `hf.co/bartowski/magnum-v4-12b-GGUF:Q5_K_M`, a Q5 quantization of the
Magnum v4 12B conversational/prose model. The model is selected specifically for
character dialogue; Nyx's exact identity and behavior remain in the plugin's
reviewable `ai.personality` and per-request instructions.

## BigOscieGF configuration

The live plugin configuration needs:

```yaml
ai:
  enabled: true
  provider: "ollama"
  endpoint: "https://GENERATED.trycloudflare.com/v1/responses"
  model: "hf.co/bartowski/magnum-v4-12b-GGUF:Q5_K_M"
  api-key: "LOCAL_RELAY_TOKEN"
```

The relay overrides the requested model as an extra safeguard.

## Availability

Nyx's AI is available only while this Windows PC, Ollama, the relay, and the
tunnel are running. Plugin fallback lines continue to work when the bridge is
offline.

Cloudflare Quick Tunnels are suitable for testing and use a new random hostname
when restarted. For dependable long-term operation, replace the Quick Tunnel
with a named Cloudflare Tunnel and stable hostname.
