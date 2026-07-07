package androidx.jvm.system.ui.tray.menu_dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.rememberDialogState
import androidx.jvm.system.ui.tray.TrayConf
import java.awt.Dialog
import java.awt.event.ComponentAdapter
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener

@Composable
internal fun BaseOptionsWindow(
    onCloseRequest: () -> Unit,
    state: DialogState = rememberDialogState(),
    resizeable: Boolean = true,
    content: @Composable WindowScope.() -> Unit,
) {
    DialogWindow(
        visible = true,
        state = state,
        undecorated = true,
        transparent = TrayConf.transparent,
        resizable = resizeable,
        onCloseRequest = onCloseRequest,
    ) {
        val focusListener = remember {
            object : WindowFocusListener {
                override fun windowGainedFocus(e: WindowEvent?) {
                    //do nothing
                }

                override fun windowLostFocus(e: WindowEvent) {
                    onCloseRequest()
                }
            }
        }
        val componentAdapter = remember {
            object : java.awt.event.ComponentAdapter() {
                var moved = false
                override fun componentResized(e: java.awt.event.ComponentEvent?) {
                    if (!moved && window.width > 0 && window.height > 0) {
                        moved = true
                        window.moveSafe()
                    }
                }

                override fun componentShown(e: java.awt.event.ComponentEvent?) {
                    if (!moved && window.width > 0 && window.height > 0) {
                        moved = true
                        window.moveSafe()
                    }
                }
            }
        }
        DisposableEffect(window) {
            window.addComponentListener(componentAdapter)
            window.addWindowFocusListener(focusListener)
            window.isAlwaysOnTop = true
            //we need this to allow click outside
            window.modalityType = Dialog.ModalityType.MODELESS
            onDispose {
                window.removeWindowFocusListener(focusListener)
                window.removeComponentListener(componentAdapter)
            }
        }
        /*
        LaunchedEffect(window) {
                // 等待窗口布局完成，否则在新版compose（1.12）中拿不到窗口尺寸
                while (window.width == 0 || window.height == 0) {
                    delay(10) // 短暂等待
                }
                window.moveSafe(position)
            }
        */
        content()
    }
}

private fun ComposeDialog.moveSafe(){
    moveSafe(IntOffset(x,y))
}
