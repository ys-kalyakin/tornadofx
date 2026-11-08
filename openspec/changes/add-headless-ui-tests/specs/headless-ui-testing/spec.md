# Spec Delta

## Purpose

Lets the JavaFX UI test suite run on machines without a display (local container or CI runner) by using the headless glass platform bundled with JavaFX, keeps flaky and environment-dependent tests out of the default run through a quarantine tag, and makes CI enforce the headless suite on every change.

## ADDED Requirements

### Requirement: UI test suite runs without a display

The test suite SHALL pass on a machine with no display server and no windowing system, with tests that start the JavaFX toolkit running against a headless glass platform provided on the test classpath.

#### Scenario: Full suite on a monitorless machine

- **WHEN** the default test suite is run on a machine with no display server
- **THEN** the build succeeds and all non-quarantined tests pass, including tests that start the JavaFX toolkit

#### Scenario: Displayed workstation unaffected

- **WHEN** the default test suite is run on a workstation with a display server and without headless properties enabled
- **THEN** the suite passes as before

### Requirement: Default run excludes quarantined tests

The default test invocation SHALL NOT execute tests marked with the quarantine tag, and a dedicated test invocation SHALL run exactly the quarantined tests.

#### Scenario: Quarantined test skipped by default

- **WHEN** a test is marked with the quarantine tag and the default test suite is run
- **THEN** that test does not execute and the suite can still pass

#### Scenario: Quarantine-only run

- **WHEN** the test suite is run in quarantine mode
- **THEN** only tests marked with the quarantine tag execute

### Requirement: Quarantine admission policy

Only tests that are flaky (fail intermittently without any related product change) or environment-dependent (depend on OS facilities unavailable without a display server, such as the system clipboard) SHALL be tagged quarantine. A test failing deterministically because of a framework defect MUST be fixed in framework code and MUST NOT be quarantined.

#### Scenario: Deterministic framework failure is not quarantined

- **WHEN** a test fails deterministically under the headless platform because framework code is broken
- **THEN** the framework code is fixed so the test passes, and the test carries no quarantine tag

#### Scenario: Flaky test is quarantined

- **WHEN** a test fails intermittently on repeated runs with no related product change
- **THEN** the test is tagged quarantine and the default suite is green again

### Requirement: Headless configuration is test-only

The headless test configuration SHALL affect only test execution. The published framework artifact, its declared dependencies, and its consumer-facing behavior MUST NOT change between a build made with and without the headless configuration.

#### Scenario: Artifact unchanged

- **WHEN** the project is built with and without the headless test configuration
- **THEN** the produced framework artifact and its dependency list are identical

### Requirement: CI runs the headless suite

The project's CI pipeline SHALL run the default test suite on a monitorless runner for every push and pull request, and SHALL fail when the suite fails.

#### Scenario: Regression fails CI

- **WHEN** a push introduces a change that makes a non-quarantined test fail headless
- **THEN** the CI run for that push fails

#### Scenario: Clean change passes CI

- **WHEN** a push keeps the default suite green headless
- **THEN** the CI run for that push succeeds
