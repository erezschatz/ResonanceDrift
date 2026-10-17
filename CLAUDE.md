# CLAUDE.md — working in this repo

ResonanceDrift is a native Android music player for an Ampache server, written
in Kotlin + Compose + Media3. One developer, one phone, one user. This file is
how to work here; what the app is lives in `docs/`.

## Read first, in this order

1. `docs/SPEC.md` — what the app is, scope, architecture, stack. The contract.
2. `docs/PLAN.md` — the ordered steps and which one is current. Then the
   current step's file in `docs/tasks/`.
3. `docs/DECISIONS.md` — why things are the way they are, and what was
   rejected. **Do not re-open a decision listed there without saying so
   explicitly**; propose a new entry instead of quietly doing it differently.
4. `docs/AMPACHE.md` when touching the server client; `docs/REDRIFT.md` when
   a question is "how did the old app do this?".
5. `docs/SETUP.md` only for toolchain problems.

A new session with no memory of previous ones should be able to pick up from
these files alone. If something needed to continue the work is not in them, the
previous session failed the discipline below — fix the gap first.

## The Unities (the working discipline)

- **Place:** everything about the project lives in this repo — spec, decisions,
  plan, tasks, code, tests, changelog. Not in chat, not in a wiki, not in
  anyone's memory.
- **Time:** the test, the doc update, the task status and the changelog entry
  are written *in the same commit* as the code they describe. Never "later".
  Whatever is unwritten at the end of a session is lost.
- **Action:** one account of the work. `PLAN.md` says where we are; the task
  file says what "done" means; `CHANGELOG.md` says what happened. They do not
  contradict each other, and nothing else claims to be the status.

## Per-commit checklist

- Code compiles and `./gradlew test` passes (claim it only if actually run).
- The task file's acceptance criteria that this commit satisfies are ticked.
- `CHANGELOG.md` has an entry under *Unreleased*.
- `docs/SPEC.md` is edited if behaviour or architecture changed.
- A new decision — any choice with a rejected alternative — gets a dated entry
  in `docs/DECISIONS.md`.
- `versionCode`/`versionName` bumped in `app/build.gradle.kts` if the commit is
  meant to be installed on the phone.

## Code rules

- Kotlin idioms, but readable ones: no clever DSLs, no extension-function soup,
  no `!!`. If a construct would be unreadable to a strong TypeScript developer
  in their first week of Kotlin, write it the plain way.
- Four packages — `ampache/`, `data/`, `playback/`, `ui/` — and that is the
  budget. No `domain/`, `usecase/`, `presentation/` layers.
- One file per screen / service / repository; split at ~400 lines.
- Comments explain *why*. Every non-obvious Media3 setting gets a one-line
  reason beside it.
- No new dependency without a row in the Stack table in `docs/SPEC.md` saying
  what it replaces and why it is rote.
- Playback logic lives in the service and nowhere else. No defensive timers or
  retries in the UI. When playback misbehaves, read `PlaybackLog` first.
- Never write code that "keeps playback alive" in the UI layer. That reflex is
  what ReDrift was made of; here it means the wrong layer was chosen.

## Division of labour

The programmer holds the helm, the LLM has the oars. Claude implements the
current step and reports what it did and what it could not verify (no Android
SDK in the cloud workspace means Claude cannot build or run the app). Erez
builds, runs on the phone, and says what happened. Claude does not claim a
build is green; Erez does.
