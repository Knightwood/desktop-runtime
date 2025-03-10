package androidx.compose.desktop.runtime.fragment

import androidx.compose.desktop.runtime.activity.FragmentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.core.bundle.Bundle
import androidx.lifecycle.*

open class Fragment() : Component() {
    val mVisibility = mutableStateOf(false)
    var activity: FragmentActivity? = null
    internal var mComposeView: FragmentComposeView? = null
    val parentLifecycle: Lifecycle
        get() {
            if (activity == null) {
                throw IllegalStateException("activity is null")
            } else {
                return activity!!.lifecycle
            }
        }
    val bundle: Bundle
        get() = activity!!.componentBundle.obtainBundle(mWho)

    fun requireActivity(): FragmentActivity {
        if (activity == null) {
            throw IllegalStateException("activity is null")
        } else {
            return activity!!
        }
    }

    fun attach(activity: FragmentActivity) {
        this.activity = activity
        parentLifecycle.addObserver(this)
    }

    /**
     * 重写此方法，调用[ComposeView]并return提供界面
     */
    open fun onCreateView(): FragmentComposeView? {
        return null
    }

    /**
     * 在activity中调用此方法显示界面
     */
    @Composable
    fun screen() {
        if (this.mComposeView == null)
            this.mComposeView = onCreateView()
        if (mVisibility.value) {
            this.mComposeView?.invoke()
        }
    }

    fun ComposeView(content: @Composable () -> Unit): FragmentComposeView {
        return FragmentComposeView { key(mWho) { content() } }
    }

    fun show() {
        mVisibility.value = true
    }

    fun hide() {
        mVisibility.value = false
    }

    override fun provideSaveState(): Bundle = bundle

}


fun interface FragmentComposeView {
    @Composable
    fun invoke()
}
