package androidx.compose.desktop.runtime.fragment

import androidx.compose.desktop.runtime.activity.BundleHolder
import androidx.compose.desktop.runtime.activity.IBundleHolder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.DialogWindowScope
import androidx.compose.ui.window.rememberDialogState
import androidx.lifecycle.Lifecycle
import com.github.knightwood.slf4j.kotlin.kLogger


/**
 * 包装DialogWindow
 *
 * ```
 * var isDialogOpen by remember { mutableStateOf(false) }
 *
 * Button(onClick = { isDialogOpen = true }) {
 *     Text(text = "Open dialog")
 * }
 *
 * if (isDialogOpen) {
 *     DialogWindow(
 *         onCloseRequest = { isDialogOpen = false },
 *         state = rememberDialogState(position = WindowPosition(Alignment.Center))
 *     ) {
 *         // Content of the window
 *     }
 * }
 * ```
 */
abstract class DialogFragment : Fragment() {

    init {
        mVisibility.value = false
    }

    @Composable
    fun Dialog(
        state: DialogState = rememberDialogState(),
        title: String = "Untitled",
        icon: Painter? = null,
        undecorated: Boolean = false,
        transparent: Boolean = false,
        resizable: Boolean = true,
        enabled: Boolean = true,
        focusable: Boolean = true,
        onPreviewKeyEvent: ((KeyEvent) -> Boolean) = { false },
        onKeyEvent: ((KeyEvent) -> Boolean) = { false },
        content: @Composable DialogWindowScope.() -> Unit
    ) {
        val closed = released.collectAsState().value
        if (!closed) {
            DialogWindow(
                onCloseRequest = ::hide,
                state = state,
                visible = mVisibility.value,
                title = title,
                icon = icon,
                undecorated = undecorated,
                transparent = transparent,
                resizable = resizable,
                enabled = enabled,
                focusable = focusable,
                onPreviewKeyEvent = onPreviewKeyEvent,
                onKeyEvent = onKeyEvent,
                content = content
            )
        }
    }

    /**
     * 调用此方法后，弹窗将结束生命周期，不可以再次显示
     */
    fun dismiss() {
        release()
    }

    override fun show() {
        super.show()
        if (released.value) {
            kLogger.error("dialog component is released,cannot show. uuid: $uuid")
        }
    }

    companion object {
        fun <T : DialogFragment> makeDialog(
            cls: Class<T>,
            lifecycle: Lifecycle,
            iBundleHolder: IBundleHolder = BundleHolder()
        ): T {
            val fragment = cls.getDeclaredConstructor().newInstance()
            fragment.attach(lifecycle, iBundleHolder)
            return fragment
        }
    }
}
