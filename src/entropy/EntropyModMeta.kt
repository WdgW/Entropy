package entropy

import arc.files.Fi
import arc.struct.Seq
import arc.util.Log
import arc.util.io.PropertiesUtils
import arc.util.serialization.JSONValue
import arc.util.serialization.Jread

class EntropyModMeta {
    var name: String = ""
    var displayName: String = ""
    var author: String = ""
    var description: String = ""
    var version: String = "0.0.0"
    var minGameVersion: String = ""
    var subtitle: String = ""
    var main: String = ""
    var java: Boolean = false
    var hidden: Boolean = false
    var pregenerated: Boolean = false
    var legacyCompatible: Boolean = false
    var iosCompatible: Boolean = false
    var texturescale: Float = 1f
    var repo: String = ""
    var dependencies: Seq<String> = Seq()
    var softDependencies: Seq<String> = Seq()
    var contentOrder: Array<String> = emptyArray()
    var contents: Array<String> = emptyArray()

    companion object {
        fun read(file: Fi): EntropyModMeta? {
            if (!file.exists()) {
                Log.warnTag("Entropy", "Mod meta file not found: ${file.path()}")
                return null
            }

            return try {
                val json = parseFile(file)
                json?.let { fromJSON(it) }
            } catch (e: Exception) {
                Log.errTag("Entropy", "Failed to read mod meta: ${file.path()}", e)
                null
            }
        }

        private fun parseFile(file: Fi): JSONValue? {
            return when (file.extension()) {
                "json" -> Jread.json(file.readString("UTF-8"))
                "hjson" -> Jread.hjson(file.readString("UTF-8"))
                "properties" -> {
                    val props = PropertiesUtils.load(file.readString("UTF-8"))
                    val map = mutableMapOf<String, Any>()
                    props.forEach { k, v -> map[k.toString()] = v }
                    JSONValue(map)
                }
                else -> null
            }
        }

        private fun fromJSON(json: JSONValue): EntropyModMeta {
            return EntropyModMeta().apply {
                name = json.getString("name", "")
                displayName = json.getString("displayName", "")
                author = json.getString("author", "")
                description = json.getString("description", "")
                version = json.getString("version", "0.0.0")
                minGameVersion = json.getString("minGameVersion", "")
                subtitle = json.getString("subtitle", "")
                main = json.getString("main", "")
                java = json.getBoolean("java", false)
                hidden = json.getBoolean("hidden", false)
                pregenerated = json.getBoolean("pregenerated", false)
                legacyCompatible = json.getBoolean("legacyCompatible", false)
                iosCompatible = json.getBoolean("iosCompatible", false)
                texturescale = json.getFloat("texturescale", 1f)
                repo = json.getString("repo", "")
                dependencies = Seq.with(json.getArray("dependencies") { it.asString() })
                softDependencies = Seq.with(json.getArray("softDependencies") { it.asString() })
                contentOrder = json.getArray("contentOrder") { it.asString() }.toTypedArray()
                contents = json.getArray("contents") { it.asString() }.toTypedArray()
            }
        }
    }

    override fun toString(): String {
        return "EntropyModMeta(\n" +
                "  name=$name,\n" +
                "  displayName=$displayName,\n" +
                "  author=$author,\n" +
                "  description=$description,\n" +
                "  version=$version,\n" +
                "  minGameVersion=$minGameVersion,\n" +
                "  subtitle=$subtitle,\n" +
                "  main=$main,\n" +
                "  java=$java,\n" +
                "  hidden=$hidden,\n" +
                "  pregenerated=$pregenerated,\n" +
                "  legacyCompatible=$legacyCompatible,\n" +
                "  iosCompatible=$iosCompatible,\n" +
                "  texturescale=$texturescale,\n" +
                "  repo=$repo,\n" +
                "  dependencies=$dependencies,\n" +
                "  softDependencies=$softDependencies,\n" +
                "  contentOrder=${contentOrder.contentToString()},\n" +
                "  contents=${contents.contentToString()}\n" +
                ")"
    }
}
