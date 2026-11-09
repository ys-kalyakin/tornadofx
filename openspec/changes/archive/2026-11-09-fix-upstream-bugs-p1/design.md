# Design

## Context

Three upstream bugs verified in the fork (see proposal.md — Why). Relevant current code:

- `Collections.kt:311-320` — `moveUpAll`/`moveDownAll` iterate `asSequence().withIndex()` and call `moveUpAt(it.index)` inside `forEach`, mutating the list mid-iteration. The same file already has a snapshot pattern (`moveToTopWhere` uses `.toList()`).
- `DataGrid.kt:134` — `fun cellFragment(fragment: KClass<DataGridCellFragment<T>>)`; the registered class is stored via `dataGrid.properties["tornadofx.cellFragment"]` and read back at `DataGrid.kt:295-296` with a cast to `KClass<DataGridCellFragment<T>>?` before `find(cellFragmentType, ...)`. `DataGridCellFragment<T>` is an abstract class extending `ItemFragment<T>`.
- `EventBus.kt` — `eventScopes: ConcurrentHashMap<action, Scope>` written on every `subscribe` (line 87), removed only in `unsubscribe(event, action)` (line 107). Invalidation (dock/undock, `times` exhaustion, `EventContext.unsubscribe()`) only flips `valid` flags; `cleanupInvalidRegistrations` (line 115) filters the `subscriptions` sets but never touches `eventScopes`. Existing tests: `CollectionsTest.kt`, `EventBusTest.kt` (headless profile).

## Goals / Non-Goals

**Goals:**
- Matched-element reordering helpers that are exception-free and index-consistent on observable lists.
- `cellFragment` accepting `DataGridCellFragment<T>` subtypes without unchecked caller casts.
- Bounded event-bus bookkeeping: scope entries removed when their action loses its last valid registration.

**Non-Goals:**
- No changes to event delivery semantics, ordering, or `runOn`/scope filtering (see `event-bus` delta: invalidation preserves delivery).
- No reordering-helper API additions or deprecations; the public signatures of `moveUpAll`/`moveDownAll` stay identical.
- No JPMS, `SortedFilteredList`, `Field` width, or lifecycle work (out of scope per proposal).

## Decisions

### D1: moveUpAll/moveDownAll — snapshot of matching indices, moved one at a time
Collect `withIndex().filter { predicate }.map { it.index }.toList()` first, then call `moveUpAt`/`moveDownAt` on the snapshot indices: ascending order for `moveUpAll` (an upward swap at index i only touches positions < i, so later indices stay valid), descending for `moveDownAll` (a downward swap at index i shifts higher indices, so process from the bottom).

- Alternative considered: snapshot matching *values* and call the existing `moveUp(item)`/`moveDown(item)` overloads — simpler, but `moveUp(item)` resolves via `indexOf`, which misbehaves when the list holds duplicate elements matching the predicate. The index snapshot has no duplicate ambiguity.
- Alternative considered: rewrite as block rotation (`subList` + `Collections.rotate`) — fewer moves, but changes observable-list notification granularity (one big change instead of per-swap) and alters which `moveUpAt`/`moveDownAt` events listeners see. Keep per-move notifications; consumers (e.g., ListView bindings) depend on them.

### D2: cellFragment — widen both the API and the stored type to `KClass<out DataGridCellFragment<T>>`
`cellFragment(fragment: KClass<out DataGridCellFragment<T>>)` and the properties-stored type become `KClass<out DataGridCellFragment<T>>?`. The `find(...)` call site is the only place needing a concrete type; it gets one localized `@Suppress("UNCHECKED_CAST")` cast to `KClass<DataGridCellFragment<T>>` (reflection-based lookup, runtime type is guaranteed by the `out` bound at registration).

- Alternative considered: make `cellFragment` generic (`fun <F : DataGridCellFragment<T>> cellFragment(fragment: KClass<F>)`) as suggested in upstream #700 — also source-compatible, but it leaks the type parameter into the stored/read-back path and still needs the same cast at `find`; `out`-projection is the smaller, more idiomatic Kotlin change.

### D3: eventScopes cleanup — remove an action's scope entry when its last valid registration is gone
Add a helper `removeScopeIfOrphaned(action)`: keep the entry only if some valid registration across all event types still uses the action. Wire it in three places:
1. `cleanupInvalidRegistrations(eventKlass)` — after filtering a type's set, check the actions of registrations that were dropped.
2. `unsubscribe(event: KClass<T>, action)` — replace the unconditional `eventScopes.remove(action)` (today it can break a second event type sharing the same action) with `removeScopeIfOrphaned(action)`.
3. `unsubscribe(registration: EventRegistration)` — already delegates to (2); no separate logic.

Invalidation during `fire` is handled by (1), which already runs at the end of every dispatch. The orphan scan is O(number of registrations for that action's event types) and runs only on unsubscribe/invalidation, never on the hot delivery path.

- Alternative considered: drop `eventScopes` and resolve scope at dispatch time from the registration itself — cleaner long-term but changes `EventRegistration`'s shape and the public `subscribe` overloads; rejected for this change (risk of semantic drift in scope-filtered delivery), noted as a possible follow-up refactor.

### D4: tests live in existing suites, default headless profile
- `CollectionsTest.kt` — moveUpAll/moveDownAll cases on `observableArrayList` and plain `mutableListOf` (multi-match blocks, boundary elements, duplicate elements), plus a guard test for `moveUp`/`moveDown` unchanged behavior.
- `EventBusTest.kt` — repeated subscribe/invalidate cycles asserting bounded scope bookkeeping (via a package-private/internal accessor if needed), `EventContext.unsubscribe()` cleanup, and delivery-semantics preservation.
- A small DataGrid test (new `DataGridFragmentTest.kt`, TestFX headless) — registering an implementing subclass via `cellFragment(MyFragment::class)` compiles and renders. Compilation acceptance is itself the #700 regression guard.

## Risks / Trade-offs

- [Reordering semantics drift] The one-position-per-match block semantics is now spec'd; index snapshot must be ordered correctly → dedicated unit tests assert exact resulting orders, including all-match and no-match cases.
- [Unchecked cast at DataGrid.kt:296] Registration-time `out` bound makes it sound, but it is still a cast → single localized `@Suppress("UNCHECKED_CAST")` with a comment; matches the fork's existing suppression policy (warnings stay at zero).
- [Over-aggressive scope cleanup] If one action lambda is shared by registrations on multiple event types, removing on first unsubscribe would break the second → `removeScopeIfOrphaned` scans all event types before removing; test covers the shared-action case.
- [EventBus concurrency] `fire` runs on FX or background threads; cleanup mutates `subscriptions`/`eventScopes` concurrently → reuse the existing `compute`-based replacement pattern (set wholesale replacement, no structural mutation of iterated collections); no new locking introduced.

## Migration Plan

Behavior-only fixes, additive API widening; no data or config migration. Rollback = revert the single commit; each fix is independent enough to revert separately if needed (three touched files, three test files).

## Open Questions

None — implementation paths, signatures, and test placements verified against the current code.
