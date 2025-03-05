package androidx.jvm.system.ui.tray

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.jvm.system.ui.tray.impl.isTraySupported
import java.util.*

/**
 * 托盘菜单由两部分组成：
 *
 * 使用java awt中的SystemTray和TrayIcon提供托盘菜单图标，点击事件监听。
 * 当托盘图标被点击后，显示一个window，在window中展示菜单内容。
 *
 *  SystemTray也提供了菜单功能，但是我们不使用它。
 *  在win平台，SystemTray实现托盘图标，使用compose实现菜单功能。
 *  在linux和mac上我们不使用SystemTray和TrayIcon，使用第三方库实现所有功能。
 */
@Composable
fun FixedSystemTray(
    icon: Painter,
    tooltip: String = "",
    menu: List<ITrayMenuItem> = remember { listOf() },
    onLeftClick: () -> Unit = {},
) {
    if (!isTraySupported)
        return
    val impl = remember {
        ServiceLoader.load(ISystemTray::class.java).firstOrNull()
    }
    impl?.ComposeSystemTray(icon, tooltip, menu, onLeftClick)
}

interface ISystemTray {
    @Composable
    fun ComposeSystemTray(
        icon: Painter,
        tooltip: String,
        menu: List<ITrayMenuItem>,
        onLeftClick: () -> Unit,
    )
}
