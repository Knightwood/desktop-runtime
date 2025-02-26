package androidx.compose.desktop.runtime.context

import androidx.compose.desktop.runtime.activity.*
import androidx.compose.desktop.runtime.core.Application
import androidx.compose.desktop.runtime.core.ManagerHolder
import androidx.compose.desktop.runtime.core.applicationInternal
import androidx.compose.desktop.runtime.domain.RunningState
import androidx.compose.desktop.runtime.domain.Stop
import androidx.compose.desktop.runtime.window.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class ContextImpl() : IContext() {
    override val application: Application
        get() = applicationInternal

    override fun windowManager(): WindowManager = ManagerHolder[WindowManager.NAME]!!

    override fun activityManager(): ActivityManager = ManagerHolder[ActivityManager.NAME]!!

    override fun exitApp() {
        activityManager().clear()
        //反正都要退出了，随便用一个协程也不是什么罪过了
        CoroutineScope(Dispatchers.Default).launch {
            ManagerHolder.runningState.emit(Stop())
        }
    }

    override fun startActivity(
        cls: Class<out Activity>,
        intent: Intent
    ) {
        startActivityInner(cls, intent, null)
    }

    override fun startActivityForResult(
        cls: Class<out Activity>,
        intent: Intent,
        block: ActivityResult
    ) {
        startActivityInner(cls, intent, block)
    }

    /**
     * 在[ActivityManager.scope]中生成并运行activity，如此，activity就跑在ui（主）线程上
     */
    private fun startActivityInner(
        cls: Class<out Activity>,
        intent: Intent,
        block: ActivityResult?
    ) {
        if (intent.launchMode == LaunchMode.SINGLE_INSTANCE) {
            val old = activityManager()[cls]
            if (old != null) {
                old.onReStart(intent)
                return
            }
        }
        activityManager().scope.launch {
            val activity = cls.getDeclaredConstructor().newInstance()
            activity.attach(this@ContextImpl, intent, block)
        }
    }
}
