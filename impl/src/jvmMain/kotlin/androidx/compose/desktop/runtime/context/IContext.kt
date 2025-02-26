package androidx.compose.desktop.runtime.context

import androidx.compose.desktop.runtime.activity.*
import androidx.compose.desktop.runtime.activity.Intent.Companion.plus
import androidx.compose.desktop.runtime.core.Application
import androidx.compose.desktop.runtime.window.WindowManager

/**
 * 仿照android，context将提供很多的系统的api或是英勇的快捷操作
 */
abstract class IContext {
    abstract val application: Application
    abstract fun windowManager(): WindowManager
    abstract fun activityManager(): ActivityManager

    /**
     * 亲手结束软件生命
     */
    open fun exitApp() {}

    open fun startActivity(
        cls: Class<out Activity>,
        data: Any?
    ) {
        startActivity(cls, LaunchMode.STANDARD + data)
    }

    open fun startActivity(
        cls: Class<out Activity>,
        intent: Intent = LaunchMode.STANDARD + null,
    ) {
    }

    open fun startActivityForResult(
        cls: Class<out Activity>,
        intent: Intent = LaunchMode.STANDARD + null,
        block: ActivityResult
    ) {
    }
}
