# documentation-site Specification

## Purpose

Provides the project's public documentation as a bilingual (English/Russian) static
site published on GitHub Pages, so consumers of this fork have a discoverable,
reachable reference that documents the current stack instead of the dead upstream
hosts.

## Requirements

### Requirement: Bilingual documentation with language switcher

The documentation site SHALL present every documentation page in English and in
Russian, with a visible language switcher in the site UI. English SHALL be the
default language for a visitor who opens the site directly. Every English page
MUST have a Russian counterpart at the corresponding language URL.

#### Scenario: Default language is English

- **WHEN** a visitor opens the site's root URL without selecting a language
- **THEN** the landing page is rendered in English

#### Scenario: Switching languages preserves the page

- **WHEN** a visitor is viewing a page in English and activates the Russian
  entry in the language switcher
- **THEN** the same page is displayed in Russian
- **AND** a reciprocal switcher entry returns the visitor to the English page

#### Scenario: Full page parity between languages

- **WHEN** the documentation build completes
- **THEN** every page present in the English navigation also exists in Russian,
  and the site build fails if a page is missing from either language tree

### Requirement: Documentation covers the current fork stack

The documentation SHALL include, at minimum: getting-started and dependency setup
for the current stack (JDK 21 toolchain, OpenJFX 21, Kotlin 2.x, `jvmTarget` 17);
core framework sections covering Views and type-safe builders, FXML, type-safe
CSS, dependency injection, async execution, EventBus, the REST client,
Workspace/Wizard, and validation; deep-dive sections covering the item controls
(TableView, ListView, ComboBox, TreeView, DataGrid), charts, dialogs and internal
windows, config and preferences, i18n messages, and diagnostics (layout debugger,
error handling); a testing guide covering TestFX-based application testing
including headless execution; and the migration section. Code examples
SHALL be written against the current public API.

#### Scenario: Navigation exposes the core sections

- **WHEN** a visitor opens the site navigation
- **THEN** entries for Getting started, the core framework sections listed above,
  and Migration are present and resolve to published pages

#### Scenario: Navigation exposes the deep-dive and testing sections

- **WHEN** a visitor opens the site navigation
- **THEN** entries for Items & controls, Charts, Dialogs & windows,
  Config & preferences, i18n & messages, Diagnostics, and Testing are present
  and resolve to published pages in both languages

#### Scenario: REST examples use the HttpClient 5 API

- **WHEN** the documentation shows REST client examples
- **THEN** they use the ported HttpClient 5 based API described in the change log
  and not the removed HttpClient 4 types

### Requirement: Migration section documents the 1.7.21 breaking changes

The documentation SHALL contain a migration section for moving from the 1.7.20
release to the current fork line, covering each breaking change recorded in the
project change log: the new platform baseline (Java 17 bytecode / OpenJFX 21),
the Kotlin 2.x consumer requirement, the REST client port to HttpClient 5, and
OSGi removal.

#### Scenario: Each recorded breaking change is covered

- **WHEN** a visitor reads the migration section
- **THEN** they find guidance for each breaking change listed in the project
  change log's "Breaking changes" subsection, including which consumers must stay
  on 1.7.20

### Requirement: Site published to GitHub Pages from the main branch

The repository's CI SHALL build the documentation site and publish it to GitHub
Pages as a project site whenever documentation or site configuration changes are
merged into the main branch (`master`). The published site SHALL be served under
the repository's GitHub Pages project URL.

#### Scenario: Merge to master triggers deployment

- **WHEN** a commit that changes files under the documentation sources or the
  site configuration lands on `master`
- **THEN** CI builds the site and the updated content is served at the project's
  GitHub Pages URL

#### Scenario: Non-main branches do not publish

- **WHEN** documentation changes exist on a feature branch or pull request
- **THEN** CI may build the site to validate it, but the published GitHub Pages
  site is not updated

### Requirement: README directs readers to the published site

The project README SHALL present the published documentation site as the primary
guide/documentation link and MUST NOT link to the dead documentation hosts
(`docs.tornadofx.io`, `tornadofx.io`).

#### Scenario: Guide link resolves to the live site

- **WHEN** a reader follows the guide/documentation link in the README
- **THEN** they reach the published documentation site on GitHub Pages

### Requirement: Documentation build is reproducible

The documentation build SHALL use pinned tool versions so that the site builds
reproducibly in CI from a clean checkout, and the same build SHALL be runnable
locally for preview without publishing.

#### Scenario: Clean checkout builds the site

- **WHEN** CI runs the documentation build on a machine with no prior state
- **THEN** the site is built successfully from the pinned dependency set without
  interactive input

#### Scenario: Local preview does not publish

- **WHEN** a contributor runs the documentation preview command locally
- **THEN** the site is served locally and nothing is published to GitHub Pages
