package androidx.compose.desktop.runtime.viewmodel

import androidx.annotation.MainThread
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelLazy
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.compose.desktop.runtime.activity.ComponentActivity
import java.lang.reflect.Constructor

/**
 * ComponentActivity实现了ViewModelStoreOwner接口， 所以可以在Activity中生成viewModel 示例：
 *
 * ```
 *
 * // CreationExtras使用方式
 * val key = object : CreationExtras.Key<Int> {}
 * val extras = MutableCreationExtras()
 * extras[key] = 2
 *
 * //VM创建方式1
 * val f1 = viewModelFactory { initializer { TestViewModel(this[key]) } }
 * val v1 = ViewModelProvider.create(this, f1)[TestViewModel::class]
 *
 * // VM创建方式2
 * val f2 = object : ViewModelProvider.Factory {
 *     override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
 *         return TestViewModel(extras[key]!!) as T
 *     }
 * }
 * val v2 = ViewModelProvider.create(this, f1)[TestViewModel::class]
 * ```
 */
class ViewModelExtension

@MainThread
public inline fun <reified VM : ViewModel> ComponentActivity.viewModels(
    noinline extrasProducer: (() -> CreationExtras)? = null,
    noinline factoryProducer: (() -> Factory)? = null
): Lazy<VM> {
    val factoryPromise = factoryProducer ?: {
        defaultViewModelProviderFactory
    }

    return ViewModelLazy(
        VM::class,
        { viewModelStore },
        factoryPromise,
        { extrasProducer?.invoke() ?: this.defaultViewModelCreationExtras }
    )
}

private val VIEWMODEL_SIGNATURE = listOf<Class<*>>(SavedStateHandle::class.java)

fun <T : ViewModel> createVM(modelClass: Class<T>, extras: CreationExtras): T {
    val constructor = findMatchingConstructor(modelClass, VIEWMODEL_SIGNATURE)
    // doesn't need SavedStateHandle
    constructor ?: // If you are using a stateful constructor and no application is available, we
    // use an instance factory instead.
    return JvmViewModelProviders.createViewModel(modelClass)

    val savedStateHandle =extras.createSavedStateHandle()

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

