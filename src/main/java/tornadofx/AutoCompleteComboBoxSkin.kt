package tornadofx

import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.control.skin.ComboBoxListViewSkin
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.util.Callback


/**
 * Extension function to combobox to add autocomplete capabilities
 * Accept in parameter a callback to create the autocomplete list based on input text
 * Default filter use the string produced by the converter of combobox and search with contains ignore case the occurrence of typed text
 * @param automaticPopupWidth Use the width required to display all content. Default: true
 */
fun <T> ComboBox<T>.makeAutocompletable(automaticPopupWidth: Boolean = true, autoCompleteFilter: ((String) -> List<T>)? = null) {
    skin = AutoCompleteComboBoxSkin(this, autoCompleteFilter, automaticPopupWidth)
}

interface Resettable {
    fun reset()
}

interface FilterHandler {
    fun handleFilterChange(text : String)
    fun validateSelection()
    fun showSuggestion() : Boolean
    fun hideSuggestion() : Boolean

}

class FilterTooltipHandler(val control : Control, val filterHandler : FilterHandler) : EventHandler<KeyEvent> , Resettable {

    private val filter = SimpleStringProperty("")
    private val tooltip = Tooltip()

    init {
        tooltip.textProperty().bind(filter)
        filter.addListener { _, _, newValue -> handleFilterChanged_(newValue) }
    }

    override fun handle(event: KeyEvent) {
        val code = event.code
        val isControlDown = event.isControlDown
        val isShiftDown = event.isShiftDown
        var filterValue: String = filter.get()

        if (isControlDown) {
            when (code) {
                KeyCode.V -> Unit
                else -> return
            }
        }
        if (isShiftDown) {
            when (code) {
                KeyCode.LEFT, KeyCode.RIGHT, KeyCode.HOME, KeyCode.END -> return
                else -> Unit
            }
        }

        when (code) {
            KeyCode.DOWN, KeyCode.UP -> {
                filterHandler.showSuggestion()
            }
            KeyCode.BACK_SPACE -> {
                if(filterValue.isNotBlank()) {
                    filterValue = filterValue.substring(0, filterValue.length - 1)
                }
            }
            KeyCode.ESCAPE -> {
                reset()
                filterHandler.hideSuggestion()
                return
            }
            KeyCode.ENTER -> {
                filterHandler.validateSelection()
                return
            }
            KeyCode.RIGHT, KeyCode.LEFT, KeyCode.HOME, KeyCode.END, KeyCode.TAB, KeyCode.SHIFT, KeyCode.CONTROL -> return
            else -> filterValue += event.text
        }

        filter.value = filterValue
    }

    private fun handleFilterChanged_(newValue: String) {
        filterHandler.handleFilterChange(newValue)
        if(newValue.isNotBlank()) {
            showTooltip()
        } else {
            tooltip.hide()
        }
    }

    private fun showTooltip() {
        if (!tooltip.isShowing) {
            val stage = control.scene.window
            val posX = stage.x + control.localToScene(control.boundsInLocal).minX
            val posY = stage.y + control.localToScene(control.boundsInLocal).minY
            tooltip.show(stage, posX, posY)
        }
    }

    override fun reset() {
        filter.value = ""
        tooltip.hide()
    }

}


class FilterInputTextHandler(val editor : TextField, val filterHandler : FilterHandler) : EventHandler<KeyEvent>, Resettable {
    private var lastText: String = ""

    override fun handle(event: KeyEvent) {
        val text = editor.text
        val inputChanged = lastText != text
        lastText = text
        val code = event.code
        val isControlDown = event.isControlDown
        val isShiftDown = event.isShiftDown
        val caretPosition = editor.caretPosition

        if (isControlDown) {
            when (code) {
                KeyCode.V -> Unit
                else -> return
            }
        }
        if (isShiftDown) {
            when (code) {
                KeyCode.LEFT, KeyCode.RIGHT, KeyCode.HOME, KeyCode.END -> return
                else -> Unit
            }
        }

        when (code) {
            KeyCode.DOWN, KeyCode.UP -> {
                if(!filterHandler.showSuggestion()) {
                    editor.positionCaret(text.length)
                }
                return
            }
            KeyCode.ESCAPE -> {
                filterHandler.hideSuggestion()
                return
            }
            KeyCode.ENTER -> {
                filterHandler.validateSelection()
                return
            }
            KeyCode.RIGHT, KeyCode.LEFT, KeyCode.HOME, KeyCode.END, KeyCode.TAB, KeyCode.SHIFT, KeyCode.CONTROL -> return
            else -> Unit
        }

        if (inputChanged) {
            filterHandler.handleFilterChange(text)
            editor.text = text
            editor.positionCaret(caretPosition)
        }
    }

    override fun reset() {
        lastText = ""
    }
}

/**
 * Auto Complete support for combobox
 * Accept a call back to make custom filter
 * Default filter use the string produced by the converter of combobox and search with contains ignore case the occurrence of typed text
 * Created by anouira on 15/02/2017.
 */
open class AutoCompleteComboBoxSkin<T>(val comboBox: ComboBox<T>, autoCompleteFilter: ((String) -> List<T>)?, automaticPopupWidth: Boolean) : ComboBoxListViewSkin<T>(comboBox), FilterHandler {
    // The base class constructor may call back into this skin before subclass
    // state is initialized, so all access to the combo box goes through the
    // skinnable instead of the constructor parameter
    private val cb: ComboBox<T> get() = skinnable as ComboBox<T>

    var autoCompleteFilter_: (String) -> List<T> = autoCompleteFilter ?: {
        cb.items.filter { current -> cb.converter.toString(current).contains(it, true) }
    }

    internal val listView = ListView<T>(cb.items)
    private var skipValueUpdate = false

    private val filterHandler by lazy<EventHandler<KeyEvent>> {
        if(cb.isEditable) {
            FilterInputTextHandler(cb.editor, this)

        } else {
            FilterTooltipHandler(cb, this)
        }

    }

    init {
        with(cb) {
            onKeyReleased = filterHandler
            focusedProperty().onChange { focus -> if(!focus) {
                resetFilter()
                cb.hide()
            }}
            // Reset the filter whenever the popup opens (replaces the old
            // package-private arrowButton click hook)
            showingProperty().onChange { showing -> if (showing) resetFilter() }
        }
        listView.selectionModel.selectedItemProperty().onChange {
            if (!skipValueUpdate) cb.value = it
        }
        listView.onUserSelect(clickCount = 1) {
            cb.value = it
            if(filterHandler is FilterTooltipHandler) {
                resetFilter()
            }
            cb.hide()
        }
        if (!automaticPopupWidth) {
            // Note that we cannot bind prefWidthProperty because JavaFX sets
            // the preferred width upon reconfiguration. (ComboBoxPopupControl.java:322)
            cb.widthProperty().onChange {
                if (!listView.prefWidthProperty().isBound) listView.prefWidth = it
            }
        }
        updateCellFactory()
        updateValue()
        cb.cellFactoryProperty().onChange { updateCellFactory() }
    }

    override fun handleFilterChange(text : String) {
        val list = autoCompleteFilter_.invoke(text)
        listView.items = (list as? ObservableList<T>) ?: list.asObservable()
        listView.requestLayout()

        if (list.isEmpty()) {
            cb.hide()
        } else {
            cb.show()
            if (listView.selectedItem == null) {
                skipValueUpdate = true
                listView.selectionModel.selectFirst()
                skipValueUpdate = false
            }
            listView.requestFocus()
        }
    }

    /** Specific auto complete implementation */
    override fun validateSelection() {
        if (!listView.selectionModel.isEmpty) {
            cb.value = listView.selectedItem
        }
        if (cb.isShowing) {
            cb.hide()
        }
    }

    override fun showSuggestion() : Boolean {
        if(!cb.isShowing) {
            cb.show()
            return true
        }
        return false
    }

    override fun hideSuggestion() : Boolean {
        if(cb.isShowing) {
            cb.hide()
            return true
        }
        return false
    }

    private fun updateCellFactory() {
        if (cb.cellFactory != null) {
            listView.cellFactory = cb.cellFactory
        } else {
            createDefaultCellFactory()
        }
    }

    private fun createDefaultCellFactory() {
        val cellFormat: (ListCell<T>.(T) -> Unit) = {
            if (it is Node) {
                graphic = it
            } else if (it != null) {
                if (converter != null)
                    text = converter.toString(it)
                else if (it is String)
                    text = it
                else
                    text = it.toString()
            }
        }
        listView.properties["tornadofx.cellFormat"] = cellFormat
        listView.cellFactory = Callback { SmartListCell(FX.defaultScope, listView) }
    }

    private fun resetFilter() {
        listView.items = cb.items
        (filterHandler as? Resettable)?.reset()
    }

    /** End of Specific auto complete implementation */

    /** Syncs the auto complete list view selection with the combo box value */
    private fun updateValue() {
        val newValue = cb.value

        val listViewSM = listView.selectionModel

        if (newValue == null) {
            listViewSM.clearSelection()
        } else {
            // RT-22386: We need to test to see if the value is in the comboBox
            // items list. If it isn't, then we should clear the listview
            // selection
            val indexOfNewValue = getIndexOfComboBoxValueInItemsList()
            if (indexOfNewValue == -1) {
                listViewSM.clearSelection()
            } else {
                val index = cb.selectionModel.selectedIndex
                if (index in cb.items.indices) {
                    val itemsObj = cb.items.get(index)
                    if (itemsObj != null && itemsObj == newValue) {
                        listViewSM.select(index)
                    } else {
                        listViewSM.select(newValue)
                    }
                } else {
                    // just select the first instance of newValue in the list
                    val listViewIndex = cb.items.indexOf(newValue)
                    if (listViewIndex > -1) {
                        listViewSM.select(listViewIndex)
                    }
                }
            }
        }
    }

    private fun getIndexOfComboBoxValueInItemsList(): Int {
        val value = cb.value
        return cb.items.indexOf(value)
    }

    override fun getPopupContent() = listView

}
