package androidx.compose.desktop.runtime.activity

import androidx.compose.desktop.runtime.fragment.IScreenComponentManager
import androidx.compose.desktop.runtime.fragment.ScreenComponentStackManager
import androidx.compose.desktop.runtime.fragment.ScreenComponent
import androidx.compose.desktop.runtime.fragment.ScreenComponentManager
import androidx.lifecycle.LifecycleOwner

open class FragmentActivity() : ComponentActivity(), IScreenComponentManager by ScreenComponentManager() {
    init {
        provideLifeCycle(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        clearScreenComponent()
    }
}
