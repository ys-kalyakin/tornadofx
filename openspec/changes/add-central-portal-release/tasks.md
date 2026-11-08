# Tasks

## 1. POM coordinates and project metadata

- [x] 1.1 Change `groupId` to `io.github.ys-kalyakin` and `version` to `2.0.0-SNAPSHOT` in `pom.xml`; verify with `./mvnw -B -q help:evaluate -Dexpression=project.groupId -DforceStdout` (and `project.version`) printing the new values
- [x] 1.2 Point `<url>` and `<scm>` at `https://github.com/ys-kalyakin/tornadofx` (https connection form), drop the `<organization>SYSE</organization>` block, and add the fork maintainer to `<developers>` (keep the three upstream authors); verify `./mvnw -B -q help:effective-pom` shows the new url/scm/developers
- [x] 1.3 Remove the `distributionManagement` block (retired `oss.sonatype.org`) and the `maven-release-plugin` `pluginManagement` entry with its dangling `-Psonatype-oss-release` argument; verify `grep -c "oss.sonatype.org" pom.xml` is `0`

## 2. Central Portal publishing wiring

- [x] 2.1 Add `org.sonatype.central:central-publishing-maven-plugin:0.12.0` with `<extensions>true</extensions>`, `<publishingServerId>central</publishingServerId>`, `<autoPublish>true</autoPublish>`; verify `./mvnw -B -q validate` passes (plugin resolves) and the build is otherwise unchanged
- [x] 2.2 Add `<gpgArguments>` with `--pinentry-mode loopback` to the existing `maven-gpg-plugin` configuration; verify `./mvnw -B -q validate` passes
- [x] 2.3 Prove the local, credential-free path: `./mvnw -B install -DskipTests` succeeds and `./mvnw -B -DskipTests package` leaves the main jar, sources jar, and javadoc jar in `target/` (no deploy attempted, no GPG required)

## 3. Release workflow

- [x] 3.1 Create `.github/workflows/release.yml` per design D4: `push` on tags `v*` plus `workflow_dispatch` with a `version` input; setup-java (Corretto 21, maven cache, `gpg-private-key`/`gpg-passphrase` from secrets); version resolution + `versions:set` + POM version assertion; `./mvnw -B test -Pheadless` gate; inline `~/.m2/settings.xml` with the `central` server from `CENTRAL_USERNAME`/`CENTRAL_PASSWORD`; `./mvnw -B -ntp deploy`; GitHub Release creation; `permissions: contents: write`; verify the YAML parses (actionlint if available, otherwise a YAML parser) and the job structure matches D4
- [x] 3.2 Cross-check the workflow against `ci.yml` so the gate uses the same test invocation (`./mvnw -B test -Pheadless`) and JDK; verify by diffing the test step

## 4. Consumer docs, changelog, runbook

- [x] 4.1 README: badge and search link → `io.github.ys-kalyakin/tornadofx`; Maven and Gradle install snippets → `io.github.ys-kalyakin:tornadofx:2.0.0`; replace the Maven-archetype quickstart with the plain dependency snippets, keeping upstream archetype/wiki links labeled as upstream; verify `grep -n "no.tornado" README.md` shows only the clearly-labeled upstream references (and the badge/snippets use the new GA)
- [x] 4.2 `docs/getting-started.en.md` and `docs/getting-started.ru.md`: dependency snippets → `io.github.ys-kalyakin:tornadofx:2.0.0` (they currently show `no.tornado:...:1.7.21-SNAPSHOT`); verify `grep -rn "no.tornado\|1.7.21-SNAPSHOT" docs/getting-started.*.md` is empty
- [x] 4.3 `docs/migration.en.md` and `docs/migration.ru.md`: add a "coordinates change" section to the breaking-changes walkthrough (only Maven coordinates move; package names stay `tornadofx.*`); verify both language files contain the section
- [x] 4.4 `CHANGELOG.md`: rename the `1.7.21-SNAPSHOT` section to `[2.0.0]` and add the coordinates/version breaking change to its "Breaking changes" subsection; verify the section renders as the top release (user-approved extra: merged and removed the pre-existing `[1.7.21-SNAPSHOT] (pre-existing duplicate entry)` section, folding its Fixed item into `[2.0.0]` as [#1163])
- [x] 4.5 Create `RELEASING.md` (repo root, maintainer-facing): one-time setup (Portal namespace check, Portal user token, GPG key creation + keyserver publication, repository secrets `CENTRAL_USERNAME`, `CENTRAL_PASSWORD`, `GPG_PRIVATE_KEY`, `GPG_PASSPHRASE`), the tag-push path, the `workflow_dispatch` path, and the local `deploy` path including the snapshot-deploy-fails-fast note; verify a reader can enumerate every prerequisite and command without opening the workflow file

## 5. Integration verification

- [x] 5.1 Run `./mvnw -B test -Pheadless` (default suite) green on the final POM, and `grep -rn "oss.sonatype.org\|sonatype-oss-release" pom.xml .github/` returns nothing
- [x] 5.2 If a throwaway GPG key is available, do a local signing dry run (`./mvnw -B -DskipTests package gpg:sign`) and confirm `.asc` files appear next to jar/sources/javadoc in `target/`; otherwise record in the change summary that signing is only proven in CI (done: the maintainer's key `ys-kalyakin <ys.kalyakin@gmail.com>` is present locally; all four artifacts signed with `.asc`)

## Workflow follow-up

- Release credentials: maintainer set `CENTRAL_USERNAME`/`CENTRAL_PASSWORD`; dedicated release GPG key generated 2026-11-08 (fingerprint `08BCB465BBE68556DE7E9368A893385C5CFEC2EE`, sign subkey `5F030B6448CF1A9A179A229CC1C97BEBFA87D627`), pinned in the POM (`keyname`), public half published to keyserver.ubuntu.com, `GPG_PRIVATE_KEY`/`GPG_PASSPHRASE` set via `gh secret set` and the export re-verified in a clean GNUPGHOME (import + sign). The maintainer is rotating the passphrase to a personal one — after that, both GPG secrets must be re-set (see RELEASING.md).
- Merge the change to `master` via PR; confirm `ci.yml` and the docs deploy stay green.
- Cut the first release per RELEASING.md: push `v2.0.0`, supervise Central Portal validation (design D7), confirm the artifact resolves from Maven Central and the README badge shows the published version.
- Archive the change after the release path is proven (or after review if the maintainer prefers to publish later); sync the `release-publishing` spec on archive.
