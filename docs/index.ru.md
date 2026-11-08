# TornadoFX

TornadoFX — это фреймворк для JavaFX на Kotlin: классы приложений без
конфигурации, типобезопасные конструкторы UI и CSS, внедрение зависимостей,
асинхронное выполнение задач, шина событий с выбором потока и REST-клиент с
автоматической конвертацией JSON.

Этот проект — поддерживаемый форк оригинального
[edvin/tornadofx](https://github.com/edvin/tornadofx) (лицензия Apache 2.0),
обновлённый до современного Java-стека:

- **Сборка:** JDK 21; публикуемый байткод — Java 17
- **JavaFX:** OpenJFX 21, подключается как Maven-зависимость с платформенными классификаторами
- **Kotlin:** 2.x (K2), `jvmTarget` 17
- **REST-клиент:** Apache HttpClient 5
- **Тесты:** JUnit 5, по умолчанию headless (Monocle) в CI

Поддержка OSGi удалена вместе со стеком эпохи Java 8/JavaFX 8 — потребителям
OSGi следует оставаться на ветке 1.7.x. Все breaking changes описаны в разделе
[Миграция с 1.7.20](migration.md).

## Что даёт фреймворк

- MVC, MVP и их производные
- Внедрение зависимостей и скоупы
- Типобезопасные конструкторы GUI
- Типобезопасные конструкторы CSS
- Первоклассная поддержка FXML
- Асинхронное выполнение задач
- EventBus с выбором потока
- Hot reload View и стилей
- REST-клиент с автоматической конвертацией JSON
- Нулевая конфигурация: без XML и аннотаций

## Документация

1. [Начало работы](getting-started.md) — требования, подключение зависимостей, первое приложение
2. [View и DI](views.md) — View, Fragment, скоупы, `inject`
3. [Типобезопасные конструкторы](builders.md) — лейауты, контролы, таблицы
4. [FXML](fxml.md) — View на основе FXML
5. [Типобезопасные стили](stylesheets.md) — CSS на Kotlin
6. [Свойства и биндинги](properties.md) — observables, ViewModel
7. [Асинхронность](async.md) — фоновая работа с возвратом в UI-поток
8. [EventBus](events.md) — типизированные события с выбором потока
9. [REST-клиент](rest.md) — клиент на HttpClient 5 с JSON-моделями
10. [Workspace и Wizard](workspace.md) — оболочка приложения и многошаговые сценарии
11. [Валидация](validation.md) — валидация полей и декорирование
12. [Миграция с 1.7.20](migration.md) — breaking changes этого форка

## Дополнительные ресурсы

- [Скринкасты](https://www.youtube.com/user/MrEdvinsyse) (серия от автора оригинала)
- [Kotlinlang Slack `#tornadofx`](https://kotlinlang.slack.com/messages/tornadofx/details/)
- [Stack Overflow `tornadofx`](http://stackoverflow.com/questions/tagged/tornadofx)
- [Примеры приложений](https://github.com/edvin/tornadofx-samples) (эпохи 1.7.x, может потребовать обновления стека)
- [Плагин для IDEA](https://github.com/edvin/tornadofx-idea-plugin)
- [Журнал изменений проекта](https://github.com/ys-kalyakin/tornadofx/blob/master/CHANGELOG.md)
