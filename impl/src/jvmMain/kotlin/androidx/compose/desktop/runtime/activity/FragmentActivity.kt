package androidx.compose.desktop.runtime.activity

import androidx.compose.desktop.runtime.fragment.IScreenComponentManager
import androidx.compose.desktop.runtime.fragment.FragmentManager

open class FragmentActivity() : ComponentActivity(), IScreenComponentManager by FragmentManager() {
    init {
        provideLifeCycle(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        clearScreenComponent()
    }
}
