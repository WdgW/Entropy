package entropy

class EventChannel {
    private val eventMap = mutableMapOf<String, EntropyEvent<*>>()

    fun <T> register(event: NamedEvent<T>) {
        eventMap[event.name] = event
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getEvent(name: String): NamedEvent<T>? {
        return eventMap[name] as? NamedEvent<T>
    }

    fun getOrRegister(name: String, create: () -> EntropyEvent<*>): EntropyEvent<*> {
        return eventMap.getOrPut(name) { create() }
    }

    fun unregister(name: String) {
        eventMap.remove(name)
    }

    fun hasEvent(name: String): Boolean = name in eventMap

    fun clear() {
        eventMap.clear()
    }

    val size: Int get() = eventMap.size

}