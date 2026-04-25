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
import entropy.EntropyContentType as ECT



class Entropy : Mod() {
    val mod: Mods.LoadedMod by lazy {Vars.mods.getMod(Entropy::class.java)}
    val contentRoot: Fi by lazy {mod.root.child("content") }
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

//        Vars.mods.addParseListener { type, values, any ->
//            Log.info("11111111111111111111111")
//            type.log()
//            values.log()
//            if (type == Reaction::class.java) {
//                Log.info("Found reaction: $any")//
//            }
//        }

        //listen for game load event
//        Events.on<ClientLoadEvent?>(ClientLoadEvent::class.java, Cons { e: ClientLoadEvent? ->
//            //show dialog upon startup
//            Time.runTask(10f, Runnable {
//                val dialog = BaseDialog("frog")
//                dialog.cont.add("behold").row()
//                //mod sprites are prefixed with the mod name (this mod is called 'entropy-java-mod' in its config)
//                dialog.cont.image(Core.atlas.find("entropy-java-mod-frog")).pad(20f).row()
//                dialog.cont.button("I see", Runnable { dialog.hide() }).size(100f, 50f)
//                dialog.show()
//            })
//        })
    }

    override fun loadContent() {
        "Loading some entropy content.".log()
        loadCustomJsonContent()
        
        UnitCommand.rebuildCommand = new UnitCommand("rebuild", "hammer", Binding.unitCommandRebuild, u -> new BuilderAI());
        
    }

    fun loadCustomJsonContent() {
        "Loading custom entropy json content.".log()
//        val parser = ContentParser()
        val runs: Seq<LoadRun> = Seq<LoadRun>()

        for (type in ECT.all) {
            val lower = type.name.lowercase()
            val folder = contentRoot.child(lower + (if (lower.endsWith("s")) "" else "s"))
            if (folder.exists()) {
                for (file in folder.findAll(Boolf { f: Fi? -> f!!.extension() == "json" || f.extension() == "hjson" })) {
                    file.name().log()
                    runs.add(LoadRun(type, file, mod))
                }
            }
        }
        runs.sort()

        for (l in runs) {
            val current = Vars.content.lastAdded
            l.log()
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
