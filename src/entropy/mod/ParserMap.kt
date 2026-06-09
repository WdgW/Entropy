package entropy.mod

import arc.util.serialization.JsonValue
import entropy.EntropyContent
import entropy.world.PowerProjectorNode
import mindustry.mod.ContentParser

object ParserMap {
    /**
     * 一个类型对应一个解析器
     */
    val parsers = mutableMapOf<Class<*>, Parser<*>>()
    @Suppress("UNCHECKED_CAST")
    fun <T> get(type: Class<T>): Parser<T>? = parsers[type] as? Parser<T>
    fun <T> register(typeClass: Class<T>,type: Parser<T>) {
        parsers[typeClass] = type
    }
    fun <T> unregister(typeClass: Class<T>) {
        parsers.remove(typeClass)
    }
    init {
        object: Parser<PowerProjectorNode>(PowerProjectorNode::class.java){
            override fun parse(jsonValue: JsonValue): PowerProjectorNode {
                return PowerProjectorNode(jsonValue.toString())
//                ContentParser
            }
        }
    }
}