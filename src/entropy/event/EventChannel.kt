package entropy.event

class EventChannel<U> {
    private val events = mutableMapOf<U, EntropyEvent<*>>()

    fun <T> register(key: U, event: EntropyEvent<T>) {
        events[key] = event
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getEvent(key: U): EntropyEvent<T>? {
        return events[key] as? EntropyEvent<T>
    }

    fun getOrRegister(key: U, create: () -> EntropyEvent<*>): EntropyEvent<*> {
        return events.getOrPut(key) { create() }
    }

    fun unregister(key: U) {
        events.remove(key)
    }

    fun hasEvent(key: U): Boolean = key in events

    fun clear() {
        events.clear()
    }

    val size: Int get() = events.size

}