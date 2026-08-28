package com.github.knightwood.example.acts

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
import androidx.compose.desktop.runtime.core.intent.Intent
import androidx.compose.ui.window.Window
import androidx.savedstate.SavedState
import kotlinx.coroutines.delay

class SplashActivity : Activity() {
    override fun onCreate(savedInstanceState: SavedState?) {
        super.onCreate(savedInstanceState)
        setContent {
            LaunchedEffect(Unit) {
                delay(600) // 延迟500毫秒
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                finish()
            }
            val state: WindowState =
                rememberWindowState(
                    position = WindowPosition.Aligned(Alignment.Center),
                    size = DpSize(300.dp, 300.dp)
                )
            Window(onCloseRequest = { finish() }, state = state) {
                LinkComposeWindow {
                    Text("启动页", fontSize = 64.sp)
                }
            }
        }
    }
}
