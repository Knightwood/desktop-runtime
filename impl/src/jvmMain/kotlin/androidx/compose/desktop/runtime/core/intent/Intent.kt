package androidx.compose.desktop.runtime.core.intent

import androidx.compose.desktop.runtime.activity.Activity
import androidx.compose.desktop.runtime.activity.ActivityResult
import androidx.compose.desktop.runtime.savestate.Token
import androidx.core.bundle.Bundle
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import org.jetbrains.annotations.ApiStatus
import kotlin.properties.Delegates

sealed interface OperateIntent

@Deprecated("use LaunchActivityIntent instead.", replaceWith = ReplaceWith("LaunchActivityIntent"))
typealias Intent = LaunchActivityIntent

/**
 * 用于描述启动activity的意图
 */
class LaunchActivityIntent : OperateIntent {
    /*
    * 从哪来
    * 如果不是从activity启动的，则此字段为null
    */
    var from: Class<out Any>? = null
        internal set

    /** 被启动的activity */
    var targetActivity: Class<out Activity> by Delegates.notNull()
        internal set

    /** 启动模式 */
    var launchMode: LaunchMode = LaunchMode.STANDARD
        internal set

    /**
     * 启动activity携带的数据
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

    fun data(action: Bundle.() -> Unit): Intent {
        mData.apply(action)
        return this
    }

    /**
     * 两个activity之间不直接持有对方引用,使用intent作为中间的桥梁.
     * 实际使用时,activity1构造intent启动activity2,activity2会持有intent
     * 需要回传数据时,使用[getMailBox]获取MutableSharedFlow,activity2向事件总线发布消息,
     * activity1观察数据变更.
     */
    private val mailBox: MutableMap<String, MutableSharedFlow<out Any>> = mutableMapOf()

    /**
     * 获取一个MutableSharedFlow作为两个activity之间沟通的桥梁
     * activity1 -> activity2
     * ```
     * activity1 启动 activity2,获取一个MutableSharedFlow观察activity2回传的结果
     * intent.getMailBox<Int>("id").collect {
     *      //.....
     * }
     *
     * activity2处理完成后使用MutableSharedFlow回传结果
     * intent?.getMailBox<Int>("id").emit(10)
     * ```
     */
    fun <T : Any> getMailBox(name: String): MutableSharedFlow<T> {
        val flow = mailBox.getOrPut(name) {
            MutableSharedFlow<T>(replay = 1)
        }
        return flow as MutableSharedFlow<T>
    }

    /**
     * 获取默认作为传递结果的信箱
     */
    suspend fun collectResult(collector: FlowCollector<ActivityResult>) {
        getMailBox<ActivityResult>(Activity.RESULT_FLOW).collect(collector)
    }

    /**
     * 在桌面端,不存在屏幕旋转,配置变更后重建activity的需求,
     * 存在将window关闭后下次打开时希望恢复上次保存数据的需求.
     *
     * 如果此id为null,则activity不使用保存和恢复状态功能.
     * 如果此id不为null,被启动的activity将使用此id关联需要保存的数据，在启动后使用此id恢复上次关闭时保存的数据。
     *
     * 在你指定了此id的情况下可以标识activity的唯一性.
     * 可是，实现状态保存和恢复，在桌面端真的有意义吗?
     */
    var token: Token? = null
        set(value) {
            //仅允许设置一次
            if (field != null) throw IllegalArgumentException("Token already set")
            field = value
        }

    /**
     * true: 目标activity显示的窗口将运行在单独的application中
     * 警告: 这是测试功能, 目前发现启用此功能会造成状态偶尔无法恢复(关闭Activity后比较短的间隔内再次启动Activity, 无法恢复状态)
     */
    @ApiStatus.Experimental
    var multiApplication: Boolean = false

    private constructor()

    constructor(
        from: Any? = null,
        to: Class<out Activity>,
        launchMode: LaunchMode = LaunchMode.STANDARD,
    ) : this() {
        if (from != null) {
            this.from = from::class.java
        }
        this.targetActivity = to
        this.launchMode = launchMode
    }
}
/**
 * 启动模式，默认为标准模式，即多个实例可以同时存在。
 */
enum class LaunchMode {
    SINGLE_INSTANCE,
    STANDARD,
    ;

    operator fun plus(data: Any?): Pair<LaunchMode, Any?> {
        return this to data
    }
}
