# TornadoFX

TornadoFX is a JavaFX framework for Kotlin: zero-config application classes,
type-safe UI and CSS builders, dependency injection, async task execution, an
event bus with thread targeting, and a REST client with JSON conversion.

This project is a maintained fork of the original
[edvin/tornadofx](https://github.com/edvin/tornadofx) (Apache License 2.0),
modernized onto the current Java stack:

- **Build toolchain:** JDK 21; published bytecode is Java 17
- **JavaFX:** OpenJFX 21, declared as Maven dependencies with platform classifiers
- **Kotlin:** 2.x (K2), `jvmTarget` 17
- **REST client:** Apache HttpClient 5
- **Tests:** JUnit 5, headless (Monocle) by default in CI

OSGi support was removed together with the Java 8/JavaFX 8 era stack — OSGi
consumers should stay on the 1.7.x line. See [Migration from 1.7.20](migration.md)
for every breaking change.

## What the framework gives you

- Both MVC and MVP and their derivatives
- Dependency injection and scopes
- Type-safe GUI builders
- Type-safe CSS builders
- First class FXML support
- Async task execution
- EventBus with thread targeting
- Hot reload of Views and Stylesheets
- REST client with automatic JSON conversion
- Zero config, no XML, no annotations

## Documentation

1. [Getting started](getting-started.md) — prerequisites, dependency setup, first app
2. [Views & DI](views.md) — View, Fragment, scopes, `inject`
3. [Type-safe builders](builders.md) — layouts, controls, tables
4. [FXML](fxml.md) — FXML-backed views
5. [Type-safe stylesheets](stylesheets.md) — CSS in Kotlin
6. [Properties & binding](properties.md) — observables, ViewModels
7. [Async](async.md) — background work on the UI thread
8. [EventBus](events.md) — typed events with thread targeting
9. [REST client](rest.md) — HttpClient 5 based client with JSON models
10. [Workspace & Wizard](workspace.md) — application shell and multi-step flows
11. [Validation](validation.md) — field validation and decorators
12. [Migration from 1.7.20](migration.md) — breaking changes in this fork

## More resources

- [Screencasts](https://www.youtube.com/user/MrEdvinsyse) (the original author's series)
- [Kotlinlang Slack `#tornadofx`](https://kotlinlang.slack.com/messages/tornadofx/details/)
- [Stack Overflow `tornadofx`](http://stackoverflow.com/questions/tagged/tornadofx)
- [Example applications](https://github.com/edvin/tornadofx-samples) (1.7.x era, may need stack updates)
- [IDEA plugin](https://github.com/edvin/tornadofx-idea-plugin)
- [Project change log](https://github.com/ys-kalyakin/tornadofx/blob/master/CHANGELOG.md)
