package com.github.knightwood.example.acts

import androidx.compose.desktop.runtime.activity.Activity
import androidx.compose.desktop.runtime.activity.ComponentActivity
import androidx.compose.desktop.runtime.viewmodel.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.*
import androidx.savedstate.SavedState
import com.github.knightwood.example.components.SampleButton
import com.github.knightwood.slf4j.kotlin.logFor
import com.github.knightwood.slf4j.kotlin.logger
import kotlin.random.Random
import kotlin.reflect.KClass

class TestViewModel2(
    val savedStateHandle: SavedStateHandle,
) : ViewModel() {
}
private val logger = logFor("TestViewModel1")
class TestViewModel1(
    val savedStateHandle: SavedStateHandle,
    i: Int
) : ViewModel() {
    init {
        logger.info("intent random value $i")
    }
    /*
        testActivity每次启动VMTestActivity都会传递一个12~20之间的随机数
        这里会使用传递过来的随机数初始化在saveStateHandle中的odd变量
        当点击按钮生成新的随机数时，覆盖saveStateHandle中的odd变量值
        点击按钮（非窗口的关闭按钮）关闭此页面时，TestActivity将得到启动结果
        由于使用了状态存储，此页面再次打开时将saveStateHandle将自动恢复为上一次的状态
     */
    val value = savedStateHandle.getStateFlow("odd", i)

    companion object {
        val key = object : CreationExtras.Key<Int> {}
        val factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(
                    modelClass: KClass<T>,
                    extras: CreationExtras
                ): T {
                    return TestViewModel1(
                        extras.createSavedStateHandle(),
                        extras[key] ?: 90
                    ) as T
                }
            }
    }
}

class VMTestActivity : ComponentActivity() {
    var tag = "VMTestActivity"
    private val logger = logFor(tag)

    val vm1: TestViewModel1 by viewModels<TestViewModel1>(extrasProducer = {
        val extras = MutableCreationExtras()
        extras[TestViewModel1.key] = intent.getData<Pair<String,Int>>()?.second?:11//从 intent中读取数据
        extras[SAVED_STATE_REGISTRY_OWNER_KEY] = this
        extras[VIEW_MODEL_STORE_OWNER_KEY] = this
        extras
    }, { TestViewModel1.factory })

    val vm2 by viewModels<TestViewModel2>()


    override fun onCreate(savedInstanceState: SavedState?) {
        super.onCreate(savedInstanceState)
        setContent {
            //由于状态存储发生在onDestroy阶段，closeActivity为true时才会触发onDestroy
            ComposeView {
                MaterialTheme {
                    Column {
                        val vm1_value = vm1.value.collectAsState()
                        Text("collect viewModel中savedStateHandle设置的值：" + vm1_value.value)
                        SampleButton("给savedStateHandle设置随机数") {
                            val randoms = Random.nextInt(0, 11)
                            vm1.savedStateHandle.set("odd", randoms)
                        }
                        SampleButton("关闭并setResult") {
                            setResult(Activity.SUCCESS, vm1.savedStateHandle.getStateFlow("odd",0).value)
                            finish()
                        }
                    }
                }
            }
        }
    }
}
