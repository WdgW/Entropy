package entropy.event

/**
 * 带名称的事件
 * @param T 事件数据类型
 * @param name 事件名称
 */
class NamedEvent<T>(override val name: String) : EntropyEvent<T>() {
    override fun getKey(): String = name
}