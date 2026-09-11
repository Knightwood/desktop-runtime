package androidx.jvm.system.ui.tray

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

sealed interface ITrayMenuItem

data object TraySeparator : ITrayMenuItem

class FixedTrayMenuItem(
    val title: String,
    val icon: Painter? = null,
    val action: (() -> Unit)? = null,
) : ITrayMenuItem {

}

class FixedTrayMenuBuilder {
    internal var menuList: List<ITrayMenuItem> = mutableListOf()

    operator fun plus(item: ITrayMenuItem) {
        menuList += item
    }

    companion object {
        fun buildTrayMenu(block:  FixedTrayMenuBuilder.() -> Unit): List<ITrayMenuItem> {
            val impl = FixedTrayMenuBuilder()
            impl.block()
            return impl.menuList
        }
    }
}

