package entropy.mod

import arc.util.serialization.JsonValue
import entropy.mod.Parser.Companion.pLog
import entropy.mod.special.MultiItem
import entropy.world.PowerProjector
import entropy.world.PowerProjectorNode

object ClassMap {
    val classMap = HashMap<String, Pair<Class<*>, (jsonValue: JsonValue, jsonPath: String) -> Any?>>()
    fun <T> addClass(name: String, classType: Class<T>, constructor: (jsonValue: JsonValue, jsonPath: String) -> T?): Boolean {
        if (classMap.containsKey(name)) return false
        classMap[name] = Pair(classType, constructor)
        return true
    }

    operator fun get(name: String) = classMap[name]

    operator fun get(name: String, default: Pair<Class<*>, (jsonValue: JsonValue, jsonPath: String) -> Any?>) = classMap[name] ?: default

    fun getClass(name: String) = classMap[name]?.first

    fun getConstructor(name: String) = classMap[name]?.second

    fun hasClass(name: String) = this[name] != null

    fun removeClass(name: String) = classMap.remove(name)

    fun clear() = classMap.clear()

    inline fun <reified T> register(name: String, noinline constructor: (jsonValue: JsonValue, jsonPath: String) -> T?): Boolean {
        if (classMap.containsKey(name)) return false
        classMap[name] = Pair(T::class.java, constructor)
        //首字母小写
        classMap[name.firstCharLowerCase()] = Pair(T::class.java, constructor)
        return true
    }

    fun String.firstCharLowerCase() = if (isEmpty()) this else replaceFirstChar { it.lowercase() }


    init {
        register("PowerProjectorNode") { jsonValue, jsonPath ->
            if (jsonValue.isNull || jsonValue.size <= 0) return@register null
            val nameValue = jsonValue.remove("name")
            if (nameValue == null || nameValue.isNull || !nameValue.isString) {
                "$jsonPath 中没有name字段或者name字段为空".pLog()
                return@register null
            }
            val name = nameValue.asString()
            if (name.isEmpty()) {
                "$jsonPath 中name字段为空".pLog()
                return@register null
            }
            return@register PowerProjectorNode(name)
        }
        register("PowerProjector") { jsonValue, jsonPath ->
            if (jsonValue.isNull || jsonValue.size <= 0) return@register null
            val nameValue = jsonValue.remove("name")
            if (nameValue == null || nameValue.isNull || !nameValue.isString) {
                "$jsonPath 中没有name字段或者name字段为空".pLog()
                return@register null
            }
            val name = nameValue.asString()
            if (name.isEmpty()) {
                "$jsonPath 中name字段为空".pLog()
                return@register null
            }
            return@register PowerProjector(name)
        }
        register("MultiItem") { _, _ -> MultiItem() }
        "_______".pLog()
    }
}