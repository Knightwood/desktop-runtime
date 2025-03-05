package androidx.jvm.system.ui.tray.impl

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import androidx.jvm.system.ui.tray.ISystemTray
import androidx.jvm.system.ui.tray.ITrayMenuItem
import androidx.jvm.system.ui.tray.FixedTrayMenuItem
import androidx.jvm.system.ui.tray.TraySeparator
import com.google.auto.service.AutoService

/**
 * 托盘图标和菜单的默认实现 使用DesktopTray添加托盘图标，点击事件监听等。 使用TrayOptions弹出window显示菜单选项。
 */
@AutoService(ISystemTray::class)
class DefaultFixedTray : ISystemTray {

    @Composable
    override fun ComposeSystemTray(
        icon: Painter,
        tooltip: String,
        menu: List<ITrayMenuItem>,
        onLeftClick: () -> Unit,
    ) {

        var popUpPosition by remember { mutableStateOf(null as DpOffset?) }
        val closeOptions = { popUpPosition = null }
        DesktopTray(
            tooltip = tooltip,
            icon = icon,
            onClick = onLeftClick,
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
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Column(
                            modifier = Modifier.widthIn(120.dp, 180.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            repeat(menu.size) {
                                val item = menu[it]
                                when (item) {
                                    is FixedTrayMenuItem -> {
                                        FixedTrayMenuItem(
                                            onClick = item.action ?: {},
                                            icon = item.icon,
                                            title = item.title
                                        )
                                    }

                                    is TraySeparator -> {
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.FixedTrayMenuItem(
    onClick: () -> Unit,
    icon: Painter?,
    title: String,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            icon?.let {
                Icon(it, contentDescription = null, modifier = Modifier.requiredSize(24.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium,modifier=Modifier.padding(bottom=4.dp))
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
