package com.github.knightwood.example.acts

import androidx.compose.desktop.runtime.activity.FragmentActivity
import androidx.compose.desktop.runtime.fragment.ScreenComponent
import androidx.compose.desktop.runtime.fragment.ComponentViewHolder
import androidx.compose.desktop.runtime.fragment.register
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.bundle.Bundle
import com.github.knightwood.example.components.SampleButton
import kotlin.random.Random

class TestFragmentActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        register<ScreenComponent1>("123")
        register<ScreenComponent1>("124")
        setContent {
            ComposeView() {
                var screen1 by remember {
                    mutableStateOf<ScreenComponent?>(screen("123"))
                }
                MaterialTheme {
                    Column {
                        HorizontalDivider()
                        screen1?.invoke()
                        SampleButton("关闭") {
                            unregister("123")
                            screen1 = null
                        }
                        SampleButton("再打开") {
                            screen1 = register<ScreenComponent1>("123")
                        }
                        HorizontalDivider()
                        screen("124")()
                        SampleButton("显示隐藏") {
                            screen("124").run {
                                if (mVisibility.value) {
                                    hide()
                                } else {
                                    show()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class ScreenComponent1 : ScreenComponent() {
    init {
        clearBundle = false
    }

    override fun onCreateView(): ComponentViewHolder {
        return ComposeView {
            val text1 = rememberSaveable() {
                mutableStateOf("rememberSaveable")
            }
            val text2 = remember() {
                mutableStateOf("remember")
            }
            MaterialTheme {
                Column {
                    Text("界面随机数：${text1.value}")
                    Text("界面随机数：${text2.value}")
                    SampleButton("生成随机数") {
                        val i = Random.nextInt(0, 11)
                        text1.value = "rememberSaveable $i"
                        text2.value = "remember $i"
                    }
                }
            }
        }
    }
}
