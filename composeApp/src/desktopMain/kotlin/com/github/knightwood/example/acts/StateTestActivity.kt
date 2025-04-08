package com.github.knightwood.example.acts

import androidx.compose.desktop.runtime.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.savedstate.SavedState
import kotlin.random.Random

class StateTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: SavedState?) {
        super.onCreate(savedInstanceState)
        setContent {
            //由于状态存储发生在onDestroy阶段，closeActivity为true时才会触发onDestroy
            ComposeView(closeActivity = true) {
                MaterialTheme {
                    val text = rememberSaveable() {
                        mutableStateOf("ede")
                    }
                    Column {
                        Text(text = "rememberSaveable测试")
                        Text(text = text.value)
                        Button(onClick = {
                            val randoms = Random.nextInt(0, 11)
                            text.value = "随机数: ${randoms}"
                        }) {
                            Text("点击生成新值，关闭此页面后，再次打开，查看变化")
                        }
                    }
                }
            }
        }
    }
}
