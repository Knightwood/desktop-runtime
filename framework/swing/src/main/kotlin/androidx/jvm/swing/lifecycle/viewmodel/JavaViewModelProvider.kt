package androidx.jvm.swing.lifecycle.viewmodel

import androidx.annotation.MainThread
import androidx.compose.desktop.runtime.viewmodel.ViewModelProviders
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras


/**
 * 原版的ViewModelProvider方法都是为kotlin编写的，使用java语言调用并不好用，
 * 所以写了此类用于兼容java语法, 将功能委托给kt中的ViewModelProvider，辅助在java中创建viewmodel.
 *
 * ```
 * JavaViewModelProvider.create(
 *                 this,
 *                 BookEditorViewModel.Companion.getFactory()
 *         ).get(BookEditorViewModel.class);
 * ```
 */
class JavaViewModelProvider
private constructor(
    store: ViewModelStore,
    factory: Factory,
    creationExtras: CreationExtras = CreationExtras.Empty,
) {

    public constructor(
        owner: ViewModelStoreOwner,
    ) : this(
        store = owner.viewModelStore,
        factory = ViewModelProviders.getDefaultFactory(owner),
        creationExtras = ViewModelProviders.getDefaultCreationExtras(owner)
    )

    public constructor(
        owner: ViewModelStoreOwner,
        factory: ViewModelProvider.Factory,
    ) : this(
        store = owner.viewModelStore,
        factory = factory,
        creationExtras = ViewModelProviders.getDefaultCreationExtras(owner)
    )

    public constructor(
        owner: ViewModelStoreOwner,
        factory: ViewModelProvider.Factory,
        creationExtras: CreationExtras = CreationExtras.Empty,
    ) : this(
        store = owner.viewModelStore,
        factory = factory,
        creationExtras = creationExtras
    )


    val vmProvider = ViewModelProvider.create(store, factory, creationExtras)

    @MainThread
    public operator fun <T : ViewModel> get(modelClass: Class<T>): T =
        vmProvider.get(modelClass.kotlin)

    @MainThread
    public operator fun <T : ViewModel> get(key: String, modelClass: Class<T>): T =
        vmProvider.get(key, modelClass.kotlin)


    companion object {
        @JvmStatic
        @JvmOverloads
        public fun create(
            owner: ViewModelStoreOwner,
            factory: Factory = ViewModelProviders.getDefaultFactory(owner),
            creationExtras: CreationExtras = ViewModelProviders.getDefaultCreationExtras(owner),
        ): JavaViewModelProvider = JavaViewModelProvider(owner.viewModelStore, factory, creationExtras)

    }
}

