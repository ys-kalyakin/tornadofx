package tornadofx.tests

import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.testfx.api.FxRobot
import org.testfx.api.FxToolkit
import tornadofx.*

class PersonGridCellFragment : DataGridCellFragment<Person>() {
    override val root = hbox {
        label {
            textProperty().bind(itemProperty.objectBinding { it?.name.orEmpty() })
        }
    }
}

/**
 * Spec scenario for DataGrid.cellFragment accepting DataGridCellFragment subtypes
 * (upstream #700): a fragment class whose KClass is a subtype of
 * DataGridCellFragment<Person> registers without a cast and renders the cells.
 */
class DataGridFragmentTest {

    val primaryStage: Stage = FxToolkit.registerPrimaryStage()

    @Test
    fun cellFragment_accepts_subclass_without_cast_and_renders_cells() {
        val grid = DataGrid(listOf(Person("Alice", 25), Person("Bob", 30)))
        grid.cellFragment(PersonGridCellFragment::class)

        FxToolkit.setupFixture {
            primaryStage.scene = Scene(VBox().apply { setPrefSize(500.0, 300.0); add(grid) })
            primaryStage.show()
        }

        val labelTexts = FxRobot().from(grid).lookup(".label").queryAll<Label>().map { it.text }
        assertTrue("Alice" in labelTexts, "Fragment should render the first person name, got: $labelTexts")
        assertTrue("Bob" in labelTexts, "Fragment should render the second person name, got: $labelTexts")
    }
}
