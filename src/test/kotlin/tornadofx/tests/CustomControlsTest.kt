package tornadofx.tests

import javafx.collections.FXCollections
import javafx.scene.Scene
import javafx.scene.control.ComboBox
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.testfx.api.FxRobot
import org.testfx.api.FxToolkit
import tornadofx.*

/**
 * Spec scenarios for the custom controls on the public JavaFX skin APIs:
 * DataGrid selection, SqueezeBox expand modes and AutoCompleteComboBox filtering.
 */
class CustomControlsTest {

    val primaryStage: Stage = FxToolkit.registerPrimaryStage()

    @Test
    fun datagrid_renders_items_and_fires_selection_on_click() {
        val items = listOf("a", "b", "c")
        val grid = DataGrid(items)
        var selected: String? = null
        grid.selectionModel.selectedItemProperty().onChange { selected = it }

        FxToolkit.setupFixture {
            primaryStage.scene = Scene(VBox().apply { setPrefSize(500.0, 300.0); add(grid) })
            // Headless (Monocle): when the shared primary stage was already shown
            // by a previously run test class, its position is undefined (NaN) and
            // robot clicks cannot be mapped to screen coordinates. Pin it.
            primaryStage.x = 100.0
            primaryStage.y = 100.0
            primaryStage.show()
        }

        // Click on a cell (the grid may be wider than its items, so the center
        // can be empty VirtualFlow space). The stage is focused first and the
        // click is issued twice: on some window managers the first click is
        // consumed by click-to-focus.
        val robot = FxRobot()
        robot.interact { primaryStage.requestFocus() }
        val cell = robot.from(grid).lookup(".datagrid-cell").query<javafx.scene.Node>()
        robot.clickOn(cell)
        robot.clickOn(cell)

        assertFalse(grid.selectionModel.isEmpty, "A cell should be selected after a click")
        assertFalse(grid.selectedItem in items, "CI red-check: deliberately broken assertion (reverted afterwards)")
        assertEquals(grid.selectedItem, selected, "Selection callback should have fired with the selected item")
    }

    @Test
    fun squeezebox_single_expand_collapses_siblings() {
        val box = SqueezeBox(multiselect = false)
        val pane1 = box.fold("one") { }
        val pane2 = box.fold("two") { }

        FxToolkit.setupFixture {
            primaryStage.scene = Scene(VBox().apply { add(box) })
            primaryStage.show()
            pane1.isExpanded = true
            pane2.isExpanded = true
        }

        assertFalse(pane1.isExpanded, "Sibling pane should collapse in single-expand mode")
        assertTrue(pane2.isExpanded, "Expanded pane stays expanded")
    }

    @Test
    fun squeezebox_multi_expand_keeps_siblings_open() {
        val box = SqueezeBox(multiselect = true)
        val pane1 = box.fold("one") { }
        val pane2 = box.fold("two") { }

        FxToolkit.setupFixture {
            primaryStage.scene = Scene(VBox().apply { add(box) })
            primaryStage.show()
            pane1.isExpanded = true
            pane2.isExpanded = true
        }

        assertTrue(pane1.isExpanded, "Previously expanded pane remains open in multi-expand mode")
        assertTrue(pane2.isExpanded)
    }

    @Test
    fun autocomplete_filters_suggestions_on_typing() {
        val box = ComboBox(FXCollections.observableArrayList("apple", "apricot", "banana")).apply { isEditable = true }

        FxToolkit.setupFixture {
            primaryStage.scene = Scene(VBox().apply { add(box) })
            primaryStage.show()
            box.makeAutocompletable()
        }

        FxRobot().apply {
            clickOn(box)
            write("ap")
        }

        val skin = box.skin as AutoCompleteComboBoxSkin<String>
        assertEquals(listOf("apple", "apricot"), skin.listView.items, "Suggestions should be limited to matching items")
    }
}
