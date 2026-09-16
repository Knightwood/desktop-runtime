@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.desktop.runtime.activity

import androidx.annotation.CallSuper
import androidx.compose.desktop.runtime.core.Singularity
import androidx.compose.desktop.runtime.savestate.ProvideAndroidCompositionLocals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.FrameWindowScope
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

/**
 *
 * 提供ViewModelStoreOwner、SaveStateRegister、SaveableStateRegister等组件，
 * compose中的rememberSavable现可以正常工作
 *
 * 在ComponentActivity中获取ViewModel
 * ```
 * class TestViewModel1(
 *     val savedStateHandle: SavedStateHandle,
 *     val i: Int,
 * ) : ViewModel(){
 *     companion object {
 *         val key = object : CreationExtras.Key<Int> {}
 *         val factory =
 *             object : ViewModelProvider.Factory {
 *                 override fun <T : ViewModel> create(
 *                     modelClass: KClass<T>,
 *                     extras: CreationExtras,
 *                 ): T {
 *                     return TestViewModel1(
 *                         extras.createSavedStateHandle(),
 *                         extras[key] ?: 90
 *                     ) as T
 *                 }
 *             }
 *     }
 * }
 *
 * class TestViewModel2(
 *     val savedStateHandle: SavedStateHandle,
 * ) : ViewModel()
 *
 * val vm1: TestViewModel1 by viewModels<TestViewModel1>(extrasProducer = {
 *      val extras = MutableCreationExtras()
 *      extras[TestViewModel1.key] = intent?.getData<Int>("random") ?: 11//从 intent中读取数据
 *      extras[SAVED_STATE_REGISTRY_OWNER_KEY] = this
 *      extras[VIEW_MODEL_STORE_OWNER_KEY] = this
 *      extras
 *  }, { TestViewModel1.factory })
 *
 * val vm2 by viewModels<TestViewModel2>()
 *
 * val vm3 = ViewModelProvider.create(
 *      owner = this,
 *      creationExtras = mutableCreationExtrasOf {
 *          this[TestViewModel1.key] = intent?.getData<Int>("random") ?: 11//从 intent中读取数据
 *      },
 *      factory = TestViewModel1.factory
 *  )[TestViewModel1::class]
 *
 * ```
 *
 */
open class ComponentActivity : Activity(),
    ViewModelStoreOwner,
    HasDefaultViewModelProviderFactory,
    SavedStateRegistryOwner
{
    private var _viewModelStore: ViewModelStore? = null
    override val viewModelStore: ViewModelStore
        /**
         * Returns the [ViewModelStore] associated with this activity
         *
         * @return a [ViewModelStore]
         * @throws IllegalStateException if called before the Activity is attached
         *    to the Application instance i.e., before onCreate()
         */
        get() {
            check(Singularity.isApplicationExist()) {
                ("Your activity is not yet attached to the " +
                        "Application instance. You can't request ViewModel before onCreate call.")
            }
            ensureViewModelStore()
            return _viewModelStore!!
        }

    @Suppress("LeakingThis")
    private val savedStateRegistryController: SavedStateRegistryController =
        SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    init {
        @Suppress("LeakingThis")
        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    viewModelStore.clear()
                }
            }
        })
        savedStateRegistryController.performAttach()
        enableSavedStateHandles()
    }

    @CallSuper
    override fun onCreate(savedInstanceState: SavedState?) {
        savedStateRegistryController.performRestore(savedState)
        super.onCreate(savedInstanceState)
    }


    override fun onSaveInstanceState(outState: SavedState) {
        super.onSaveInstanceState(outState)
        savedStateRegistryController.performSave(outState)
    }


    private fun ensureViewModelStore() {
        if (_viewModelStore == null) {
            _viewModelStore = ViewModelStore()
        }
    }

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory by lazy {
        androidx.compose.desktop.runtime.viewmodel.MySavedStateViewModelFactory
    }

    public override val defaultViewModelCreationExtras: CreationExtras
        get() {
            val extras = MutableCreationExtras()
            extras[SAVED_STATE_REGISTRY_OWNER_KEY] = this
            extras[VIEW_MODEL_STORE_OWNER_KEY] = this
            return extras
        }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
    }

    /**
     * 调用[androidx.compose.ui.window.Window]时在传入的content函数中调用此函数
     * 功能：
     * 1. 使Activity链接ComposeWindow生命周期
     * 2. 向Compose子视图提供ViewModelStoreOwner、SaveStateRegister、SaveableStateRegister等组件
     */
    @Composable
    override fun FrameWindowScope.LinkWindow(content: @Composable FrameWindowScope.() -> Unit){
        this@ComponentActivity.composeWindow = this.window
        //这里的lifecycle是composeContainer的提供的
        val lc: LifecycleOwner = LocalLifecycleOwner.current
        remember {
            lc.lifecycle.addObserver(parentLifecycleObserver)
        }

        //适配compose 1.9，我们需要使用自己的状态存储恢复覆盖掉kmp内部的。
        ProvideAndroidCompositionLocals(
            id = idn.toString(),
            context = this@ComponentActivity,
            activityLifecycleOwner = this@ComponentActivity,
            fragmentLifecycleOwner = null,
            viewModelStoreOwner = this@ComponentActivity,
            savedStateRegistryOwner = this@ComponentActivity
        ) {
            content()
        }
        //显示添加到Activity的弹窗
        dialogsMgr.invoke(Unit)
    }

}
