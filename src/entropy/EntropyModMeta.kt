package entropy

import arc.files.Fi
import arc.struct.Seq
import arc.util.Log
import arc.util.serialization.Json
import arc.util.serialization.JsonReader
import arc.util.serialization.JsonValue

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
        private val reader = JsonReader()
        private val json = Json()

        fun read(file: Fi): EntropyModMeta? {
            if (!file.exists()) {
                Log.warn("[Entropy] Mod meta file not found: ${file.path()}")
                return null
            }

            return try {
                val content = file.readString("UTF-8")
                parseContent(content, file.extension())
            } catch (e: Exception) {
                Log.err("[Entropy] Failed to read mod meta: ${file.path()}")
                e.printStackTrace()
                null
            }
        }

        private fun parseContent(content: String, extension: String): EntropyModMeta? {
            val root: JsonValue? = when (extension) {
                "json", "hjson" -> {
                    val cleaned = content.lines()
                        .filter { !it.trimStart().startsWith("//") && !it.trimStart().startsWith("#") }
                        .joinToString("\n")
                    reader.parse(cleaned)
                }
                else -> {
                    Log.warn("[Entropy] Unsupported file extension: $extension")
                    return null
                }
            }

            return root?.let { fromJsonValue(it) }
        }

        private fun fromJsonValue(json: JsonValue): EntropyModMeta {
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
                
                dependencies = Seq<String>().apply {
                    val deps = json.get("dependencies")
                    deps?.let {
                        for (child in it) {
                            add(child.asString())
                        }
                    }
                }
                
                softDependencies = Seq<String>().apply {
                    val softDeps = json.get("softDependencies")
                    softDeps?.let {
                        for (child in it) {
                            add(child.asString())
                        }
                    }
                }
                
                contentOrder = json.get("contentOrder")?.let { arr ->
                    arr.map { it.asString() }.toTypedArray()
                } ?: emptyArray()
                
                contents = json.get("contents")?.let { arr ->
                    arr.map { it.asString() }.toTypedArray()
                } ?: emptyArray()
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
