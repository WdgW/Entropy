package entropy.event

abstract class EntropyEvent<T> {
    private val listeners = mutableListOf<(T) -> Unit>()

    fun on(func: (T) -> Unit) = listeners.add(func)

    fun remove(func: (T) -> Unit) = listeners.remove(func)


    fun fire(data: T) = listeners.toList().forEach { it.invoke(data) }

    fun clear() = listeners.clear()

    abstract fun getKey(): Any

    fun listenerCount(): Int = listeners.size
}