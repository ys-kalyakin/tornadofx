# Proposal: Pre-release polish for 1.7.21

## Why

Before publishing 1.7.21, the main source set emits 62 compiler warnings, three
of which are Kotlin progression warnings scheduled to become hard compile errors
(KTLC-365 sites at `ItemControls.kt:70`, KTLC-288 at `Json.kt:268`) — a rebuild
blocker on the next Kotlin upgrade and off-brand for a fork whose entire premise
is the modern stack. Independently, the README still promises daily snapshot
publishing to `oss.sonatype.org`, a service decommissioned in 2025 that this fork
never used, which misleads anyone evaluating the project today.

## What Changes

- Eliminate all compiler warnings from the main source set (currently 62):
  - 3 progression warnings that will become language errors
    (`ItemControls.kt:70` ×2 per KTLC-365, `Json.kt:268` per KTLC-288)
  - 16 call sites of Java-deprecated APIs: `URL(String)` constructor ×4,
    `Class.newInstance()` in dynamic view/instance instantiation (App, FX, Json,
    ...), `String.decapitalize()` ×2, `sumByDouble` ×1, and similar
  - Remaining code-quality warnings: redundant casts, always-true `is` checks,
    always-left Elvis operands, one unnecessary safe call, explicit
    `java.lang.Integer`/`java.lang.Double` references instead of `kotlin.Int`/`Double`
- Rewrite the README section "Snapshots are published to Sonatype" so it no
  longer claims publishing this project does not do (state plainly that no
  snapshot repository is published by this fork until a publishing setup exists)
- No public API changes: every fix is internal to a function body or a private
  call site; behavior is preserved (e.g. `Class.newInstance()` →
  `getDeclaredConstructor().newInstance()` over the same no-arg constructor)

Out of scope, deliberately:

- Publishing mechanics for the release (channel, groupId, deploy automation,
  GPG) — a separate, not-yet-made decision
- Updating installation coordinates/versions in README and docs pages — those
  are correct today (they reference the actually published 1.7.20 / the
  in-development `1.7.21-SNAPSHOT`) and change at release time under the
  publishing change, not now

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `platform-compatibility`: add a requirement that main sources compile
  warning-clean under the pinned toolchain, including the elimination of Kotlin
  progression warnings that are scheduled to become language errors — the same
  code-hygiene genre as the existing "No JDK-internal APIs in shipped code"
  requirement. The README correction is documentation-only and needs no spec
  delta.

## Impact

- **Code**: ~10 files under `src/main/java/tornadofx/` (ItemControls, Json, CSS,
  FX, App, InternalWindow, Keyboard, Binding, Forms, Animation, Lib,
  Layouts and similar); no test source changes expected
- **Build**: no plugin or configuration changes required; the full build and the
  385-test suite must stay green
- **Docs**: `README.md` only (one section rewritten); the MkDocs site is untouched
- **Compatibility**: jar bytecode behavior unchanged; no deprecations added or
  removed from the public surface
