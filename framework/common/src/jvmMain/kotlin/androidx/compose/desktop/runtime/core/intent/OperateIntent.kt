package androidx.compose.desktop.runtime.core.intent

import androidx.core.bundle.Bundle

interface OperateIntent

/**
 * 启动模式，默认为标准模式，即多个实例可以同时存在。
 */
enum class LaunchMode {
    SINGLE_INSTANCE,
    STANDARD,
    ;

    operator fun plus(data: Any?): Pair<LaunchMode, Any?> {
        return this to data
    }
}

/**
 * activity/LifecycleJFrame回传结果类
 */
data class ComponentResult(
    val resultCode: Int,
    val data: Bundle?
)

/**
 * 结果回调,将ComponentResult拆分,用于在某些时候将返回值转换成回调形式
 */
fun interface ComponentResultCallback {
    fun invoke(resultCode: Int, data: Bundle?)
}

