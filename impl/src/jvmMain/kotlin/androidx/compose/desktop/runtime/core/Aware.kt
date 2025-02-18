package androidx.compose.desktop.runtime.core

/**
 * 用于拆分application的初始化，避免onCreate方法过于臃肿。
 */
fun interface Aware {
    fun onCreate(application: Application)
}
