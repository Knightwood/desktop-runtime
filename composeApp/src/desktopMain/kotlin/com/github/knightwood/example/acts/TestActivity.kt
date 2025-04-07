package com.github.knightwood.example.acts

import androidx.compose.desktop.runtime.activity.Activity
import androidx.compose.material3.Text
import androidx.compose.desktop.runtime.activity.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.core.bundle.Bundle
import androidx.jvm.system.core.AppPathProvider
import com.github.knightwood.slf4j.kotlin.logFor
import me.i18n.resources.app_name
import org.jetbrains.compose.resources.stringResource
import kotlin.random.Random


open class TestActivity : Activity() {
    val randoms = Random.nextInt(0, 11)
    var tag = "Activity$randoms"
    private val logger = logFor(tag)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ComposeView(onCloseRequest = { exitApp() }) {
                MaterialTheme {
                    Column {
                        Text(text = "rememberSaveable测试")
                        Button(onClick = {
                            val intent = Intent()
                            //因为大多数时候我们不需要保存恢复数据，clearSaveState默认为true
                            //但我们这里希望保存恢复数据，因此需要设置clearSaveState为false
                            intent.clearSaveState = false
                            //需要注意，activity取回保存起来的数据依靠uuid，因此需要在这里设置uuid
                            intent.uuid = "11"
                            startActivity(StateTestActivity::class.java, intent)
                        }) {
                            Text("点击启动状态测试页面")
                        }

                        HorizontalDivider()
                        Button(onClick = {
                            val intent = Intent()
                            intent.clearSaveState = false
                            intent.uuid = "12"
                            startActivity(VMTestActivity::class.java, intent)
                        }) {
                            Text("点击启动vm测试页面")
                        }

                        HorizontalDivider()
                        Button(onClick = {
                            val intent = Intent()
                            intent.clearSaveState = false
                            intent.uuid = "13"
                            startActivity(TestFragmentActivity::class.java, intent)
                        }) {
                            Text("点击启动fragment测试页面")
                        }

                        HorizontalDivider()
                        Text("资源加载，当前路径等信息")
                        val appname = stringResource(me.i18n.resources.Res.string.app_name)
                        Text(appname)
                        val pathProvider = AppPathProvider.getInstance()
                        val userHome = pathProvider.userHome
                        val installPath = pathProvider.installPath
                        val installedJarPath = pathProvider.installedJarPath
                        val installedExePath = pathProvider.installedExePath
                        val configPath = pathProvider.configDirPath
                        val info =
                            " 用户目录: $userHome\n 安装路径: $installPath\n 程序jar包路径: $installedJarPath\n exe文件路径: $installedExePath\n 配置目录路径: $configPath\n"
                        Text(info)
                    }
                }
            }
        }
    }

    override fun onReStart(intent: Intent?) {
        super.onReStart(intent)
        logger.info("onReStart")
    }

    override fun onPause() {
        super.onPause()
        logger.info("onPause")
    }

    override fun onResume() {
        super.onResume()
        logger.info("onResume")
    }

    override fun onStart() {
        super.onStart()
        logger.info("onStart")
    }

    override fun onStop() {
        super.onStop()
        logger.info("onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        logger.info("onDestroy")
    }
}
