package androidx.compose.desktop.runtime.activity

import androidx.compose.desktop.runtime.activity.result.ActivityResultCallback
import java.util.UUID
import kotlin.properties.Delegates

class Intent {
    /*
    * 从哪来
    * 如果不是从activity启动的，则此字段为null
    */
    var from: Class<out Any>? = null
        internal set

    /* 被启动的activity */
    var targetActivity: Class<out Activity> by Delegates.notNull()
        internal set

    /* 启动模式 */
    var launchMode: LaunchMode = LaunchMode.STANDARD

    /* 携带的数据 */
    @PublishedApi
    internal var mData: Any? = null

    /**
     * 是否在销毁activity时销毁保存的状态。
     * 如果存在activity启动后恢复上一次activity关闭时保存数据的需求，把此字段置为false。
     * 否则，activity销毁时也会连带着销毁存储的状态。
     */
    var clearSaveState: Boolean = true

    /**
     * 被启动的activity使用此uuid标记自己，且使用此uuid读取上次关闭时保存的数据。
     *
     * 如果启动模式为单例，又没指定uuid，则使用目标activity class的canonicalName
     *
     * 可是，实现状态保存和恢复，在桌面端真的有意义吗？
     */
    var uuid: String = UUID.randomUUID().toString()

    /**
     * If true, the activity will not be attached to current application scope
     *
     * 假如，应用启动后先显示splash页面，然后splash关闭自己，接着启动MainActivity，
     * 且MainActivity的deAttach为true
     * (即MainActivity的window不被[WindowManager]管理，运行在单独的application中)，
     * 则需要将splash自己的intent属性中的[exitAppWhenEmpty]修改为false，避免splash关闭后应用进程退出 eg:
     * ```
     * fun main() = startApplication(MainApplication::class.java, SplashActivity::class.java){
     *     exitAppWhenEmpty=false //可以在这里配置
     * }
     *
     * class SplashActivity : Activity() {
     *     override fun onCreate(intent: Intent) {
     *         super.onCreate(intent)
     *         setContentView {
     *             LaunchedEffect(Unit) {
     *                 delay(600) // 延迟500毫秒
     *                 val intent2 = LaunchMode.SINGLE_INSTANCE + "Hello World"
     *                 intent2.deAttach = false
     *                 startActivity(MainActivity::class.java, intent2)
     *                 intent.exitAppWhenEmpty=false //注意这里，也可以动态修改
     *                 finish()
     *             }
     *             //......
     *         }
     *     }
     * }
     *
     * class MainActivity : ComponentActivity() {
     *  //.....
     * }
     * ```
     */
    var multiApplication: Boolean = false

    constructor()

    constructor(
        to: Class<out Activity>,
        data: Any? = null,
        launchMode: LaunchMode = LaunchMode.STANDARD,
    ) {
        this.targetActivity = to
        this.mData = data
        this.launchMode = launchMode
    }

    constructor(
        from: Any,
        to: Class<out Activity>,
        data: Any? = null,
        launchMode: LaunchMode = LaunchMode.STANDARD,
    ) {
        if (from is Class<*>) {
            this.from = from
        } else {
            this.from = from::class.java
        }
        this.targetActivity = to
        this.mData = data
        this.launchMode = launchMode
    }

    fun configuration(block: Intent.() -> Unit): Intent {
        block.invoke(this)
        return this
    }

    /**
     * 读取Intent中保存的数据，如果没有数据或者类型不匹配，返回null
     *
     * @param T
     * @return
     */
    inline fun <reified T> getData(): T? {
        if (mData == null) return null
        return mData as? T
    }

    fun putData(data: Any?) {
        mData = data
    }
}
