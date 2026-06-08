package entropy

import entropy.Entropy.Companion.log
import entropy.Entropy.Companion.modJsonFi
import entropy.world.PowerProjector
import entropy.world.PowerProjectorNode
import entropy.world.PowerProjectorT
import mindustry.ai.UnitCommand
import mindustry.content.Items
import mindustry.input.Binding
import mindustry.type.Category
import mindustry.type.ItemStack

object Test {
    fun loadTestContent() {
        "Loading some entropy test content.".log()

        UnitCommand.assistCommand = UnitCommand("assist", "players", Binding.unitCommandAssist){BuilderAIn(true)}
        modJsonFi.readString().log()
        PowerProjector("电力护盾器").apply {
            requirements(Category.defense,ItemStack.with(Items.copper, 10000))
            consumePower(10f)
            itemConsumer = consumeItem(Items.phaseFabric,2).boost()
            size = 6
            phaseRadiusBoost = 80f
            radius = 240f

        }
        PowerProjectorT("电力护盾器2").apply {
            requirements(Category.defense,ItemStack.with(Items.copper, 10000))
            consumePower(10f)
            itemConsumer = consumeItem(Items.phaseFabric,2).boost()
            size = 6
            phaseRadiusBoost = 80f
            radius = 240f

        }
        PowerProjectorNode("电力护盾节点").apply {
            requirements(Category.defense,ItemStack.with(Items.copper, 1000))
            radiusScl = 0.6f
//            consumePower(10f)
        }

//        val modjson = json.readField()

    }
}
