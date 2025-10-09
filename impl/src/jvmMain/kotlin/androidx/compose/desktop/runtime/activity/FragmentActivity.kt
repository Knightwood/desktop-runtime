package androidx.compose.desktop.runtime.activity

import androidx.compose.desktop.runtime.fragment.IFragmentComponentManager
import androidx.compose.desktop.runtime.fragment.FragmentManager

open class FragmentActivity() : ComponentActivity(), IFragmentComponentManager by FragmentManager() {
    init {
        provideLifeCycle(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        release()
    }
}
