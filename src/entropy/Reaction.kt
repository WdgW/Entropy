package entropy

import arc.graphics.Color
import mindustry.content.Fx
import mindustry.content.StatusEffects
import mindustry.entities.Effect
import mindustry.type.StatusEffect

class Reaction(val name: String, val reactants: Array<StatusEffect>): EntropyContent{
    companion object {
        val all = mutableListOf<Reaction>()
    }
    init {
        all.add(this)
    }

    /**
     * 基础伤害不应该放在这
     */
//    var damage: Int = 0

    /**
     * 反应伤害倍率
     */
    var damageMultiplier: Float = 1.0f

    //反应后对单位的倍率
    var speedMultiplier: Float = 1.0f
    var reloadMultiplier: Float = 1.0f
    var healthMultiplier: Float = 1.0f

    /**
     * 反应ui颜色
     */
    var uiColor: Color = Color.black

    /**
     * 反应特效
     */
    var effect: Effect = Fx.none


    /**
     * 催化剂
     */
    var catalyst: StatusEffect = StatusEffects.none

    /**
     * 生成效果
     */
    var product: StatusEffect = StatusEffects.none

    /**
     * 是否消耗反应物
     */
    var consumeReactants: Boolean = true
    var cooldownTick: Int = 60

    override fun getContentType(): EntropyContentType {
        return EntropyContentType.reaction
    }
/*
name: "反应显示名称",
description: "反应，记录参与反应的状态",
type: "reaction",//可不写
// 原版通用字段（部分示例）
damage: 0,
damageMultiplier: 1.0,
speedMultiplier: 1.0,
reloadMultiplier: 1.0,
healthMultiplier: 1.0,
color: "ffffff",
effect: "none",
applyEffect: "none",
permanent: false,
// ========== 自定义反应系统 ==========
comment: "另一个反应：电击 + 水（催化剂）→ 麻痹，范围电击特效",
reactants: ["shock"],
catalyst: "water",
product: "paralyze",
consumeReactants: false,
cooldown: 30,
effects: {
  damage: {
    amount: 10,
    radius: 40,
    lightning: true,   // 是否生成闪电
    lightningCount: 3
  },
  effect: {
    type: "lightning",
    color: "88aaff",
    size: 20
  }
}
*/
}
