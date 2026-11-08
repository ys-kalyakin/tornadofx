# Design

## Context

The POM (see `pom.xml`) currently publishes as `no.tornado:tornadofx:1.7.21-SNAPSHOT`, keeps `distributionManagement` pointed at the retired `oss.sonatype.org` hosts, and configures `maven-release-plugin` with `-Psonatype-oss-release` — a profile that does not exist. The `maven-gpg-plugin` is already bound to the `deploy` phase, and `maven-source-plugin` / `maven-javadoc-plugin` already run in the main build, so a complete signed bundle is mostly wired. The maintainer's `io.github.ys-kalyakin` namespace on the Sonatype Central Portal is verified. CI runs the headless test suite on JDK 21 (Corretto) via `.github/workflows/ci.yml`. The README's snapshot section already states that no snapshot repository is published. Planning decisions: first release version **2.0.0**, release mechanics **tag push + GitHub Actions** with a manual dispatch fallback.

## Goals / Non-Goals

**Goals:**

- One canonical publish path: tag push → gated workflow → Central Portal, with a local fallback using the same POM wiring.
- POM that passes Central Portal validation with no manual repair.
- Consumer docs and migration guide reflecting the new coordinates and 2.0.0.

**Non-Goals:**

- Snapshot publishing to any remote repository (local `install` remains the dev workflow).
- Release cadence policy, 2.0.1+ planning, or automating CHANGELOG authorship.
- Generating a starter archetype for the fork; the upstream archetype links stay but are labeled as upstream resources.

## Decisions

### D1 — Coordinates and version stamping

`groupId` → `io.github.ys-kalyakin`, `artifactId` stays `tornadofx`, POM version → `2.0.0-SNAPSHOT`. The release workflow stamps the version from the tag with `versions-maven-plugin:2.24.0` (`versions:set -DnewVersion=<tag-minus-v> -DgenerateBackupPoms=false`), invoked directly by coordinates — no POM entry needed.

*Alternatives:* CI-friendly `${revision}` property — cleaner immutability but requires `flatten-maven-plugin` and touches every consumer-facing POM detail for little gain; `maven-release-plugin` (`release:prepare/perform`) — already half-configured but its `prepare` dance (two commits, tag, branch juggling) is awkward inside Actions, and its current config references the dead profile. Stamping from the tag keeps a single source of truth: the tag.

### D2 — Publishing through `central-publishing-maven-plugin`

Add `org.sonatype.central:central-publishing-maven-plugin:0.12.0` to `<build><plugins>` with `<extensions>true</extensions>`, `<publishingServerId>central</publishingServerId>`, `<autoPublish>true</autoPublish>`. It replaces the default deploy participant, uploads a bundle, waits for Portal validation, and triggers publication. The dead `distributionManagement` block and the stale `maven-release-plugin` `pluginManagement` entry are removed. Snapshots are not supported by the Portal: a stray `deploy` of a `-SNAPSHOT` version fails fast with a clear plugin error — acceptable, since `install` is the local workflow and it never triggers the deploy phase.

*Alternatives:* plain `mvn deploy` against the Portal REST endpoint via `altDeploymentRepository` — loses the plugin's client-side bundle assembly and validation polling; keeping `distributionManagement` pointing at the Portal — same minus polling, and keeps a second, confusing channel.

### D3 — Signing with the existing `maven-gpg-plugin`

Keep the existing `deploy`-phase binding; add `<gpgArguments>` with `--pinentry-mode loopback` so non-interactive CI signing works. In Actions, `actions/setup-java` imports the key from the `GPG_PRIVATE_KEY` secret via its `gpg-private-key` input and exposes `GPG_PASSPHRASE`; the workflow maps it to the `MAVEN_GPG_PASSPHRASE` environment variable that gpg-plugin 3.2.x reads. Locally, the agent handles the passphrase.

*Alternatives:* signing in a separate CI step with raw `gpg` CLI — duplicates what the plugin already does and needs `maven.deploy.skip` gymnastics; in-POM passphrase — secret leakage, rejected.

### D4 — Release workflow shape (`.github/workflows/release.yml`)

Triggers: `push` on tags `v*` and `workflow_dispatch` with a `version` input (tag name without `v`). Single job, `permissions: contents: write`:

1. Checkout (with tag), `actions/setup-java@v4` — Corretto 21, `cache: maven`, `gpg-private-key`/`gpg-passphrase` from secrets.
2. Resolve version (dispatch input or `${GITHUB_REF_NAME#v}`), run `versions:set`, assert the POM now carries the release version (fails on tag/typo mismatch).
3. Gate: `./mvnw -B test -Pheadless` — same suite and profile as `ci.yml`.
4. Generate `~/.m2/settings.xml` inline with a `central` server entry (Portal user token from `CENTRAL_USERNAME`/`CENTRAL_PASSWORD` secrets); run `./mvnw -B -ntp deploy`.
5. Create the GitHub Release for the tag (action pinned by SHA or major tag) with auto-generated release notes.

No matrix, no staging environment: one Ubuntu job mirrors `ci.yml`.

### D5 — POM project metadata

`<url>` and `<scm>` → `https://github.com/ys-kalyakin/tornadofx` (https connection; the current `git@github.com:` SSH form is unusual for published POMs). `<developers>`: keep the three upstream authors (they wrote the code, Apache 2.0 obligations) and add the fork maintainer (`ys-kalyakin`, GitHub noreply email `ys-kalyakin@users.noreply.github.com` — replaceable at implementation if the maintainer prefers another address). Drop the project-level `<organization>SYSE</organization>`: it misattributes the fork. `<name>` and `<description>` stay (description is a Central validation requirement and already present).

### D6 — Documentation updates

- **README**: Maven Central badge and search link → `io.github.ys-kalyakin/tornadofx`; Maven/Gradle snippets → `io.github.ys-kalyakin:tornadofx:2.0.0`; the Maven-archetype quickstart is replaced by the plain dependency snippets with upstream archetype/wiki links kept and explicitly labeled as upstream (the upstream archetype generates `no.tornado` projects that this release does not serve).
- **`docs/getting-started.{en,ru}.md`**: dependency snippets → new GA and `2.0.0` (they currently show `no.tornado:...:1.7.21-SNAPSHOT`).
- **`docs/migration.{en,ru}.md`**: new section for the coordinates change under the existing breaking-changes walkthrough (name change → same package `tornadofx.*`, only Maven coordinates move).
- **`CHANGELOG.md`**: the `1.7.21-SNAPSHOT` section becomes `[2.0.0]` and gains the coordinates/version breaking change.
- **`RELEASING.md`** (repo root, maintainer-facing, deliberately not part of the consumer docs site): one-time setup (create/verify Portal namespace, Portal user token, GPG key + publication to a keyserver, repository secrets `CENTRAL_USERNAME`, `CENTRAL_PASSWORD`, `GPG_PRIVATE_KEY`, `GPG_PASSPHRASE`) and the two publish paths (tag push; `workflow_dispatch`; local `deploy` with credentials).

### D7 — First release is supervised, not automated blind

The apply phase lands code/config/docs and proves the bundle locally (`package` producing jar/sources/javadoc, and gpg signing if a test key is present). The actual 2.0.0 publication happens once, supervised: maintainer pushes `v2.0.0`, watches the Portal validation state in the Portal UI. Only after that succeeds is the channel considered proven.

## Risks / Trade-offs

- [Portal validation rejects the bundle (e.g., javadoc jar or signature quirks)] → The bundle is proven locally first (jar/sources/javadoc present, `gpg:sign` output checked); the first release is supervised per D7 so a rejection is fixed and the tag re-cut; Central rejects duplicate versions, so a repaired release needs a re-tag, never a re-push.
- [Accidental `mvn deploy` on a snapshot fails mid-phase with a Portal error] → Documented in RELEASING.md; `install` remains the local workflow and is unaffected; the plugin's fast failure is itself the guard against silently publishing nothing.
- [GPG passphrase plumbing in CI is finicky] → Use the setup-java `gpg-private-key` input plus the `MAVEN_GPG_PASSPHRASE` env var and `--pinentry-mode loopback` — the standard combination; a workflow dry-run against a draft (Portal "publishing" state not confirmed) can be exercised before autoPublish is trusted.
- [`versions:set` on a mistyped tag (e.g., `v2.0.0-rc`) stamps a non-release-looking version] → The version assertion step right after stamping makes any mismatch fail before tests run; pre-release suffixes like `-rc1` are legal for the Portal (validated as pre-release), so this is a naming convention issue, not a blocker.
- [Keeping upstream developers in the POM may look odd for a fork] → Retaining authorship is the honest attribution for an Apache 2.0 fork and satisfies the Portal's developer requirement; the maintainer entry makes the publisher clear.
