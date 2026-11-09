# Proposal: fix-upstream-bugs-p1

## Why

Triage of the upstream `edvin/tornadofx` issue tracker (2026-11-09) identified three high-priority bugs that are still present, and verified in our fork's code, in this repository: a crashing `ConcurrentModificationException` in `moveUpAll`/`moveDownAll` (upstream #1340), an overly strict `DataGrid.cellFragment` API that forces callers to unsafe casts (upstream #700), and a permanent memory leak in the event bus scope registry (upstream #1009). Fixing them now closes the three cheapest high-impact defects found by the upstream triage, before they surface in consumer bug reports against our releases.

## What Changes

- **`moveUpAll`/`moveDownAll` no longer throw `ConcurrentModificationException`** (upstream #1340): both helpers (and the shared private move-at-index paths they drive) must operate on a snapshot of matching elements instead of iterating the list while mutating it, matching the pattern already used by `moveToTopWhere`/`moveToBottomWhere` in `Collections.kt`.
- **`DataGrid.cellFragment()` accepts `DataGridCellFragment` subclasses** (upstream #700): the API signature is relaxed from `KClass<DataGridCellFragment<T>>` to accept any subtype of `DataGridCellFragment<T>`, so implementers of the interface no longer need `as KClass<DataGridCellFragment<T>>` casts. Source-compatible: existing call sites keep compiling.
- **Event bus stops leaking scope entries** (upstream #1009): invalidating event subscriptions (dock/undock cycles, `times`-limited registrations, `unsubscribe()` from `EventContext`) now also removes the corresponding `eventScopes` entries, so the registry no longer grows without bound for long-lived applications.
- Regression tests for all three behaviors, runnable in the default headless test profile.

Out of scope (deferred to follow-up changes per the triage report): JPMS `ResourceBundle` loading (#1286), `SortedFilteredList` index-mapping cluster (#608/#1044/#1212), `Field` label width sync (#950), `onDock` lifecycle edge cases (#1020), workspace scope cleanup (#810/#1318), P3 items, and upstream PR submission mechanics.

## Capabilities

### New Capabilities
- `collection-helpers`: the mutable-list mutation helpers (`moveUpAll`, `moveDownAll`, and sibling move/reorder helpers) safely reorder elements of observable and plain lists, including the concurrent-modification and index-consistency guarantees for repeated/predicate-matched moves.
- `event-bus`: the `FXEvent` subscription registry lifecycle — subscribe/unsubscribe/invalidation semantics, scope bookkeeping, and the no-leak guarantee for per-action scope entries.

### Modified Capabilities
- `ui-controls`: new requirement for `DataGrid.cellFragment()` accepting any `DataGridCellFragment<T>` subtype class, keeping the existing render-and-select requirements unchanged.

## Impact

- **Code**: `src/main/java/tornadofx/Collections.kt` (move helpers), `src/main/java/tornadofx/DataGrid.kt` (`cellFragment` signature and its `find(cellFragmentType, ...)` use), `src/main/java/tornadofx/EventBus.kt` (`eventScopes` cleanup on invalidation; `cleanupInvalidRegistrations` and the `fire`/`unsubscribe` paths).
- **Tests**: new unit tests in `src/test/kotlin` for the collection helpers; event bus leak/cleanup tests; a `DataGrid.cellFragment` generic-acceptance test (headless profile).
- **API/compatibility**: no breaking changes — `cellFragment` relaxation is source-compatible; collection helpers and event bus fixes only change observable behavior for the broken paths.
- **Consumers**: users of list reordering helpers, DataGrid cell fragments, and long-running apps using event bus subscriptions no longer hit crashes, compile-time casts, or unbounded memory growth.
