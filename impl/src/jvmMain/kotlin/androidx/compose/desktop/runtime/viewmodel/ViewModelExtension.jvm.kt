package androidx.compose.desktop.runtime.viewmodel

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelLazy
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.compose.desktop.runtime.activity.ComponentActivity

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