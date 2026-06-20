package entropy.mod

import arc.util.serialization.JsonValue
import entropy.mod.Parser.Companion.pLog
import entropy.mod.special.MultiItem
import entropy.world.PowerProjector
import entropy.world.PowerProjectorNode

/**
 * 类型映射表，提供类型安全的类注册和获取
 */
object ClassMap {
    private val classMap = HashMap<String, ClassInfo>()

    private data class ClassInfo(
        val classType: Class<*>,
        val constructor: (jsonValue: JsonValue, jsonPath: String) -> Any?
    )

    @Suppress("UNCHECKED_CAST")
    fun <T> addClass(
        name: String,
        classType: Class<T>,
        constructor: (jsonValue: JsonValue, jsonPath: String) -> T?
    ): Boolean {
        if (classMap.containsKey(name)) return false
        classMap[name] = ClassInfo(classType) { jsonValue, jsonPath ->
            constructor(jsonValue, jsonPath)
        }
        //首字母小写
        val lowerName = name.firstCharLowerCase()
        if (lowerName != name) {
            classMap[lowerName] = ClassInfo(classType) { jsonValue, jsonPath ->
                constructor(jsonValue, jsonPath)
            }
        }
        return true
    }

    operator fun get(name: String): ClassInfo? = classMap[name]

    @Suppress("UNCHECKED_CAST")
    fun <T> get(name: String, default: ClassInfo): ClassInfo = classMap[name] ?: default

    fun getClass(name: String): Class<*>? = classMap[name]?.classType

    @Suppress("UNCHECKED_CAST")
    fun <T> getConstructor(name: String): ((jsonValue: JsonValue, jsonPath: String) -> T?)? {
        return classMap[name]?.constructor as? (JsonValue, String) -> T?
    }

    fun hasClass(name: String): Boolean = name in classMap

    fun removeClass(name: String) = classMap.remove(name)

    fun clear() = classMap.clear()

    inline fun <reified T> register(
        name: String,
        noinline constructor: (jsonValue: JsonValue, jsonPath: String) -> T?
    ): Boolean {
        if (classMap.containsKey(name)) return false
        classMap[name] = ClassInfo(T::class.java) { jsonValue, jsonPath ->
            constructor(jsonValue, jsonPath)
        }
        //首字母小写
        val lowerName = name.firstCharLowerCase()
        if (lowerName != name) {
            classMap[lowerName] = ClassInfo(T::class.java) { jsonValue, jsonPath ->
                constructor(jsonValue, jsonPath)
            }
        }
        return true
    }

    private fun String.firstCharLowerCase(): String =
        if (isEmpty()) this else replaceFirstChar { it.lowercaseChar() }

    init {
        register<PowerProjectorNode>("PowerProjectorNode") { jsonValue, jsonPath ->
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
            PowerProjectorNode(name)
        }
        register<PowerProjector>("PowerProjector") { jsonValue, jsonPath ->
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
            PowerProjector(name)
        }
        register<MultiItem>("MultiItem") { _, _ -> MultiItem() }
        "_______".pLog()
    }
}