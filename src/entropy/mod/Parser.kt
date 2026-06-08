package entropy.mod

import arc.util.serialization.Json
import arc.util.serialization.JsonReader
import arc.util.serialization.JsonValue
import arc.util.serialization.Jval
import entropy.Entropy.Companion.log
import entropy.mod.ParserMap.register

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
    abstract fun parse(content: String): T
}