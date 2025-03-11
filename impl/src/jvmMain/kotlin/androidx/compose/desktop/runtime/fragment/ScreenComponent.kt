package androidx.compose.desktop.runtime.fragment

import androidx.compose.desktop.runtime.activity.IBundleHolder
import androidx.compose.desktop.runtime.domain.ProvideAndroidCompositionLocals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.*

/**
 * 用法：
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
open class ScreenComponent() : IScreenComponent() {
    val mVisibility = mutableStateOf(true)
    private var mComposeView: ComponentViewHolder? = null

    override fun onStateChanged(event: Lifecycle.Event) {}

    fun attach(parentLifecycle: Lifecycle, bundleHolder: IBundleHolder) {
        prepare(parentLifecycle,bundleHolder)//先初始化，然后监听父级的生命周期进行同步
    }

    /**
     * 重写此方法，调用[ComposeView]并return提供界面
     */
    open fun onCreateView(): ComponentViewHolder? {
        return null
    }

    /**
     * 在activity中调用此方法显示界面
     */
    @Composable
    operator fun invoke() {
        if (this.mComposeView == null)
            this.mComposeView = onCreateView()
        if (mVisibility.value) {
            this.mComposeView?.invoke()
        }
    }

    fun ComposeView(content: @Composable () -> Unit): ComponentViewHolder {
        return ComponentViewHolder {
            key(uuid) {
                ProvideAndroidCompositionLocals(
                    id = uuid,
                    context = null,
                    lifecycleOwner = this,
                    viewModelStoreOwner = this,
                    savedStateRegistryOwner = this
                ) {
                    content()
                }
            }
        }
    }

    fun show() {
        mVisibility.value = true
    }

    fun hide() {
        mVisibility.value = false
    }


}


fun interface ComponentViewHolder {
    @Composable
    fun invoke()
}
