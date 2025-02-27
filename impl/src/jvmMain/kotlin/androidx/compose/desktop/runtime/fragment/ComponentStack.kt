package androidx.compose.desktop.runtime.fragment

interface IComponentStack {
    fun push(component: ScreenComponent)
    fun pop()
    fun peek(): ScreenComponent?
    fun clear()
}

class ComponentStack {

}
