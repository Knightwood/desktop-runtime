package androidx.compose.desktop.runtime.fragment

import androidx.compose.runtime.snapshots.SnapshotStateList

class FragmentManager {
    private val stack = SnapshotStateList<Fragment>()

    fun push(component: Fragment) {
        stack.add(component)
    }

    fun pop() {
        stack.removeLast()
    }

    fun clear() {
        stack.clear()
    }

    fun get(key: String): Fragment {
        return stack.find { it.mWho == key } ?: throw IllegalArgumentException("Component not found")
    }

    fun pop(fragment: Fragment) {
        stack.remove(fragment)
    }
}
