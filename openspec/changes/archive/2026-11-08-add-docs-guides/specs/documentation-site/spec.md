# documentation-site — Delta Specification

## MODIFIED Requirements

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
