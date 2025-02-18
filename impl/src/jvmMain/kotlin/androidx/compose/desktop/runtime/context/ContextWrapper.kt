package androidx.compose.desktop.runtime.context

import androidx.compose.desktop.runtime.activity.Activity
import androidx.compose.desktop.runtime.activity.ActivityManager
import androidx.compose.desktop.runtime.activity.ActivityResult
import androidx.compose.desktop.runtime.activity.Intent
import androidx.compose.desktop.runtime.core.Application
import androidx.compose.desktop.runtime.window.WindowManager

open class ContextWrapper : IContext() {
    lateinit var mBase: IContext

    override val application: Application
        get() = mBase.application

    override fun windowManager(): WindowManager = mBase.windowManager()

    override fun activityManager(): ActivityManager = mBase.activityManager()

    override fun startActivity(
        cls: Class<out Activity>,
        data: Any?
    ) {
        mBase.startActivity(cls, data)
    }

    override fun startActivity(
        cls: Class<out Activity>,
        intent: Intent
    ) {
        mBase.startActivity(cls, intent)
    }

    override fun startActivityForResult(
        cls: Class<out Activity>,
        intent: Intent,
        block: ActivityResult
    ) {
        mBase.startActivityForResult(cls, intent, block)
    }
}
