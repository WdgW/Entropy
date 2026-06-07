package entropy.event

class NamedEvent<T>(val name: String) : EntropyEvent<T>() {
    override fun getKey(): Any = name as Any
}