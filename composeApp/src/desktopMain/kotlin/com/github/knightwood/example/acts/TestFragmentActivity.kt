package com.github.knightwood.example.acts

import androidx.compose.desktop.runtime.activity.ComponentActivity
import androidx.compose.desktop.runtime.fragment.*
import androidx.compose.desktop.runtime.savestate.Token
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
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.Window
import androidx.savedstate.SavedState
import com.github.knightwood.example.components.SampleButton
import kotlin.random.Random

class TestFragmentActivity() : ComponentActivity() {
    val fragmentProvider = FragmentProvider(this.lifecycle)

    /**
     * 通过FragmentProvider创建的Fragment会在调用FragmentProvider.remove时自动保存状态.
     * 未通过FragmentProvider创建的Fragment,会跟随attach的host lifecycle在ON_DESTROY时自动保存状态.
     * 单纯的使用if条件显示和不显示Fragment是不会自动保存状态的.
     */
    val f1 get() = fragmentProvider.obtain<Fragment1>(Token("123"))
    val f2 by activityOwnedFragment<Fragment1>(Token("124"))
    val testDialog by null.ownedFragment<TestDialog>(Token("dialog1"))

    override fun onCreate(savedInstanceState: SavedState?) {
        super.onCreate(savedInstanceState)
        setContent {
            Window(onCloseRequest = { finish() }) {
                Link2ComposeWindow {
                    MaterialTheme {
                        Column {
// =====================================================================================//
                            var f1Exist by remember { mutableStateOf(true) }
                            Text("测试fragment的状态存储")
                            if (f1Exist) {
                                f1.Screen()
                            }
                            Row {
                                SampleButton("关闭") {
                                    f1Exist = false
                                    fragmentProvider.remove(Token("123"))
                                }
                                SampleButton("再打开") {
                                    f1Exist = true
                                }
                            }


// =====================================================================================//

                            HorizontalDivider()
                            Text("fragment的显示隐藏")
                            f2.Screen()
                            SampleButton("显示隐藏") {
                                f2.run {
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
                            Text("关闭弹窗即销毁，点击销毁后无法再显示")

                            testDialog.Screen()
                            Row {
                                SampleButton("显示dialog1") {
                                    testDialog.show()
                                }
                                SampleButton("销毁dialog1") {
                                    testDialog.dismiss()
                                }
                            }

// =====================================================================================//
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

class Fragment1 : Fragment() {
    override fun onCreateView(): ComposableView {
        return ComposableView {
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


class TestDialog : DialogWindowFragment() {

    override fun onCreateView(): ComposableView {
        return ComposableView {
            DialogWindow(
                onCloseRequest = {
                    dismiss()
                },
                visible = mVisibility.value,
            ) {
                Link2ComposeDialogWindow {
                    MaterialTheme {
                        Column {
                            Text("dialog")
                            SampleButton("隐藏dialog1") {
                                hide()
                            }
                        }
                    }
                }
            }
        }
    }
}
