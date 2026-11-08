# Headless baseline triage (tasks 1.2 / 2.1)

Baseline run: full default suite under the `headless` profile (Monocle, software prism),
display server unavailable: `env -u DISPLAY -u WAYLAND_DISPLAY ./mvnw test -Pheadless`.

**Result: 385 tests, 1 failure, 0 errors, 0 skipped.**

All 23 toolkit-starting test classes ran under the headless glass platform.
Only one class failed; everything else — including `ClipboardTest` (system clipboard has an
in-memory Monocle implementation), `AsyncTest`, `MultipleLifecycleAsyncAppTest`, `StylesheetErrorTest`,
`FXMLTest`, `ViewDockingTests` — passed.

## The one failure

`CustomControlsTest.datagrid_renders_items_and_fires_selection_on_click:46`
`A cell should be selected after a click ==> expected: <false> but was: <true>`

**Classification (design D4): test-code assumption, not a framework defect, not environment-dependent.**

Mechanism (verified with a temporary probe test, since removed):

1. The suite shares one JVM-wide toolkit and one cached primary stage across classes.
2. When a previously run class has already shown the primary stage (isolated reproducer:
   `ChildInterceptorTest` registers a full TornadoFX `App` on it in `@BeforeAll`), a later
   `FxToolkit.setupFixture` that swaps the scene leaves the stage with an **undefined position**
   (`stage.x == NaN`) under Monocle. With a real windowing system (GTK) the position is always
   defined, which is why the displayed run stayed green.
3. `FxRobot.clickOn(cell)` maps the cell through `localToScreen`, which yields NaN bounds, so the
   synthetic click is delivered nowhere: `MOUSE_PRESSED` never reached the cell.
4. The DataGrid selection logic itself is healthy: a directly delivered `MOUSE_PRESSED` selected
   the item in the same broken state.

**Fix (task 2.3):** `CustomControlsTest` now pins `primaryStage.x/y` in `setupFixture` before
showing, so the robot always has defined screen coordinates. Assertions unchanged.

**Quarantine candidates from baseline: none.**

Task 3.3 is therefore a deliberate no-op: `ClipboardTest` passes headless (Monocle provides an
in-memory clipboard implementation), and no test failed intermittently. The mechanism itself is
verified with a temporary tagged test (removed afterwards): the default run reports
`Tests run: 0, BUILD SUCCESS` for a quarantined test, and `mvn test -Pquarantine` executes it.
The first future flaky or environment-dependent test gets `@Tag("quarantine")` per design D4.

## Notes

- Bisection path: full-suite failure → predecessor-prefix subset → first half → `{BuildersTest, ChildInterceptorTest}` → `ChildInterceptorTest` + `CustomControlsTest` (red), while every other single predecessor was green.
- No deterministic framework-side (skins/CSS/events) failure surfaced, so task 2.2 has no targets under the current baseline.
- Artifact identity (task 5.1): the jar is not byte-reproducible (zip entry timestamps differ on
  every build, even same-profile back-to-back), so identity was verified by content: `mvnw
  dependency:list` with and without `-Pheadless` is identical, and an extracted-contents
  `diff -rq` of the two jars is empty (class files byte-identical).
