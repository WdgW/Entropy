package entropy.mod

import arc.files.Fi
import entropy.mod.Parser.Companion.parseLog
import entropy.mod.Parser.Companion.toHashMap
import entropy.mod.Parser.Companion.toJsonValue

/**
 * 类型别名
 * @param typeAliasFile typealias.json/typealias.hjson文件的Fi对象
 */
class TypeAlias(val typeAliasFile: Fi) {
    val typeAliasMap = HashMap<String, Class<*>>()
    init {
        val lines = typeAliasFile.readString().toHashMap()
        if (lines != null) {
            for ((key, value) in lines) {
                if (key !is String) {
                    "$key 不是字符串".tALog()
                    continue
                }
                if (value !is String) {
                    "$value 不是字符串".tALog()
                    continue
                }
                val classType = ClassMap.getClass(value)
                if (classType == null) {
                    "$value 不存在".tALog()
                    continue
                }
                typeAliasMap[key] = classType

            }
        }
    }
    fun String.tALog() = " [TypeAlias] ${typeAliasFile.name()} $this".parseLog()
    override fun toString(): String {
        return typeAliasMap.toString()
    }
    fun get(typeName: String) = get(typeName, null)
    fun get(typeName: String, defaultValue: Class<*>? = null) = typeAliasMap[typeName] ?: defaultValue
}