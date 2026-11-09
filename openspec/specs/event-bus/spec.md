# event-bus Specification

## Purpose

Defines the lifecycle contract of the `FXEvent` subscription registry: delivery semantics for scoped event subscriptions and a no-leak guarantee — invalidated subscriptions must not leave unbounded bookkeeping entries behind in long-running applications.

## Requirements

### Requirement: Scope registry does not accumulate invalidated subscriptions
The event bus SHALL remove a subscription's scope bookkeeping entry when that subscription becomes invalid, so that repeated subscribe/invalidate cycles (dock/undock of the owning component, exhausted `times` limit, or `unsubscribe()` requested from an event context) do not grow the registry without bound.

#### Scenario: Repeated dock/undock cycles do not accumulate entries
- **WHEN** a UIComponent subscribes to an `FXEvent` on dock and its subscription is invalidated on undock, and this cycle repeats many times
- **THEN** the total number of retained scope entries for that event action remains constant, regardless of the number of cycles

#### Scenario: Scope entry removed when unsubscribe comes from event context
- **WHEN** a subscriber action calls `unsubscribe()` on its event context during delivery and the owning component is undocked
- **THEN** the subscription is invalidated and its scope entry is removed

### Requirement: Invalidation preserves delivery semantics
Cleaning up invalidated subscriptions SHALL NOT change event delivery: an event SHALL be delivered exactly once to each still-valid subscriber matching the event's scope, and SHALL NOT be delivered to subscribers that were invalidated before or during the same dispatch.

#### Scenario: Valid subscriber still receives events after cleanup
- **WHEN** an event fires after other subscriptions to the same event type have been invalidated and cleaned up
- **THEN** every still-valid matching subscriber receives the event exactly once

#### Scenario: Subscriber undocked during dispatch does not receive the event
- **WHEN** handling one event invalidates another subscriber's subscription to that same event
- **THEN** the invalidated subscriber is not invoked for that dispatch
