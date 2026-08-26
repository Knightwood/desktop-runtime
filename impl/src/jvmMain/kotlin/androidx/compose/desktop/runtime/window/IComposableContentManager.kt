package androidx.compose.desktop.runtime.window

import androidx.compose.desktop.runtime.savestate.Token

/**
 * 记录与某idn(token)关联的状态、activity根视图等内容
 */
class ActivityContentEntity() {
    var isAttachedToApplication = false

    /**
     * activity 根视图
     * 注意: 需要在此compose函数实现中调用 [androidx.compose.ui.window.Window]
     */
    var rootContent: ApplicationComposableContent? = null
        set(value) {
            if (field != null) {
                throw IllegalStateException("rootContent already set")
            }
            field = value
        }
}
