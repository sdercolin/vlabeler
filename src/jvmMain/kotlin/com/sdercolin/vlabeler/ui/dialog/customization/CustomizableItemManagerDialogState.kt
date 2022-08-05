package com.sdercolin.vlabeler.ui.dialog.customization

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.toMutableStateList
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.string.Strings
import java.awt.Desktop
import java.io.File

abstract class CustomizableItemManagerDialogState<T : CustomizableItem>(
    items: List<T>,
    val title: Strings,
    val directory: File,
    val allowExecution: Boolean,
    protected val appRecordStore: AppRecordStore
) {

    val snackbarHostState = SnackbarHostState()

    private val _items = items.toMutableStateList()
    val items: List<T> get() = _items

    var selectedIndex: Int? = null
        private set

    fun toggleItemDisabled() {
        items[requireNotNull(selectedIndex)].toggleDisabled()
    }

    var isShowingFileSelector: Boolean = false
        protected set

    abstract fun saveDisabled()

    val selectedItem = selectedIndex?.let { items.getOrNull(it) }

    fun canRemoveItem(): Boolean = selectedItem?.canRemove ?: false

    fun removeItem() {
        require(canRemoveItem())
        requireNotNull(selectedItem).remove()
    }

    fun canExecute(): Boolean {
        return allowExecution && (selectedItem?.executable ?: false)
    }

    fun execute() {
        require(canExecute())
        requireNotNull(selectedItem).execute()
    }

    fun selectItem(index: Int) {
        selectedIndex = index
    }

    fun openDirectory() {
        Desktop.getDesktop().open(directory)
    }

    fun openFileSelectorForNewItem() {
        isShowingFileSelector = true
    }

    fun reloadItems(items: List<T>) {
        _items.clear()
        _items.addAll(items)
    }

    suspend fun handleFileSelectorResult(file: File?) {
        isShowingFileSelector = false
        file?.let { addNewItem(it) }
    }

    private suspend fun addNewItem(configFile: File) {
        runCatching { importNewItem(configFile) }.getOrElse {
            snackbarHostState.showSnackbar(it.message.orEmpty())
            return
        }
        selectedIndex = items.size
    }

    protected abstract suspend fun importNewItem(configFile: File)
}

@Composable
fun rememberCustomizableItemManagerDialogState(
    type: CustomizableItem.Type,
    appState: AppState
) = when (type) {
    CustomizableItem.Type.MacroPlugin -> rememberMacroPluginManagerState(appState)
    CustomizableItem.Type.TemplatePlugin -> TODO()
    CustomizableItem.Type.Labeler -> TODO()
}
