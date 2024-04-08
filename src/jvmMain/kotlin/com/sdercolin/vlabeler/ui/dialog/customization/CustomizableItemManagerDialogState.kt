package com.sdercolin.vlabeler.ui.dialog.customization

import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.exception.CustomizableItemRemovingException
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.string.*
import java.awt.Desktop
import java.io.File

abstract class CustomizableItemManagerDialogState<T : CustomizableItem>(
    val title: Strings,
    val importDialogTitle: Strings,
    val definitionFileExtension: String,
    val directory: File,
    val allowExecution: Boolean,
    protected val appState: AppState,
    protected val appRecordStore: AppRecordStore,
) {

    private val _items = mutableStateListOf<T>()
    val items: List<T> get() = _items

    private val newlyAddedItemNames = mutableSetOf<String>()

    var selectedIndex: Int? by mutableStateOf(null)
        private set

    fun loadItems(items: List<T>) {
        val oldNames = _items.map { it.name }.toSet()
        _items.clear()
        _items.addAll(items)
        if (oldNames.isNotEmpty()) {
            val newNames = _items.map { it.name }.toSet()
            val newlyAddedNames = newNames - oldNames
            moveNewItemsToBottom(newlyAddedNames)
        }
    }

    private fun moveNewItemsToBottom(newlyAddedNames: Set<String>) {
        for (name in newlyAddedNames) {
            val item = _items.first { it.name == name }
            _items.remove(item)
            _items.add(item)
            selectedIndex = _items.indexOf(item)
        }
    }

    abstract fun reload()

    fun toggleItemDisabled(index: Int) {
        items[index].toggleDisabled()
        saveDisabled(index)
    }

    var isShowingFileSelector: Boolean by mutableStateOf(false)
        protected set

    abstract fun saveDisabled(index: Int)

    val selectedItem get() = selectedIndex?.let { items.getOrNull(it) }

    fun canRemoveCurrentItem(): Boolean = selectedItem?.canRemove ?: false

    fun requestRemoveCurrentItem() {
        appState.confirmIfRemoveCustomizableItem(this, requireNotNull(selectedItem))
    }

    fun removeItem(item: CustomizableItem) {
        require(item.canRemove)
        require(item in items)
        if (!item.remove()) {
            appState.showError(CustomizableItemRemovingException(null))
            return
        }
        selectedIndex = null
        reload()
    }

    fun canExecuteSelectedItem(): Boolean {
        return allowExecution && (selectedItem?.canExecute() ?: false)
    }

    fun executeSelectedItem() {
        require(canExecuteSelectedItem())
        requireNotNull(selectedItem).execute()
    }

    fun selectItem(index: Int) {
        selectedIndex = index
    }

    fun cancelSelection() {
        selectedIndex = null
    }

    fun openDirectory() {
        Desktop.getDesktop().open(directory)
    }

    fun openFileSelectorForNewItem() {
        isShowingFileSelector = true
    }

    suspend fun handleFileSelectorResult(file: File?) {
        isShowingFileSelector = false
        file?.let { addNewItem(it) }
    }

    private suspend fun addNewItem(configFile: File) {
        val item = runCatching { importNewItem(configFile) }.getOrElse {
            Log.error(it)
            appState.showSnackbar(it.message.orEmpty(), duration = SnackbarDuration.Indefinite)
            return
        }
        newlyAddedItemNames.add(item)
        reload()
    }

    fun finish() {
        appState.closeCustomizableItemManagerDialog()
    }

    protected abstract suspend fun importNewItem(configFile: File): String
}

@Composable
fun rememberCustomizableItemManagerDialogState(
    type: CustomizableItem.Type,
    appState: AppState,
) = when (type) {
    CustomizableItem.Type.MacroPlugin -> rememberMacroPluginManagerState(appState)
    CustomizableItem.Type.TemplatePlugin -> rememberTemplatePluginManagerState(appState)
    CustomizableItem.Type.Labeler -> rememberLabelerPluginManagerState(appState)
}
