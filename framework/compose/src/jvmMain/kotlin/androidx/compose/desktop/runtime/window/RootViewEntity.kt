package androidx.compose.desktop.runtime.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.DialogWindowScope
import androidx.compose.ui.window.FrameWindowScope

/**
 * @property Scope 作用域, [ApplicationScope]、[FrameWindowScope]、[DialogWindowScope] 等
 * @property rootContent 记录某组件的根布局
 * @property isAttached 若已经attach到Scope并且进入了重组作用域, 为true
 */
class RootViewEntity<Scope> {
    var isAttached: Boolean = false
    var rootContent: (@Composable Scope.() -> Unit)? = null
        internal set(value) {
            if (field != null) {
                throw IllegalStateException("rootContent already set")
            }
            field = value
        }

    constructor()
    constructor(root: @Composable Scope.() -> Unit) {
        this.rootContent = root
    }
}

/**
 * 管理根视图[RootViewEntity]
 * @param Scope 视图发生的作用域, 可以指定为Unit不提供任何作用域.
 */
class RootViewMgr<Scope> {
    private val views: SnapshotStateList<RootViewEntity<Scope>> = SnapshotStateList()

    /**
     * 在compose作用域内调用, 显示所有的根视图
     * ```
     * //window
     * val mgr = RootViewMgr<ApplicationScope>()
     * val window = RootViewEntity<ApplicationScope> {
     *     Window(onCloseRequest = {}) {
     *
     *     }
     * }
     * attachDialog(window)
     *
     * //applicationScope,调用invoke显示所有添加到管理器的窗口
     * application {
     *     mgr.invoke()
     * }
     * ```
     */
    @Composable
    fun invoke(scope: Scope) {
        views.forEach { current ->
            key(current) {//避免无谓的重组
                current.rootContent?.invoke(scope)
            }
        }
    }


    /**
     * 添加一个要显示的根视图
     *
     * 根视图这个compose函数内部需要调用
     * [androidx.compose.ui.window.Window]或是[androidx.compose.ui.window.DialogWindow]
     * 这与Scope泛型关联有,但是不大, 比如Window要显示在ApplicationScope内,
     * 但是DialogWindow可以放在ApplicationScope中成为普通弹窗,也可以放在FrameWindowScope中成为模态弹窗
     *
     * ```
     * //dialog
     * val mgr = RootViewMgr<FrameWindowScope>()
     * val dialog = RootViewEntity<FrameWindowScope> {
     *     DialogWindow(onCloseRequest = {}) {
     *
     *     }
     * }
     * attachDialog(dialog)
     *
     * //window
     * val mgr = RootViewMgr<ApplicationScope>()
     * val window = RootViewEntity<ApplicationScope> {
     *     Window(onCloseRequest = {}) {
     *
     *     }
     * }
     * attachDialog(window)
     * ```
     *
     */
    @Synchronized
    fun attach(window: RootViewEntity<Scope>) {
        if (window.isAttached) return
        views.add(window)
        window.isAttached = true
    }

    /**
     * 移除根视图
     */
    fun deAttach(window: RootViewEntity<Scope>) {
        window.isAttached = false
        views.remove(window)
    }

    fun clear() = views.clear()

    fun isEmpty() = views.isEmpty()
}

/**
 * 记录与某idn(token)关联的状态、activity根视图等内容
 */
typealias ActivityRootViewEntity = RootViewEntity<ApplicationScope>

/**
 * 非嵌套的顶层Dialog可以显示在ApplicationScope或是FrameWindowScope, 二者没有继承关系, 只好仅提供FrameWindowScope
 */
typealias DialogRootViewEntity = RootViewEntity<FrameWindowScope?>

typealias NestedDialogRootViewEntity = RootViewEntity<DialogWindowScope>
