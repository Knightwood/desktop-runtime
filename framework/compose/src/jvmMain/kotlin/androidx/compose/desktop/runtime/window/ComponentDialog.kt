package androidx.compose.desktop.runtime.window

import androidx.annotation.CallSuper
import androidx.compose.desktop.runtime.activity.Activity
import androidx.compose.desktop.runtime.core.context.Context
import androidx.compose.desktop.runtime.fragment.BasicComponent
import androidx.compose.desktop.runtime.savestate.ApplicationSaveStateSaver
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
import androidx.compose.ui.window.DialogWindow

/**
 * 用于显示[DialogWindow]
 *
 * 核心原理：
 * 调用DialogWindow显示弹窗窗口，根据调用DialogWindow时是否存在父级窗口，可以分为模态和非模态
 * ```
 * application {
 *     var windowVisible by remember { mutableStateOf(true) }
 *     if (windowVisible) {
 *         Window(onCloseRequest = { windowVisible = false }){
 *             //在窗口内部调用，由于存在父级窗口，会显示为模态窗口
 *             DialogWindow(onCloseRequest = {}){}
 *         }
 *     }
 *     //application中直接调用，由于没有父级窗口，会显示非模态窗口
 *     DialogWindow(onCloseRequest = {}){}
 * }
 * ```
 * 我们将调用DialogWindow的ComponentDialog根视图添加到不同父组件根视图中以显示窗口弹窗
 * 根据添加到的父组件，区分出模态、非模态、嵌套弹窗等。
 *
 * 生命周期：
 * ComponentDialog生命周期会跟随DialogWindow, 创建ComponentDialog不可以指定HostLifecycle.
 *
 * 使用方式：
 * ```
 * // 1. 定义弹窗实现
 *
 * class TestDialog : ComponentDialog() {
 *     override fun onCreate(savedInstanceState: SavedState?) {
 *         super.onCreate(savedInstanceState)
 *         setContentView {
 *             DialogWindow(
 *                 onCloseRequest = { dismiss() },
 *                 visible = mVisibility.value,
 *             ) {
 *                 LinkDialogWindow {
 *                     Button(onClick = {
 *                         val testDialog = nestDialog<TestDialog>()
 *                         testDialog.show()
 *                     }) {
 *                         Text("嵌套dialog")
 *                     }
 *                 }
 *             }
 *         }
 *     }
 * }
 * // 2. 生成实例
 * val testDialog = componentDialog<TestDialog>(this, Token("dialog1"))
 *
 * //3. 显示、隐藏、销毁
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

    /**
     * 由框架内部调用，开始组件的生命周期。
     *
     * @param token 标识唯一性，不为null时自动从[ApplicationSaveStateSaver]中注册获取SavedState实例
     * @param context 上下文
     * @param hostLifecycle 宿主生命周期。无用参数，由于ComponentDialog同步SwingDialog生命周期，传递此参数也不会起作用。
     */
    override fun attach(token: Token?, context: Context, hostLifecycle: Lifecycle?) {
        super.attach(token, context, null)
    }

    /**
     * 设置弹窗窗口根视图，只要保持在show方法调用前调用即可
     * 因此可以在onCreate方法中设置视图，也可以在创建dialog后，show()方法调用之前设置视图
     *
     * 调用此方法需在传入content实现中调用[DialogWindow]
     * ```
     * setContentView {
     *     DialogWindow(
     *         onCloseRequest = { dismiss() },
     *         visible = mVisibility.value,
     *     ) {
     *         LinkDialogWindow {
     *             Button(onClick = {
     *                 val testDialog = nestDialog<TestDialog>()
     *                 testDialog.show()
     *             }) {
     *                 Text("嵌套dialog")
     *             }
     *         }
     *     }
     * }
     * ```
     */
    fun setContentView(content: @Composable ComponentDialog.() -> Unit) {
        this.rootViewEntity.rootContent = { content() }
    }

    /**
     * 显示弹窗窗口，需要在调用[setContentView]之后调用.
     */
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
     *
     * 将ComponentDialog根视图从重组树上移除, 触发DialogWindow中DisposeEffect以销毁SwingDialog，
     * 不再显示compose视图，结束SwingDialog生命周期。
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
     * 需要在[DialogWindow]内部调用
     *
     * 功能：
     * 1. 使ComponentDialog链接ComposeDialogWindow生命周期
     * 2. 向Compose子视图提供ViewModelStoreOwner、SaveStateRegister、SaveableStateRegister等组件
     */
    @Composable
    fun DialogWindowScope.LinkDialogWindow(content: @Composable DialogWindowScope.() -> Unit) {
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
     * 生成嵌套的ComponentDialog实例,新的弹窗窗口将以当前ComponentDialog作为父组件,显示为一个模态弹窗窗口
     *
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

