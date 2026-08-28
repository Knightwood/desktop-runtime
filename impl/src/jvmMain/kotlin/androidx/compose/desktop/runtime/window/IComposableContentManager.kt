package androidx.compose.desktop.runtime.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.FrameWindowScope
import com.sun.java.swing.plaf.windows.resources.windows

interface RootViewEntity<Scope> {
    var isAttached: Boolean
    var rootContent: (@Composable Scope.() -> Unit)?
}

class RootViewMgr<Scope, RootView : RootViewEntity<Scope>> {
    private val views: SnapshotStateList<RootView> = SnapshotStateList()

    @Composable
    fun Content(scope: Scope) {
        views.forEach { current ->
            key(current) {//避免无谓的重组
                current.rootContent?.invoke(scope)
            }
        }
    }

    /**
     * 移除window，这会使window进入onDispose
     */
    fun deAttach(window: RootView) {
        window.isAttached = false
        views.remove(window)
    }

    /**
     * 添加一个要显示的window，如果添加之前没有window，则调用prepare方法。
     */
    @Synchronized
    fun attach(window: RootView) {
        if (window.isAttached) return
        views.add(window)
        window.isAttached = true
    }

    fun clear() {
        views.clear()
    }
}

/**
 * 记录与某idn(token)关联的状态、activity根视图等内容
 */
class ActivityRootViewEntity() : RootViewEntity<ApplicationScope> {
    override var isAttached = false

    /**
     * activity 根视图
     * 注意: 需要在此compose函数实现中调用 [androidx.compose.ui.window.Window]
     */
    override var rootContent: ApplicationComposableContent? = null
        set(value) {
            if (field != null) {
                throw IllegalStateException("rootContent already set")
            }
            field = value
        }
}

class DialogRootViewEntity() : RootViewEntity<FrameWindowScope?> {
    override var isAttached = false

    /**
     * dialog 根视图
     * 注意: 需要在此compose函数实现中调用 [androidx.compose.ui.window.DialogWindow]
     */
    override var rootContent: (@Composable FrameWindowScope?.() -> Unit)? = null
        set(value) {
            if (field != null) {
                throw IllegalStateException("rootContent already set")
            }
            field = value
        }
}
