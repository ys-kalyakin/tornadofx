# collection-helpers Specification

## Purpose

Makes the predicate-based list reordering helpers (`moveUpAll`, `moveDownAll` and their single-element counterparts) safe on observable and plain lists: they move every matching element by one position without throwing and without corrupting indices.

## Requirements

### Requirement: moveUpAll moves all matching elements one position up safely
`moveUpAll(predicate)` SHALL move every element satisfying the predicate one index up — unless that element is already at the top — and SHALL NOT throw `ConcurrentModificationException` when the list is an observable list.

#### Scenario: Matches between non-matches gather at the top
- **WHEN** `moveUpAll` is called on a list where elements satisfying the predicate sit between non-matching elements
- **THEN** the matched elements gather at the top in their original relative order, non-matched elements follow in their original relative order, and no exception is thrown

#### Scenario: Element already at the top stays
- **WHEN** `moveUpAll` is called and a matching element is already at index 0
- **THEN** that element remains at index 0

### Requirement: moveDownAll moves all matching elements one position down safely
`moveDownAll(predicate)` SHALL move every element satisfying the predicate one index down — unless that element is already at the bottom — and SHALL NOT throw `ConcurrentModificationException` when the list is an observable list.

#### Scenario: Matches between non-matches gather at the bottom
- **WHEN** `moveDownAll` is called on a list where elements satisfying the predicate sit between non-matching elements
- **THEN** the matched elements gather at the bottom in their original relative order, no exception is thrown, and change notifications fire once per list modification

#### Scenario: Element already at the bottom stays
- **WHEN** `moveDownAll` is called and a matching element is already at the last index
- **THEN** that element remains at the last index

### Requirement: Single-element move helpers stay correct after rework
`moveUp(predicate)` and `moveDown(predicate)` SHALL keep moving only the first matching element by one position, unchanged by the `moveUpAll`/`moveDownAll` rework.

#### Scenario: Only first match moves
- **WHEN** `moveUp` is called on a list with several matching elements
- **THEN** exactly the first matching element (in current list order) is moved one position up
