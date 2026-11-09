# ui-controls Specification

## Purpose

Keeps the framework's custom controls and skins working when rebuilt on the JavaFX 21 public skin and behavior APIs, preserving their user-visible behavior.

## Requirements

### Requirement: DataGrid renders and selects items
The DataGrid control SHALL display its items as grid cells with a customizable cell factory and SHALL fire selection events when the user selects cells.

#### Scenario: Items render as cells
- **WHEN** a DataGrid is populated with items
- **THEN** each item is displayed through the cell factory in the grid

#### Scenario: Selection event fires
- **WHEN** the user selects an item cell
- **THEN** the selection callback fires with that item

### Requirement: DataGrid cellFragment accepts DataGridCellFragment subtypes
`DataGrid.cellFragment()` SHALL accept the class of any type that is a subtype of `DataGridCellFragment<T>` for the DataGrid's item type, so implementations of the interface can be registered without casting the class reference to `KClass<DataGridCellFragment<T>>`.

#### Scenario: Subclass of DataGridCellFragment is accepted without a cast
- **WHEN** a class implementing `DataGridCellFragment<T>` is registered via `cellFragment(MyFragment::class)` on a DataGrid of items of type `T`
- **THEN** the registration compiles and the fragment class is used to render the grid cells

#### Scenario: Existing exact-type usage keeps working
- **WHEN** `cellFragment` is called with a class reference that statically is `KClass<DataGridCellFragment<T>>`
- **THEN** the call continues to compile and behave as before

### Requirement: SqueezeBox multi-pane accordion behavior
The SqueezeBox control SHALL provide expandable/collapsible titled panes with both single-expand and multi-expand modes.

#### Scenario: Single-expand mode collapses siblings
- **WHEN** a pane is expanded in single-expand mode
- **THEN** the previously expanded pane collapses

#### Scenario: Multi-expand mode keeps siblings open
- **WHEN** panes are expanded in multi-expand mode
- **THEN** previously expanded panes remain open

### Requirement: AutoCompleteComboBox suggestion filtering
The auto-complete combo box SHALL filter a suggestion list as the user types into its editor and SHALL commit the selected suggestion.

#### Scenario: Typing filters suggestions
- **WHEN** the user types a prefix into the editor
- **THEN** the shown suggestions are limited to matching items

### Requirement: ListMenu CSS-stylable properties
The ListMenu control SHALL expose its styleable properties (including fixed graphic size) through the standard JavaFX styleable-property mechanism so CSS rules can style them.

#### Scenario: CSS rule applies graphic fixed size
- **WHEN** a CSS rule sets the fixed graphic size property on a ListMenu
- **THEN** the menu items render graphics at the specified size

### Requirement: Custom skins built on public skin APIs
Custom skins and controls MUST NOT import `com.sun.javafx.scene.control.*`; they SHALL be implemented against the public JavaFX skin API (`javafx.scene.control.skin`) and the supported behavior mechanism of the target JavaFX version.

#### Scenario: Clean compile against JavaFX 21
- **WHEN** the main sources are compiled against OpenJFX 21
- **THEN** no compilation unit imports `com.sun.javafx.scene.control.*` and the build succeeds
