package androidx.compose.desktop.runtime.core

import androidx.lifecycle.LifecycleOwner
import androidx.compose.desktop.runtime.activity.Activity
import androidx.compose.desktop.runtime.activity.Intent
import androidx.compose.desktop.runtime.context.ContextImpl
import androidx.compose.desktop.runtime.context.ContextWrapper
import kotlinx.coroutines.*

abstract class ApplicationBase : ContextWrapper(), LifecycleOwner {
    init {
        mBase = ContextImpl()
    }

    /**
     * 此协程最终会随着进程结束而结束，不必担心生命周期
     */
    val scope: CoroutineScope = CoroutineScope(Dispatchers.Default) + SupervisorJob() + CoroutineName("Application")

    /**
     * 启动所有的服务，比如窗口管理、activity管理
     */
    internal fun startAllService() = ManagerHolder.startAll()

    /**
     * 启动MainActivity，第一个显示出来的窗口
     */
    fun startMainThread(mainActivity: Class<out Activity>, intentBuilder: (Intent.() -> Unit)?) {
        val intent = Intent()
        intentBuilder?.invoke(intent)
        startActivity(mainActivity, intent)
        windowManager().prepare()
    }

}

