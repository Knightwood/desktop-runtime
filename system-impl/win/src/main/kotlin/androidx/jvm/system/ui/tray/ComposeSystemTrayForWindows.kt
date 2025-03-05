package com.jetbrains.kmp.toolkit.tray.win

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.jetbrains.kmp.toolkit.compose.BaseOptionDialog
import com.jetbrains.kmp.toolkit.tray.IComposeSystemTray
import ir.amirab.util.compose.IconSource
import ir.amirab.util.compose.StringSource
import ir.amirab.util.compose.action.MenuItem
import ir.amirab.util.desktop.windows.moveSafe

internal object ComposeSystemTrayForWindows : IComposeSystemTray {
    @Composable
    override fun ComposeSystemTray(
        icon: IconSource,
        tooltip: StringSource,
        menu: List<MenuItem>,
        onClick: () -> Unit,
    ) {
        var popUpPosition by remember { mutableStateOf(null as DpOffset?) }
        val closeOptions = { popUpPosition = null }
        AwtTray(
            tooltip = tooltip.rememberString(),
            icon = icon.rememberPainter(),
            onClick = onClick,
            onRightClick = {
                popUpPosition = it
            }
        )
        popUpPosition.let { position ->
            if (position != null) {
                TrayOptions(
                    position,
                    closeOptions,
                ) {
                    Text("测试托盘菜单")
//                    SubMenu(menu, closeOptions)
                }
            }
        }
    }
}


@Composable
private fun TrayOptions(
    position: DpOffset,
    onRequestClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    val state = rememberDialogState(
        size = DpSize.Unspecified,
        position = WindowPosition.Absolute(
            x = position.x,
            y = position.y,
        )
    )

    BaseOptionDialog(
        onCloseRequest = onRequestClose,
        resizeable = false,
        state = state,
        content = {
            LaunchedEffect(window) {
                window.moveSafe(position)
            }
            content()
        }
    )
}
