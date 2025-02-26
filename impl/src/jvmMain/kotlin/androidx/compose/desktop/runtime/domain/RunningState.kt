package androidx.compose.desktop.runtime.domain


sealed class RunningState()

data object IDLE : RunningState()

data class Stop(
    val message: String? = null,
    val exception: Throwable? = null,
) : RunningState()
