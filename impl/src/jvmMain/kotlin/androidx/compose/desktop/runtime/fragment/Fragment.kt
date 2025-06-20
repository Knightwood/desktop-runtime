package androidx.compose.desktop.runtime.fragment

import androidx.compose.desktop.runtime.activity.IBundleHolder
import androidx.compose.desktop.runtime.domain.ProvideAndroidCompositionLocals
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
 *         return ComposeView {
 *             MaterialTheme {
 *                  //省略
 *             }
 *         }
 *     }
 * }
 *
 * val a = ScreenComponent1()//生成实例
 * a.attach(this,bundleHolder)//绑定生命周期
 * a()//显示界面
 * ```
 */
open class Fragment() : IScreenComponent() {
    val mVisibility = mutableStateOf(true)
    private val mComposeView: IComposableViewHolder? by lazy {
        onCreateView()
    }

    fun attach(parentLifecycle: LifecycleOwner, bundleHolder: IBundleHolder) {
        prepare(parentLifecycle.lifecycle, bundleHolder)//先初始化，然后监听父级的生命周期进行同步
    }

    /**
     * 重写此方法，return ComposeViewHolder
     */
    open fun onCreateView(): IComposableViewHolder? {
        return null
    }

    /**
     * 在activity中调用此方法显示界面
     */
    @Composable
    operator fun invoke() {
        if (mVisibility.value) {
            this.mComposeView?.invoke()
        }
    }

    open fun show() {
        mVisibility.value = true
    }

    open fun hide() {
        mVisibility.value = false
    }


}

fun interface IComposableViewHolder {
    @Composable
    operator fun invoke()
}

class ComposeViewHolder(private val component: IScreenComponent) : IComposableViewHolder {
    var composeContent: ComposableContent? = null

    @Composable
    override fun invoke() {
        ProvideAndroidCompositionLocals(
            id = component.uuid,
            context = null,
            lifecycleOwner = component,
            viewModelStoreOwner = component,
            savedStateRegistryOwner = component
        ) {
            key(component.uuid) {
                composeContent?.invoke()
            }
        }
    }

    fun setContent(content: @Composable () -> Unit) {
        this.composeContent = content
    }
}
