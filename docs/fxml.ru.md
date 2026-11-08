# FXML

TornadoFX поддерживает первоклассный FXML: View или Fragment загружает свой
root из FXML-файла, а сам компонент выступает контроллером — отдельный класс
контроллера не нужен.

## View на основе FXML

Объявите тип root и загрузите его через `by fxml()`:

```kotlin
class MainView : View("Main") {
    override val root: VBox by fxml("/view/main.fxml")

    val greeting: Label by fxid()

    fun sayHello() {
        greeting.text = "Hello, TornadoFX"
    }
}
```

В FXML-файле укажите `fx:controller` на класс View:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>
<VBox xmlns="http://javafx.com/javafx" xmlns:fx="http://javafx.com/fxml"
      fx:controller="com.example.MainView">
    <children>
        <Label fx:id="greeting" text="Hello"/>
        <Button text="Say hello" onAction="#sayHello"/>
    </children>
</VBox>
```

## Доступ к узлам через fxid()

Узлы с `fx:id` внедряются объявлением свойства с `by fxid()`:

```kotlin
val greeting: Label by fxid()
```

## Фрагменты и перезагрузка FXML

Fragment загружает FXML так же:

```kotlin
class DetailFragment : Fragment("Detail") {
    override val root: GridPane by fxml("/view/detail.fxml")
}
```

Чтобы перезагрузить FXML-содержимое в существующий root (удобно при hot
reload):

```kotlin
loadFXML("/view/main.fxml")
```

## FXML или конструкторы

Используйте FXML, когда нужны лейауты, дружественные Scene Builder, или
разделение «дизайнер/разработчик». Используйте
[типобезопасные конструкторы](builders.md) ради проверки типов на этапе
компиляции и нативной композиции на Kotlin — оба подхода свободно
комбинируются: View на FXML может достраивать узлы в Kotlin и наоборот.
