package androidx.compose.desktop.runtime.core

/**
 * 用于拆分application的初始化，避免onCreate方法过于臃肿。
 */
sealed interface Aware

fun interface SyncAware: Aware {
    fun onCreate(application: Application)
}

fun interface AsyncAware :Aware {
    suspend fun onCreate(application: Application)
}
