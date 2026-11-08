# Tasks

## 1. Site scaffolding

- [x] 1.1 Create `mkdocs.yml`: Material theme, `mkdocs-static-i18n` plugin in suffix mode with `default_language: en`, `site_url: https://ys-kalyakin.github.io/tornadofx/`, single shared `nav` covering the 13 pages from design D3. Verify: `mkdocs build` succeeds on a scaffold with placeholder pages.
- [x] 1.2 Create `docs/requirements.txt` with exact pinned versions of `mkdocs-material` and `mkdocs-static-i18n`. Verify: `pip install -r docs/requirements.txt` succeeds and `mkdocs --version` runs.
- [x] 1.3 Add `site/` to `.gitignore`. Verify: after a local `mkdocs build`, `git status --short` shows no build output.

## 2. Language parity checker

- [x] 2.1 Create `docs/check-parity.py`: compares `.en.md`/`.ru.md` page sets under `docs/`, exits non-zero on any page missing a counterpart, supports an explicit allowlist for intentional exceptions. Verify: run against a deliberately unpaired temp file (fails with the page name) and against the full set (passes).

## 3. English content (default language)

- [x] 3.1 Write `docs/index.en.md` and `docs/getting-started.en.md`: fork overview + current stack, prerequisites, Maven/Gradle setup with `jvmTarget` 17, first App/View, hot reload. Verify: examples match the current README and build (`mvnw`/pom) facts; `mkdocs build` renders them.
- [x] 3.2 Write `views.en.md`, `builders.en.md`, `fxml.en.md`, `stylesheets.en.md`: View/Fragment/DI, type-safe builders, FXML, type-safe CSS. Verify: code samples use only APIs present in `src/main/java/tornadofx/`; `mkdocs build` renders them.
- [x] 3.3 Write `properties.en.md`, `async.en.md`, `events.en.md`: observables/binding/ItemViewModel, `runAsync`/`ui`/`asyncItems`, EventBus. Verify: code samples compile against current sources; `mkdocs build` renders them.
- [x] 3.4 Write `rest.en.md`, `workspace.en.md`, `validation.en.md`: REST client on the HttpClient 5 API (including `AuthContext` and per-request timeout), Workspace/Wizard, validation. Verify: REST section uses no HttpClient 4 types; `mkdocs build` renders them.
- [x] 3.5 Write `migration.en.md` covering every entry in CHANGELOG `1.7.21-SNAPSHOT` → "Breaking changes" (platform baseline, Kotlin 2.x consumer requirement, HttpClient 5 port, OSGi removal, CSS URL handler relocation, `ViewModel.bind` facades) with a "stay on 1.7.20" note for OSGi/legacy consumers. Verify: checklist against the CHANGELOG section shows one-to-one coverage.
- [x] 3.6 Add upstream attribution line to the site footer/copyright config only if content was adapted from `edvin/tornadofx-guide` during 3.1–3.5. Verify: footer renders or is deliberately omitted.

## 4. Russian content (parity)

- [x] 4.1 Write `index.ru.md` and `getting-started.ru.md` translating 3.1 content. Verify: `mkdocs build` renders them.
- [x] 4.2 Write `views.ru.md`, `builders.ru.md`, `fxml.ru.md`, `stylesheets.ru.md` translating 3.2. Verify: `mkdocs build` renders them.
- [x] 4.3 Write `properties.ru.md`, `async.ru.md`, `events.ru.md` translating 3.3. Verify: `mkdocs build` renders them.
- [x] 4.4 Write `rest.ru.md`, `workspace.ru.md`, `validation.ru.md` translating 3.4. Verify: `mkdocs build` renders them.
- [x] 4.5 Write `migration.ru.md` translating 3.5. Verify: `mkdocs build` renders it.

## 5. GitHub Actions workflow

- [x] 5.1 Create `.github/workflows/docs.yml`: `pull_request` (docs paths) runs parity check + `mkdocs build --strict` with no publish; `push` to `master` (same paths) + `workflow_dispatch` additionally deploys via `actions/configure-pages` (Pages enablement), `actions/upload-pages-artifact` and `actions/deploy-pages` with `pages: write` / `id-token: write` permissions and a `pages` concurrency group. Verify: workflow YAML parses (`python -c "import yaml,..."` or `actionlint` if available); local `python docs/check-parity.py && mkdocs build --strict` replicates the build job and exits 0.

## 6. README updates

- [x] 6.1 Update README "Getting started": Guide/documentation links point to the published Pages URL; remove `docs.tornadofx.io` and `tornadofx.io` links; mark the upstream wiki as legacy 1.7.x docs; fix the stale Gradle snippet (`jvmTarget` `1.8` → `17`). Verify: `grep -E "docs.tornadofx.io|tornadofx.io"` over README.md returns nothing; new guide link resolves to the Pages URL.

## 7. Integration verification

- [x] 7.1 Clean-environment check: fresh clone (or clean worktree), `pip install -r docs/requirements.txt`, `python docs/check-parity.py`, `mkdocs build --strict` — all exit 0 and `site/` contains both `en` (root) and `ru` trees with the language switcher present in the rendered HTML. Verify: listed commands exit 0; `site/ru/` contains the 13-page set.
- [x] 7.2 Review workflow semantics: deploy job triggers only on `master` (paths filter), PR job never publishes, permissions/concurrency correct. Verify: manual inspection of `docs.yml` against spec requirement "Site published to GitHub Pages from the main branch".

## Workflow follow-up

- Merge to `master`, watch the first `docs.yml` run enable GitHub Pages and publish; confirm the site is reachable at the project Pages URL with both languages switching correctly.
- Archive the change after the project's review requirements are satisfied.
