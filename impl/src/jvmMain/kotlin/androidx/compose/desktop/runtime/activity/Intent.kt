package androidx.compose.desktop.runtime.activity

import java.util.UUID


data class Intent(
    val launchMode: LaunchMode = LaunchMode.STANDARD,
    var data: Any? = null
) {
    /**
     * 是否在销毁activity时销毁保存的状态。
     * desktop端并没有重建activity保存状态的需求，ComponentActivity实现目的在于兼容android组件。
     * 如果存在activity启动后恢复上一次activity关闭时保存数据的需求，把此字段置为false。
     * 否则，activity销毁时也会连带着销毁存储的状态。
     */
    var clearSaveState: Boolean = true

    /**
     * 如果不为null，则启动的activity使用此uuid标记自己。
     * 如果不为null，下一次恢复数据时将使用此uuid读取保存的数据。
     *
     * 可是，实现状态保存和恢复，在桌面端真的有意义吗？
     */
    var uuid: String? = null

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

    companion object {
        infix operator fun LaunchMode.plus(data: Any?) = Intent(this, data)

    }
}
