package viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import kotlin.reflect.KClass

class BookEditorViewModel(
    val value: Int,
    val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    fun print() {
        println("hello")
    }

    companion object {
        val creationExtrasKey1 = object : CreationExtras.Key<Int> {}

        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                val intValue = extras[creationExtrasKey1]!!
                val savedStateHandle = extras.createSavedStateHandle()
                val vm = BookEditorViewModel(intValue, savedStateHandle)
                return vm as T
            }
        }
    }
}
