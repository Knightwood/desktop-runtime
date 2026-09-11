package androidx.compose.desktop.runtime.viewmodel

import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import java.lang.reflect.Constructor
import kotlin.reflect.KClass

internal object JvmViewModelProviders {

    /**
     * Creates a new [ViewModel] instance using the no-args constructor if available, otherwise
     * throws a [RuntimeException].
     */
    @Suppress("DocumentExceptions")
    fun <T : ViewModel> createViewModel(modelClass: Class<T>): T =
        try {
            modelClass.getDeclaredConstructor().newInstance()
        } catch (e: NoSuchMethodException) {
            throw RuntimeException("Cannot create an instance of $modelClass", e)
        } catch (e: InstantiationException) {
            throw RuntimeException("Cannot create an instance of $modelClass", e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Cannot create an instance of $modelClass", e)
        }
}

internal object ViewModelProviders {

    internal fun getDefaultFactory(owner: ViewModelStoreOwner): ViewModelProvider.Factory =
        if (owner is HasDefaultViewModelProviderFactory) {
            owner.defaultViewModelProviderFactory
        } else {
            DefaultViewModelProviderFactory
        }

    internal fun getDefaultCreationExtras(owner: ViewModelStoreOwner): CreationExtras =
        if (owner is HasDefaultViewModelProviderFactory) {
            owner.defaultViewModelCreationExtras
        } else {
            CreationExtras.Empty
        }
}

internal object DefaultViewModelProviderFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        return JvmViewModelProviders.createViewModel(modelClass.java)
    }
}

@JvmOverloads
public fun ViewModelProvider.Companion.create(
    owner: ViewModelStoreOwner,
    factory: Factory = ViewModelProviders.getDefaultFactory(owner),
    creationExtras: CreationExtras = ViewModelProviders.getDefaultCreationExtras(owner),
): ViewModelProvider = ViewModelProvider.create(owner.viewModelStore, factory, creationExtras)
