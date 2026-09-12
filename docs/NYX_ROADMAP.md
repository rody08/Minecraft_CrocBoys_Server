# Nyx: saved plan and local benchmark handoff

Saved on 2026-09-12. This is the continuation point for Nyx work when Codex
allowance is available again. The current scope is documentation and a resumable
local model benchmark. The plugin features below are agreed plans, not implemented
or deployed changes. No new benchmark result or replacement model is claimed here.

## Confirmed starting point

| Area | Recorded state |
| --- | --- |
| Server | Purpur 26.2 build 2622, Java 25; see [SERVER.md](SERVER.md) |
| Plugin | Local BigOscieGF source is 0.5.2; last recorded live version is 0.5.1 |
| PC | AMD Ryzen 7 9800X3D, Radeon RX 6900 XT with 16 GB VRAM, about 32 GB RAM |
| Inference | Ollama 0.34.0 using Vulkan |
| Installed baseline | `hf.co/bartowski/magnum-v4-12b-GGUF:Q5_K_M` |
| Data path | Production uses the authenticated local relay and tunnel described in [OLLAMA.md](OLLAMA.md) |

The goal is a typical complete reply in **3-5 seconds while gaming**, with enough
GPU headroom to keep the game comfortable. This is a target, not a measured result.
Start benchmark runs at **4,096 context tokens**. The previously observed 32K
context consumed roughly 6 GB for KV cache alone; do not reuse that setting as the
starting point on a GPU shared with a game.

## Model comparison

Use the installed Magnum model as the baseline. Compare these exact candidate
tags after explicitly downloading any missing models:

| Candidate tag | Approximate download size |
| --- | ---: |
| `qwen3.5:9b-q4_K_M` | 6.6 GB |
| `gemma4:12b-it-qat` | 7.2 GB |
| `qwen3:14b-q4_K_M` | 9.3 GB |

Download size is not total VRAM use. Record each resolved model digest and the
run settings; tags can move. Resume only against the recorded model and workload
identity. A different digest, context size, prompt suite, or other generation
setting requires a fresh comparable run. There is no predetermined winner.

Use synthetic conversations only. Compare the same cases and generation settings
first with the PC idle and then during representative gaming. Record the game,
graphics settings, and workload in the run notes. Review complete-response latency,
slow replies, output length, errors, and perceived game smoothness alongside the
actual replies. Separate cold model loading from warm inference. Local Ollama
measurements omit the production tunnel, relay, server queue, and chat delivery;
an eventual end-to-end check is still needed before claiming the gameplay target.

Quality review should cover expressive but concise dialogue, follow-ups, correct
player attribution, explicit corrections, refusal to invent remembered facts,
quiet event handling, and valid action markers. Confirm that trust, item, and
building boundaries still hold. Fast replies that lose Nyx's character or violate
the plugin's action format do not establish a winner.

## Running and resuming the benchmark

The local runner is `scripts/nyx_benchmark.py`, with the PowerShell entry point
`scripts/Start-NyxBenchmark.ps1` and synthetic cases in
`scripts/nyx-benchmark/cases.json`. It uses Python's standard library and calls
local Ollama directly. Saved per-case JSON and reports belong under Git-ignored
`artifacts/nyx-benchmark/<run>/`; preserve that directory to continue an interrupted
run and review earlier answers without repeating completed work.

From the repository root, start a short comparison in the background:

```powershell
.\scripts\Start-NyxBenchmark.ps1 -Suite Screen -RunDir artifacts/nyx-benchmark/screen-idle -Sampling Stable -Context 4096 -Workload Idle -Background
```

Run the **same command with the same `-RunDir`** to resume. Completed cases are
kept; changed settings, source, fixtures, Ollama runtime, or model digests are
rejected so unlike measurements cannot silently mix. Use a new run directory for
changed inputs. Missing models are recorded as retryable and skipped. Download
them explicitly with `ollama pull <exact-tag>` or choose `-PullMissing` when
starting a run intended to download candidates.

`Screen` defaults to two repetitions; `Full` defaults to three. The default model
list is Magnum plus all three candidates above. Use `-Models` with an array of
exact tags to narrow it, `-Cases` with an array of case IDs to select prompts, or
`-Repetitions` to set the repeat count. `-Sampling` accepts `Stable`,
`Recommended`, or `Both`; keep it fixed for a comparison. Initial context is
4,096; use `-Context 8192` only as a separate follow-up experiment if useful.

For a fuller comparison while actually playing a representative game:

```powershell
.\scripts\Start-NyxBenchmark.ps1 -Suite Full -RunDir artifacts/nyx-benchmark/full-gaming -Sampling Stable -Context 4096 -Workload Gaming -Background
```

`-Workload Gaming` is a label only: it does not launch a game or measure FPS.
Record gameplay conditions and responsiveness yourself. Use a corresponding
`Full` idle run with the same models, sampling, context, and repetitions to
compare the effect of gaming. Omit `-Background` to watch a run in the terminal.

Read `report.html` or `report.md` for results, `summary.json` for structured
metrics, and `blind-review.html` to review replies without model labels. The
runner writes each case atomically. To regenerate saved reports without making
inference requests, add `-ReportOnly` to the run's command and omit `-Background`.

The wrapper uses the existing `.pebblehost/python/Scripts/python.exe` when
available, then falls back to `python`. The runner creates uniquely named
`nyx-benchmark-*` Ollama aliases for its context and sampling settings and cleans
up its own aliases afterward. It does not change the relay selection, `.env`, or
production configuration. Running inference still shares the PC's GPU, so label
and compare workloads deliberately.

## Agreed player experience

Nyx is an expressive goth companion with concise warmth and teasing. She should
respond to the conversation naturally, remember useful context, and avoid forcing
old jokes or excessive narration into unrelated replies. Ship the following in
order, with each stage built and tested before starting the next:

1. **Memory for every player.** Automatically retain useful facts and broad
   gameplay summaries by default, with a player opt-out. Include building,
   mining, combat, and travel summaries instead of raw event streams. Explicit
   player corrections override inference. Attribute records by UUID and keep
   source and recency information so another player's claim does not silently
   become a personal fact. Provide private player controls to inspect, correct,
   clear, and disable their own memory. Import legacy owner memory once.
2. **Moods and occasional suggestions.** Keep expressive moods separate from
   the existing 0/50/100 trust states and item permissions. The configured owner
   stays locked at trust 100. Allow occasional useful suggestions to nearby
   players, with a quiet toggle and limits that prevent interruptions from
   dominating chat.
3. **Feedback.** Start with in-game reply ratings and tickets so players can
   report awkward replies or incorrect memories. Use that evidence to improve
   prompts and behavior; do not assume an external service is required.

Memory collection, mood behavior, suggestions, and ratings/tickets remain deferred.
Do not treat these descriptions as existing commands or configuration options.

## Implementation findings to address before memory expansion

These findings came from local 0.5.2 source inspection. Recheck them against the
current source when implementation resumes:

- Memory markers are saved before `ModelOutputGuard` validates the reply. Guard
  and validate the entire response before committing memory or any other action.
- Shared conversation history attributes speakers by name. Carry UUID identity
  through queued requests, history, and memory; a display name is presentation.
- The serialized AI queue schedules a synchronous commit without awaiting that
  commit. Keep the next request behind the completed main-thread state update so
  it sees the previous reply and actions in order.
- `plugin.yml` gates the root command with `bigosciegf.admin`, while the Java
  command handler checks `nyx.admin`. Reconcile the permissions and command
  routing before adding player controls; ordinary players need private access
  to their own settings without administrative access.
- Clear and disable operations must invalidate pending work as well as stored
  records. An in-flight reply must not restore cleared memory or persist facts
  after opt-out. Test this race explicitly.
- Legacy `owner-memory.txt` import needs a persistent migration marker so a
  restart cannot reimport facts after the player clears them.

Use the Paper API and Java versions in the plugin build and [SERVER.md](SERVER.md).
Keep model output as proposals validated by plugin code. Existing item validation,
same-player building confirmation, bounds checks, and WorldEdit undo remain part
of the action boundary.

## Next session and completion boundary

Read this file, [OLLAMA.md](OLLAMA.md), [DECISIONS.md](DECISIONS.md), the plugin
README/build/`plugin.yml`, and the working-tree diff before continuing. Preserve
unrelated edits and any partial benchmark run. Finish and review the local model
comparison before selecting a replacement or expanding the plugin. Record the
chosen digest, settings, observed tradeoffs, and benchmark run path when evidence
supports a choice; otherwise retain the baseline and document what is unresolved.

Build and test any later plugin change, update its README for player-visible
behavior, and keep local source version distinct from recorded live version.
There is **no authorization to upload, stop, restart, or change production** in
this task. A later deployment requires an explicit request, exact local source
and remote destination, and confirmation that a suitable backup exists under
[OPERATIONS.md](OPERATIONS.md).
