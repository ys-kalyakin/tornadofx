# Design: add-github-pages-docs

## Context

This fork (`ys-kalyakin/tornadofx`) modernized the framework (JDK 21 / OpenJFX 21 /
Kotlin 2.x, HttpClient 5 REST client, OSGi removed — CHANGELOG `1.7.21-SNAPSHOT`)
but its only documentation entry points are dead hosts (`docs.tornadofx.io`,
`tornadofx.io/dokka`). CI already runs on GitHub Actions (`ci.yml`, headless test
suite). The upstream GitBook guide sources (`edvin/tornadofx-guide`) are Apache 2.0,
the same license as this repository, so content may be adapted with attribution.
The repo root has a `.gitignore` (Maven/IDE entries) without a docs build output
entry yet. See proposal.md for motivation and `specs/documentation-site/spec.md`
for the behavior contract.

## Goals / Non-Goals

**Goals:**

- A compact bilingual (EN default + RU) documentation site maintained in this repo.
- Reproducible builds: pinned Python toolchain, strict MkDocs build, EN/RU page
  parity enforced in CI.
- Deployment to GitHub Pages as a project site from `master` only.
- README updated to point readers at the published site.

**Non-Goals:**

- Full port of the ~20-chapter upstream guide (future changes can extend the site;
  the navigation is stable enough to grow).
- Dokka-generated API reference (KDocs) — the site links to source, not generated
  API docs.
- Custom domain, versioned docs, or search analytics.
- Any change to library code, Maven artifacts, or the existing test workflow.

## Decisions

### D1: MkDocs Material + mkdocs-static-i18n (suffix mode)

- `mkdocs.yml` with `theme: material`, `site_url: https://ys-kalyakin.github.io/tornadofx/`.
- i18n via the `mkdocs-static-i18n` plugin in **suffix** mode: colocated
  `docs/<page>.en.md` and `docs/<page>.ru.md`. `default_language: en`, so English
  is served at `/`, Russian under `/ru/` — matches the spec's default-language and
  parity requirements. Material provides the header language selector and search
  over both languages.
- Alternatives: directory mode (`en/`/`ru/` trees) — separates trees, making
  forgotten translations likelier; VitePress — rejected (user chose MkDocs
  Material; also avoids adding a Node toolchain to a Maven/Kotlin repo); Jekyll —
  rejected (weak RU/EN switching support, GH-native but harder to control).

### D2: Parity enforcement = script + strict build

- A small Python script (`docs/check-parity.py`) compares the sets of `.en.md` and
  `.ru.md` page files and exits non-zero on mismatch (with an explicit allowlist
  for intentionally untranslated assets such as the landing media, if any). This
  directly implements the spec's "build fails if a page is missing from either
  language tree".
- `mkdocs build --strict` fails on warnings, including broken internal nav links.

### D3: Content page set (compact)

One navigation section per topic; every page exists in EN and RU:

1. `index` — what this fork is, current stack, status vs upstream
2. `getting-started` — prerequisites, Maven/Gradle dependency setup, `jvmTarget` 17,
   first App/View, hot reload
3. `views` — View/Fragment, DI (`inject`), scopes
4. `builders` — type-safe builders: layouts, controls, tables/trees
5. `fxml` — FXML support
6. `stylesheets` — type-safe CSS
7. `properties` — observables, binding, `ItemViewModel`/`ViewModel`
8. `async` — `runAsync`/`ui`/`asyncItems`
9. `events` — EventBus with thread targeting
10. `rest` — REST client on HttpClient 5, JSON models, per-request timeouts
11. `workspace` — Workspace and Wizard
12. `validation` — validation context and decorators
13. `migration` — 1.7.20 → 1.7.21 breaking changes (mirrors CHANGELOG "Breaking
    changes": platform baseline, Kotlin 2.x consumer requirement, HttpClient 5,
    OSGi removal, CSS URL handler relocation, `ViewModel.bind` facade types)

Content is written fresh against this codebase (README, CHANGELOG, `src/main/java/tornadofx/`)
and adapted from the upstream guide where useful, with an attribution line.
Every code sample must compile against the current public API (no jcenter-era
snippets, no HttpClient 4, no OSGi).

### D4: Separate `docs.yml` workflow, official Pages actions

- `.github/workflows/docs.yml`: `push` on `master` with `paths` filter
  (`docs/**`, `mkdocs.yml`, `docs/check-parity.py`, `docs/requirements.txt`,
  the workflow file) + `workflow_dispatch`; plus `pull_request` with the same
  paths that builds only (parity check + strict build, no publish).
- Deploy job uses `actions/configure-pages` (with Pages enablement so no manual
  repo setting is required), `actions/upload-pages-artifact` and
  `actions/deploy-pages`; permissions: `pages: write`, `id-token: write`;
  concurrency keyed to `pages` to serialize deploys. This publishes a project
  site under `/tornadofx/` without a `gh-pages` branch.
- Alternatives: fold into `ci.yml` — rejected (Pages permissions and deploy
  concurrency would apply to every test run; separate file keeps the test suite
  untouched); `gh-pages` branch force-push — rejected (legacy, noisy history).

### D5: Pinned tooling

- `docs/requirements.txt` pins exact versions of `mkdocs-material` (which pins its
  own MkDocs range) and `mkdocs-static-i18n`; CI uses `setup-python` (stable
  Python 3.x) + `pip install -r docs/requirements.txt`. Reproducible per the spec;
  local preview is `pip install -r docs/requirements.txt && mkdocs serve`.

### D6: README updates

- "Getting started": Guide/documentation links point to the published Pages URL;
  remove `docs.tornadofx.io` and `tornadofx.io/dokka`; keep alive external links
  (screencasts, Slack, Stack Overflow); mark the upstream wiki as legacy 1.7.x
  documentation. Also fix the stale Gradle `jvmTarget` snippet shown in README
  (`1.8` → `17`) while touching the file.

## Risks / Trade-offs

- [Upstream guide sources cannot be fetched from this machine (network restrictions)]
  → Content is written fresh from README/CHANGELOG/source; upstream is reference
  only. Attribution still applies to adapted phrasing; if nothing is fetched, no
  attribution is needed.
- [RU translation effort grows if pages accumulate]
  → Compact page set (13 pages); parity script gates merges so drift is caught
  immediately, not by audit.
- [GitHub Pages not yet enabled on the repository]
  → Deploy job runs `actions/configure-pages` with Pages enablement; if the org/repo
  policy still blocks it, the workflow fails visibly on first run and Pages must be
  enabled once in repo settings (documented in the workflow comments).
- [Documentation rot as the framework evolves]
  → Non-goal to solve fully; the docs workflow runs on master so content edits ship
  the same way code does. `migration` page is anchored to CHANGELOG, which the
  project already maintains.

## Migration Plan

No runtime migration. Deployment order within the change: add docs sources and
workflow, merge, first run of `docs.yml` enables and publishes Pages; then update
README links in the same change. Rollback = revert the workflow file and disable
the Pages deployment; the site is stateless and regenerable.

## Open Questions

None — user decisions recorded in the proposal (compact scope, MkDocs Material,
English default).
