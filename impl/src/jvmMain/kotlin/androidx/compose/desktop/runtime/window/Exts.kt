package androidx.compose.desktop.runtime.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.eventFlow
import kotlinx.coroutines.launch

@Composable
fun LifecycleLogger(flag: String = "") {
    val lc = LocalLifecycleOwner.current
//    remember {
//        application.scope.launch {
//            lc.lifecycle.currentStateFlow.collect {
//                println("$flag New State: $it")
//            }
//        }
//    }
    //这里的LaunchedEffect是不能监听到destroy事件和状态的，
    // 因为这里的协程会在destroy之前就不存在了，所以需要使用上面那样的长生命周期的协程做监听
    LaunchedEffect(lc) {
        launch {
            lc.lifecycle.currentStateFlow.collect {
                println("$flag New State: $it")
            }
        }
        launch {
            lc.lifecycle.eventFlow.collect {
                println("$flag New Event: $it")
            }
        }
    }
}
