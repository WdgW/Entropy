package entropy

import mindustry.world.blocks.power.PowerNode

class PowerProjectorNode(name: String) : PowerNode(name), EntropyBlock {
    var radiusScl = 96f

    inner class PowerProjectorNodeBuild : PowerNodeBuild(), EntropyBuilding {
        val radiusScl
            get() = this@PowerProjectorNode.radiusScl

    }
}