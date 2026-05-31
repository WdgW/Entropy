package entropy

abstract class Event<T> {
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

class EventChannel {
    private val eventMap = mutableMapOf<String, Event<*>>()
    
    abstract class NamedEvent<T>(val name: String) : Event<T>()
    
    fun <T> register(event: NamedEvent<T>) {
        eventMap[event.name] = event
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getEvent(name: String): NamedEvent<T>? {
        return eventMap[name] as? NamedEvent<T>
    }

    fun unregister(name: String) {
        eventMap.remove(name)
    }
}
