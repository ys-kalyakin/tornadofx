# Tasks

## 1. Collection helpers (`moveUpAll`/`moveDownAll`, upstream #1340)

- [x] 1.1 Rework `moveUpAll` in `src/main/java/tornadofx/Collections.kt` to snapshot matching indices (ascending) and move them one at a time via `moveUpAt`, per design D1; verify with a new test
- [x] 1.2 Rework `moveDownAll` the same way with descending index order; verify with a new test
- [x] 1.3 Add tests to `src/test/kotlin/tornadofx/tests/CollectionsTest.kt`: multi-match block order on `observableArrayList` and `mutableListOf` (exact resulting order, no exception), element already at top/bottom stays, all-elements-match and no-match cases, duplicate elements matching the predicate, and a guard test that `moveUp`/`moveDown` still move only the first match — run `./mvnw test -Dtest=CollectionsTest` and confirm green

## 2. `DataGrid.cellFragment` subtype acceptance (upstream #700)

- [x] 2.1 Widen `DataGrid.cellFragment` parameter and the `tornadofx.cellFragment` properties-stored type to `KClass<out DataGridCellFragment<T>>` in `src/main/java/tornadofx/DataGrid.kt`, keeping the `find(...)` call site working with one localized `@Suppress("UNCHECKED_CAST")`; verify the module compiles with `./mvnw compile` and no new compiler warnings
- [x] 2.2 Add `src/test/kotlin/tornadofx/tests/DataGridFragmentTest.kt` (TestFX headless profile): a concrete fragment class implementing `DataGridCellFragment<Person>` registered via `cellFragment(MyFragment::class)` without a cast renders grid cells — run `./mvnw test -Dtest=DataGridFragmentTest` and confirm green

## 3. Event bus scope cleanup (upstream #1009)

- [x] 3.1 Add `removeScopeIfOrphaned(action)` in `src/main/java/tornadofx/EventBus.kt` per design D3 (entry kept only while a valid registration uses the action across all event types); wire it into `cleanupInvalidRegistrations` and replace the unconditional `eventScopes.remove(action)` in `unsubscribe(event, action)`; verify the module compiles
- [x] 3.2 Add tests to `src/test/kotlin/tornadofx/tests/EventBusTest.kt`: repeated subscribe/invalidate cycles keep scope bookkeeping bounded (assert via internal accessor), `EventContext.unsubscribe()` during dispatch removes the scope entry, a shared action lambda across two event types survives unsubscribing from one of them, and valid subscribers still receive events exactly once after cleanup (undocked-during-dispatch subscriber is not invoked) — run `./mvnw test -Dtest=EventBusTest` and confirm green

## 4. Integration verification

- [x] 4.1 Run the full default test suite `./mvnw test` (headless profile) and confirm all tests pass with zero new compiler warnings; confirm no unintended `docs/images/*.png` changes in `git status` before staging

## Workflow follow-up

- Commit as a single change branch (e.g., `fix-upstream-bugs-p1`) and merge via PR; reference upstream issues #1340, #700, #1009 in the PR/commit messages.
- After merge, optionally open upstream PRs against `edvin/tornadofx` with the three fixes and link them in the issues (triage report recommends this; tracked separately).
- Archive the change with `/opsx:archive` once merged (syncs `collection-helpers` and `event-bus` into main specs and updates `ui-controls`).
