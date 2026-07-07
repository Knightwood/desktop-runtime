package com.github.knightwood.example

import androidx.compose.desktop.runtime.activity.Activity
import androidx.compose.desktop.runtime.core.startApplication
import androidx.compose.desktop.runtime.utils.UncaughtExceptionContent
import androidx.compose.desktop.runtime.utils.setUncaughtExceptionHandler
import androidx.compose.desktop.runtime.window.*
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.window.ApplicationScope
import androidx.jvm.system.core.*
import androidx.jvm.system.ui.tray.FixedSystemTray
import com.github.knightwood.example.acts.SplashActivity
import com.github.knightwood.slf4j.kotlin.logFor

//fun main() = startApplication(
//    SplashActivity::class.java,
//    MainApplication::class.java
//)
var mainActivity: Activity? = null

class Main

//或者
fun main() {
    setUncaughtExceptionHandler()
    startApplication<SplashActivity, MainApplication>(
        applicationContent = object : ApplicationContentWrapper {
            @Composable
            override fun ApplicationScope.invoke(content: ComposableContent) {
                UncaughtExceptionContent {
                    content()
                    SystemTray()
                }
            }
        }
    )
}

@Composable
fun ApplicationScope.SystemTray() {
    val painter = painterResource("icons/app_icon.svg")
    val icon1 = rememberVectorPainter(Icons.Default.Settings)
    val icon2 = rememberVectorPainter(Icons.Default.ExitToApp)
    FixedSystemTray(
        icon = painter, tooltip = "hello",
        primaryAction = {
            mainActivity?.run {
                show()
                windowHolder.composeWindow.setWindowSizeState(WindowSizeState.Restore)
            }
        },
        menu = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column {
                    DropdownMenuItem(
                        onClick = {},
                        leadingIcon = {
                            Icon(icon1, null)
                        },
                        text = {
                            Text("Hello")
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        onClick = {
                            log.info("exit")
                            MainApplication.ctx.exitApp()
                        },
                        leadingIcon = {
                            Icon(icon2, null)
                        },
                        text = {
                            Text("exit")
                        }
                    )
                }
            }
        }
    )
}


private val log = logFor("main")

private var hide = mutableStateOf(false)

/*
如果希望应用窗口关闭后不结束运行，而且点击托盘图标可以显示界面，
因此有几点说明：
1. 点击窗口的关闭只是隐藏窗口，不可以结束application运行，
2. 如果下面程序没有“val number = flow.collectAsState()”这一句，当窗口隐藏后，application就会自动退出
    原因：虽然程序里的flow.collectAsState() 是一句废话，根本不会有事件发送，也不会有消费
    但是没有他，application在执行完隐藏窗口逻辑后，会继续向下执行逻辑，
    我们没有手动结束程序，程序逻辑中也没有调用退出函数，但程序会因为再无可执行的逻辑自然结束
 */
//val flow = MutableStateFlow(1)
//fun main() = application(false) {
//    val number = flow.collectAsState()
//    if (!hide.value) {
//        Window(onCloseRequest = {
//            hide.value = true
//        }) {
//
//        }
//    }
//    SystemTray()
//}

