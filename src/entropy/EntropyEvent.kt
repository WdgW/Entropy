package entropy

abstract class EntropyEvent<T> {
    private val listeners = mutableListOf<(T) -> Unit>()

    fun listen(func: (T) -> Unit) {
        listeners.add(func)
    }

    fun remove(func: (T) -> Unit) {
        listeners.remove(func)
    }

    fun emit(data: T) {
        listeners.toList().forEach { it.invoke(data) }
    }

    fun listenerCount(): Int = listeners.size
}