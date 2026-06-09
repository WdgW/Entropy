package entropy

import arc.files.Fi
import arc.struct.Seq
import arc.util.Log
import arc.util.serialization.Json
import entropy.mod.EntropyModMeta
import entropy.mod.Parser.Companion.check
import entropy.mod.Parser.Companion.pLog
import entropy.mod.Parser.Companion.toJsonValue
import entropy.mod.TypeAlias
import mindustry.Vars
import mindustry.mod.Mod
import mindustry.mod.Mods.LoadedMod
import entropy.EntropyContentType as ECT

class Entropy : Mod() {
    companion object {
        val mod: LoadedMod by lazy { Vars.mods.getMod(Entropy::class.java) }
        val contentRoot: Fi by lazy { mod.root.child("content") }
        val modJsonFi: Fi by lazy {
            if (mod.root.child("mod.json").exists()) mod.root.child("mod.json") else mod.root.child("mod.hjson")
        }
        var entropyModMeta: EntropyModMeta? = null
        val json: Json = Json()

        fun loadModMeta(jsonString: String) {
            try {

                val root = json.readValue(EntropyModMeta::class.java, null, modJsonFi.readString().toJsonValue())

                // 读取 contents 字段
                root.content?.let { contents ->
                    "Found custom contents: $contents".log()
                }
                entropyModMeta = root
                return
            } catch (e: Exception) {
                Log.warn("[Entropy] Failed to parse custom contents: ${e.message}")
            }
            entropyModMeta = null;
        }


        inline infix fun <T> Boolean.ifTrue(condition: () -> T): T? = if (this) condition() else null
        infix fun <T> Boolean.ifTrue(condition: T): T? = if (this) condition else null
        inline infix fun <T> Boolean.ifFalse(condition: () -> T): T? = if (!this) condition() else null
        infix fun <T> Boolean.ifFalse(condition: T): T? = if (!this) condition else null
    }

    // val configs
    //var isLoadExamples: Boolean = contentRoot.exists(false) {
    //   // TODO
    //    false
    //}

    //fun <T> Fi.existsOrDefault(default: T, func: () -> T): T {
    //return if (exists()) func() else default
//}

    init {
        "-----------------------------------".log()
    }

    override fun loadContent() {
        loadModMeta(modJsonFi.readString("UTF-8"))

        Test.loadTestContent()

        if (entropyModMeta == null) return
        val modMeta = entropyModMeta!!
        modMeta.test ifTrue {
            val testFi = contentRoot.child("test")
            val testContentFi = testFi.child("content")
            if (!testContentFi.exists()) return@ifTrue

            val file = testContentFi.child("typealias.json")
            if (!file.exists()) return@ifTrue
            val typeAlias = TypeAlias(file)

            val jsons =
                testContentFi.findAll { f: Fi -> f.extension() == "json" || f.extension() == "hjson" && f.name() != "typealias.json" }
            loop@ for (json in jsons) {
//                json.readString().log()
                val jsonValue = json.readString().toJsonValue()
                if (jsonValue == null || jsonValue.isNull) {
                    "[${json.path()}] 中json解析失败".pLog()
                    continue
                }

                if (jsonValue.isObject) {
                    check(jsonValue, json.path(), typeAlias).ifTrue { continue@loop }
                } else if (jsonValue.isArray) {
                    for ((index, item) in jsonValue.withIndex()) {
                        check(item, "${json.path()}[$index]", typeAlias).ifTrue { continue@loop }
                    }
                }


            }


        }
    }

    fun loadCustomJsonContent() {
        "Loading custom entropy json content.".log()
//        val parser = ContentParser()
        val runs: Seq<LoadRun> = Seq<LoadRun>()

        for (type in ECT.all) {
            val lower = type.name.lowercase()
            val folder = contentRoot.child(lower + (if (lower.endsWith("s")) "" else "s"))
            if (folder.exists()) {
                for (file in folder.findAll { f: Fi -> f.extension() == "json" || f.extension() == "hjson" }) {
                    file.name().log()
                    runs.add(LoadRun(type, file, mod))
                }
            }
        }
        runs.sort()

        for (l in runs) {
            val current = Vars.content.lastAdded
//            l.log()
            //TODO(还没写完:~:)
            when (l.type) {
                ECT.reaction -> {

                }
            }
//            try {
//                //this binds the content but does not load it entirely
//                val loaded: Content? =
//                    parser.parse(l.mod, l.file.nameWithoutExtension(), l.file.readString("UTF-8"), l.file, l.type)
//                Log.debug(
//                    "[@] Loaded '@'.",
//                    l.mod.meta.name,
//                    (if (loaded is UnlockableContent) loaded.localizedName else loaded)
//                )
//            } catch (e: Throwable) {
//                if (current !== Vars.content.lastAdded && Vars.content.lastAdded != null) {
//                    parser.markError(Vars.content.lastAdded, l.mod, l.file, e)
//                } else {
//                    val error = ErrorContent()
//                    parser.markError(error, l.mod, l.file, e)
//                }
//            }
        }
    }


    data class LoadRun(val type: ECT, val file: Fi, val mod: LoadedMod) : Comparable<LoadRun> {
        override fun compareTo(other: LoadRun): Int {
            val mod = this.mod.name.compareTo(other.mod.name)
            if (mod != 0) return mod
            return this.file.name().compareTo(other.file.name())
        }

        override fun toString(): String {
            return "LoadRun(mod=${mod.name}," +
                    "\ntype=$type" +
                    "\nfile=${file.nameWithoutExtension()}," +
                    "\ncontent=${file.readString("UTF-8")})"
        }
    }

}



