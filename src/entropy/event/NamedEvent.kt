package entropy.event

class NamedEvent<T>(val name: String) : EntropyEvent<T>() {
    override fun getKey(): String = name
}