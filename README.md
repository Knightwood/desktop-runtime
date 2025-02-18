# 这是一个compose multiplatform desktop端的框架。

仿照了android平台的开发模式，
1. 提供Application类用于初始化逻辑；
2. 提供Activity将Window包装起来，方便开发。
   1. 可以使用startActivity函数跳转到新的window，
   2. ComponentActivity实现了ViewModelStoreOwner、LifecycleOwner等接口，可以提供ViewModel实例，支持SavedStateHandle等。
   3. 可以在启动Activity时增加启动配置，比如单例模式，传递参数等
   4. activity跟随window拥有完整的生命周期

# 示例
## 原有开发方式
```kotlin
fun main() = application {
   val state: WindowState =
      rememberWindowState(position = WindowPosition.Aligned(Alignment.Center), size = DpSize(300.dp, 300.dp))
   Window(
      state = state,
      alwaysOnTop = true,
      title = "example",
      undecorated = true,
      transparent = true,
   ) {
      Text("启动页", fontSize = 64.sp)
   }
}
```


## 现在

### 1. application方便你写一些初始化逻辑
```kotlin
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 程序结束时，手动刷新日志
        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        context.stop() // 确保所有日志都被刷新
    }
}
```

### 2. Activity方便你写一些逻辑

```kotlin
class SplashActivity : Activity() {
    
    //熟悉的onCreate
   override fun onCreate() {
      super.onCreate()
       //调用setContentView实现界面
      setContentView {
         LaunchedEffect(Unit) {
            delay(600) // 延迟500毫秒
            // 跳转到新的Window，应该叫他Activity，当然，可以配置启动模式和一些参数
            startActivity(TestActivity::class.java,) 
            finish() // 结束当前Window，应该叫他Activity
         }
         val state: WindowState =
            rememberWindowState(position = WindowPosition.Aligned(Alignment.Center), size = DpSize(300.dp, 300.dp))
         ComposeView(
            state = state,
            alwaysOnTop = true,
            title = "example",
            undecorated = true,
            transparent = true,
         ) {
            Text("启动页", fontSize = 64.sp)
         }
      }
   }
}

```

### 3. 最后，使用main函数启动整个程序

```kotlin

fun main() = startApplication(
    mainActivity = SplashActivity::class.java,
    applicationClass = MainApplication::class.java
)
```

### 4. 关于使用ViewModel

```kotlin
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

```
