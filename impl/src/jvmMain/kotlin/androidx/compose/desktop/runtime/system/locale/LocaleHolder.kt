package androidx.compose.desktop.runtime.system.locale

import kotlinx.coroutines.flow.MutableStateFlow
import java.util.*

object LocaleHolder {
    var lastLocale: Locale = Locale.getDefault()
        private set
    val currentLocaleState: MutableStateFlow<Locale> = MutableStateFlow(Locale.getDefault())

    suspend operator fun invoke(locale: Locale) {
        if (currentLocaleState.value != locale) {
            Locale.setDefault(locale)
            lastLocale = currentLocaleState.value
            currentLocaleState.emit(locale)
        }
    }
}
