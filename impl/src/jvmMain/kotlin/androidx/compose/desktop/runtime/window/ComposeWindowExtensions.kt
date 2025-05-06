package androidx.compose.desktop.runtime.window

import androidx.compose.ui.awt.ComposeWindow
import javax.swing.JFrame

/**
 * 修改窗口大小，状态 比如最大化，最小化，还原等
 */
fun ComposeWindow.setWindowSizeState(state: WindowSizeState) {
    this.extendedState = when (state) {
        WindowSizeState.Max -> JFrame.MAXIMIZED_BOTH
        WindowSizeState.Min -> JFrame.ICONIFIED
        WindowSizeState.Restore -> JFrame.NORMAL
    }
}
