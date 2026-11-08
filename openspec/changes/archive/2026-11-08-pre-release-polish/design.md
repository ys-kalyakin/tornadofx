# Design

## Context

A full-toolchain compile of the main sources emits 62 warnings; the complete
inventory was captured during exploration. Three are Kotlin progression warnings
("will become an error in language version 2.4 / 2.5", KTLC-365 and KTLC-288);
16 are uses of Java-deprecated APIs; the rest are code-quality diagnostics.
Separately, README promises snapshot publishing to the decommissioned
`oss.sonatype.org`. See proposal.md for motivation, the spec delta for the
requirement this design implements.

Toolchain facts that constrain the fixes:

- No `languageVersion`/`apiVersion` is pinned in the pom — the build uses the
  compiler defaults — so warnings today are exactly what the compiler reports;
  nothing filters them.
- Two of the four `URL(String)` sites (FX.kt:370, CSS.kt:463) are already
  wrapped in `catch (MalformedURLException)` — the exception type the old
  constructor throws.
- The `Class.newInstance()` sites instantiate framework components dynamically
  (views, scoped instances, JSON models). `Class.newInstance()` propagates the
  constructor's own exception, while the replacement
  `getDeclaredConstructor().newInstance()` wraps it in
  `InvocationTargetException`.

## Goals / Non-Goals

**Goals:**

- Main source set compiles with zero warnings on the pinned toolchain
- The three progression-warning constructs are eliminated (the specific
  warnings no longer appear in the compile output)
- The README no longer claims snapshot publishing
- Behavior-preserving fixes only: same exception types surfaced to callers,
  same public API

**Non-Goals:**

- Build-config enforcement of warning-freeness (e.g. `-Werror` /
  `allWarningsAsErrors`): it would hard-fail every future toolchain upgrade on
  newly introduced warning categories. Revisit after the Kotlin 2.5 migration.
- Test-source warnings: the requirement and this change cover main sources only
- Publishing mechanics and coordinate/version text updates (proposal's out-of-scope list)
- Refactoring the touched code beyond what warning removal requires

## Decisions

### D1: Fix, don't suppress

Every warning is fixed at the source; no `@Suppress` annotations.

*Implementation amendment (apply phase)*: one documented exception exists —
`Binding.kt` keeps a cast that K2 reports as a redundant cast but that is
actually required for overload resolution (without it the compiler selects the
`Format` overload and fails). The site carries a targeted
`@Suppress("USELESS_CAST")` with a comment explaining why the cast stays.

*Why*: the zero-warning requirement is about the code being genuinely clean;
suppression would hide future regressions of the same kinds. Alternative
considered — targeted `@Suppress("DEPRECATION")` on `Class.newInstance()` sites —
rejected: it keeps deprecated-API usage the next Kotlin release may harden.

### D2: `Class.newInstance()` replacement with exception unwrapping

Replace each `Class.newInstance()` with `getDeclaredConstructor().newInstance()`
through one internal helper that unwraps `InvocationTargetException` and rethrows
its cause, so callers keep receiving the same exception type as today. The
helper lives alongside the existing internal helpers (Lib.kt-style, not part of
the public API surface documented for consumers).

*Why*: plain replacement would change observable behavior (callers currently
catch the constructor's own exception type). Alternatives considered: (a) plain
replacement and let `InvocationTargetException` propagate — breaks behavior;
(b) `kotlin.reflect` `createInstance()` — adds reflection overhead and changes
construction semantics (looks for a no-arg constructor via KClass, same
effect but heavier).

### D3: `URL(String)` replacement preserving `MalformedURLException`

Replace `URL(String)` with a construction path that maps `URISyntaxException`
back to `MalformedURLException` (e.g. `try { URI(s).toURL() } catch (e:
URISyntaxException) { throw MalformedURLException(e.message, e) }`). The two
existing catch sites (FX.kt, CSS.kt) therefore keep working unchanged.

*Why*: preserves behavior exactly. Alternative — switch the catch sites to
`URISyntaxException` — spreads the API change into more files for no benefit.

### D4: Mechanical cleanups taken as-is

- `ItemControls.kt:70`: drop the always-true `!is DoubleProperty` / `!is
  FloatProperty` guards (an `IntegerProperty` receiver can never be those types
  in this context — the compiler has proven the checks redundant)
- `Json.kt:268`: add the explicit return type the warning asks for
- Redundant casts, unnecessary safe call, always-left Elvis operands: remove
  the redundant operation
- `java.lang.Integer`/`java.lang.Double` references: use `kotlin.Int`/`Double`
  (identical erased types; signature-compatible)
- `String.decapitalize()` → `replaceFirstChar(...)`, `sumByDouble` → `sumOf`:
  direct replacements recommended by the deprecation messages

### D5: README section replaced, not deleted

The "Snapshots are published to Sonatype" section is replaced by a two-sentence
statement that this fork does not publish snapshots (and none of the upstream
snapshot infrastructure applies), keeping the install snippets above it as the
single source of installation truth. Coordinates/version updates in those
snippets stay out of scope (release-time change).

### D6: Verification of the progression scenario

The spec scenario "Ready for the next Kotlin language version" is verified by a
one-off compile with `-progressive` passed to kotlin-maven-plugin arguments
(temporary edit, reverted after the check). If the current compiler does not
promote these diagnostics under `-progressive`, the fallback check is that the
three progression warnings disappear from the normal compile output — the
constructs flagged by the language committee are then provably gone, which is
the normative content of the requirement.

## Risks / Trade-offs

- [Behavior drift from exception-type changes] → D2/D3 explicitly preserve
  exception types; the full test suite (385 tests) must pass unchanged
- [Zero-warning bar is a snapshot, not a guarantee] → without build enforcement
  new warnings can creep in; accepted (see Non-Goals), CI compile output stays
  visible on every PR
- [`-progressive` may not be exercisable on the pinned compiler] → fallback
  check defined in D6; the requirement's substance (no scheduled-to-fail
  constructs) is still verified
- [Some cleanups touch widely used files (FX.kt, CSS.kt)] → changes are
  call-site only and covered by the existing suite; no public signatures change

## Migration Plan

Single commit-series on a branch; no deployment, no data migration. Rollback =
revert the branch. Nothing consumer-visible changes, so no CHANGELOG breaking
entry is needed; a short "Fixed"/"Changed" note about warning-free main sources
and the README correction can be added to the existing 1.7.21 section at
release time.

## Open Questions

None.
