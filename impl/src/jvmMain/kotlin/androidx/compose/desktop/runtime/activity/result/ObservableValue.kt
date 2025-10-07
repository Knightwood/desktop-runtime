package androidx.compose.desktop.runtime.activity.result;

class ObservableFlow<T> {
    val observers = mutableListOf<Observer<T>>()
    private var value: T? = null

    public fun setValue(value: T?) {
        this.value = value
        notifyObservers()
    }

    private fun notifyObservers() {
        val t = value
        if (t != null) {
            observers.forEach { it.onChanged(t) }
        }
    }

    public fun getValue(): T? {
        return value
    }

    public fun removeObserver(observer: Observer<T>) {
        observers.remove(observer)
    }

    public fun observe(observer: Observer<T>) {
        observers.add(observer)
    }

    fun clear() {
        observers.clear()
    }
}

fun interface Observer<T> {
    fun onChanged(value: T)
}
