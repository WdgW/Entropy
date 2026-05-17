package entropy

import arc.files.Fi
import arc.struct.Seq
import arc.util.Log
import arc.util.serialization.JsonReader
import arc.util.serialization.JsonValue
import mindustry.mod.Mods

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

        fun readFromLoadedMod(loadedMod: Mods.LoadedMod): EntropyModMeta {
            val meta = loadedMod.meta
            return EntropyModMeta().apply {
                name = meta.name ?: ""
                displayName = meta.displayName ?: ""
                author = meta.author ?: ""
                description = meta.description ?: ""
                version = meta.version ?: "0.0.0"
                minGameVersion = meta.minGameVersion ?: ""
                subtitle = meta.subtitle ?: ""
                main = meta.main ?: ""
                java = meta.java
                hidden = meta.hidden
                pregenerated = meta.pregenerated
                legacyCompatible = meta.legacyCompatible
                iosCompatible = meta.iosCompatible
                texturescale = meta.texturescale
                repo = meta.repo ?: ""
                
                // 尝试从 mod.hjson 中读取自定义的 contents 字段
                try {
                    val hjsonFile = loadedMod.root.child("mod.hjson")
                    if (hjsonFile.exists()) {
                        val content = hjsonFile.readString("UTF-8")
                        Log.info("[Entropy] Reading mod.hjson for custom fields: ${hjsonFile.absolutePath()}")
                        
                        // 简单解析查找 contents 字段
                        parseCustomContents(content, this)
                    }
                } catch (e: Exception) {
                    Log.warn("[Entropy] Failed to read custom contents field: ${e.message}")
                }
            }
        }
        
        private fun parseCustomContents(content: String, result: EntropyModMeta) {
            try {
                // 对于 HJSON，先做基本清理
                val cleaned = content.lines()
                    .filter { line ->
                        val trimmed = line.trimStart()
                        !(trimmed.startsWith("//") || trimmed.startsWith("#"))
                    }
                    .joinToString("\n")
                
                val root = reader.parse(cleaned)
                
                // 读取 contents 字段
                root.get("contents")?.let { contentsNode ->
                    val list = mutableListOf<String>()
                    for (child in contentsNode) {
                        list.add(child.asString())
                    }
                    result.contents = list.toTypedArray()
                    Log.info("[Entropy] Found custom contents: ${list.joinToString()}")
                }
            } catch (e: Exception) {
                Log.warn("[Entropy] Failed to parse custom contents: ${e.message}")
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
