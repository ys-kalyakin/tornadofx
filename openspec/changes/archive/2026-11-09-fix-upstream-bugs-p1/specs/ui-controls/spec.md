# Spec Delta

## ADDED Requirements

### Requirement: DataGrid cellFragment accepts DataGridCellFragment subtypes
`DataGrid.cellFragment()` SHALL accept the class of any type that is a subtype of `DataGridCellFragment<T>` for the DataGrid's item type, so implementations of the interface can be registered without casting the class reference to `KClass<DataGridCellFragment<T>>`.

#### Scenario: Subclass of DataGridCellFragment is accepted without a cast
- **WHEN** a class implementing `DataGridCellFragment<T>` is registered via `cellFragment(MyFragment::class)` on a DataGrid of items of type `T`
- **THEN** the registration compiles and the fragment class is used to render the grid cells

#### Scenario: Existing exact-type usage keeps working
- **WHEN** `cellFragment` is called with a class reference that statically is `KClass<DataGridCellFragment<T>>`
- **THEN** the call continues to compile and behave as before
