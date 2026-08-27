package androidx.compose.desktop.runtime.activity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

object ActivityLifecycleOwner {
    private val ActivityLocalLifecycleOwner = compositionLocalOf<LifecycleOwner?> { null }

    public val current: LifecycleOwner
        @Composable get() = ActivityLocalLifecycleOwner.current ?: LocalLifecycleOwner.current

    public infix fun provides(
        viewModelStoreOwner: LifecycleOwner?,
    ): ProvidedValue<LifecycleOwner?> {
        return ActivityLocalLifecycleOwner.provides(viewModelStoreOwner)
    }
}

object FragmentLifecycleOwner {
    private val FragmentLocalLifecycleOwner = compositionLocalOf<LifecycleOwner?> { null }

    public val current: LifecycleOwner
        @Composable get() = FragmentLocalLifecycleOwner.current ?: LocalLifecycleOwner.current

    public infix fun provides(
        viewModelStoreOwner: LifecycleOwner?,
    ): ProvidedValue<LifecycleOwner?> {
        return FragmentLocalLifecycleOwner.provides(viewModelStoreOwner)
    }
}
