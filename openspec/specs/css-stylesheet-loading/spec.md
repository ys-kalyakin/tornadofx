# css-stylesheet-loading Specification

## Purpose

Keeps the type-safe stylesheet pipeline working on the modular JDK: programmatic stylesheets are served to JavaFX through `css://` URLs handled by framework-registered handlers.

## Requirements

### Requirement: css:// URL handler serves rendered stylesheets
The framework SHALL register a URL stream handler for the `css://` protocol so that a URL of the form `css://<StylesheetClassName>` resolves to the CSS text rendered by that type-safe stylesheet class.

#### Scenario: Type-safe stylesheet loads in a running UI
- **WHEN** an application adds a subclass of the type-safe stylesheet API to a Scene or control
- **THEN** the styles from that stylesheet are applied to the UI without errors

#### Scenario: Rendered CSS matches the type-safe definition
- **WHEN** the handler serves a `css://` URL for a stylesheet class that defines a rule
- **THEN** the returned stream contains the rendered CSS text for that rule

### Requirement: Base64 inline stylesheet URLs
The `css://` handler SHALL serve URLs whose port is 64 by decoding the URL host as Base64 and returning the decoded bytes.

#### Scenario: Inline stylesheet content is served
- **WHEN** a `css://` URL with port 64 and a Base64-encoded host is opened
- **THEN** the handler returns the decoded content as the input stream

### Requirement: Handler relocated out of JDK package namespaces
The `css://` URL handler classes MUST NOT reside in packages under `sun.*` (split-package conflict with `java.base`); they SHALL live in a framework namespace. The prior `sun.net.www.protocol.css` package MUST NOT ship in the artifact.

#### Scenario: Artifact contains no relocated-internal handler packages
- **WHEN** the built jar is inspected
- **THEN** no classes exist under `sun/net/www/protocol/css/` and stylesheet loading still works per the requirements above
