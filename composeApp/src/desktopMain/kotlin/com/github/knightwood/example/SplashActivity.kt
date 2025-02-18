package com.github.knightwood.example

import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.desktop.runtime.activity.Activity
import kotlinx.coroutines.delay

class SplashActivity : Activity() {
    override fun onCreate() {
        super.onCreate()
        setContentView {
            LaunchedEffect(Unit) {
                delay(600) // 延迟500毫秒
                startActivity(TestActivity::class.java, "Hello World")
                finish()
            }
            val state: WindowState =
                rememberWindowState(position = WindowPosition.Aligned(Alignment.Center), size = DpSize(300.dp, 300.dp))
            ComposeView(
                state = state,
                alwaysOnTop = true,
                title = "Multi-Devs Control",
                undecorated = true,
                transparent = true,
            ) {
                Text("启动页", fontSize = 64.sp)
            }
        }
    }
}
