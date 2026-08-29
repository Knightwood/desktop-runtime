package androidx.compose.desktop.runtime.activity

import androidx.compose.desktop.runtime.core.context.ContextImpl
import androidx.compose.desktop.runtime.core.intent.IOperateIntentProcessor
import androidx.compose.desktop.runtime.core.intent.IntentProcessor
import androidx.compose.desktop.runtime.core.intent.LaunchActivityIntent
import androidx.compose.desktop.runtime.core.intent.LaunchMode
import androidx.compose.desktop.runtime.savestate.Token
import androidx.jvm.system.di.InstanceKoinComponent
import androidx.jvm.system.di.inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named

/**
 * 管理所有的activity
 */
class ActivityManager : InstanceKoinComponent {
    val scope by inject<CoroutineScope>(named<ActivityManager>())

    // activity map
    private val activityMap: MutableMap<Token, Activity> = mutableMapOf()

    //任务栈
    internal val stack = mutableListOf<Activity>()
    val activityStack: List<Activity> get() = stack

    private val launcherManager by inject<ActivityLauncher>() {
        parametersOf(stack, scope)
    }

    fun launchActivity(intent: LaunchActivityIntent) {
        launcherManager.start(intent)
    }

    /**
     * 好吧，目前没有可实现的
     */
    fun prepare(): ActivityManager {
        getKoin().get<IntentProcessor>().registerProcessor(
            launcherManager.launchActivityIntentProcessor
        )
        return this
    }

    operator fun get(uuid: Token?): Activity? {
        return activityMap[uuid]
    }

    /**
     * 如果activity使用标准模式，使用此方法将只能找到最早添加的实例
     */
    operator fun get(cls: Class<out Activity>): Activity? {
        return activityMap.values.find { it.javaClass == cls }
    }

    fun register(uuid: Token, activity: Activity) {
        activityMap[uuid] = activity
        stack.add(activity)
    }

    fun remove(uuid: Token) {
        activityMap.remove(uuid)
        stack.remove(activityMap[uuid])
    }

    fun release() {
        activityMap.values.forEach {
            it.finish()
        }
        activityMap.clear()
        stack.clear()
    }

}
interface IActivityLauncher {
    fun start(intent: LaunchActivityIntent)
}

/**
 * 实现解析intent,启动activity功能
 * @param stack activity栈
 */
internal class ActivityLauncher(
    val stack: List<Activity>,
    val coroutineScope: CoroutineScope,
) : IActivityLauncher, InstanceKoinComponent {
    val launchActivityIntentProcessor = object : IOperateIntentProcessor<LaunchActivityIntent> {
        override fun process(intent: LaunchActivityIntent): Boolean {
            start(intent)
            return true
        }
    }

    /**
     * 实现启动activity逻辑
     * 所有地方的启动activity最终都会走到这里
     */
    override fun start(intent: LaunchActivityIntent) {
        if (intent.launchMode == LaunchMode.SINGLE_INSTANCE) {
            val old = stack
                .map { activity -> activity::class.java to activity }
                .find { (clazz, instance) ->
                    clazz.name == intent.targetActivity.name
                }
            if (old != null) {
                val (clazz, instnace) = old
                instnace.onReStart(intent)
                return
            }
        }
        coroutineScope.launch {
            val activity = intent.targetActivity.getDeclaredConstructor().newInstance()
            activity.attach(ContextImpl.createBaseContextForActivity(), intent)
        }
    }
}
