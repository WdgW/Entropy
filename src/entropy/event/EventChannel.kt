package entropy.event

/**
 * 事件通道，提供类型安全的事件存储和获取
 * @param K 键类型
 * @param T 事件数据类型
 */
class EventChannel<K, T> {
    private val events = linkedMapOf<K, EntropyEvent<T>>()

    fun register(key: K, event: EntropyEvent<T>) {
        events[key] = event
    }

    fun getEvent(key: K): EntropyEvent<T>? {
        return events[key]
    }

    fun getOrRegister(key: K, create: () -> EntropyEvent<T>): EntropyEvent<T> {
        return events.getOrPut(key) { create() }
    }

    fun unregister(key: K) {
        events.remove(key)
    }

    fun hasEvent(key: K): Boolean = key in events

    fun clear() {
        events.clear()
    }

    val size: Int get() = events.size

    /**
     * 遍历所有事件
     */
    fun forEach(action: (K, EntropyEvent<T>) -> Unit) {
        events.forEach(action)
    }

    /**
     * 获取所有键
     */
    fun keys(): Set<K> = events.keys

    /**
     * 获取所有事件
     */
    fun allEvents(): Collection<EntropyEvent<T>> = events.values
}