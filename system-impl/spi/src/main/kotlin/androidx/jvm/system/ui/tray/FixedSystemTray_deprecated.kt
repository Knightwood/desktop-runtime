package androidx.jvm.system.ui.tray

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope


@Deprecated("使用ApplicationScope.FixedSystemTray(icon,tooltip,primaryAction,menu)替代")
@Composable
fun ApplicationScope.FixedSystemTray(
    icon: Painter,
    tooltip: String = "",
    menu: List<ITrayMenuItem> = remember { listOf() },
    onLeftClick: () -> Unit = {},
) {
    FixedSystemTray(icon, tooltip, primaryAction = onLeftClick) {
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

//<editor-fold desc="托盘菜单item项">
@Deprecated("")
sealed interface ITrayMenuItem

@Deprecated("")
data object TraySeparator : ITrayMenuItem

@Deprecated("")
class FixedTrayMenuItem(
    val title: String,
    val icon: Painter? = null,
    val action: (() -> Unit)? = null,
) : ITrayMenuItem {

}

@Deprecated("")
class FixedTrayMenuBuilder {
    internal var menuList: List<ITrayMenuItem> = mutableListOf()

    @Deprecated("")
    operator fun plus(item: ITrayMenuItem) {
        menuList += item
    }

    companion object {
        @Deprecated("")
        fun buildTrayMenu(block:  FixedTrayMenuBuilder.() -> Unit): List<ITrayMenuItem> {
            val impl = FixedTrayMenuBuilder()
            impl.block()
            return impl.menuList
        }
    }
}
//</editor-fold>
