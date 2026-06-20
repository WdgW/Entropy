package entropy

import entropy.Entropy.Companion.modJsonFi
import entropy.world.PowerProjector
import entropy.world.PowerProjectorNode
import mindustry.Vars.tilesize
import mindustry.content.Items
import mindustry.type.Category
import mindustry.type.ItemStack
import mindustry.world.consumers.ConsumeItems

object Test {
    fun loadTestContent() {
        "Loading some entropy test content.".log()

//        UnitCommand.assistCommand = UnitCommand("assist", "players", Binding.unitCommandAssist){BuilderAIn(true)}\

        modJsonFi.readString().log()
        PowerProjector("电网力墙").apply {
            requirements(
                Category.effect,
                ItemStack.with(
                    Items.lead,
                    2000,
                    Items.titanium,
                    1750,
                    Items.silicon,
                    1250,
                    Items.phaseFabric,
                    1000,
                    Items.plastanium,
                    1000
                )
            )
//            itemConsumer = consumeItem(Items.phaseFabric,2).boost()
            itemConsumer = ConsumeItems(ItemStack.with(Items.phaseFabric, 1, Items.surgeAlloy, 1))
            size = 6
            phaseRadiusBoost = 80f
            radius = 240f
            phaseUseTime = 60f * 2f
            perNodeConsumption = 0.33f

            consumesPower = true
//            outputsPower = true
            consumePower(10f)
            baseExplosiveness = 25f

        }
        PowerProjectorNode("电力护盾节点").apply {
            requirements(
                Category.effect,
                ItemStack.with(Items.copper, 1000, Items.beryllium, 500, Items.phaseFabric, 300, Items.lead, 1000)
            )
            radiusScl = 0.6f
            laserRange = 240 * radiusScl * 0.9f / tilesize
            maxNodes = 10
            consumesPower = true
            outputsPower = true
            consumePowerBuffered(6000f)
            baseExplosiveness = 2f

//            consumePower(10f)
        }

//        val modjson = json.readField()

    }
}
