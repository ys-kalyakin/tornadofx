# Proposal: add-github-pages-docs

## Why

The project's documentation is unreachable: the README links to `docs.tornadofx.io`
(GitBook guide), `tornadofx.io/dokka` (KDocs) and the upstream wiki, and the first two
hosts are dead. Consumers of this fork have no discoverable documentation, and the
upstream guide that still exists does not describe this fork's current stack
(JDK 21 / OpenJFX 21 / Kotlin 2.x, HttpClient 5 REST client, OSGi removal — see
CHANGELOG `1.7.21-SNAPSHOT`).

## What Changes

- Add an MkDocs Material documentation site to the repository:
  - Markdown sources under `docs/` (English and Russian trees), `mkdocs.yml` config.
  - Bilingual content with a UI language switcher; **English is the default**,
    Russian available via the switcher.
  - Content is a compact, new documentation for this fork (not a full port of the
    upstream guide): getting started, dependency setup, core framework sections
    (Views/builders, FXML, type-safe CSS, DI/scopes, async, EventBus, REST client,
    Workspace/Wizard, validation), and a migration section for the 1.7.20 → 1.7.21
    breaking changes. Material may be adapted from the Apache-2.0 licensed upstream
    guide (`edvin/tornadofx-guide`) and updated for the current stack.
- Add a GitHub Actions workflow that builds the site and deploys it to GitHub Pages
  as a project site (published under `https://ys-kalyakin.github.io/tornadofx/`).
- Update the README "Getting started" section: point Guide/Documentation links at
  the new site, drop dead links (`docs.tornadofx.io`, `tornadofx.io/dokka`).
- Ignore the generated site output (`site/`) in git.

## Capabilities

### New Capabilities

- `documentation-site`: the published bilingual documentation site — content scope
  and structure, EN/RU language switching, GitHub Pages deployment via CI, and
  README discoverability of the site.

### Modified Capabilities

- None. Existing capabilities (css-stylesheet-loading, headless-ui-testing,
  platform-compatibility, rest-client, ui-controls) are unaffected; documentation
  does not change framework behavior.

## Impact

- **New files**: `docs/` (site sources), `mkdocs.yml`, `.github/workflows/docs.yml`.
- **CI**: a new docs deploy job/workflow runs alongside the existing `ci.yml` test
  suite; it does not modify the existing test workflow.
- **Modified files**: `README.md` (link section), `.gitignore` (site output).
- **Dependencies**: Python toolchain (MkDocs Material) used only by the docs build
  job and optional local preview; no new dependencies for the framework artifact.
- **No changes** to library code, public API, or published Maven artifacts.
