package androidx.compose.desktop.runtime.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import java.lang.Class
import java.lang.reflect.Constructor
import kotlin.reflect.KClass

object SavedStateViewModelFactory: ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        return createVM(modelClass.java, extras)
    }
}

private val VIEWMODEL_SIGNATURE = listOf<Class<*>>(SavedStateHandle::class.java)

internal fun <T : ViewModel> createVM(modelClass: Class<T>, extras: CreationExtras): T {
    val constructor = findMatchingConstructor(modelClass, VIEWMODEL_SIGNATURE)
    // doesn't need SavedStateHandle
    constructor ?: // If you are using a stateful constructor and no application is available, we
    // use an instance factory instead.
    return JvmViewModelProviders.createViewModel(modelClass)

    val savedStateHandle = extras.createSavedStateHandle()

    return constructor.newInstance(savedStateHandle)
        ?: throw IllegalArgumentException("ViewModel class $modelClass has no constructor with $VIEWMODEL_SIGNATURE")
}

internal fun <T> findMatchingConstructor(
    modelClass: Class<T>,
    signature: List<Class<*>>
): Constructor<T>? {
    for (constructor in modelClass.constructors) {
        val parameterTypes = constructor.parameterTypes.toList()
        if (signature == parameterTypes) {
            @Suppress("UNCHECKED_CAST")
            return constructor as Constructor<T>
        }
        if (signature.size == parameterTypes.size && parameterTypes.containsAll(signature)) {
            throw UnsupportedOperationException(
                "Class ${modelClass.simpleName} must have parameters in the proper " +
                        "order: $signature"
            )
        }
    }
    return null
}

