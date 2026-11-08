# Spec Delta

## Purpose

Defines the behavior of the framework's REST client on the modernized HTTP stack (Apache HttpClient 5), preserving the 1.7.x feature set: methods, entity handling, auth, and pluggable engines.

## ADDED Requirements

### Requirement: HTTP method support
The REST client SHALL support GET, POST, PUT, PATCH, DELETE and HEAD requests, returning a response that exposes status, headers, body text and a parsed JSON view of the body.

#### Scenario: POST with JSON body
- **WHEN** a client posts a JSON entity to an HTTP endpoint
- **THEN** the server receives the entity and the client response exposes the status code and response body

#### Scenario: Response JSON access
- **WHEN** a request returns a JSON body
- **THEN** the response exposes it as a JSON object without additional parsing setup

### Requirement: Basic authentication
The client SHALL support basic auth configured on the client builder, applied to requests against the configured scope.

#### Scenario: Basic auth credentials sent
- **WHEN** the client is built with basic auth credentials for a host and a request is made to that host
- **THEN** the request carries the corresponding Authorization header

### Requirement: Pluggable engines with Apache HttpClient 5 default-capable engine
The REST client SHALL keep its engine abstraction, and SHALL provide a first-class engine implementation built on Apache HttpClient 5. Public engine, request, and response classes exposed by the client SHALL be based on HttpClient 5 types.

#### Scenario: Apache engine performs a request
- **WHEN** the HTTP engine built on Apache HttpClient 5 is selected and a request is executed
- **THEN** the request completes and the response reflects the server's status and body

#### Scenario: Engine can be swapped
- **WHEN** an application selects a different engine implementation
- **THEN** all REST operations work through that engine without API changes to the Rest facade

### Requirement: Timeouts and error handling
The client SHALL propagate request timeout configuration to the underlying HTTP engine, and SHALL surface transport failures as errors observable by the caller.

#### Scenario: Timeout configuration is honored
- **WHEN** a request timeout is configured and the endpoint does not respond within it
- **THEN** the request fails with a timeout error rather than hanging indefinitely
