package entropy.mod

import entropy.mod.special.MultiItem

object ParserMap {
    /**
     * 一个类型对应一个解析器
     */
    val parsers = mutableMapOf<Class<*>, Parser<*>>()

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(type: Class<T>): Parser<T>? = parsers[type] as? Parser<T>

    fun <T> register(type: Parser<T>) {
        parsers[type.typeClass] = type
    }

    fun <T> unregister(typeClass: Class<T>) {
        parsers.remove(typeClass)
    }

    init {
        Parser.MultiItemParser(MultiItem::class.java)
    }
}