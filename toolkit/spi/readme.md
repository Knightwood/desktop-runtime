## 使用
```kotlin
InstanceContext.startUp()
```

## 基本原理：

spi模块定义多平台共用接口，win、mac、linux模块实现spi模块中的接口，并使用koin提供实例。
spi将实现一个独立的koin application，还定义了一个用于提供koin module的接口ModuleProvider，各平台module实现此接口并标注AutoService注解。
spi模块将查找所有ModuleProvider，调用其provide方法得到各平台实现的spi定义的接口实现。
每个平台模块使用koin提供接口实例时需要添加平台标识符。

```kotlin
// spi模块

interface PlatformWindowHelper {
    fun bringToFront()
}

enum class Platform {
    WIN("win"), Linux("linux"), Mac("mac")
}
interface ModuleProvider {
    fun provide(): Module
}

val Platform.current: Platform
get() {
    // 返回当前平台的枚举
    TODO("Not yet implemented")
}

/**
 * 获取当前平台下某个类的实例
 */
inline fun <reified T> getPlatformInstance(): T {
    koin.get<T>(named(Platform.current))
}

//win模块
class WinWindowHelper : PlatformWindowHelper {
    override fun bringToFront() {}
}

object WinPlatformKoinModule : ModuleProvider {
    val module = module {
        //定义了spi module中的接口实现
        //且使用平台标识符标识唯一性
        //这样当集成了linux、win等多平台模块，获取接口实例时可以传入当前平台标识符获取对应实例
        single<PlatformWindowHelper>(named(Platform.WIN)) { WinWindowHelper() }
    }
    override fun provide(): Module {
        return module
    }
}

//app模块，获取当前平台下的WindowHelper
val windowHelper = getPlatformInstance<PlatformWindowHelper>()

```
