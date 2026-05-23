package com.github.knightwood.example.acts

import androidx.compose.desktop.runtime.activity.Activity
import androidx.compose.material3.Text
import androidx.compose.desktop.runtime.activity.Intent
import androidx.compose.desktop.runtime.utils.err.SwingErrorDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.jvm.system.core.AppPathProvider
import androidx.jvm.system.core.keepDirExist
import androidx.jvm.system.process.ProcessLocker
import androidx.savedstate.SavedState
import com.github.knightwood.example.mainActivity
import com.github.knightwood.slf4j.kotlin.logFor
import kotlinx.coroutines.*
import me.i18n.resources.app_name
import org.jetbrains.compose.resources.stringResource
import kotlin.random.Random


open class TestActivity : Activity() {
    val randoms = Random.nextInt(0, 11)
    var tag = "Activity$randoms"
    private val logger = logFor(tag)
    private val scope = CoroutineScope(Dispatchers.Default) + SupervisorJob()

    override fun onCreate(savedInstanceState: SavedState?) {
        super.onCreate(savedInstanceState)
        mainActivity = this
        setContent {
            ComposeView(onCloseRequest = { finish() }) {
                MaterialTheme {
                    Column {
                        Text(text = "rememberSavable测试")
                        Button(onClick = {
                            val intent = Intent(this@TestActivity, StateTestActivity::class.java)
                            //因为大多数时候我们不需要保存恢复数据，clearSaveState默认为true
                            //但我们这里希望保存恢复数据，因此需要设置clearSaveState为false
                            intent.clearSaveState = false
                            //需要注意，activity取回保存起来的数据依靠uuid，因此需要在这里设置uuid
                            intent.uuid = "11"
                            startActivity(intent)
                        }) {
                            Text("点击启动状态测试页面")
                        }

                        HorizontalDivider()
                        Button(onClick = {
                            val intent = Intent(
                                this@TestActivity,
                                VMTestActivity::class.java
                            ).configuration {
                                clearSaveState = false
                                uuid = "12"
                                putData("randoms" to Random.nextInt(12, 20))
                            }
                            startActivityForResult(intent) { result, data ->
                                logger.info("result: $result, data: $data")
                            }
                        }) {
                            Text("点击启动vm测试页面")
                        }

                        HorizontalDivider()
                        Button(onClick = {
                            val intent =
                                Intent(this@TestActivity, TestFragmentActivity::class.java)
                            intent.clearSaveState = false
                            intent.uuid = "13"
                            startActivity(intent)
                        }) {
                            Text("点击启动fragment测试页面")
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {

                            Button(onClick = {
                                scope.launch {
                                    throw RuntimeException("测试异常")
                                }
                            }) {
                                //如果协程作用域不是SupervisorJob，会在抛出异常后结束掉协程作用域，无法再次使用
                                Text("协程抛出异常")
                            }
                            Button(onClick = {
                                throw RuntimeException("测试异常")
                            }) {
                                Text("ui抛出异常")
                            }
                            Button(onClick = {
                                exitApp()
                            }) {
                                Text("退出程序")
                            }
                        }

                        Button(onClick = {
                            SwingErrorDialog.showErrorDialog(RuntimeException("测试异常弹窗"))
                        }) {
                            Text("swing弹窗")
                        }

                        Button(onClick = {
                            val lockfile = AppPathProvider.provider.internalConfigDirPath
                                .keepDirExist()
                                .resolve("lockfile.lock").toNioPath()
                            ProcessLocker.lock(lockfile)
                        }) {
                            Text("获取文件锁")
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
        mainActivity = null
        logger.info("onDestroy")
    }
}
