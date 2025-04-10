
<p align="center"><h1 align="center">Android like desktop framework.</h1></p>
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

##  Overview

* 提供Application类用于初始化逻辑。
* 提供Window管理，其被包装为Activity类，方便开发。
* 提供IScreenComponent组件，类似于Fragment，将界面实现和逻辑拆分。
* 使用IScreenComponent组件包装DialogWindow，更易于使用。
* Activity及其子类、IScreenComponent组件实现了ViewModelStoreOwner、LifecycleOwner等接口，可以提供ViewModel实例，支持SavedStateHandle等。
* 使rememberSaveable正常工作。

---

##  Getting Started

###  安装

1. clone项目
```kotlin
git clone https://github.com/Knightwood/desktop-runtime.git
```
2. 将项目打包到本地仓库
```
./gradlew publishToMavenLocal
```
3. 依赖并使用
```kotlin
implementation("com.github.knightwood:desktop-runtime:1.0.0"){ isChanging=true }
implementation("com.github.knightwood:jvm-system-spi:1.0.0"){ isChanging=true }
implementation("com.github.knightwood:jvm-system-win:1.0.0")
```

###  使用
#### 问题
1. 在某些系统中无法显示菜单
调用 `System.setProperty("skiko.renderApi", "OPENGL")`设置图形后端为opengl或许会解决问题

#### 基本使用方式
创建一个Application类和MainActivity类，并使用startApplication函数启动项目。

* MainApplication
```kotlin
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ctx = this
        //通过修改locale，修改软件的语言显示
        Locale.setDefault(Locale.US)
        AppInfoProvider.provide {
            appName = "测试"
            isDevMode = true
        }
    }

    companion object {
        lateinit var ctx: Application
    }
}

```

* MainActivity
```kotlin
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state = rememberWindowState(position = WindowPosition.Aligned(Alignment.Center), size = DpSize(300.dp, 300.dp))
            ComposeView(
                onCloseRequest = { finish() },
                state = state,
                alwaysOnTop = true,
                title = "标题",
                undecorated = true,
                transparent = true,
            ) {
                Text("主页", fontSize = 64.sp)
            }
        }
    }
}
```

* startApplication函数作为入口
```kotlin
fun main() = startApplication<SplashActivity, MainApplication>(
    applicationContent = {
        //这里可以直接访问ApplicationScope，从这里创建系统托盘
        val painter = painterResource("icons/app_icon.svg")
        val icon1 = rememberVectorPainter(Icons.Default.Settings)
        val icon2 = rememberVectorPainter(Icons.Default.ExitToApp)
        FixedSystemTray(icon = painter, tooltip = "hello",
            menu = remember {
                buildTrayMenu {
                    this + FixedTrayMenuItem("settings", icon = icon1)
                    this + TraySeparator
                    this + FixedTrayMenuItem("exit", icon = icon2) {
                        log.info("exit")
                        MainApplication.ctx.exitApp()
                    }
                }
            })
    }
)
```

#### 创建viewmodel、 正常使用rememberSaveable

可以查看composeApp模块下的示例：VMTestActivity、StateTestActivity

##### viewmdoel
最简单的使用方式，这跟在android中使用是一样的。
需要继承自ComponentActivity

```kotlin
class TestViewModel(val savedStateHandle: SavedStateHandle) : ViewModel() {}

class VMTestActivity : ComponentActivity() {
    val vm1: TestViewModel by viewModels<TestViewModel>()

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContent {
         ComposeView {
            MaterialTheme {
               //其余代码省略
            }
         }
      }
   }
   //其余代码省略
}
```

##### 恢复状态
若需要在关闭activity时保存状态，再次打开时恢复状态：
1. 需要配置启动此activity的intent
2. closeActivity 需要为真

```kotlin
val intent = Intent()
//因为大多数时候我们不需要保存恢复数据，clearSaveState默认为true
//但我们这里希望保存恢复数据，因此需要设置clearSaveState为false
intent.clearSaveState = false
//需要注意，activity取回保存起来的数据依靠uuid，因此需要在这里设置uuid
intent.uuid = "11"
startActivity(SaveStateTestActivity::class.java, intent)
```

```kotlin
class SaveStateTestActivity : ComponentActivity() {
   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContent {
         //由于状态存储发生在onDestroy阶段，closeActivity为true时才会触发onDestroy
          ComposeView(closeActivity = true) {
            MaterialTheme {
               Column {
                  val text = rememberSaveable() {
                     mutableStateOf("ede")
                  }
                  Text(text = text.value)
               }
            }
         }
      }
   }
   //其余代码省略
}
```

#### Fragment & DialogFragment
可以查看composeApp模块下的示例：TestFragmentActivity
需要activity继承自ComponentActivity，或者你可以参照FragmentActivity的实现自由发挥。

fragment继承自IScreenComponent，与activity相似，也实现了ViewModelStoreOwner, LifecycleOwner, LifecycleEventObserver,
HasDefaultViewModelProviderFactory, SavedStateRegistryOwner等接口。

```kotlin

class TestFragmentActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       //注册fragment和dialogFragment
        register<Fragment1>("123")
        register<Fragment1>("124")
        register<TestDialog>("dialog1")
        setContent {
            ComposeView() {
                //可以通过screen函数获取fragment
                var screen1 by remember {
                    mutableStateOf<Fragment?>(screen("123"))
                }
                MaterialTheme {
                    Column {
                        screen1?.invoke()//显示fragment
                        Row {
                            SampleButton("关闭") {
                                unregister("123") //关闭fragment
                                screen1 = null
                            }
                            SampleButton("再打开") {
                                //重新生成一个fragment
                                screen1 = register<Fragment1>("123")
                            }
                        }

                        //弹窗
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

                        //可以直接使用，不需要注册的弹窗
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
```
##### fragment

```kotlin
class Fragment1 : Fragment() {
    init {
        //如果为true，则不会在onDestroy时保存数据，也不会在 onCreate中恢复数据
        //默认为true
        clearBundle = false
    }

    override fun onCreateView(): ComponentViewHolder {
        return ComposeView {
            MaterialTheme {
               
            }
        }
    }
}

```
##### DialogFragment
```kotlin
class TestDialog : DialogFragment() {
    override fun onCreateView(): ComponentViewHolder {
        return ComposeView {
            Dialog() {
                MaterialTheme {
                    
                }
            }
        }
    }
}
```

###  运行demo
使用下面的命令运行项目
`gradle desktopRun -DmainClass=com.github.knightwood.example.MainKt --quiet` &nbsp; 

```sh
❯ gradle desktopRun -DmainClass=com.github.knightwood.example.MainKt --quiet
```


---

##  Contributing

- **💬 [Join the Discussions](https://github.com/Knightwood/desktop-runtime/discussions)**: Share your insights, provide feedback, or ask questions.
- **🐛 [Report Issues](https://github.com/Knightwood/desktop-runtime/issues)**: Submit bugs found or log feature requests for the `desktop-runtime` project.
- **💡 [Submit Pull Requests](https://github.com/Knightwood/desktop-runtime/blob/main/CONTRIBUTING.md)**: Review open PRs, and submit your own PRs.

<details closed>
<summary>Contributing Guidelines</summary>

1. **Fork the Repository**: Start by forking the project repository to your github account.
2. **Clone Locally**: Clone the forked repository to your local machine using a git client.
   ```sh
   git clone https://github.com/Knightwood/desktop-runtime
   ```
3. **Create a New Branch**: Always work on a new branch, giving it a descriptive name.
   ```sh
   git checkout -b new-feature-x
   ```
4. **Make Your Changes**: Develop and test your changes locally.
5. **Commit Your Changes**: Commit with a clear message describing your updates.
   ```sh
   git commit -m 'Implemented new feature x.'
   ```
6. **Push to github**: Push the changes to your forked repository.
   ```sh
   git push origin new-feature-x
   ```
7. **Submit a Pull Request**: Create a PR against the original project repository. Clearly describe the changes and their motivations.
8. **Review**: Once your PR is reviewed and approved, it will be merged into the main branch. Congratulations on your contribution!
</details>

<details closed>
<summary>Contributor Graph</summary>
<br>
<p align="left">
   <a href="https://github.com{/Knightwood/desktop-runtime/}graphs/contributors">
      <img src="https://contrib.rocks/image?repo=Knightwood/desktop-runtime">
   </a>
</p>
</details>

---

##  License

This project is protected under the [SELECT-A-LICENSE](https://choosealicense.com/licenses) License. For more details, refer to the [LICENSE](https://choosealicense.com/licenses/) file.

---

##  Acknowledgments

- List any resources, contributors, inspiration, etc. here.

---
