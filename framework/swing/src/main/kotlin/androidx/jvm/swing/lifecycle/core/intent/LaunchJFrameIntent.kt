package androidx.jvm.swing.lifecycle.core.intent

import androidx.compose.desktop.runtime.core.intent.LaunchMode
import androidx.compose.desktop.runtime.core.intent.OperateIntent
import androidx.compose.desktop.runtime.savestate.IToken
import androidx.compose.desktop.runtime.savestate.Token
import androidx.core.bundle.Bundle
import androidx.jvm.swing.lifecycle.jFrame.JFrameResult
import androidx.jvm.swing.lifecycle.jFrame.LifecycleJFrame
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.properties.Delegates

class LaunchJFrameIntent : OperateIntent {
    /*
    * 从哪来
    * 如果不是从jFrame启动的，则此字段为null
    */
    var from: Class<out Any>? = null
        internal set

    /** 被启动的jFrame */
    var targetJFrame: Class<out LifecycleJFrame> by Delegates.notNull()
        internal set

    /** 启动模式 */
    var launchMode: LaunchMode = LaunchMode.STANDARD
        internal set

    /**
     * 启动jFrame携带的数据
     * 未来可能迁移为SaveSate类型
     */
    var mData: Bundle = Bundle()
        private set

    /**
     * 读取Intent中保存的数据，如果没有数据或者类型不匹配，返回null
     *
     * @param T
     * @return
     */
    inline fun <reified T> getData(name: String): T? {
        return mData.get(name) as? T?
    }

    fun data(action: Bundle.() -> Unit): LaunchJFrameIntent {
        mData.apply(action)
        return this
    }

    /**
     * 两个jFrame之间不直接持有对方引用,使用intent作为中间的桥梁.
     * 实际使用时,jFrame1构造intent启动jFrame2,jFrame2会持有intent
     * 需要回传数据时,使用[getMailBox]获取MutableSharedFlow,jFrame2向事件总线发布消息,
     * jFrame1观察数据变更.
     */
    private val mailBox: MutableMap<String, MutableSharedFlow<out Any>> = mutableMapOf()

    /**
     * 获取一个MutableSharedFlow作为两个jFrame之间沟通的桥梁
     * jFrame1 -> jFrame2
     * ```
     * jFrame1 启动 jFrame2,获取一个MutableSharedFlow观察jFrame2回传的结果
     * intent.getMailBox<Int>("id").collect {
     *      //.....
     * }
     *
     * jFrame2处理完成后使用MutableSharedFlow回传结果
     * intent?.getMailBox<Int>("id").emit(10)
     * ```
     */
    fun <T : Any> getMailBox(name: String): MutableSharedFlow<T> {
        val flow = mailBox.getOrPut(name) {
            MutableSharedFlow<T>(replay = 1)
        }
        return flow as MutableSharedFlow<T>
    }

    val jFrameResultFlow get() = getMailBox<JFrameResult>(LifecycleJFrame.DEFAULT_RESULT_FLOW)

    /**
     * 获取默认作为传递结果的信箱
     */
    suspend fun collectJFrameResult(collector: FlowCollector<JFrameResult>) {
        getMailBox<JFrameResult>(LifecycleJFrame.DEFAULT_RESULT_FLOW).collect(collector)
    }

    /**
     * 在桌面端,不存在屏幕旋转,配置变更后重建jFrame的需求,
     * 存在将window关闭后下次打开时希望恢复上次保存数据的需求.
     *
     * 如果此id为null,则jFrame不使用保存和恢复状态功能.
     * 如果此id不为null,被启动的jFrame将使用此id关联需要保存的数据，在启动后使用此id恢复上次关闭时保存的数据。
     *
     * 在你指定了此id的情况下可以标识jFrame的唯一性.
     * 可是，实现状态保存和恢复，在桌面端真的有意义吗?
     */
    var token: Token? = null
        set(value) {
            //仅允许设置一次
            if (field != null) throw IllegalArgumentException("Token already set")
            field = value
        }

    @JvmName("setTokenForJava")
    fun setToken(token: IToken) {
        this.token = Token(token.value)
    }

    private constructor()

    constructor(
        from: Any? = null,
        to: Class<out LifecycleJFrame>,
        launchMode: LaunchMode = LaunchMode.STANDARD,
    ) : this() {
        if (from != null) {
            this.from = from::class.java
        }
        this.targetJFrame = to
        this.launchMode = launchMode
    }
}
