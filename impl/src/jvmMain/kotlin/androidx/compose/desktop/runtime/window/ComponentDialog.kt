package androidx.compose.desktop.runtime.window

import androidx.annotation.CallSuper
import androidx.compose.desktop.runtime.activity.Activity
import androidx.compose.desktop.runtime.core.context.Context
import androidx.compose.desktop.runtime.fragment.BasicComponent
import androidx.compose.desktop.runtime.savestate.ProvideAndroidCompositionLocals
import androidx.compose.desktop.runtime.savestate.Token
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.DialogWindowScope
import androidx.compose.ui.window.WindowScope
import androidx.jvm.system.di.inject
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.reflect.KClass

/**
 * 用于显示[androidx.compose.ui.window.DialogWindow]
 *
 * ComponentDialog生命周期需要跟随DialogWindow的生命周期,
 * 因此, 创建ComponentDialog时不可以指定HostLifecycle.
 * ```
 * class TestDialog : ComponentDialog() {
 *     override fun onCreate(savedInstanceState: SavedState?) {
 *         super.onCreate(savedInstanceState)
 *         setContentView {
 *             DialogWindow(
 *                 onCloseRequest = {
 *                     dismiss()
 *                 },
 *                 visible = mVisibility.value,
 *             ) {
 *                 Link2ComposeDialogWindow {
 *                     MaterialTheme {
 *                         Column {
 *                             Text("dialog")
 *                             SampleButton("隐藏dialog1") {
 *                                 hide()
 *                             }
 *                         }
 *                     }
 *                 }
 *             }
 *         }
 *     }
 * }
 *
 * val testDialog = componentDialog<TestDialog>(this, Token("dialog1"))
 *
 * testDialog.show()
 * testDialog.hide()
 * //dismiss之后无法再次显示
 * testDialog.dismiss()
 * ```
 */
open class ComponentDialog : BasicComponent() {
    val mVisibility = mutableStateOf(false)
    internal var rootViewEntity = RootViewEntity<Unit>()
    internal var parent: ComponentDialog? = null
    internal val childrenDialogs = RootViewMgr<Unit>()
    val windowManager by inject<WindowManager>()
    internal var modal = false
    private var destroyed by mutableStateOf(false)

    override fun attach(token: Token?, context: Context, hostLifecycle: Lifecycle?) {
        super.attach(token, context, null)
    }

    fun setContentView(content: @Composable Unit.() -> Unit) {
        this.rootViewEntity.rootContent = content
    }

    @CallSuper
    open fun show() {
        if (destroyed) {
            throw IllegalStateException("Already destroyed, cannot show again")
        }
        mVisibility.value = true
        if (parent != null) {
            parent?.attachDialog(rootViewEntity)
        } else {
            if (modal && context is Activity) {
                (context as Activity).attachDialog(rootViewEntity)
            } else {
                windowManager.attachDialog(rootViewEntity)
            }
        }
    }

    @CallSuper
    open fun hide() {
        mVisibility.value = false
    }

    /**
     * dismiss之后不允许再次显示
     */
    @CallSuper
    open fun dismiss() {
        if (destroyed) {
            return
        }
        mVisibility.value = false
        if (rootViewEntity.isAttached) {
            if (parent != null) {
                parent?.deAttachDialog(rootViewEntity)
            } else {
                if (modal && context is Activity) {
                    (context as Activity).deAttachDialog(rootViewEntity)
                } else {
                    windowManager.deAttachDialog(rootViewEntity)
                }
            }
        } else {
            //没有界面,直接将生命周期同步到ON_DESTROY
            deAttachHostLifecycle()
            syncLife(ON_DESTROY)
            onDestroy()
        }
        destroyed = true
    }

    override fun finish() {
        this.dismiss()
    }

    /**
     * 同步Compose DialogWindow的生命周期, 为compose提供生命周期组件
     */
    @Composable
    fun DialogWindowScope.Link2ComposeDialogWindow(content: @Composable DialogWindowScope.() -> Unit) {
        //这里的lifecycle是composeContainer的提供的
        val lc: LifecycleOwner = LocalLifecycleOwner.current
        remember {
            attachHostLifecycle(lc.lifecycle)
        }
        ProvideAndroidCompositionLocals(
            id = idn.toString(),
            context = context!!,
            activityLifecycleOwner = null,
            fragmentLifecycleOwner = this@ComponentDialog,
            viewModelStoreOwner = this@ComponentDialog,
            savedStateRegistryOwner = this@ComponentDialog
        ) {
            content()
        }
        childrenDialogs.invoke(Unit)
    }

    //<editor-fold desc="生成嵌套弹窗">
    /**
     * 移除Dialog
     */
    internal fun deAttachDialog(window: RootViewEntity<Unit>) {
        childrenDialogs.deAttach(window)
    }

    /**
     * 添加一个要显示的Dialog。
     */
    @Synchronized
    internal fun attachDialog(window: RootViewEntity<Unit>) {
        childrenDialogs.attach(window)
    }

    /**
     * 生成ComponentDialog实例
     * @param cls 要生成的ComponentDialog的class
     * @param token 标识ComponentDialog保存状态的唯一性,可传入null,表示不使用状态保存恢复功能
     */
    fun <T : ComponentDialog> nestDialog(
        cls: KClass<T>,
        token: Token? = null,
    ): T {
        val constructor = cls.java.getDeclaredConstructor()
        constructor.isAccessible = true
        val instance = constructor.newInstance() as T
        instance.attach(token, context!!, null)
        instance.modal = true
        instance.parent = this@ComponentDialog
        return instance
    }

    /**
     * 生成ComponentDialog实例
     * @param token 标识保存状态的唯一性,可传入null,表示不使用状态保存恢复功能
     */
    inline fun <reified T : ComponentDialog> nestDialog(
        token: Token? = null,
    ): T {
        return nestDialog(T::class, token)
    }

//</editor-fold>
}
//<editor-fold desc="生成弹窗">

/**
 * 生成ComponentDialog实例
 * @param cls 要生成的ComponentDialog的class
 * @param token 标识ComponentDialog保存状态的唯一性,可传入null,表示不使用状态保存恢复功能
 */
fun <T : ComponentDialog> componentDialog(
    cls: KClass<T>,
    context: Context,
    modal: Boolean = false,
    token: Token? = null,
): T {
    val constructor = cls.java.getDeclaredConstructor()
    constructor.isAccessible = true
    val instance = constructor.newInstance() as T
    instance.attach(token, context, null)
    instance.modal = modal
    return instance
}

/**
 * 生成ComponentDialog实例
 * @param token 标识ComponentDialog保存状态的唯一性,可传入null,表示不使用状态保存恢复功能
 */
inline fun <reified T : ComponentDialog> componentDialog(
    context: Context,
    modal: Boolean = false,
    token: Token? = null,
): T {
    return componentDialog(T::class, context, modal, token)
}

//</editor-fold>

