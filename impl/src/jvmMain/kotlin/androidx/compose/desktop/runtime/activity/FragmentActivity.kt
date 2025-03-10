package androidx.compose.desktop.runtime.activity

import androidx.compose.desktop.runtime.fragment.FragmentManager
import androidx.compose.desktop.runtime.fragment.Fragment

open class FragmentActivity : ComponentActivity() {
    val fragmentManager: FragmentManager = FragmentManager()
    val componentBundle: BundleHolder = BundleHolder()

    inline fun <reified T : Fragment> register() {
        val fragment = T::class.java.getDeclaredConstructor().newInstance()
        fragmentManager.push(fragment)
        fragment.attach(this)
    }

    fun unregister(fragment: Fragment) {
        fragmentManager.pop(fragment)
        fragment.release()
    }

}
