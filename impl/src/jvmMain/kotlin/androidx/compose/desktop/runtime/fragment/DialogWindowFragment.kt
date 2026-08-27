package androidx.compose.desktop.runtime.fragment

import androidx.compose.desktop.runtime.savestate.ProvideAndroidCompositionLocals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.DialogWindowScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * DialogWindowFragment生命周期需要跟随DialogWindow的生命周期,
 * 因此, 创建DialogWindowFragment时不可以指定HostLifecycle.
 * 用法:
 * ```
 * class TestDialog : DialogWindowFragment() {
 *
 *     override fun onCreateView(): ComposableView {
 *         return ComposableView {
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
 *                         }
 *                     }
 *                 }
 *             }
 *         }
 *     }
 * }
 *
 * val testDialog by null.ownedFragment<TestDialog>(Token("dialog1"))
 *
 * testDialog.show()
 * testDialog.hide()
 * //dismiss之后无法再次显示
 * testDialog.dismiss()
 * ```
 */
open class DialogWindowFragment : Fragment() {
    var windowClosed by mutableStateOf(false)
        private set

    fun dismiss() {
        windowClosed = true
    }

    init {
        mVisibility.value = false
        if (isAttachedHostLifecycle()) {
            throw IllegalStateException("不可以指定宿主生命周期")
        }
    }

    @Composable
    override fun Screen() {
        if (!windowClosed) {
            this.mComposeView?.invoke()
        }
    }

    override fun show() {
        if (lifecycle.currentState == Lifecycle.State.DESTROYED
            || windowClosed
        ) {
            return
        }
        super.show()
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
            context = null,
            activityLifecycleOwner = null,
            fragmentLifecycleOwner = this@DialogWindowFragment,
            viewModelStoreOwner = this@DialogWindowFragment,
            savedStateRegistryOwner = this@DialogWindowFragment
        ) {
            content()
        }
    }
}
