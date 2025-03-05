package androidx.jvm.system.ui.tray

import androidx.compose.ui.graphics.painter.Painter

class TrayMenuItem(
    val label: String,
    val icon: Painter? = null,
    val onClick: (() -> Unit)? = null,
    val subMenu: List<TrayMenuItem>
){

}

fun buildTrayMenu() {

}
