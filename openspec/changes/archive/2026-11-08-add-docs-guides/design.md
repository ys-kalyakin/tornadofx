# Design

## Context

The site (`docs/*.en.md` + `docs/*.ru.md`, `mkdocs.yml`, Material theme, mkdocs-i18n plugin) currently covers 12 topics; `docs/check-parity.py` fails the build when any English page lacks a Russian counterpart. CI deploys the site on every merge to `master` touching docs. The existing "Documentation covers the current fork stack" requirement holds a closed "at minimum" list that this change expands. All framework areas to be documented already exist in the codebase with stable public APIs and, in several cases, with existing in-repo test coverage that doubles as usage reference (`PreferencesTest`, the TestFX-based test suite, the headless Maven profile).

## Goals / Non-Goals

**Goals:**

- Close the largest content holes with focused, snippet-verified pages rather than porting the full upstream guide.
- Ship the consumer-facing testing guide that the repository's own headless infrastructure already proves out.
- Keep every existing site mechanism intact: parity gate, i18n switcher, pinned build, auto-deploy.

**Non-Goals:**

- Animation/slideshow and shapes/vector-math pages (follow-up candidates).
- Keyboard/commands reference, packaging/deployment guidance, OSGi (removed in 2.0).
- Any Dokka-replaceable API reference content — pages link to the API docs for the long tail.
- Changes to build tooling, CI, or the parity checker.

## Decisions

### D1: Page set mapped to source-of-truth files

Each new page documents one cohesive area, with the implementation grounded in specific source files so examples stay against the real API:

| Page | Covers | Source of truth |
|---|---|---|
| `testing` | TestFX setup for consumers, headless Monocle execution, lifecycle patterns | `src/test/kotlin/tornadofx/tests/*`, headless Maven profile, CI workflow |
| `items-controls` | tableview/columns/cellFormat/edit model, listview, combobox (`bindSelected`, autocompletion), treeview, DataGrid | `ItemControls.kt`, `TableView.kt`, `ListView.kt`, `TreeView.kt`, `DataGrid.kt`, `AutoCompleteComboBoxSkin.kt` |
| `charts` | builders for standard JavaFX chart types | `Charts.kt` |
| `dialogs-windows` | alerts/confirm, `dialog` builder, internal windows, decorations | `Dialogs.kt`, `InternalWindow.kt`, `Decoration.kt` |
| `config-preferences` | `config { }` / `preferences { }` delegates | `Component.kt` (config/preferences), `PreferencesTest.kt` |
| `i18n-messages` | resource bundles, `messages` delegate, custom locators | `Messages.kt` |
| `diagnostics` | layout debugger, event log, error handling | `LayoutDebugger.kt`, `EventLog.kt`, `ErrorHandler.kt` |

### D2: Navigation placement follows the learning path

New entries interleave with existing ones instead of appending a block: Items & controls and Charts after "Type-safe builders"; Dialogs & windows, Config & preferences, i18n & messages, Diagnostics after "Workspace & Wizard"; Testing between Validation and Migration. Both language trees get identical nav structure (the i18n plugin derives it from the shared `nav`).

### D3: Snippet verification without new tooling

No snippet-extraction test harness is introduced. Verification convention: each page's Kotlin snippets are pasted into a scratch test source compiled against the project during implementation (then discarded), plus manual review against the source files in D1. This keeps the change docs-only while giving the "compiles as written" bar teeth.

### D4: Bilingual workflow — English first, translation second

Each page is written in English, then translated; Russian terminology follows the vocabulary already used by the existing `*.ru.md` pages so the site stays consistent. `check-parity.py` needs no changes (ALLOWLIST stays empty) — it enforces both files exist as soon as both are written.

### D5: Testing guide is consumer-oriented, repo-pattern-based

The guide teaches consumers their own setup (TestFX + Monocle dependencies, headless system properties, JUnit 5 wiring) shown for both Maven and Gradle where they differ, with examples modeled on — but not copy-pasted from — this repository's test suite. It links to the repo's CI/headless profile as a live reference implementation rather than duplicating its internals.

## Risks / Trade-offs

- [Page scope creep toward full upstream-guide ports] → each page is capped to the areas listed in D1 and links to the API reference for the long tail.
- [Snippets drift from the API over time] → compile-once verification at implementation; ongoing drift is handled by the existing docs maintenance flow, no new harness.
- [Russian terminology inconsistency with existing pages] → translation follows existing `*.ru.md` vocabulary; final pages reviewed against them.
- [14 new files in one change] → grouped tasks land page pairs incrementally; deploy happens only on `master` merge, so partial states never publish.

## Migration Plan

Documentation-only; no migration or rollback concerns beyond reverting the commit. Existing pages, nav entries, and build tooling are untouched.

## Open Questions

None — page titles in nav and per-page section order are polish-level, resolved during implementation.
