package entropy.mod

import arc.util.serialization.Json
import arc.util.serialization.JsonReader
import arc.util.serialization.JsonValue
import arc.util.serialization.Jval
import entropy.Entropy.Companion.log
import entropy.mod.ParserMap.register
import mindustry.Vars
import mindustry.content.Planets
import mindustry.ctype.Content
import mindustry.ctype.ContentType
import mindustry.ctype.MappableContent
import mindustry.ctype.UnlockableContent
import java.lang.reflect.Field

abstract class Parser<T>(val typeClass:Class<T>) {
    companion object {
        val json = Json()
        var jsonReader: JsonReader = JsonReader()
        fun String.toJsonValue(): JsonValue? {
            try {
                return jsonReader.parse(Jval.read(this).toString(Jval.Jformat.plain))
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
        fun String.toHashMap(): HashMap<*, *>? {
            return json.readValue(HashMap::class.java,toJsonValue())
        }
        fun String.parseLog() = " [Parser] $this".log()
    }
    init {
        register(typeClass, this)
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
    abstract fun parse(obj: T, jsonValue: JsonValue,availableNamespace: Array<String>)

    fun parse(obj: T,str: String,availableNamespace: Array<String>) = parse(obj, str.toJsonValue()!!,availableNamespace)

    abstract class ContentParser<T: Content>(typeClass:Class<T>) : Parser<T>(typeClass){
        override fun parse(obj: T, jsonValue: JsonValue,availableNamespace: Array<String>) {
            if (jsonValue.isNull) return
            jsonValue.remove("id")
        }
    }

    abstract class MappableContentParser<T: MappableContent>(typeClass:Class<T>) : ContentParser<T>(typeClass){
        override fun parse(obj: T, jsonValue: JsonValue,availableNamespace: Array<String>) {
            super.parse(obj, jsonValue,availableNamespace)
            if (jsonValue.isNull) return
            jsonValue.remove("name")
        }
    }

    abstract class UnlockableContentParser<T: UnlockableContent>(typeClass:Class<T>) : MappableContentParser<T>(typeClass){
//        val fields = mapOf("shownPlanets" to )

        override fun parse(obj: T, jsonValue: JsonValue,availableNamespace: Array<String>) {
            super.parse(obj, jsonValue,availableNamespace)
            if (jsonValue.isNull) return
            val shownPlanets = jsonValue.remove("shownPlanets") ?: return
            if (shownPlanets.isNull) return
            when {
                shownPlanets.isString -> obj.shownPlanets.add(Vars.content.planet(shownPlanets.asString()))
                shownPlanets.isArray -> {
                    for (planet in shownPlanets){
                        if (planet.isNull) continue
                        when {
                            planet.isString -> obj.shownPlanets.add(Vars.content.planet(planet.asString()))
                            planet.isLong -> obj.shownPlanets.add(Vars.content.getByID(ContentType.planet,planet.asLong().toInt()))
                        }
                    }
                }
            }

        }
    }
}