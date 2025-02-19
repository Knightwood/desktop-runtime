package androidx.compose.desktop.runtime.activity

import androidx.compose.desktop.runtime.context.IContext

data class Intent(
    val launchMode: LaunchMode = LaunchMode.STANDARD,
    val data: Any? = null
) {
    /**
     * If true, the app will exit when the windows list is empty in the window
     * manager
     *
     * if false, You can manually call [IContext.exitApp] to exit the
     * application process
     */
    var exitAppWhenEmpty: Boolean = false

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
    var deAttach: Boolean = false

    companion object {
        infix operator fun LaunchMode.plus(data: Any?) = Intent(this, data)

    }
}
