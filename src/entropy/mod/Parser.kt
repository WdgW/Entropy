@file:Suppress("NOTHING_TO_INLINE")

package entropy.mod

import arc.graphics.Color
import arc.util.serialization.Json
import arc.util.serialization.JsonReader
import arc.util.serialization.JsonValue
import arc.util.serialization.Jval
import entropy.log
import entropy.mod.ParserMap.register
import entropy.mod.special.MultiItem
import mindustry.Vars
import mindustry.ctype.Content
import mindustry.ctype.ContentType
import mindustry.ctype.MappableContent
import mindustry.ctype.UnlockableContent
import mindustry.type.Item

abstract class Parser<T>(val typeClass: Class<T>) {
    companion object {
        val json = Json()
        var jsonReader: JsonReader = JsonReader()
        inline fun String.toJsonValue(): JsonValue? {
            try {
                return jsonReader.parse(Jval.read(this).toString(Jval.Jformat.plain))
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        inline fun String.toHashMap(): HashMap<*, *>? {
            return json.readValue(HashMap::class.java, toJsonValue())
        }

        inline fun <T> T.pLog() = this.log("[P]")

        /**
         * 检查jsonValue是否符合typeAlias的定义(含classMap)
         * @param jsonValue json文件内容
         * @param jsonPath json文件路径
         * @param typeAlias 类型别名
         * @return 是否合规
         */
        fun check(
            jsonValue: JsonValue,
            jsonPath: String,
            typeAlias: TypeAlias
        ): Boolean {
            val type = jsonValue.get("type")
            if (type == null) {
                "[$jsonPath] 中没有找到type字段".pLog()
                return true
            }
            if (!type.isString) {
                "[$jsonPath] 中type字段不是字符串".pLog()
                return true
            }
            val typeName = type.toString()
            val classType = ClassMap.getClass(typeName)
            if (classType == null) {
                "[$jsonPath] 中type字段$typeName 不存在".pLog()
                return true
            }
            val content = ParserMap[classType]
            if (content == null) {
                "[$jsonPath] $typeName 不存在解析器".pLog()
                return true
            }
            return false
        }
    }

    init {
        register(this)
    }

    /**
     * 对jsonValue进行解析，将解析结果赋值给obj,过程中会对jsonValue进行修改, 如果jsonValue原始数据需要保留, 则在解析前备份jsonValue
     *
     * availableNamespace: 请在解析前将所有相关内容注册，并将可用前缀添加到availableNamespace中，解析时会根据前缀+内容名查找对应的内容
     *
     * 1.id--json不应该修改这个属性
     *
     * 2.name属性请提前解析, 这里不解析
     *
     * 3.一般只对对象类型的属性进行解析
     */
    abstract fun parse(obj: T, jsonValue: JsonValue, jsonPath: String, availableNamespace: Array<String> = emptyArray())

    /**
     * 从字符串解析，重载方法
     * @throws NullPointerException 当json字符串解析失败时
     */
    fun parse(obj: T, str: String, availableNamespace: Array<String> = emptyArray()) {
        val jsonValue = str.toJsonValue()
            ?: throw NullPointerException("JSON解析失败: $str")
        parse(obj, jsonValue, "", availableNamespace)
    }

    /**
     * 从字符串解析，安全版本
     * @return 是否解析成功
     */
    fun parseOrNull(obj: T, str: String, availableNamespace: Array<String> = emptyArray()): Boolean {
        val jsonValue = str.toJsonValue() ?: return false
        parse(obj, jsonValue, "", availableNamespace)
        return true
    }

    abstract class ContentParser<T : Content>(typeClass: Class<T>) : Parser<T>(typeClass) {
        override fun parse(obj: T, jsonValue: JsonValue, jsonPath: String, availableNamespace: Array<String>) {
            if (jsonValue.isNull) return
            jsonValue.remove("id")
            jsonValue.remove("type")
        }
    }

    abstract class MappableContentParser<T : MappableContent>(typeClass: Class<T>) : ContentParser<T>(typeClass) {
        override fun parse(obj: T, jsonValue: JsonValue, jsonPath: String, availableNamespace: Array<String>) {
            super.parse(obj, jsonValue, jsonPath, availableNamespace)
            if (jsonValue.isNull) return
            jsonValue.remove("name")
        }
    }

    abstract class UnlockableContentParser<T : UnlockableContent>(typeClass: Class<T>) : MappableContentParser<T>(typeClass) {
//        val fields = mapOf("shownPlanets" to )

        override fun parse(obj: T, jsonValue: JsonValue, jsonPath: String, availableNamespace: Array<String>) {
            super.parse(obj, jsonValue, jsonPath, availableNamespace)
            if (jsonValue.isNull) return
            val shownPlanets = jsonValue.remove("shownPlanets") ?: return
            if (shownPlanets.isNull) return
            when {
                shownPlanets.isString -> obj.shownPlanets.add(Vars.content.planet(shownPlanets.asString()))
                shownPlanets.isArray -> {
                    for (planet in shownPlanets) {
                        if (planet.isNull) continue
                        when {
                            planet.isString -> obj.shownPlanets.add(Vars.content.planet(planet.asString()))
                            planet.isLong -> obj.shownPlanets.add(
                                Vars.content.getByID(
                                    ContentType.planet,
                                    planet.asLong().toInt()
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    class ItemParser<T : Item>(typeClass: Class<T>) : UnlockableContentParser<T>(typeClass) {
        override fun parse(obj: T, jsonValue: JsonValue, jsonPath: String, availableNamespace: Array<String>) {
            super.parse(obj, jsonValue, jsonPath, availableNamespace)
            if (jsonValue.isNull) return
            //无需特殊处理的
        }
    }

    class MultiItemParser<T : MultiItem>(typeClass: Class<T>) : Parser<T>(typeClass) {
        override fun parse(obj: T, jsonValue: JsonValue, jsonPath: String, availableNamespace: Array<String>) {
            if (jsonValue.isNull) return
            jsonValue.remove("type")
            val items = jsonValue.get("items") ?: return
            when {
                items.isNull || items.size <= 0 -> {
                    "[$jsonPath] 中items字段为空".pLog()
                }

                items.isArray -> {
                    val itemArray = Array<Item?>(items.size) { null }

                    items.forEachIndexed { index, it ->
                        if (!it.isObject || it.size <= 0) return@forEachIndexed
                        val name = it.remove("name")

                        if (checkFieldIsNull(name, jsonPath, index)) return
                        val colorField = it.remove("color")
                        var color = Color(Color.black)
                        if (!checkFieldIsNull(colorField, jsonPath, index)) color = Color.valueOf(colorField.asString())
                        val item = Item(name.asString(), color)
                        itemArray[index] = item
                        if (it.isNull || it.size <= 0) return@forEachIndexed
                        json.readFields(item, it)
                    }

                    obj.items = itemArray
                }

                items.isObject -> {
                    val itemArray = Array<Item?>(items.size) { null }

                    items.forEachIndexed { index, it ->
                        if (!it.isObject || it.size <= 0) return@forEachIndexed
                        val name = it.name ?: return@forEachIndexed
                        val colorField = it.remove("color")
                        var color = Color(Color.black)
                        if (!checkFieldIsNull(colorField, jsonPath, index)) color = Color.valueOf(colorField.asString())
                        val item = Item(name, color)
                        itemArray[index] = item
                        if (it.isNull || it.size <= 0) return@forEachIndexed
                        json.readFields(item, it)
                    }

                    obj.items = itemArray
                }
            }
        }

        fun checkFieldIsNull(name: JsonValue?, jsonPath: String, index: Int): Boolean {
            if (name == null || name.isNull) {
                "[$jsonPath] 中items字段 第${index}个元素 中没有name字段或者name字段为空".pLog()
                return true
            }
            return false
        }


    }
}