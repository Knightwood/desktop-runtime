package androidx.compose.desktop.runtime.context

import androidx.compose.desktop.runtime.activity.*
import androidx.compose.desktop.runtime.activity.result.ActivityResultCallback
import androidx.compose.desktop.runtime.core.Application
import androidx.compose.desktop.runtime.core.ServiceHolder
import androidx.compose.desktop.runtime.core.applicationInternal
import androidx.compose.desktop.runtime.domain.Stop
import androidx.compose.desktop.runtime.window.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class ContextImpl() : IContext() {
    override val application: Application
        get() = applicationInternal

    override fun windowManager(): WindowManager = ServiceHolder[WindowManager.NAME]!!

    override fun activityManager(): ActivityManager = ServiceHolder[ActivityManager.NAME]!!

    override fun exitApp() {
        //反正都要退出了，随便用一个协程也不是什么罪过了
        CoroutineScope(Dispatchers.Default).launch {
            ServiceHolder.runningState.emit(Stop())
        }
    }

    override fun startActivity(
        intent: Intent,
    ) {
        startActivityInner(intent)
    }

    override fun startActivityForResult(
        intent: Intent,
        block: ActivityResultCallback,
    ) {
        startActivityInner(intent, block)
    }

    /**
     * 在[ActivityManager.scope]中生成并运行activity，如此，activity就跑在ui（主）线程上
     */
    private fun startActivityInner(
        intent: Intent,
        callback: ActivityResultCallback? = null
    ) {
        if (intent.launchMode == LaunchMode.SINGLE_INSTANCE) {
            intent.uuid = intent.targetActivity.canonicalName
            val old = activityManager().get(intent.uuid)
            if (old != null) {
                old.onReStart(intent)
                return
            }
        }
        activityManager().scope.launch {
            val activity = intent.targetActivity.getDeclaredConstructor().newInstance()
            activity.attach(this@ContextImpl, intent)
            if (callback != null) {
                launch(Dispatchers.Default) {
                    activity.internalResultFlow.observe {
                        callback.invoke(it.resultCode, it.data)
                    }
                }
            }
        }
    }
}
