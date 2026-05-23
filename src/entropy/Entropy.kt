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
import arc.util.Strings
import java.util.Locale

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
        
        UnitCommand.assistCommand = UnitCommand("assist", "players", Binding.unitCommandAssist){BuilderAIn(true)}  
        
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
    class ModMeta {
    /** Name as defined in mod.json. Stripped of colors, but may contain spaces. */
    var name: String = ""

    /** Name without spaces in all lower case. */
    var internalName: String = ""

    /** Minimum game version that this mod requires, e.g. "140.1" */
    var minGameVersion: String = "0"

    var displayName: String = ""
    var author: String = ""
    var description: String = ""
    var subtitle: String = ""
    var version: String = ""
    var main: String = ""
    var repo: String = ""

    var dependencies: Seq<String> = Seq.with()
    var softDependencies: Seq<String> = Seq.with()

    /** Hidden mods are only server-side or client-side, and do not support adding new content. */
    var hidden: Boolean = false

    /** If true, this mod should be loaded as a Java class mod. */
    var java: Boolean = false

    /** If true, this script mod is compatible with iOS. */
    var iosCompatible: Boolean = false

    /** To rescale textures with a different size. */
    var texturescale: Float = 1.0f

    /** If true, bleeding is skipped and no content icons are generated. */
    var pregenerated: Boolean = false

    /** If set, load the mod content in this order by content names. */
    var contentOrder: Array<String> = emptyArray()

    /** Mod from an older major version that is compatible with the latest one as well. */
    var legacyCompatible: Boolean = false

    companion object {
        private val blacklistedMods: ObjectSet<String> = ObjectSet.with(
            "ui-lib", "braindustry", "schema",
            "scheme-size:1.0.5", "scheme-size:1.0.4", "scheme-size:1.0.3",
            "scheme-size:1.0.1", "scheme-size:1.0.0", "scheme-size:1.1.0",
            "scheme-size:1.0.4.1"
        )
        const val maxModSubtitleLength: Int = 40
    }
    
    fun isBlacklisted(): Boolean {
        return blacklistedMods.contains(name) || blacklistedMods.contains("$name:$version")
    }

    fun shortDescription(): String {
        val desc = if (subtitle.isNotEmpty()) subtitle
                   else if (description.isNotEmpty() && description.length <= maxModSubtitleLength) description
                   else ""
        return Strings.truncate(desc, maxModSubtitleLength, "...")
    }

    fun cleanup() {
        name = Strings.stripColors(name)
        displayName = Strings.stripColors(displayName)
        if (displayName.isEmpty()) displayName = name
        if (version.isEmpty()) version = "0"
        author = Strings.stripColors(author)
        description = Strings.stripColors(description)
        subtitle = Strings.stripColors(subtitle).replace("\n", "")
        internalName = name.lowercase(Locale.ROOT).replace(" ", "-")
    }

    fun getMinMajor(): Int {
        val ver = minGameVersion.ifEmpty { "0" }
        val dot = ver.indexOf(".")
        return if (dot != -1) Strings.parseInt(ver.substring(0, dot), 0)
               else Strings.parseInt(ver, 0)
    }

    override fun toString(): String {
        return "ModMeta{" +
                "name='$name'" +
                ", minGameVersion='$minGameVersion'" +
                ", displayName='$displayName'" +
                ", author='$author'" +
                ", description='$description'" +
                ", subtitle='$subtitle'" +
                ", version='$version'" +
                ", main='$main'" +
                ", repo='$repo'" +
                ", dependencies=$dependencies" +
                ", softDependencies=$softDependencies" +
                ", hidden=$hidden" +
                ", java=$java" +
                ", texturescale=$texturescale" +
                ", pregenerated=$pregenerated" +
                '}'
    }
}
}
