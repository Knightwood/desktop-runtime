package com.github.knightwood.example.acts

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
import kotlin.random.Random
import kotlin.reflect.KClass

class TestViewModel2(
    val savedStateHandle: SavedStateHandle,
) : ViewModel() {
}

class TestViewModel1(
    val savedStateHandle: SavedStateHandle,
    val i: Int
) : ViewModel() {
    val value = savedStateHandle.getStateFlow("odd", -1)

    companion object {
        val key = object : CreationExtras.Key<Int> {}
        val factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
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
        extras[TestViewModel1.key] = 2
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
                    }
                }
            }
        }
    }
}
