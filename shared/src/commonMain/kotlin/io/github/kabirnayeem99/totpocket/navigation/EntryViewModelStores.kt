package io.github.kabirnayeem99.totpocket.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * Gives each back-stack entry its own [ViewModelStore], so a screen's ViewModel lives exactly as
 * long as the entry: surviving rotation, cleared (stopping its audio and timers) when popped.
 * Itself scoped to the Activity.
 */
class EntryViewModelStores : ViewModel() {
    private val owners = mutableMapOf<String, ViewModelStoreOwner>()

    fun ownerFor(entryId: String): ViewModelStoreOwner = owners.getOrPut(entryId) {
        val store = ViewModelStore()
        object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = store
        }
    }

    fun clear(entryId: String) {
        owners.remove(entryId)?.viewModelStore?.clear()
    }

    override fun onCleared() {
        owners.values.forEach { it.viewModelStore.clear() }
        owners.clear()
    }
}
