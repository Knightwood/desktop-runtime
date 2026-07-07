package androidx.jvm.system.ui.tray

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import androidx.jvm.system.ui.tray.impl.FixedTray
import androidx.jvm.system.ui.tray.impl.isTraySupported
import androidx.jvm.system.ui.tray.menu.DefaultFixedTrayMenuScope
import androidx.jvm.system.ui.tray.menu.FixedTrayMenuScope
import androidx.jvm.system.ui.tray.menu_dialog.BaseOptionsWindow
import androidx.jvm.system.utils.GlobalDensity
import com.github.knightwood.slf4j.kotlin.logFor
import com.google.auto.service.AutoService
import java.util.*

/**
 * 托盘菜单由两部分组成：
 *
 * 使用java awt中的SystemTray和TrayIcon提供托盘菜单图标，点击事件监听。
 * awt中的托盘虽然提供了右键菜单，但是不灵活、界面不好看、显示中文存在问题。
 * 因此，我们自定义实现右键菜单显示，当托盘图标被点击后，显示一个window，在window中展示菜单内容。
 */
@Composable
fun ApplicationScope.FixedSystemTray(
    icon: Painter,
    tooltip: String = "",
    primaryAction: () -> Unit = {},
    menu: @Composable FixedTrayMenuScope.() -> Unit,
) {
    if (!isTraySupported)
        return
    val impl = remember {
        ServiceLoader.load(IFixedSystemTray::class.java).firstOrNull()
    }
    impl?.run {
        TrayImpl(icon, tooltip, primaryAction, menu)
    }
}

interface IFixedSystemTray {
    @Composable
    fun ApplicationScope.TrayImpl(
        icon: Painter,
        tooltip: String,
        primaryAction: () -> Unit,
        menu: @Composable FixedTrayMenuScope.() -> Unit,
    )
}


/**
 * 托盘图标和菜单的默认实现 使用DesktopTray添加托盘图标，点击事件监听等。 使用TrayOptions弹出window显示菜单选项。
 */
@AutoService(IFixedSystemTray::class)
internal class DefaultFixedTrayFixed : IFixedSystemTray {
    private val logger = logFor("托盘菜单默认实现")

    @Composable
    override fun ApplicationScope.TrayImpl(
        icon: Painter,
        tooltip: String,
        primaryAction: () -> Unit,
        menu: @Composable FixedTrayMenuScope.() -> Unit,
    ) {
        var popUpPosition by remember { mutableStateOf<IntOffset?>(null) }
        val closeOptions = { popUpPosition = null }
        FixedTray(
            tooltip = tooltip,
            icon = icon,
            onClick = primaryAction,
            onRightClick = {
                popUpPosition = it
                logger.debug("点击位置:$it")
            }
        )
        popUpPosition?.let { position ->
            val position = with(GlobalDensity) {
                WindowPosition.Absolute(
                    x = position.x.toDp(),
                    y = position.y.toDp(),
                )
            }
            val state = rememberDialogState(
                size = DpSize.Companion.Unspecified,
                position = position,
            )
            BaseOptionsWindow(
                onCloseRequest = closeOptions,
                resizeable = false,
                state = state,
                content = {
                    DefaultFixedTrayMenuScope.menu()
                }
            )
        }
    }
}

object TrayConf {
    var transparent = false
}
