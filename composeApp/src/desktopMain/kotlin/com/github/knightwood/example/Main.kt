package com.github.knightwood.example

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.desktop.runtime.activity.Activity
import androidx.compose.desktop.runtime.activity.Intent
import androidx.compose.desktop.runtime.core.startApplication
import kotlin.random.Random

fun main() = startApplication( SplashActivity::class.java,MainApplication::class.java,)

@Composable
fun Activity.test(intent: Intent?) {
    ComposeView() {
        MaterialTheme {
            Column {
                Text(intent?.data?.toString() ?: "First")
                Button(onClick = {
                    val randoms = Random.nextInt(0, 11)
//                    startActivity(MainActivity::class.java, "随机数${randoms}")
                    startActivity(TestActivity::class.java, "随机数${randoms}")
                }) {
                    Text("启动新页面")
                }
            }
        }
    }
}

