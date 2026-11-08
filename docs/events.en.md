# EventBus

The EventBus lets components communicate without direct references. Events are
typed data classes; subscriptions declare which thread they run on.

## Define and fire an event

```kotlin
import tornadofx.FXEvent

class CustomerSelected(val customer: Customer) : FXEvent()

// fire from anywhere
fire(CustomerSelected(customer))
```

## Subscribe

Inside any Component (View, Controller, Fragment, App):

```kotlin
class DetailsView : View() {
    init {
        subscribe<CustomerSelected> {
            show(it.customer)
        }
    }
}
```

Subscriptions are tied to the component's lifecycle — unsubscribed when the
component is destroyed, so there is no leak-prone manual bookkeeping.

## Thread targeting

Each event type declares the thread its subscribers run on. The default is the
JavaFX Application Thread — you fire from any thread and subscribers always
observe the event on the UI thread:

```kotlin
class DataRefreshed(val rows: List<Row>) : FXEvent()
```

Background-targeted events run their subscribers on a worker thread instead —
useful for logging, persistence or other off-UI work:

```kotlin
import tornadofx.EventBus.RunOn

class AuditLogged(val entry: String) : FXEvent(runOn = RunOn.BackgroundThread)
```

## Scoped events

Events can be fired into a specific `Scope`, so components in different scopes
do not cross-deliver: `fire(event, scope)`. Unscoped events reach all scopes.

## When to use the EventBus

- Decoupling Views from Controllers (and from each other)
- One-to-many notifications (e.g. "data changed" to every open View)
- Cross-scope communication without singletons

For request/response style flows inside one screen, prefer direct injection
([Views & DI](views.md)) — the bus shines for broadcast semantics.
