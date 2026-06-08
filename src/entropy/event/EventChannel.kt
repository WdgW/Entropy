package entropy.event

class EventChannel {
    private val events = mutableMapOf<Any, EntropyEvent<*>>()

    fun <T> register(event: EntropyEvent<T>) {
        events[event.getKey()] = event
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getEvent(key: Any): EntropyEvent<T>? {
        return events[key] as? EntropyEvent<T>
    }

    fun getOrRegister(key: Any, create: () -> EntropyEvent<*>): EntropyEvent<*> {
        return events.getOrPut(key) { create() }
    }

    fun unregister(key: Any) {
        events.remove(key)
    }

    fun hasEvent(name: String): Boolean = name in events

    fun clear() {
        events.clear()
    }

    val size: Int get() = events.size

}