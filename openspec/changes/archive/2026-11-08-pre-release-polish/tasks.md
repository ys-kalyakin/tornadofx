# Tasks

Verification baseline: `JAVA_HOME=$(echo /home/helloween/tools/amazon-corretto-21*) ./mvnw -B clean compile 2>&1 | grep -c "^\[WARNING\]"` reports the main-source warning count (62 at exploration time, one of which is the javadoc block). The full suite is `./mvnw -B test -Pheadless` (385 tests, all green at exploration).

## 1. Progression warnings (scheduled future compile errors)

- [x] 1.1 `ItemControls.kt:70` — remove the always-true `!is DoubleProperty` / `!is FloatProperty` guards on the `IntegerProperty` receiver (KTLC-365, both warnings); verify the two `Check for instance is always 'true' ... error in language version 2.4` warnings no longer appear in the compile output
- [x] 1.2 `Json.kt:268` — add the explicit return type the compiler asks for (KTLC-288); verify the `error in language version 2.5` warning is gone

## 2. `Class.newInstance()` replacement (10 sites)

- [x] 2.1 Add one internal helper that performs no-arg construction via `getDeclaredConstructor().newInstance()`, unwraps `InvocationTargetException` and rethrows its cause so callers keep today's exception types (design D2); verify it compiles and existing instantiation tests pass
- [x] 2.2 Apply the helper at all `Class.newInstance()` sites: `App.kt:121`, `FX.kt:286`, `FX.kt:416`, `FX.kt:431`, `Json.kt:88`, `Json.kt:155`, `Json.kt:320`, `Messages.kt:34`, `Rest.kt:506`, `css/Handler.kt:20`; verify each `newInstance() ... is deprecated. Deprecated in Java` warning is gone from the compile output
- [x] 2.3 Run the affected test areas (`App`, `FX`, `Json`, `Rest`, CSS handler, `Messages`/resource-bundle tests) via `./mvnw -B test -Pheadless` and confirm green

## 3. `URL(String)` replacement (4 sites)

- [x] 3.1 Replace the deprecated `URL(String)` constructor at `CSS.kt:463`, `CSS.kt:491`, `FX.kt:370`, `InternalWindow.kt:136` with a `URI(...).toURL()` path that maps `URISyntaxException` to `MalformedURLException` so the existing catch sites (`CSS.kt:464`, `FX.kt:371`) keep working (design D3); verify the four `constructor(p0: String!): URL` warnings are gone and stylesheet-loading / FX tests pass

## 4. Remaining Java-deprecated APIs

- [x] 4.1 `Messages.kt:14` — drop the `AccessController.doPrivileged` wrapper: on JDK 17+ (the framework baseline) it is a pass-through and `java.security.AccessController` is deprecated for removal; remove the private `doPrivileged` helper and call the actions directly; verify the `class AccessController is deprecated` warning is gone and resource-bundle tests pass
- [x] 4.2 `Rest.kt:453` — migrate `client.execute(request, engine.context)` off the HttpClient 5 deprecated two-arg overload: use a non-deprecated `execute` variant; if only the response-handler form exists, run the current wrap/intercept logic inside the handler and make sure the returned response's body stays readable afterwards (buffer the entity inside the handler if needed); verify the `execute ... is deprecated` warning is gone and the full Rest test area passes headless
- [x] 4.3 Replace `String.decapitalize()` with `replaceFirstChar(...)` at `ItemControls.kt:387`, `ItemControls.kt:407`, `Properties.kt:87`, `Properties.kt:126`; verify the four warnings are gone
- [x] 4.4 Replace `sumByDouble` with `sumOf` at `Keyboard.kt:121`, `Keyboard.kt:126`, `ListMenu.kt:133`, `SmartResize.kt:452`; verify the four warnings are gone

## 5. Code-quality warnings

- [x] 5.1 Remove redundant casts (`Binding.kt:125`, `Binding.kt:126`), the always-true `is` check (`FX.kt:281`), the unnecessary safe call (`FX.kt:427`), and the insignificant `inline` (`CSS.kt:1451`); verify these five warnings are gone
- [x] 5.2 Remove always-left Elvis operands at `Forms.kt:66`, `Keyboard.kt:116`, `Keyboard.kt:122`, `Keyboard.kt:127`, `ListMenu.kt:135`, `SqueezeBox.kt:66`, `SqueezeBox.kt:71`; verify the seven warnings are gone
- [x] 5.3 Replace `java.lang.Integer`/`Long`/`Double`/`Float`/`Boolean`/`String` references with `kotlin.Int`/`Long`/`Double`/`Float`/`Boolean`/`String` at `ItemControls.kt:71`, `Json.kt:308`, `Json.kt:333`, `Json.kt:334`, `Json.kt:366`, `Nodes.kt:731`, `ViewModel.kt:127`–`132`; verify the twelve "not recommended for use in Kotlin" warnings are gone
- [x] 5.4 Remove redundant spread operators (`Validation.kt:95`, `ViewModel.kt:66`, `ViewModel.kt:219`, `ViewModel.kt:233`) and redundant conversion calls (`SmartResize.kt:509`, `SmartResize.kt:555`); verify the six warnings are gone

## 6. README correction

- [x] 6.1 Replace the "Snapshots are published to Sonatype" section in `README.md` with a short statement that this fork does not publish snapshot repositories (design D5); verify no remaining reference to `oss.sonatype.org` in README and that the section no longer claims daily snapshot publishing

## 7. Zero-warning verification (integration)

- [x] 7.1 Full clean build: `./mvnw -B clean compile` emits no `[WARNING]` lines for the main source set (javadoc warning for `JavaDocTrap.java` resolved by adding the missing javadoc comment); record the before/after warning count in the task notes
- [x] 7.2 Full test suite green: `./mvnw -B test -Pheadless` passes all tests with the warning fixes applied
- [x] 7.3 Progression check per design D6: compile once with `-progressive` passed to kotlin-maven-plugin args (temporary edit, reverted after the check); if the pinned compiler does not promote the diagnostics, record the fallback check — the three progression warnings from task 1 are absent from the normal compile output

## Workflow follow-up

- Archive the change after review; at release time add a short note to the 1.7.21 CHANGELOG section about warning-free main sources and the README correction.

- Verification note: main-source compiler warnings 62 (57 Kotlin + javadoc block) before the change, 0 after; full headless suite 385/385 green; `-progressive` compile succeeds with the temporary pom edit reverted.
