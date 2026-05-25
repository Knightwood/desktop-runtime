package androidx.jvm.system.windows

import com.github.knightwood.slf4j.kotlin.logFor
import com.google.auto.service.AutoService

private val logger = logFor("WindowHelper")

@AutoService(PlatformWindowHelper::class)
open class WindowsWindowHelper : PlatformWindowHelper {
    override fun bringToFront(desc: WindowDesc) {

    }
}
