package entropy.mod

import arc.util.serialization.Json
import arc.util.serialization.JsonReader
import arc.util.serialization.JsonValue
import arc.util.serialization.Jval
import entropy.mod.ParserMap.register

abstract class Parser<T>(val typeClass:Class<T>) {
    companion object {
        val json = Json()
        var jsonReader: JsonReader = JsonReader()
        fun String.toJsonValue(): JsonValue = jsonReader.parse(Jval.read(this).toString())

    }
    init {
        register(typeClass, this)
    }
    abstract fun parse(content: String): T
}