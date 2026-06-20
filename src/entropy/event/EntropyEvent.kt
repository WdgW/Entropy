package entropy.event

/**
 * 事件基类
 * @param T 事件数据类型
 */
abstract class EntropyEvent<T> {
    private val listeners = mutableListOf<(T) -> Unit>()

    fun on(func: (T) -> Unit): Boolean = listeners.add(func)

    fun remove(func: (T) -> Unit): Boolean = listeners.remove(func)

    fun fire(data: T) {
        listeners.toList().forEach { it.invoke(data) }
    }

    fun clear() = listeners.clear()

    fun listenerCount(): Int = listeners.size

    abstract fun getKey(): Any
}