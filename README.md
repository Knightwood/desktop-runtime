<p align="center"><h1 align="center">compose & swing</h1></p>
<p align="center"><h2 align="center">桌面窗口框架</h2></p>

<p align="center">
	<em><code>❯ wow... is kotlin</code></em>
</p>
<p align="center">
	<img src="https://img.shields.io/github/license/Knightwood/desktop-runtime?style=default&logo=opensourceinitiative&logoColor=white&color=0080ff" alt="license">
	<img src="https://img.shields.io/github/last-commit/Knightwood/desktop-runtime?style=default&logo=git&logoColor=white&color=0080ff" alt="last-commit">
	<img src="https://img.shields.io/github/languages/top/Knightwood/desktop-runtime?style=default&color=0080ff" alt="repo-top-language">
	<img src="https://img.shields.io/github/languages/count/Knightwood/desktop-runtime?style=default&color=0080ff" alt="repo-language-count">
</p>
<p align="center"><!-- default option, no dependency badges. -->
</p>
<p align="center">
	<!-- default option, no dependency badges. -->
</p>
<br>

## Overview

* 为compose桌面端带来类似android的开发体验
    * 完善的窗口管理，提供Activity、ComponentDialog、Fragment、Intent、Context、Application组件
    * 为各个组件提供Lifecycle、Viewmodel、SavedState支持
    * 可保存整个应用的状态以便下次打开时恢复
* 为swing提供Lifecycle、Viewmodel、SavedState支持

---

# 快速开始

## 添加依赖

1. clone项目

```
git clone https://github.com/Knightwood/desktop-runtime.git
```

2. 将项目打包到本地仓库

```
./gradlew publishToMavenLocal
```

3. 添加依赖

```kotlin
val version = "1.3.0"
val group = "com.github.knightwood.desktop"
implementation("$group:framework-common:$version")
implementation("$group:framework-compose:$version")
implementation("$group:framework-swing:$version")
implementation("$group:toolkit-spi:$version")
implementation("$group:toolkit-mac:$version")
implementation("$group:toolkit-win:$version")
implementation("$group:toolkit-linux:$version")
implementation("$group:compose-tray:$version")
```

1.3.0之前：

```kotlin
implementation("com.github.knightwood:desktop-runtime:1.0.0") { isChanging = true }
implementation("com.github.knightwood:jvm-system-spi:1.0.0") { isChanging = true }
implementation("com.github.knightwood:jvm-system-win:1.0.0")
```

## 开发Compose应用

在使用compose框架时，我们需要在main方法中使用application方法启动compose环境，调用Window显示窗口
```kotlin
fun main() {
    application {
        Window(onCloseRequest = {})
    }
}
```
要显示多窗口，需要在application{}中调用更多的Window函数，窗口管理成为灾难，
也无法创建跟随窗口生命周期的ViewModel，无法使用rememberSavable、无法将保存的状态导出、恢复等。

我们封装了窗口管理，使Activity作为一个窗口的载体，且具有跟随窗口的生命周期，提供Lifecycle、Viewmodel、SavedState支持。
要实现一个应用，现在需要三步：

1. 实现Application（可选）

Application用于数据、逻辑初始化。

```kotlin
class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}
```

2. 实现MainActivity

Activity将作为窗口载体，提供Lifecycle、Viewmodel、SavedState支持。
可以像Android中那样使用Intent启动其他窗口（Activity），传递参数、获取结果等。

```kotlin
class MainActivity : ComponentActivity() {
    //获取ViewModel
    val vm by viewModels<TestViewModel>()

    override fun onCreate(savedInstanceState: SavedState?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state: WindowState = rememberWindowState()
            //窗口
            Window(onCloseRequest = { finish() }, state = state) {
                //链接窗口生命周期
                LinkWindow {
                    // 这里写窗口中的界面
                    // 启动另一个Activity并获取结果
                    Button(onClick = {
                        val intent = Intent(this@MainActivity, VMTestActivity::class.java).apply {
                            token = Token("vmtest-1")
                            data {
                                putInt("random", Random.nextInt(10, 100))
                            }
                        }
                        scope.launch {
                            startActivityForResult(intent) { result, data ->
                                vmActivityResult = data.toString()
                                logger.info("data: $data")
                            }
                        }
                    }) {
                        Text("点击启动vm测试页面")
                    }
                }
            }
        }
    }
}
```

3. 实现main方法启动应用

```kotlin
fun main() {
    // 启动时需要指定第一个启动的窗口，Application子类
    startApplication<MainActivity, MainApplication>()
    // 如果没有什么东西需要放在Application中初始化，也可以不实现Application的子类，
    // 只需在启动时将泛型指定为Application基类即可
    startApplication<MainActivity, Application>()
}
```

## 开发Swing应用

我们也为swing中的JFrame、JDialog、JPanel等组件增加了Lifecycle、Viewmodel、SavedState支持，
以便可以更好的与compose 桌面端混合开发，复用ViewModel组件、架构。
同时，Swing部分与Compose部分没有任何耦合，可以独立使用在Swing应用开发中。

1. 实现主窗口

```java
public class MainScreen extends ComponentJFrame {

    public MainScreen() {
        setupUI();
    }

    @Override
    public void onCreate(@Nullable SavedState savedInstanceState) {
        super.onCreate(savedInstanceState);
        //获取ViewModel
        viewModel = JavaViewModelProvider.create(this).get(ExampleViewModel.class);
    }

    private void setupUI() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //打开另一个JFrame并获取结果
                LaunchJFrameIntent intent = new LaunchJFrameIntent(this, BookEditorExample.class, LaunchMode.STANDARD);
                intent.setTokenForJava(Tokens.of("BookEditorExample"));
                JFrameManager.openJFrameForResult(intent, new ComponentResultCallback() {
                    @Override
                    public void invoke(int resultCode, @Nullable Bundle data) {
                        logger.info("LaunchJFrameIntent resultCode: {}, data: {}", resultCode, data);
                        tv_bookInfo.setText(data.toString());
                    }
                });
            }
        });
    }

}

```

2. 实现Main方法

```java
public class Main {

    public static void main(String[] args) {
        try {
            Singularity.INSTANCE.boot();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    MainScreen screen = new MainScreen();
                    screen.setVisible(true);
                }
            });
        } catch (Exception e) {
            logger.error("err", e);
        }
    }
}
```

---

# 详细用法

Compose框架与swing框架可以同时使用，它们有些类具有共同的基类，。
比如在Compose Activity中打开ComponentJFrame，或者反过来。
同时使用时，两个框架使用同一个ApplicationSaveStateSaver实例、IntentProcessor实例（Intent解析依旧是各自框架实现）。
因此可以使用ApplicationSaveStateSaver实例获取和导入ComponentActivity、ComponentJFrame保存的状态。

## Compose桌面端框架

更具体的使用可以查看composeApp模块源码

### 窗口管理实现

首先说几个框架定义的概念：

1. 应用根视图
application函数需要传递content参数，在此参数中调用Window即可显示窗口
```kotlin
fun main() {
    application {
        Window(onCloseRequest = {})
    }
}
```
我们把application的content参数实现称为应用根视图

2. Activity根视图：
每个Activity都会持有一个根视图，此视图是个普通的Compose函数，只不过此函数内部会调用Window
```kotlin
typealias ComposeContent = @Composable () -> Unit

val window1 : ComposeContent = {
    Window(onCloseRequest = {})
}
```

3. 框架内窗口、框架外窗口
直接在application{}中调用的Window函数称为框架外窗口
application{}中调用Activity根视图函数，显示的窗口称为框架内窗口

好了，现在来讲窗口管理实现原理

核心是"用一个可观察列表驱动重组"：
将每个“Activity根视图”放入 [SnapshotStateList]，在 `application {}` 中遍历并调用，
列表变化即触发重组：加入则窗口显示，移除则窗口进入 `onDispose` 并不再显示。

```kotlin
typealias ComposeContent = @Composable () -> Unit
val windows = SnapshotStateList<ComposeContent>()

application {
    windows.forEach { it() }
}

//Activity根视图，根视图内部会调用[Window]
val window1 : ComposeContent = {
    Window(onCloseRequest = {})
}

// 显示窗口
windows.add(window1)
// 关闭窗口
windows.remove(window1)
```

### Application

Application可作为数据初始化入口，如果没有数据需要初始化，也可以不实现。

onCreate方法回调时机将早于任何Activity启动。
onDestroy方法会在所有窗口关闭后回调。

```kotlin
class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
```

#### 启动Application

startApplication方法允许传入applicationContent接口实现和一个intentBuilder匿名函数。

* intentBuilder： 用于修改第一个显示的Activity的Intent，比如修改启动模式、传递数据等。
* applicationContent：提供自定义application{}根视图显示，自定义框架内窗口显示时机、添加托盘图标、为内层compose视图提供CompositionLocal值

示例：
```kotlin
fun main() {
    setUncaughtExceptionHandler()
    startApplication<SplashActivity, MainApplication>(
        applicationContent = object : ApplicationRootContent {
            @Composable
            override fun ApplicationScope.invoke(windows: ComposableContent) {
                UncaughtExceptionContent { //捕获Compose界面未捕获的错误，并显示为弹窗提示
                    windows() // 框架内所有要显示的Window，必须调用，否则不显示任何框架内窗口
                    SystemTray() //增加托盘显示
                }
            }
        },
        intentBuilder = {
            launchMode = LaunchMode.STANDARD
            data {
                putInt("targetId", 1)
            }
        }
    )
}
```

注：
* 如果没有实现自定义的Application子类，可以在调用startApplication方法时将第二个泛型参数指定为Application类。

#### ApplicationRootContent 接口

在“窗口管理实现”一节讲了框架实现窗口管理的原理：
WindowManager内部调用了application函数，在应用根视图中遍历并显示所有要显示的窗口(Activity根视图)，由于Activity根视图函数内部调用了Window函数，就相当于Window函数在application{}中调用，也就能显示为窗口。
这有个致命问题：用户无法自定义application的content参数实现，也就无法在application{}中添加任何内容，
包括不限于托盘图标、原始的Window函数调用、使用Compose函数包裹Activity根视图调用以提供CompositionLocal值。

为解决此问题，增加了ApplicationRootContent接口：
* 传递此接口，WindowManager会调用接口函数，就将框架内所有窗口显示功能（AllWindows函数）作为参数传递，
允许用户自定义application{}中的内容显示，自行决定AllWindows显示时机，提供托盘图标、脱离框架的Window、提供CompositionLocal值等。
* 不传递此接口，WindowManager将直接在application{}中调用AllWindows函数，显示所有框架内的窗口。

```kotlin
var userInsteadApplicationRootContent: ApplicationRootContent? = null

/**
 * 此函数用于遍历并显示所有要显示的Activity根视图
 */
@Compose
fun AllWindows(){
    windows.forEach { it() }
}

application {
    //如果用户传递了接口，将AllWindows函数包装为普通compsoe函数传递到接口，让用户决定框架内窗口显示时机，
    //提供其他要在application{}中显示的组件
    userInsteadApplicationRootContent?.Invoke(scope = this, content = { AllWindows() }) 
        ?: this.AllWindows() //否则，直接调用AllWindows函数显示所有框架内窗口
}


```

#### 获取框架中的各种服务实例

获取服务实例有两种方式：

1. 在Context环境中使用getService方法
2. 在任意地方使用getServiceInstance方法

```kotlin
getService(ApplicationSaveStateSaver::class)

//公共：
getServiceInstance<ApplicationSaveStateSaver>()
getServiceInstance<IntentProcessor>()

//compose:
getServiceInstance<ActivityManager>()
getServiceInstance<ActivityLauncher>()
getServiceInstance<WindowManager>()

//swing:
getServiceInstance<JFrameManager>()
getServiceInstance<JFrameLauncher>()

//getServiceInstance 方法实际上是调用的 ServiceBooter.getService方法
//也可以使用ServiceBooter.getService获取服务实例

```

#### 使用runOnUIThread运行在Swing 主线程的逻辑。

```kotlin
runOnUIThread(mutex) {
    lifecycleRegistry.handleLifecycleEvent(ON_CREATE)
    lifecycleRegistry.currentState = Lifecycle.State.CREATED
}
```


### Activity

在此框架中 一个Activity = 一个窗口。

**使用Intent启动Activity时如果没有设置token，则Activity不会向ApplicationSaveStateSaver保存与恢复状态。**

启动Activity就是打开窗口（向WindowManager中添加根视图），调用finish结束Activity就是关闭窗口。

框架中提供了Activity或者ComponentActivity两个基类用于实现Activity，
前者仅提供生命周期组件，后者额外提供ViewModelStoreOwner、SaveStateRegister、SaveableStateRegister等组件

实现Activity时需要在onCreate函数中手动调用setContent设置根视图，
为了灵活，根视图不会自动调用Window函数，需要手动在根视图中调用Window函数，以便启动Activity后能显示窗口，
并需要在Window函数content参数实现中调用LinkWindow手动将Activity的生命周期绑定到ComposeWindow，
窗口内容视图要放置在LinkWindow内部，像下面那样：

```kotlin
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: SavedState?) {
        super.onCreate(savedInstanceState)
        //调用setContent设置根视图
        setContent {//根视图实现，需要调用Window，否则启动Activity后是不会显示窗口的
            val state: WindowState = rememberWindowState()
            Window(onCloseRequest = { finish() }, state = state) {
                //调用此方法让Activity链接ComposeWindow生命周期
                LinkWindow {
                    //这里开始窗口视图内容
                    Scaffold {

                    }
                }
            }
        }
    }
}
```

Activity具有完整的生命周期，自身不产生生命周期事件，而是**同步 [ComposeWindow] 的生命周期**。
以下是ComposeWindow的生命周期

| Swing listener callbacks     | Lifecycle event | Lifecycle state change |
|------------------------------|-----------------|------------------------|
| windowIconified(最小化)         | ON_STOP         | STARTED → CREATED      |
| windowDeiconified(还原)        | ON_START        | CREATED → STARTED      |
| windowLostFocus(失去焦点、隐藏)     | ON_PAUSE        | RESUMED → STARTED      |
| windowGainedFocus(获得焦点、恢复显示) | ON_RESUME       | STARTED → RESUMED      |
| dispose(移除window)            | ON_DESTROY      | CREATED → DESTROYED    |

除了ON_CREATE, 在ComposeWindow生命周期变化时，将自动回调Activity中的生命周期方法。

```kotlin

override fun onPause() {
    super.onPause()
}

override fun onResume() {
    super.onResume()
}

override fun onStart() {
    super.onStart()
}

override fun onStop() {
    super.onStop()
}

override fun onDestroy() {
    super.onDestroy()
}
```

如果启动Activity时设置启动模式为“单例”，则再次启动Activity时会回调onRestart方法，且传入再次启动时传递的Intent

```kotlin
    override fun onReStart(intent: Intent?) {
    super.onReStart(intent)
}
```

#### 启动并获取参数，设置返回结果

* 启动activity并获取结果

示例：

1. 在MainActivity中启动详情页面并获取结果

```
 val intent = Intent(this@MainActivity, DetailActivity::class.java).apply {
     multiApplication = true
     token = Token("detail-1")
     data {
         putInt("param1", 1024)
         putString("param2", "str")
     }
 }
 scope.launch {
     startActivityForResult(intent) { result, data ->
         vmActivityResult = data.toString()
        logger.info("data: $data")
    }
}
```

2. 在详情页面设置结果并关闭页面

 ```
//获取启动参数
val params1 = intent?.getData<Int>("param1")
或者
val params1 = intent?.mData.get<Int>("param1")

//设置结果并关闭
setResult(Activity.SUCCESS, bundleOf("result" to value))
finish()
```

* 启动activity除了使用context中的方法，还可以：

1. 使用ActivityManager [ActivityManager.startActivity]
2. 获取ActivityLauncher 启动activity

  ```
在context中
getService<IActivityLauncher>(IActivityLauncher::class).start(intent)
在任意地方
ServiceBooter.getService<IActivityLauncher>(IActivityLauncher::class).start(intent)
```

3. 获取IntentProcessor 启动activity

  ```
在context中
getService<IntentProcessor>(IntentProcessor::class).start(intent)
在任意地方
ServiceBooter.getService<IntentProcessor>(IntentProcessor::class).start(intent)
```

* 除了上面使用startActivityForResult，通过回调接口获取结果外，还可以从intent中的activityResultFlow中collect结果

实际上，startActivityForResult方法回调接口就是封装自intent中的activityResultFlow

```
 intent.activityResultFlow.collect { result ->

 }
 ```

除了使用预定义的activityResultFlow，还可以设定自定义的信箱用于两个activity之间的数据传递

```
 activity1 启动 activity2,获取一个MutableSharedFlow观察activity2回传的结果
 intent.getMailBox<Int>("id").collect {
      //.....
 }

 activity2处理完成后使用MutableSharedFlow回传结果
 intent?.getMailBox<Int>("id").emit(10)
 ```

#### 获取ViewModel

在ComponentActivity中获取ViewModel

```
 class TestViewModel1(
     val savedStateHandle: SavedStateHandle,
     val i: Int,
 ) : ViewModel(){
     companion object {
         val key = object : CreationExtras.Key<Int> {}
         val factory =
             object : ViewModelProvider.Factory {
                 override fun <T : ViewModel> create(
                     modelClass: KClass<T>,
                     extras: CreationExtras,
                 ): T {
                     return TestViewModel1(
                         extras.createSavedStateHandle(),
                         extras[key] ?: 90
                     ) as T
                 }
             }
     }
 }

 class TestViewModel2(
    val savedStateHandle: SavedStateHandle,
) : ViewModel()

val vm1: TestViewModel1 by viewModels<TestViewModel1>(extrasProducer = {
     val extras = MutableCreationExtras()
     extras[TestViewModel1.key] = intent?.getData<Int>("random") ?: 11//从 intent中读取数据
     extras[SAVED_STATE_REGISTRY_OWNER_KEY] = this
     extras[VIEW_MODEL_STORE_OWNER_KEY] = this
     extras
}, { TestViewModel1.factory })

val vm2 by viewModels<TestViewModel2>()

val vm3 = ViewModelProvider.create(
     owner = this,
     creationExtras = mutableCreationExtrasOf {
         this[TestViewModel1.key] = intent?.getData<Int>("random") ?: 11//从 intent中读取数据
     },
     factory = TestViewModel1.factory
)[TestViewModel1::class]

```

### ComponentDialog

* 实现弹窗

```kotlin
class TestDialog : ComponentDialog() {

    override fun onCreate(savedInstanceState: SavedState?) {
        super.onCreate(savedInstanceState)
        setContentView {
            DialogWindow(
                onCloseRequest = { dismiss() },
                visible = mVisibility.value,
            ) {
                LinkDialogWindow {
                    MaterialTheme {
                        Button(onClick = {
                            val testDialog = nestDialog<TestDialog>()
                            testDialog.show()
                        }) {
                            Text("打开嵌套dialog")
                        }
                    }
                }
            }
        }
    }
}

```

* 打开弹窗

如果modal指定为true，将打开模态弹窗
```kotlin
val testDialog =
    componentDialog<TestDialog>(
        context = this@TestFragmentActivity,
        modal = true,
        token = Token("dialog1")
    )
testDialog.show()
```

* 在弹窗中打开嵌套的弹窗

```kotlin
val testDialog = nestDialog<TestDialog>()
testDialog.show()
```

#### 获取ViewModel
与Activity中获取ViewModel一样。

### Fragment

用于拆分Screen结构，将大的Compose页面拆分成几个部分，不能用来显示窗口，功能类似于Android中的Fragment。
Fragment组件也提供了Lifecycle、Viewmodel、SavedState支持。
Lifecycle将跟随父组件的生命周期。

* 使用

实现Fragment
```kotlin
class Fragment1 : Fragment() {
    init {
        lifecycleListener = object : LifecycleEventObserver {
            override fun onStateChanged(
                source: LifecycleOwner,
                event: Lifecycle.Event,
            ) {
                logger.debug("StateChanged to ${event}")
            }
        }
    }

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
```

在Activity中使用Fragment

```kotlin
class TestFragmentActivity() : ComponentActivity() {
    //可以先使用Activity的生命周期生成一个FragmentProvider，通过FragmentProvider获取Fragment实例
    val fragmentProvider = FragmentProvider(this.lifecycle)
    val f1 = fragmentProvider.obtain<Fragment1>(this, Token("122"))
    
    //也可以直接使用Activity的生命周期生成Fragment实例
    val f2 by this.lifecycle.ownedFragment<Fragment1>(this, Token("123"))
    val f3 by activityOwnedFragment<Fragment1>(this, Token("124"))

    @Composable
    private fun extracted2() {
        Column{
            Text("fragment2")
            f2.Screen() //Fragment将显示在这里
            SampleButton("显示/隐藏") {
                if (mVisibility.value) {
                    f2.hide()
                } else {
                    f2.show()
                }
            }
        }
    }
}
```


## Swing框架

更具体的使用可以查看:framework:swing模块中的test源码

### 启动应用

启动应用时不必像compose框架那样使用startApplication方法，也不必实现Application子类。
需要在Main方法中调用Singularity.INSTANCE.boot()启动一些框架的内置服务，
然后与传统的Swing应用一样，在生成JFrame实例，使用setVisible即可显示窗口。

示例：
```java
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            {//Flataf
                System.setProperty("flatlaf.uiScale", "1.25");
                FlatLightLaf.setup();
            }
            Singularity.INSTANCE.boot();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    MainScreen screen = new MainScreen();
                    screen.setVisible(true);
                }
            });
        } catch (Exception e) {
            logger.error("err", e);
        }
    }
}
```

**直接使用setVisible方法显示的JFrame将无法通过Intent获取启动参数，设置结果.**

### ComponentJFrame

#### 打开窗口

除了生成JFrame实例然后调用setVisible方法显示窗口
```
MainScreen screen = new MainScreen();
screen.setVisible(true);
```

还可以使用LaunchJFrameIntent，使用方式与上面Compose章节中的Intent是类似的，
不同之处：
1. 没有为Swing提供Context，要使用Intent打开窗口，无法直接在某个Context中直接调用openJFrame、openJFrameForResult方法。
而是需要调用JFrameManager.openJFrame、JFrameManager.openJFrameForResult方法。
2. java中无法使用kotlin中的value class，要给组件设置Token，需要使用Tokens.of方法获取Token实例

使用Inent启动JFrame示例：
```
LaunchJFrameIntent intent = new LaunchJFrameIntent(this, BookEditorExample.class, LaunchMode.STANDARD);
//Token是value class，java中无法使用value class，所以需要下面这样生成token实例并给Intent设置token
intent.setTokenForJava(Tokens.of("BookEditorExample"));

//启动JFrame并获取结果
JFrameManager.openJFrameForResult(intent, new ComponentResultCallback() {
    @Override
    public void invoke(int resultCode, @Nullable Bundle data) {
        logger.info("LaunchJFrameIntent resultCode: {}, data: {}", resultCode, data);
        tv_bookInfo.setText(data.toString());
    }
});
```

除此之外，在swing中也能使用IntentProcessor打开窗口，这一点与compose中一样
```kotlin
ServiceBooter.getService<IntentProcessor>(IntentProcessor::class).start(intent)
或者
getServiceInstance<IntentProcessor>().start(intent)
```


#### 状态保存与恢复

ComponentJFrame自动启用状态保存与恢复有两种方式：

1. 使用Intent启动ComponentJFrame时设置Token
```
LaunchJFrameIntent intent = new LaunchJFrameIntent(this, BookEditorExample.class, LaunchMode.STANDARD);
intent.setTokenForJava(Tokens.of("BookEditorExample"));
```

2. 手动从ApplicationSaveStateSaver获取一个SavedState实例设置给ComponentJFrame
```
 ApplicationSaveStateSaver service = ServiceBooter.INSTANCE.getService(ApplicationSaveStateSaver.class);
 MainScreen screen = new MainScreen();
 screen.setSavedState(service.obtain(Tokens.of("main-0")));
 screen.setVisible(true);
```

#### ViewModel
获取ViewModel与Compose中一样

### ComponentJDialog

### ComponentJPanel

## 导出与加载应用状态
```kotlin
class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // 从磁盘中读取保存的状态数据,恢复到状态存储器,以便activity打开后能恢复状态
        getService(ApplicationSaveStateSaver::class)
            .import(mutableMapOf())
    }

    override fun onDestroy() {
        super.onDestroy()
        // application结束时将状态保存到磁盘，这里只是为了演示，只打印了一下
        getService(ApplicationSaveStateSaver::class)
            .export()
            .also {
                logger.info("Application save state saver changed to $it")
            }
    }
}
```
Compose框架与swing框架均

---

## 其他 (乱七八糟的呓语)

### window的原理

通常我们会这么使用Window

```
 application {
     var windowClosed by remember { mutableStateOf(false) }
     var mVisible by remember { mutableStateOf(true) }
     if (!windowClosed) {
         Window(
             onCloseRequest = { windowClosed = true },
             isVisible = mVisible,
         ) {
              //界面
         }
     }
 }
 ```

1. 当windowClosed为false时调用了Window函数, compose内容挂载到重组树显示Compose界面、JFrame窗口.
2. 当点击窗口的关闭按钮, 回调Window的onCloseRequest, 将windowClosed置为true, Window从重组树上卸载,
   触发DisposableEffect,将JFrame的isVisible置为false, JFrame不再显示, 且ComposeWindow生命周期走到ON_DESTROY

显示原理：
Window函数重组，内部创建继承自JFrame的ComposeWindow实例，之后会添加一个JPanel，JPanel会持有ComposeWindowContainer，
ComposeWindowContainer会使用ComposeSceneMediator渲染Compose视图
这个ComposeWindowContainer还实现了WindowFocusListener、WindowListener，给JFrame添加状态监听并将其转换为生命周期
关闭原理：
Window函数重组，从重组树上剥离，触发DisposableEffect执行onDispose将JFrame的isVisible置为false取消显示,
ComposeWindow生命周期走到ON_DESTROY
窗口关闭的整个过程是先将Window从重组树上移除, 然后ComposeWindow才会触发ON_DESTROY的生命周期事件,
而不是先触发ComposeWindow的ON_DESTROY的生命周期事件,再将Window从重组树上移除.

ps：显示JFrame时需要将JFrame.isVisible 赋值为true; 不再显示JFrame时需要将JFrame.isVisible 赋值为false,

Window的部分实现代码如下:

```

 fun Window(content: @Composable ()->Unit ) {
     val window = remember{
         ComposeWindow(content).apply{ //一开始就创建JFrame并加载compose视图, 生命周期走到ON_CREATE
             isVisible = true
         }
     }
     DisposableEffect(){
         onDispose {//compose视图不再显示, 取消显示JFrame, 此时生命周期走到ON_DESTROY
             window.isVisible = false
         }
     }
 }

 class ComposeWindow(val content: @Composable ()->Unit) :JFrame{
     init{
         addPanel(ComposeWindowPanel(content,this))
     }
 }

 class ComposeWindowPanel(val content: @Composable ()->Unit, val jFrame:JFrame): JPanel{
     private var composeContainer: ComposeContainer? = ComposeContainer()

     fun setContent(
         content: @Composable () -> Unit
     ) {
         composeContainer.setContent {
             CompositionLocalProvider(
                 LocalWindow provides window
             ) {
                 WindowContentLayout(modifier, content)
             }
         }
     }
 }


 class ComposeContainer(
     val container: JLayeredPane
 ) : WindowFocusListener,
     WindowListener{

     private val mediator = ComposeSceneMediator(

     //监听窗口状态
     override fun windowIconified(e: WindowEvent) {
         isMinimized = true
         updateLifecycleState()
         onWindowPositionChanged()
     }

     //显示compose内容
     fun setContent(content: @Composable () -> Unit) {
         mediator.setContent(content)
     }

     //根据窗口状态设置生命周期状态
     private fun updateLifecycleState() {
         architectureComponentsOwner.setLifecycleState(
             when {
                 isDisposed -> State.DESTROYED
                 isDetached || isMinimized -> State.CREATED
                 !isDetached && !isMinimized && isFocused -> State.RESUMED
                 else -> State.STARTED
             }
         )
     }

 }
 ```

### 数据保存恢复

1. 数据的保存是在Activity的onSaveInstanceState()中调用了SavedStateRegistryController的performSave()方法来实现
2. SavedStateRegistryController是SavedStateRegistry的控制类，关于数据的保存和恢复都转发给了该类处理，performSave()
   方法最终最后转交到SavedStateRegistry的performSave()中。
3. performSave()主要是将需要保存的数据写入到Activity的Bundle对象实现
4. 数据的恢复即在onCreate()调用了performRestore()方法，将保存的数据取出恢复
5. 对于需要保存的数据，实现SavedStateProvider接口，注册一下需要保存的数据；取回数据时；外部通过使用和传给
   registerSavedStateProvider() 方法时一样的 key 来取数据，并在取了之后将数据从
   mRestoredState 中移除。
6. ViewModel创建时默认已经实现了SavedStateProvider等接口，实现了数据保存时从ViewModel中获取数据，恢复时给ViewModel赋值。

### 关闭窗口

[Activity.finish] - 关闭窗口，生命周期[ON_DESTROY]
[Activity.hide] - 隐藏窗口（可以再次显示），生命周期[ON_PAUSE]
关闭窗口：
点击关闭按钮，触发Window的onCloseRequest, 此时需要将Compose内容从重组树上卸载,
不点击关闭按钮,从程序逻辑中关闭窗口,其实也是Compose内容从重组树上卸载,
两种方式流程是一致的，调用finish方法即可。

```
     * Window(
     *  onCloseRequest = { finish() },
     *  visible = mVisibility,
     * )
     * 
 ```

1. 手动点击窗口的"X"关闭按钮, Window触发onCloseRequest回调, 调用finish函数
2. 直接调用finish函数
   会触发如下流程：
   调用WindowManager的deAttachWindow将rootContent移除，applicationScope重组，
   承载着ComposeWindow的rootContent从重组树上被删除，不再显示。
   rootContent内部的ComposeWindow触发onDispose流程，ComposeWindow生命周期走到ON_DESTROY状态，
   由于activity同步ComposeWindow的生命周期，于是activity也会进入[ON_DESTROY]状态，
   并调用[onDestroy]方法, 移除WindowManager中注册的compose视图.

生命周期流程:
activity -> observe and sync -> window lifecycle
即
close application window -> window lifecycle update to ON_DESTROY
-> activity sync window lifecycle -> activity lifecycle will set to ON_DESTROY and invoke onDestroy function

注:

1. 如果ComposeWindow已显示,则从WindowManager中移除compose视图(此视图函数中调用了Window函数)
2. 如果ComposeWindow未显示,则直接使生命周期进入ON_DESTROY

### 啊！

1. 在某些系统中无法显示菜单
   调用 `System.setProperty("skiko.renderApi", "OPENGL")`设置图形后端为opengl或许会解决问题

2. 未完成的多语言切换
   compose resource目前可以使用多国语言，但是它不给你动态修改的功能，相关类和方法都是internal的。
   但是，它的功能实现实际上依赖于Java.Locale，因此我们可以通过在compose刷新之前修改Java.Locale，
   从而半支持多国语言的动态切换（这需要触发整个页面compose的重绘）。

首先，修改java默认locale，然后关闭窗口，此时compose进入onStop状态，
重新打开窗口，compose重加载，重新读取了Java.Locale，从而语言得到了修改。
