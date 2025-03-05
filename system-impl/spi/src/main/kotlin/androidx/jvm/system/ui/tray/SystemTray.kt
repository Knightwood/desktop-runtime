package androidx.jvm.system.ui.tray

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.util.*

@Composable
fun SystemTray() {
    val impl = remember {
        ServiceLoader.load(ISystemTray::class.java).firstOrNull()
    }
    impl?.view()
}

interface ISystemTray {
    fun view()
}
