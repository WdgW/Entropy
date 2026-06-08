package entropy

import arc.files.Fi
import arc.struct.Seq
import arc.util.Log
import arc.util.serialization.Json
import arc.util.serialization.Jval
import entropy.mod.ClassMap
import entropy.mod.EntropyModMeta
import entropy.mod.Parser
import entropy.mod.Parser.Companion.parseLog
import entropy.mod.Parser.Companion.toHashMap
import entropy.mod.Parser.Companion.toJsonValue
import entropy.mod.ParserMap
import entropy.mod.TypeAlias
import mindustry.Vars
import mindustry.ai.UnitCommand
import mindustry.content.Items
import mindustry.input.Binding
import mindustry.mod.Mod
import mindustry.mod.Mods.LoadedMod
import mindustry.type.Category
import mindustry.type.ItemStack
import entropy.EntropyContentType as ECT

class Entropy : Mod() {
    companion object{
        val mod: LoadedMod by lazy {Vars.mods.getMod(Entropy::class.java)}
        val contentRoot: Fi by lazy {mod.root.child("content") }
        val modJsonFi: Fi by lazy {if (mod.root.child("mod.json").exists()) mod.root.child("mod.json")else mod.root.child("mod.hjson") }
        var entropyModMeta: EntropyModMeta? = null
        val json : Json = Json()

        fun loadModMeta(jsonString: String) {
            try {

                val root = json.readValue(EntropyModMeta::class.java, null,modJsonFi.readString().toJsonValue())

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
        fun <T>  T.log(){
            Log.infoTag("Entropy", this.toString())
        }

        inline infix fun <T> Boolean.ifTrue(condition:()->T): T? = if (this) condition() else null
        infix fun <T> Boolean.ifTrue(condition:T): T? = if (this) condition else null
        inline infix fun <T> Boolean.ifFalse(condition:()->T): T? = if (!this) condition() else null
        infix fun <T> Boolean.ifFalse(condition:T): T? = if (!this) condition else null
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
            val testContentFi = contentRoot.child("test").child("content")
            if (!testContentFi.exists()) return@ifTrue

            val file = testContentFi.child("typealias.json")
            if (!file.exists()) return@ifTrue
            val typeAlias = TypeAlias(file)
            typeAlias.log()

            val jsons = testContentFi.findAll { f: Fi -> f.extension() == "json" || f.extension() == "hjson" && f.name() != "typealias.json" }
            for (json in jsons){
                json.name().log()
//                json.readString().log()
                val jsonValue = json.readString().toJsonValue()
                if (jsonValue == null) {
                    "[${json.path()}] 中json解析失败".parseLog()
                    continue
                }
                val type = jsonValue["type"]
                if (type == null) {
                    "[${json.path()}] 中没有找到type字段".parseLog()
                    continue
                }
                if (!type.isString) {
                    "[${json.path()}] 中type字段不是字符串".parseLog()
                    continue
                }
                val typeName = type.toString()
                val classType = typeAlias.get(typeName, ClassMap.getClass(typeName))
                if (classType == null) {
                    "[${json.path()}] 中type字段$typeName 不存在".parseLog()
                    continue
                }
                val content = ParserMap.get(classType)
                if (content == null) {
                    "[${json.path()}] $typeName 不存在解析器".parseLog()
                    continue
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
            when (l.type){
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

