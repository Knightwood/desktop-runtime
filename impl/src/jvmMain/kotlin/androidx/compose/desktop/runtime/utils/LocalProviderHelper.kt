package androidx.compose.desktop.runtime.utils

import androidx.compose.runtime.*


infix fun <T> ProvidableCompositionLocal<T>.providesNullable(value: T?): ProvidedValue<T>? {
    return if (value != null) {
        this.provides(value)
    } else null
}

@Composable
@NonSkippableComposable
fun CompositionLocalProviderNullable(vararg values: ProvidedValue<*>?, content: @Composable () -> Unit) {
    val array = values.filterNotNull()
    CompositionLocalProvider(values = array.toTypedArray(), content = content)
}

