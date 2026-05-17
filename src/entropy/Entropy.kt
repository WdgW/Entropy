package entropy

import arc.files.Fi
import arc.func.Boolf
import arc.struct.Seq
import arc.util.Log
import mindustry.Vars
import mindustry.mod.Mod
import mindustry.mod.Mods
import mindustry.mod.Mods.LoadedMod
import mindustry.input.Binding
import mindustry.ai.UnitCommand
import mindustry.ai.types.BuilderAI
import mindustry.content.UnitTypes
import entropy.EntropyContentType as ECT
import entropy.BuilderAIn


class Entropy : Mod() {
    private var mod: Mods.LoadedMod? = null
    private var contentRoot: Fi? = null
    private var modMeta: EntropyModMeta? = null
    
    // val configs
    // var isLoadExamples: Boolean = contentRoot.exists(false) {
    //     // TODO
    //     false
    // }
    
    // fun <T> Fi.existsOrDefault(default: T, func: () -> T): T {
    // return if (exists()) func() else default
// }

    override fun loadContent() {
        "-----------------------------------".log()
        
        // 此时模块已经完全加载，可以安全访问了
        mod = Vars.mods.getMod(Entropy::class.java)
        mod?.let { loadedMod ->
            contentRoot = loadedMod.root.child("content")
            
            // 使用新方法，从已加载的 mod 中读取，再额外解析自定义字段
            modMeta = EntropyModMeta.readFromLoadedMod(loadedMod)
            
            modMeta.log()
            
            // 获取自定义contents字段
            modMeta?.contents?.forEach { contentType ->
                "Loading content type: $contentType".log()
            }
        }
        
        "Loading some entropy content.".log()
        loadCustomJsonContent()
        
        UnitCommand.assistCommand = UnitCommand("assist", "players", Binding.unitCommandAssist){BuilderAIn(true)}  
        
    }

    fun loadCustomJsonContent() {
        "Loading custom entropy json content.".log()
        
        val loadedMod = mod ?: run {
            "Mod not loaded yet, skipping.".log()
            return
        }
        
        val root = contentRoot ?: run {
            "Content root not found, skipping.".log()
            return
        }
        
        val runs: Seq<LoadRun> = Seq<LoadRun>()
        
        val allowedContents = modMeta?.contents?.toSet() ?: ECT.all.map { it.name.lowercase() }.toSet()

        for (type in ECT.all) {
            val lower = type.name.lowercase()
            if (lower !in allowedContents) {
                "Skipping content type: $lower (not in contents list)".log()
                continue
            }
            val folder = root.child(lower + (if (lower.endsWith("s")) "" else "s"))
            if (folder.exists()) {
                for (file in folder.findAll(Boolf { f: Fi? -> f!!.extension() == "json" || f.extension() == "hjson" })) {
                    file.name().log()
                    runs.add(LoadRun(type, file, loadedMod))
                }
            }
        }
        runs.sort()

        for (l in runs) {
            val current = Vars.content.lastAdded
            l.log()
            when (l.type){
                ECT.reaction -> {

                }
            }
        }
    }

    fun <T>  T.log(){
        Log.infoTag("Entropy",this.toString())
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
