# Proposal

## Why

The fork's publishing infrastructure is still wired to infrastructure it cannot use: the POM declares `no.tornado` coordinates, points `distributionManagement` at the retired `oss.sonatype.org` hosts, and references a `sonatype-oss-release` profile that no longer exists — so no release of this fork can ever be published. The maintainer has a verified `io.github.ys-kalyakin` namespace on the Sonatype Central Portal; the project is otherwise release-ready (tests green, docs live, warnings cleaned), so publishing the first fork release to Maven Central under the new namespace is the remaining milestone.

## What Changes

- **BREAKING** — Publication coordinates change from `no.tornado:tornadofx` to `io.github.ys-kalyakin:tornadofx` (artifactId `tornadofx` unchanged). Consumers must update their dependency declarations.
- **BREAKING** — First release under the new namespace is version **2.0.0** (decided in planning): new coordinates plus the already-accumulated breaking changes (JDK 21 toolchain, Kotlin 2.3+ consumer requirement, OSGi removal, HttpClient 5 port) are honestly marked by a major bump. The POM version moves from `1.7.21-SNAPSHOT` to `2.0.0-SNAPSHOT`.
- Publishing channel moves to the **Sonatype Central Portal** via `central-publishing-maven-plugin`; the dead `oss.sonatype.org` `distributionManagement` block and the stale `maven-release-plugin` configuration (with its dangling `-Psonatype-oss-release` argument) are removed. Releases only — no snapshot repository is published (matching the README statement already in place).
- A tag-triggered **GitHub Actions release workflow** publishes a release when a `v*` tag is pushed: it runs the headless test suite as a gate, stamps the release version from the tag, builds and GPG-signs the bundle (jar, sources, javadoc), publishes through the Central Portal, and creates a GitHub Release. A manual `workflow_dispatch` path is kept as fallback.
- **POM project metadata** (url, scm, developers) now describes this fork: `ys-kalyakin/tornadofx` repo links and the maintainer are added; upstream authors and the Apache 2.0 license are retained.
- **Consumer-facing docs** (README badge and install snippets, `docs/getting-started.{en,ru}.md`, `docs/migration.{en,ru}.md`) are updated to the new coordinates and 2.0.0, and the coordinates change is recorded in CHANGELOG's Breaking changes.
- A **local release runbook** documents the one-time prerequisites (GPG key, Central Portal user token, repository secrets) and how to cut a release by tag or locally.

## Capabilities

### New Capabilities

- `release-publishing`: Publishing this fork's releases to Maven Central under the `io.github.ys-kalyakin` namespace — coordinates and POM metadata, the Central-Portal-only release channel, the tag-triggered release workflow with its test gate, and the consumer-facing coordinate documentation.

### Modified Capabilities

- *(none — the documentation-site requirements already mandate that install snippets and the migration section track the project's recorded breaking changes; updating them is implementation against the existing spec)*

## Impact

- **Build**: `pom.xml` — groupId/version, removal of dead `distributionManagement` + `maven-release-plugin` block, addition of `central-publishing-maven-plugin` (0.12.x) and `versions-maven-plugin` (2.24.x, invoked from CI only), url/scm/developers metadata. `maven-gpg-plugin` (already bound to `deploy`) signs release artifacts.
- **CI**: new `.github/workflows/release.yml`; requires repository secrets `CENTRAL_USERNAME`/`CENTRAL_PASSWORD` (Portal user token) and `GPG_PRIVATE_KEY`/`GPG_PASSPHRASE` — one-time setup tasks, not code.
- **External**: artifacts appear in Maven Central under `io.github.ys-kalyakin:tornadofx:2.0.0`; Central Portal namespace verification is already done by the maintainer.
- **Docs**: README.md, CHANGELOG.md, docs/getting-started.{en,ru}.md, docs/migration.{en,ru}.md, plus a new local release runbook.
- Out of scope: any 2.0.1+ cadence policy, snapshot publishing, Gradle/Maven consumer migration tooling beyond doc guidance.
