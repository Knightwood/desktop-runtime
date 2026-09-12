package androidx.jvm.swing.lifecycle.jFrame

import androidx.compose.desktop.runtime.core.getServiceInstance
import androidx.compose.desktop.runtime.core.intent.IOperateIntentProcessor
import androidx.compose.desktop.runtime.core.intent.IntentProcessor
import androidx.compose.desktop.runtime.core.intent.LaunchMode
import androidx.compose.desktop.runtime.core.intent.registerProcessor
import androidx.compose.desktop.runtime.savestate.Token
import androidx.jvm.swing.lifecycle.core.intent.LaunchJFrameIntent
import androidx.jvm.system.di.InstanceKoinComponent
import androidx.jvm.system.di.inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import javax.swing.SwingUtilities

/**
 * 管理所有的jFrame
 */
class JFrameManager : InstanceKoinComponent {
    val scope by inject<CoroutineScope>(named<JFrameManager>())

    // jFrame map
    private val jFrameMap: MutableMap<Token, LifecycleJFrame> = mutableMapOf()

    //任务栈
    internal val stack = mutableListOf<LifecycleJFrame>()
    val jFrameStack: List<LifecycleJFrame> get() = stack

    val launcherManager: IJFrameLauncher = JFrameLauncher(stack)

    fun launchJFrame(intent: LaunchJFrameIntent) {
        launcherManager.start(intent)
    }

    operator fun get(uuid: Token?): LifecycleJFrame? {
        return jFrameMap[uuid]
    }

    /**
     * 如果jFrame使用标准模式，使用此方法将只能找到最早添加的实例
     */
    operator fun get(cls: Class<out LifecycleJFrame>): LifecycleJFrame? {
        return jFrameMap.values.find { it.javaClass == cls }
    }

    fun register(uuid: Token, jFrame: LifecycleJFrame) {
        jFrameMap[uuid] = jFrame
        stack.add(jFrame)
    }

    fun remove(uuid: Token) {
        jFrameMap.remove(uuid)
        stack.remove(jFrameMap[uuid])
    }

    fun release() {
        jFrameMap.values.forEach {
            it.finish()
        }
        jFrameMap.clear()
        stack.clear()
    }

    companion object {
        @JvmStatic
        fun openJFrame(intent: LaunchJFrameIntent) {
            getServiceInstance<JFrameManager>().launchJFrame(intent)
        }

        @JvmStatic
        suspend fun openJFrameForResultSuspend(
            intent: LaunchJFrameIntent,
            callback: JFrameResultCallback,
        ) {
            val mgr = getServiceInstance<JFrameManager>()
            mgr.launchJFrame(intent)
            intent.collectJFrameResult {
                callback.invoke(it.resultCode, it.data)
            }
        }

        @JvmStatic
        fun openJFrameForResult(
            intent: LaunchJFrameIntent,
            callback: JFrameResultCallback,
        ) {
            val mgr = getServiceInstance<JFrameManager>()
            mgr.launchJFrame(intent)
            mgr.scope.launch(Dispatchers.Default) {
                intent.collectJFrameResult {
                    callback.invoke(it.resultCode, it.data)
                    cancel()
                }
            }
        }

        @JvmStatic
        fun openJFrameForResult(intent: LaunchJFrameIntent): SharedFlow<JFrameResult> {
            getServiceInstance<JFrameManager>().launchJFrame(intent)
            return intent.jFrameResultFlow
        }
    }
}

interface IJFrameLauncher {
    fun start(intent: LaunchJFrameIntent)
}

/**
 * 实现解析intent,启动jFrame功能
 * @param stack jFrame栈
 */
internal class JFrameLauncher(
    val stack: List<LifecycleJFrame>,
) : IJFrameLauncher, InstanceKoinComponent {

    val launchJFrameIntentProcessor = object : IOperateIntentProcessor<LaunchJFrameIntent> {
        override fun process(intent: LaunchJFrameIntent): Boolean {
            start(intent)
            return true
        }
    }

    init {
        getKoin()
            .get<IntentProcessor>()
            .registerProcessor(launchJFrameIntentProcessor)
    }

    /**
     * 实现启动jFrame逻辑
     * 所有地方的启动jFrame最终都会走到这里
     */
    override fun start(intent: LaunchJFrameIntent) {
        if (intent.launchMode == LaunchMode.SINGLE_INSTANCE) {
            val old = stack
                .map { jFrame -> jFrame::class.java to jFrame }
                .find { (clazz, instance) ->
                    clazz.name == intent.targetJFrame.name
                }
            if (old != null) {
                val (clazz, instnace) = old
                instnace.onReStart(intent)
                return
            }
        }
        SwingUtilities.invokeLater {
            val jFrame = intent.targetJFrame.getDeclaredConstructor().newInstance()
            jFrame.attach(intent)
            jFrame.isVisible = true
        }
    }
}
