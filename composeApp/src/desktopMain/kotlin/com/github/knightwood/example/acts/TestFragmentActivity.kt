package com.github.knightwood.example.acts

import androidx.compose.desktop.runtime.activity.ComponentActivity
import androidx.compose.desktop.runtime.fragment.*
import androidx.compose.desktop.runtime.savestate.Token
import androidx.compose.desktop.runtime.window.ComponentDialog
import androidx.compose.desktop.runtime.window.componentDialog
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.Window
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.savedstate.SavedState
import com.github.knightwood.example.components.SampleButton
import com.github.knightwood.example.components.ScrollbarBox
import com.github.knightwood.example.components.TextSwitch
import com.github.knightwood.slf4j.kotlin.logFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus
import kotlin.random.Random

class TestFragmentActivity() : ComponentActivity() {
    val fragmentProvider = FragmentProvider(this.lifecycle)
    private val logger = logFor("TestFragmentActivity")
    private val scope = CoroutineScope(Dispatchers.Default) + SupervisorJob()

//    init {
//        lifecycleListener = object : LifecycleEventObserver {
//            override fun onStateChanged(
//                source: LifecycleOwner,
//                event: Lifecycle.Event,
//            ) {
//                logger.debug("StateChanged to ${event}")
//            }
//        }
//    }

    override fun onCreate(savedInstanceState: SavedState?) {
        super.onCreate(savedInstanceState)
        setContent {
            Window(onCloseRequest = { finish() }) {
                LinkComposeWindow {
                    MaterialTheme {
                        val scrollState = rememberScrollState()
                        ScrollbarBox(
                            scrollState = scrollState,
                            orientation = Orientation.Vertical,
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().verticalScroll(scrollState)
                            ) {
                                extracted1()
                                extracted1_1()
                                extracted2()
                                extracted3()
                            }
                        }

                    }
                }
            }
        }
    }


    val f1 by this.lifecycle.ownedFragment<Fragment1>(this, Token("123"))

    @Composable
    private fun ColumnScope.extracted1() {
        HorizontalDivider()
        Text("测试fragment的状态存储")
        f1.Screen()
        Row {
            SampleButton("隐藏") {
                f1.hide()
            }
            SampleButton("显示") {
                f1.show()
            }
            SampleButton("关闭") {
                f1.finish()
                //fragmentProvider.remove(Token("123"))
            }
        }
    }

    /**
     * 通过FragmentProvider创建的Fragment会在调用FragmentProvider.remove时自动保存状态.
     * 未通过FragmentProvider创建的Fragment,会跟随attach的host lifecycle在ON_DESTROY时自动保存状态.
     * 单纯的使用if条件显示和不显示Fragment是不会自动保存状态的.
     */
    val f1_1 = fragmentProvider.obtain<Fragment1>(this, Token("123_1"))

    @Composable
    private fun ColumnScope.extracted1_1() {
        HorizontalDivider()
        Text("使用fragmentProvider提供实例")
        f1_1.Screen()
        Row {
            SampleButton("隐藏") {
                f1_1.hide()
            }
            SampleButton("显示") {
                f1_1.show()
            }
            SampleButton("关闭") {
                fragmentProvider.remove(Token("123_1"))
            }
        }
    }

    val f2 by activityOwnedFragment<Fragment1>(this, Token("124"))

    @Composable
    private fun ColumnScope.extracted2() {
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
    }

    @Composable
    private fun ColumnScope.extracted3() {
        HorizontalDivider()
        Text("测试dialog，关闭弹窗即销毁")
        var modal by remember { mutableStateOf(false) }
        Row {
            TextSwitch("模态", modal, { modal = it })
            SampleButton("打开弹窗") {
                val testDialog =
                    componentDialog<TestDialog>(
                        context = this@TestFragmentActivity,
                        modal = modal,
                        token = Token("dialog1")
                    )
                testDialog.show()
            }
        }
    }
}

class Fragment1 : Fragment() {
//    private val logger = logFor("Fragment1")
//    private val scope = CoroutineScope(Dispatchers.Default) + SupervisorJob()
//
//    init {
//        lifecycleListener = object : LifecycleEventObserver {
//            override fun onStateChanged(
//                source: LifecycleOwner,
//                event: Lifecycle.Event,
//            ) {
//                logger.debug("StateChanged to ${event}")
//            }
//        }
//    }

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

var dialogCount = 0

class TestDialog : ComponentDialog() {
    init {
        dialogCount++
    }

    override fun onCreate(savedInstanceState: SavedState?) {
        super.onCreate(savedInstanceState)
        setContentView {
            DialogWindow(
                onCloseRequest = {
                    dismiss()
                    dialogCount--
                },
                visible = mVisibility.value,
            ) {
                Link2ComposeDialogWindow {
                    MaterialTheme {
                        Column {
                            Text("dialog：${dialogCount}")
                            Button(onClick = {
                                val testDialog = nestDialog<TestDialog>()
                                testDialog.show()
                            }) {
                                Text("嵌套dialog")
                            }
                        }
                    }
                }
            }
        }
    }
}
