package androidx.compose.desktop.runtime.fragment

import androidx.compose.desktop.runtime.activity.Activity
import androidx.compose.desktop.runtime.core.BasicComponent
import androidx.compose.desktop.runtime.savestate.ProvideAndroidCompositionLocalsForDialog
import androidx.compose.desktop.runtime.savestate.Token
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.Lifecycle
import kotlin.reflect.KClass

/**
 *
 * fragment不能用于承载顶层窗口,内部不会与[androidx.compose.desktop.runtime.window.WindowManager]关联，
 * 调用[androidx.compose.ui.window.Window]是无效的.
 *
 * fragment的用处在于分解Activity视图结构,比如首页导航栏上有3个按钮,也就表示有三个页面,
 * 每个页面又会通向其他页面,即这是三个独立或关联的导航树,
 * 此时当然可以直接使用NavHost,构建三个导航树,但这样一来,很多东西会耦合进Activity.
 * 于是可以使用Fragment将三个导航树隔离开来,此时,Fragment与android中的Fragment就很相似了.
 *
 *
 * 用法：
 *
 * ```
 * class Fragment1 : Fragment() {
 *
 *     override fun onCreateView(): ComposableView {
 *         return ComposableView {
 *             MaterialTheme {
 *                  //省略
 *             }
 *         }
 *     }
 * }
 *
 * val a = Fragment1()//生成实例
 * a.attach(Token("a1"),parentLifecycle)//绑定生命周期
 * a.Screen()() //显示界面,使用状态保存机制
 * ```
 */
open class Fragment() : BasicComponent() {
    val mVisibility = mutableStateOf(true)
    private val mComposeView: ComposableView? by lazy {
        onCreateView()
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
    open fun onCreateView(): ComposableView? {
        return null
    }

    /**
     * 在activity中调用此方法显示界面 此方法会提供状态保存机制
     */
    @Composable
    fun Screen() {
        ProvideAndroidCompositionLocalsForDialog(
            id = idn.toString(),
            context = null,
            lifecycleOwner = this,
            viewModelStoreOwner = this,
            savedStateRegistryOwner = this
        ) {
            if (mVisibility.value) {
                this.mComposeView?.invoke()
            }
        }
    }

    open fun show() {
        mVisibility.value = true
    }

    open fun hide() {
        mVisibility.value = false
    }

    fun interface ComposableView {
        @Composable
        operator fun invoke()
    }

}

/**
 * 生成fragment实例
 */
fun <T : Fragment> fragment(cls: KClass<T>, parentLifecycle: Lifecycle, token: Token? = null): T {
    val constructor = cls.java.getDeclaredConstructor()
    constructor.isAccessible = true
    val instance = constructor.newInstance() as T
    instance.attach(token, parentLifecycle)
    return instance
}

/**
 * 生成fragment实例
 */
inline fun <reified T : Fragment> fragment(parentLifecycle: Lifecycle, token: Token? = null): T {
    return fragment(T::class, parentLifecycle, token)
}

/**
 * 在activity中生成延迟初始化的fragment实例
 * ```
 * val instance by fragment<ExampleFragment>(Token("a1"))
 * ```
 */
inline fun <reified T : Fragment> Activity.fragment(token: Token? = null): Lazy<T> {
    val act = this
    return object : Lazy<T> {
        private var cached: T? = null
        override val value: T
            get() {
                if (cached == null) {
                    cached = fragment<T>(act.lifecycle, token)
                }
                return cached!!
            }

        override fun isInitialized(): Boolean = cached != null

    }
}
