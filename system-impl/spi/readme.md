spi模块下添加多平台共用的接口， win、mac、linux模块实现spi模块中的接口，并使用koin提供实例
每个平台模块使用koin提供实例时需要添加平台标识符，如：
```kotlin
// spi模块

interface PlatformWindowHelper {
    fun bringToFront()
}

enum class Platform {
    WIN("win"),Linux("linux"),Mac("mac")
}
val Platform.current: Platform
    get(){
        // 返回当前平台的枚举
        TODO("Not yet implemented")
    }

/**
 * 获取当前平台下某个类的实例
 */
inline fun<reified T> getPlatformInstance(): T{
    koin.get<T>(named(Platform.current))
}

//win模块
class WinWindowHelper : PlatformWindowHelper {
    override fun bringToFront() {}
}

object WinPlatformKoinModule {
    val module = module {
        single<PlatformWindowHelper>(named(Platform.WIN)) { WinWindowHelper() }
    }
}

//app模块，获取当前平台下的WindowHelper
val windowHelper = getPlatformInstance<PlatformWindowHelper>()

```
