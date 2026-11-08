# platform-compatibility Specification

## Purpose

Defines the platform baselines under which TornadoFX is built and consumed on the modern stack: JDK, OpenJFX, Kotlin, and the prohibition of JDK-internal APIs in shipped code.

## Requirements

### Requirement: Modern platform baseline
The framework SHALL build with a JDK 21 toolchain, compile against OpenJFX 21, and publish bytecode that a JDK 17+ runtime can load. The framework SHALL be built with Kotlin 2.x.

#### Scenario: Build on JDK 21
- **WHEN** the project is built with a JDK 21 toolchain
- **THEN** the build succeeds and the produced jar contains Java 17 class files

#### Scenario: Consumer on the new stack
- **WHEN** an application runs on a JDK 17+ runtime with JavaFX 21 and the built TornadoFX artifact on its classpath
- **THEN** framework components (App, View, layouts) initialize and render without linking errors

### Requirement: OpenJFX as explicit dependency
The build SHALL declare the OpenJFX 21 modules it compiles against (base, graphics, controls, fxml, media, web) as project dependencies instead of relying on a JDK-bundled JavaFX.

#### Scenario: Dependency declaration resolves from Maven Central
- **WHEN** the project is built from a clean environment with only Maven Central configured
- **THEN** all compile dependencies, including the OpenJFX modules, resolve successfully and no jcenter repository is referenced

### Requirement: No JDK-internal APIs in shipped code
Main sources MUST NOT import `sun.*`, `com.sun.glass.*`, `com.sun.xml.internal.*`, or other JDK-internal packages, and MUST NOT require `--add-exports`/`--add-opens` of JDK-internal packages to compile or run. Use of JavaFX implementation packages (`com.sun.javafx.*`) is permitted only where no public equivalent exists and MUST be confined to `tornadofx` namespaces.

#### Scenario: Clean compile without internal access flags
- **WHEN** the main sources are compiled on JDK 21 without `--add-exports` or `--add-opens` flags for `sun.*`, `com.sun.glass.*`, or `com.sun.xml.internal.*` packages
- **THEN** compilation succeeds

#### Scenario: No internal classes shipped
- **WHEN** the built jar is inspected
- **THEN** it contains no class files under a `sun.` package root

### Requirement: Warning-clean main sources
Main sources SHALL compile with zero compiler warnings under the pinned JDK 21
/ Kotlin 2.x toolchain. Main sources MUST NOT contain constructs that the Kotlin
language committee has scheduled to become compile errors (progression
warnings), so that upgrading the Kotlin compiler does not break the build.
Fixing warnings MUST NOT change the public API or observable runtime behavior.

#### Scenario: Zero-warning build
- **WHEN** the main sources are compiled with the pinned toolchain using the project's standard build
- **THEN** the compiler emits no warnings for the main source set

#### Scenario: Ready for the next Kotlin language version
- **WHEN** the main sources are compiled with Kotlin progressive mode enabled, which turns scheduled language changes into errors
- **THEN** compilation succeeds
