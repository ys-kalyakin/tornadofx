# Releasing

How to publish a release of this fork to Maven Central under the
`io.github.ys-kalyakin` namespace. The canonical path is automated: push a
`v*` tag and the [release workflow](.github/workflows/release.yml) tests,
signs, and publishes. A manual `workflow_dispatch` and a fully local
`mvn deploy` use the same POM wiring as fallbacks.

## One-time setup

1. **Central Portal namespace** — the `io.github.ys-kalyakin` namespace must be
   verified for your account at <https://central.sonatype.com>
   (Account → Namespaces; the `io.github.<login>` namespace verifies through
   GitHub). Already done for this project.
2. **Portal user token** — on the Portal, open Account → *Generate user
   token*. Keep the two parts of the token (username and password); they are
   not your login credentials.
3. **GPG key** — create one if you have none and publish the public key to a
   keyserver (the Portal validates signatures against public keyservers):

   ```bash
   gpg --full-generate-key                       # RSA/sign-only or ed25519
   gpg --list-secret-keys --keyid-format long    # note the fingerprint
   gpg --keyserver keys.openpgp.org --send-key <FINGERPRINT>
   ```

   **Release signing key (generated 2026-11-08):** fingerprint
   `08BCB465BBE68556DE7E9368A893385C5CFEC2EE`, uid
   `ys-kalyakin (tornadofx release) <ys.kalyakin@gmail.com>` — ed25519 certify
   primary + ed25519 signing subkey, published to `keyserver.ubuntu.com`. The
   fingerprint is pinned in the POM (`maven-gpg-plugin` → `keyname`), so CI and
   local signing always use exactly this key.

4. **Repository secrets** (GitHub → Settings → Secrets and variables →
   Actions), all four are required by the release workflow:

   | Secret             | Value                                                        |
   |--------------------|--------------------------------------------------------------|
   | `CENTRAL_USERNAME` | username part of the Portal user token                       |
   | `CENTRAL_PASSWORD` | password part of the Portal user token                       |
   | `GPG_PRIVATE_KEY`  | `gpg --armor --export-secret-key <FINGERPRINT>`              |
   | `GPG_PASSPHRASE`   | passphrase of the GPG key                                    |

   Confirm with `gh secret list`.

## Publish by tag (canonical)

```bash
git tag v2.0.0 && git push origin v2.0.0
```

The workflow resolves the version from the tag (`v` prefix stripped), stamps
the POM with it (and fails on any mismatch), runs the headless test suite as a
gate, signs and publishes the bundle through the Central Portal
(`autoPublish`), then creates the GitHub Release with generated notes.

Watch the run under *Actions* and the validation state under
<https://central.sonatype.com/publishing>. Supervise the **first** release in
the Portal UI; `autoPublish` moves `VALIDATED` to `PUBLISHED` automatically.

If the Portal rejects the bundle: fix, delete the failed tag and draft
release, re-tag, push again. Central versions are immutable — never re-push
the same version.

## Publish by workflow_dispatch (fallback)

GitHub → Actions → **Release** → *Run workflow*, enter `version` as the
release version **without** the leading `v` (for example `2.0.0`). The gated
path is identical; the `v<version>` tag is created at the dispatched commit
when the GitHub Release step runs.

## Publish locally (fallback)

Prerequisites: the release GPG key in your local keyring and a Portal user
token. Add a `central` server entry to `~/.m2/settings.xml`:

```xml
<server>
  <id>central</id>
  <username><!-- token username --></username>
  <password><!-- token password --></password>
</server>
```

Then run the gate and the deploy:

```bash
./mvnw -B test -Pheadless
./mvnw -B -Pheadless -Dgpg.skip=false deploy
```

`deploy` re-runs the test suite as part of the lifecycle — keep `-Pheadless`
on monitorless machines (the same Monocle profile the CI gate uses). Signing
is skipped by default in the POM (`gpg.skip=true`, so plain `install` needs no
key); release deploys re-enable it with `-Dgpg.skip=false`, which signs the
bundle during the `verify` phase.

`deploy` of a `-SNAPSHOT` version fails fast by design — the Central Portal
does not accept snapshots. Use `./mvnw install` for local development; this
fork publishes no snapshot repository (see the README's *Snapshots* section).

## Verifying the bundle locally (no credentials needed)

```bash
./mvnw -B -DskipTests package                        # jar, -sources.jar, -javadoc.jar in target/
./mvnw -B -DskipTests -Dgpg.skip=false package gpg:sign   # adds a .asc signature next to each
```

## After a release

The workflow never pushes the stamped version back to the repository. Bump the
development version with an ordinary PR after the release (for example
`2.0.0` → `2.0.1-SNAPSHOT` in `pom.xml`).
