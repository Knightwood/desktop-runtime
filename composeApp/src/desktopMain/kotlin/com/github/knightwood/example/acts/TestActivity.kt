package com.github.knightwood.example.acts

import androidx.compose.material3.Text
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.compose.desktop.runtime.activity.ComponentActivity
import androidx.compose.desktop.runtime.activity.Intent
import androidx.compose.desktop.runtime.viewmodel.viewModels
import com.github.knightwood.example.ui.TestUI
import com.github.knightwood.slf4j.kotlin.logger
import kotlin.random.Random
import kotlin.reflect.KClass

class TestViewModel(
    val savedStateHandle: SavedStateHandle,
    val i: Int
) : ViewModel() {
}

open class TestActivity : ComponentActivity() {
    val randoms = Random.nextInt(0, 11)
    var tag = "Activity$randoms"
    private val logger = logger(tag)

    val one = object : CreationExtras.Key<Int> {}
    val vm: TestViewModel by viewModels<TestViewModel>(extrasProducer = {
        val extras = MutableCreationExtras()
        extras[one] = 2
        extras[SAVED_STATE_REGISTRY_OWNER_KEY] = this
        extras[VIEW_MODEL_STORE_OWNER_KEY] = this
        extras
    }, {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                return TestViewModel(
                    extras.createSavedStateHandle(),
                    extras[one] ?: 90
                ) as T
            }
        }
    })

    override fun onCreate() {
        super.onCreate()
        setContentView {
            TestUI(intent)
        }
        logger.info("onCreate：vm参数：" + vm.i)
        logger.info("onCreate：vm：" + vm)
    }

    override fun onReStart(intent: Intent?) {
        super.onReStart(intent)
        logger.info("onReStart")
    }

    override fun onPause() {
        super.onPause()
        logger.info("onPause")
    }

    override fun onResume() {
        super.onResume()
        logger.info("onResume")
    }

    override fun onStart() {
        super.onStart()
        logger.info("onStart")
    }

    override fun onStop() {
        super.onStop()
        logger.info("onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        logger.info("onDestroy")
    }
}
