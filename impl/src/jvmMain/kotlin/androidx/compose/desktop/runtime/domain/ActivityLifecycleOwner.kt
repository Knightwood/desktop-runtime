package androidx.compose.desktop.runtime.domain

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.LifecycleOwner

object ActivityLifecycleOwner {
    private val LocalLifecycleOwner = compositionLocalOf<LifecycleOwner?> { null }

    public val current: LifecycleOwner?
        @Composable get() = LocalLifecycleOwner.current ?: throw IllegalStateException(
            "CompositionLocal LifecycleOwner not present"
        )

    public infix fun provides(
        viewModelStoreOwner: LifecycleOwner,
    ): ProvidedValue<LifecycleOwner?> {
        return LocalLifecycleOwner.provides(viewModelStoreOwner)
    }
}
