# Proposal

## Why

The documentation site covers 12 topics, while the framework ships substantial feature areas with no documentation at all: the item controls deep-dive surface (TableView/ListView/ComboBox/TreeView/DataGrid), charts, dialogs and internal windows, config/preferences, i18n messages, and diagnostics (layout debugger, event log, error handling). Most importantly, there is no guidance for the single most-asked question of any UI framework — how to test a TornadoFX application — even though the repository already runs a proven headless (Monocle/TestFX) setup that consumers can copy. The gap makes the fork harder to adopt than its documentation quality deserves.

## What Changes

- Add a flagship **Testing your TornadoFX app** guide: TestFX setup, headless testing with Monocle, lifecycle/scope patterns, reusing the patterns proven by this repository's own test suite.
- Add six bilingual topic pages closing the biggest content holes:
  - **Items & controls** — tableview/columns/cellFormat/edit model, listview, combobox (`bindSelected`, autocompletion), treeview, DataGrid.
  - **Charts** — chart builders for the standard JavaFX chart types.
  - **Dialogs & windows** — alerts/confirm, `dialog` builder, internal windows, decorations.
  - **Config & preferences** — the `config { }` and `preferences { }` delegates.
  - **i18n & messages** — resource bundles, `messages` support, encoding, custom locators.
  - **Diagnostics** — layout debugger, event log, error handling (`FX.errorHandler`).
- Every new page ships in English and Russian (mechanically enforced by `docs/check-parity.py`), is wired into the mkdocs navigation, and its code examples compile against the current public API.
- Update the `documentation-site` coverage requirement to reflect the expanded minimum coverage.
- Non-goals (explicitly out of scope): animation/slideshow and shapes/vector-math pages, keyboard/commands reference, application packaging/deployment guides, porting the full upstream guide verbatim, and any API reference (Dokka covers that).

## Capabilities

### New Capabilities

<!-- None; this change extends an existing capability. -->

### Modified Capabilities

- `documentation-site`: the "Documentation covers the current fork stack" requirement's minimum-coverage list expands to include the testing guide and the six new topic areas, with a navigation scenario to match.

## Impact

- **Files**: seven new page pairs under `docs/` (`*.en.md` + `*.ru.md` each), `mkdocs.yml` navigation updated. No build, dependency, or source-code changes.
- **CI**: existing docs deployment flow deploys automatically when these pages merge to `master` (per the existing release/deploy requirements); the parity gate covers the new pages with no script changes.
- **Content quality bar**: every snippet compiles against the current API (checked in implementation), consistent tone/style with the existing pages; no dead links to removed APIs (e.g. HttpClient 4).
- **Compatibility**: documentation-only change; no runtime behavior, public API, or build impact.
