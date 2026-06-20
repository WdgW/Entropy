package entropy.mod

import entropy.mod.special.MultiItem

/**
 * 解析器映射表，提供类型安全的解析器注册和获取
 */
object ParserMap {
    private val parsers = mutableMapOf<Class<*>, Parser<*>>()

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(type: Class<T>): Parser<T>? {
        return parsers[type] as? Parser<T>
    }

    fun <T> register(type: Parser<T>) {
        parsers[type.typeClass] = type
    }

    fun unregister(typeClass: Class<*>) {
        parsers.remove(typeClass)
    }

    /**
     * 检查指定类型的解析器是否存在
     */
    fun <T> hasParser(type: Class<T>): Boolean {
        return type in parsers
    }

    /**
     * 获取所有已注册的解析器类型
     */
    fun registeredTypes(): Set<Class<*>> = parsers.keys

    /**
     * 清除所有解析器
     */
    fun clear() = parsers.clear()

    init {
        Parser.MultiItemParser(MultiItem::class.java)
    }
}