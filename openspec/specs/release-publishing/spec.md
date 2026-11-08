# release-publishing Specification

## Purpose

Publishes this fork's releases to Maven Central under the `io.github.ys-kalyakin` namespace, so consumers resolve `io.github.ys-kalyakin:tornadofx` with a complete, signed artifact bundle, driven by a tag-triggered GitHub Actions workflow with a documented local fallback.

## Requirements

### Requirement: Publication coordinates

The project SHALL publish its releases to Maven Central under the coordinates `io.github.ys-kalyakin:tornadofx`, keeping the `tornadofx` artifactId unchanged. The first release under these coordinates SHALL have version 2.0.0, and the development POM version SHALL carry the `-SNAPSHOT` suffix of the next release between releases.

#### Scenario: Consumer resolves the published artifact

- **WHEN** a consumer declares the dependency `io.github.ys-kalyakin:tornadofx:2.0.0`
- **THEN** the artifact resolves from Maven Central

#### Scenario: Old coordinates are retired

- **WHEN** the project's build configuration or consumer-facing documentation is inspected
- **THEN** `no.tornado` no longer appears as this project's publication groupId, and the README badge links to the `io.github.ys-kalyakin` artifact

### Requirement: Release bundle satisfies Central requirements

Every published release SHALL provide, for the same release version: the main jar, a sources jar, a javadoc jar, and the POM, each accompanied by a GPG signature, such that Central Portal validation passes without manual repair.

#### Scenario: Published bundle is complete and signed

- **WHEN** a release version is published to Maven Central
- **THEN** Central hosts the jar, sources jar, javadoc jar, and POM for that version, each with its `.asc` GPG signature

### Requirement: Published metadata describes this fork

The published POM SHALL identify this fork as the project: the project URL and SCM coordinates SHALL point at the fork repository (`ys-kalyakin/tornadofx`), the developer list SHALL include the maintainer of this fork alongside the retained upstream authors, and the Apache 2.0 license entry SHALL be retained.

#### Scenario: POM inspection after publication

- **WHEN** the published POM of a release is inspected
- **THEN** its url and scm entries reference the fork repository, the developers list contains the fork maintainer, and the license is Apache 2.0

### Requirement: Central Portal is the only release channel

Publishing SHALL go through the Sonatype Central Portal, and the build configuration SHALL NOT reference the retired `oss.sonatype.org` hosts. Only release versions SHALL be published; the project SHALL NOT publish to any snapshot repository, consistent with the README's snapshot statement.

#### Scenario: Release deployment targets the Central Portal

- **WHEN** a release version is deployed from the release workflow
- **THEN** the upload goes to the Central Portal publishing endpoint and results in a published release after Portal validation

#### Scenario: Local development is unblocked without release credentials

- **WHEN** a contributor runs `mvn install` on a `-SNAPSHOT` version without GPG keys or Portal credentials
- **THEN** the build completes locally and nothing is published

### Requirement: Tag push triggers a gated release workflow

Pushing a tag matching `v<version>` (for example `v2.0.0`) SHALL trigger a GitHub Actions workflow that runs the headless test suite as a gate, stamps the release version from the tag (tag without the `v` prefix), builds and GPG-signs the release bundle, publishes it through the Central Portal, and creates a GitHub Release for the tag. If the test gate fails, nothing SHALL be published.

#### Scenario: Tag push publishes a release

- **WHEN** the maintainer pushes the tag `v2.0.0`
- **THEN** the workflow runs the headless test suite, and after it passes publishes version `2.0.0` to the Central Portal and creates a GitHub Release for the tag

#### Scenario: Failing tests block publication

- **WHEN** the headless test suite fails during a tag-triggered run
- **THEN** the workflow stops and no artifacts are published for that tag

### Requirement: Manual release dispatch fallback

The release workflow SHALL also be runnable manually via `workflow_dispatch` with the target version as input, following the same gated publish path (tests, signed bundle, Central Portal, GitHub Release).

#### Scenario: Maintainer runs the workflow by hand

- **WHEN** the maintainer dispatches the release workflow for version `2.0.0`
- **THEN** the workflow performs the same gated publish path as a tag push and publishes version `2.0.0`

### Requirement: Local release runbook is documented

The repository SHALL document the release procedure so a release can be produced without reverse-engineering the setup: the one-time prerequisites (GPG key registered with the Central Portal, Portal user token, required repository secrets) and the steps to publish by tag or locally.

#### Scenario: Following the runbook

- **WHEN** the maintainer follows the documented release procedure
- **THEN** they can identify every prerequisite to configure once and the exact commands to publish a release, without inspecting the workflow source
