package androidx.compose.desktop.runtime.fragment

import androidx.compose.desktop.runtime.activity.ISaveStateHolder
import androidx.compose.desktop.runtime.core.IScreenComponent
import androidx.compose.desktop.runtime.domain.ProvideAndroidCompositionLocalsForDialog
import androidx.compose.desktop.runtime.window.ComposableContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.*

/**
 * 用法：
 *
 * ```
 * class ScreenComponent1 : ScreenComponent() {
 *     init {
 *         clearBundle = false
 *     }
 *
 *     override fun onCreateView(): ComponentViewHolder {
 *         return setContent {
 *             MaterialTheme {
 *                  //省略
 *             }
 *         }
 *     }
 * }
 *
 * val a = ScreenComponent1()//生成实例
 * a.attach(this,bundleHolder)//绑定生命周期
 * a()          //显示界面,不使用状态保存机制
 * a.Screen()() //显示界面,使用状态保存机制
 * ```
 */
open class Fragment() : IScreenComponent() {
    val mVisibility = mutableStateOf(true)
    private val mComposeView: AbstractComposableView? by lazy {
        onCreateView()
    }

    fun attach(parentLifecycle: LifecycleOwner, bundleHolder: ISaveStateHolder) {
        prepare(parentLifecycle.lifecycle, bundleHolder)//先初始化，然后监听父级的生命周期进行同步
    }

    /**
     * 重写此方法，return ComposeViewHolder
     *
     * ```
     * override fun onCreateView(): AbstractComposableView {
     *      return setContent {
     *          //compose视图，省略
     *      }
     * }
     */
    open fun onCreateView(): AbstractComposableView? {
        return null
    }

    /**
     * 在activity中调用此方法显示界面
     * 此方法不使用状态保存机制
     */
    @Composable
    operator fun invoke() {
        if (mVisibility.value) {
            this.mComposeView?.invoke()
        }
    }

    /**
     * 在activity中调用此方法显示界面 此方法会提供状态保存机制
     */
    @Composable
    fun Screen() {
        ProvideAndroidCompositionLocalsForDialog(
            id = uuid,
            context = null,
            lifecycleOwner = this,
            viewModelStoreOwner = this,
            savedStateRegistryOwner = this
        ) {
            this.invoke()
        }
    }

    open fun show() {
        mVisibility.value = true
    }

    open fun hide() {
        mVisibility.value = false
    }

    fun setContent(content: @Composable () -> Unit): AbstractComposableView {
        return object : AbstractComposableView {
            @Composable
            override fun invoke() {
                content()
            }
        }
    }

}

fun interface AbstractComposableView {
    @Composable
    operator fun invoke()
}