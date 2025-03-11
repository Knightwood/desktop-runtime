package com.github.knightwood.example.acts

import androidx.compose.desktop.runtime.activity.FragmentActivity
import androidx.compose.desktop.runtime.fragment.Fragment
import androidx.compose.desktop.runtime.fragment.ComponentViewHolder
import androidx.compose.desktop.runtime.fragment.DialogFragment
import androidx.compose.desktop.runtime.fragment.register
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
        register<Fragment1>("123")
        register<Fragment1>("124")
        register<TestDialog>("dialog1")
        setContent {
            ComposeView() {
                var screen1 by remember {
                    mutableStateOf<Fragment?>(screen("123"))
                }
                MaterialTheme {
                    Column {
// =====================================================================================//
                        Text("测试fragment的状态存储")
                        screen1?.invoke()
                        Row {
                            SampleButton("关闭") {
                                unregister("123")
                                screen1 = null
                            }
                            SampleButton("再打开") {
                                screen1 = register<Fragment1>("123")
                            }
                        }


// =====================================================================================//

                        HorizontalDivider()
                        Text("fragment的显示隐藏")
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
// =====================================================================================//
                        HorizontalDivider()
                        Text("测试dialog fragment")
                        Text("关闭弹窗即隐藏，点击销毁后无法再显示")
                        val dialog by remember {
                            mutableStateOf(screen("dialog1"))
                        }
                        dialog()
                        Row {
                            SampleButton("打开dialog1") {
                                dialog.show()
                            }
                            SampleButton("销毁dialog1") {
                                (dialog as DialogFragment).dismiss()
                            }
                        }


// =====================================================================================//

                        HorizontalDivider()
                        Text("测试不添加到管理器的dialog fragment")
                        Text("关闭弹窗即隐藏，点击销毁后无法再显示")
                        val dialog2 by remember {
                            mutableStateOf(
                                DialogFragment.makeDialog(
                                    TestDialog::class.java,
                                    this@TestFragmentActivity.lifecycle
                                )
                            )
                        }
                        dialog2()
                        Row {
                            SampleButton("打开dialog2") {
                                dialog2.show()
                            }
                            SampleButton("销毁dialog2") {
                                (dialog2 as DialogFragment).dismiss()
                            }
                        }

                    }
                }
            }
        }
    }
}

class Fragment1 : Fragment() {
    init {
        //如果为true，则不会在onDestroy时保存数据，也不会在 onCreate中恢复数据
        //默认为true
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


class TestDialog : DialogFragment() {
    override fun onCreateView(): ComponentViewHolder {
        return ComposeView {
            Dialog() {
                MaterialTheme {
                    Column {
                        Text("dialog")
                    }
                }
            }
        }
    }
}
