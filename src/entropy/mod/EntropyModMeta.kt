package entropy.mod

import arc.files.Fi
import entropy.Entropy
import mindustry.mod.Mods

class EntropyModMeta: Mods.ModMeta() {
    /**
     * 内容文件夹
     */
    var content: Array<String>? = null
    /**
     * 处理后的内容文件夹
     */
    var contentDir: Array<Fi>? = null

    /**
     * 是否加载测试内容
     */
    var test = false

    override fun cleanup() {
        super.cleanup()
        content?.let { content ->
            contentDir = Array<Fi>(
                size = content.size,
                init = { Entropy.Companion.contentRoot.child(content[it]) }
            )
        }
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
                ", isTest=$test" +
                ", content=$content" +
                ", contentDir=$contentDir" +
                '}'
    }

}