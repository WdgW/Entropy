package entropy.type.item

import arc.graphics.Color
import mindustry.type.Item
import mindustry.type.Liquid

/**
 * 创建一个物品和对应的液体和气体
 * @param name 物品名称
 * @param itemColor 物品颜色
 * @param liquidColor 液体颜色
 * @param gasColor 气体颜色
 * @return 物品、液体和气体的三元组
 */
fun createItemWithFluid(
    name: String,
    itemColor: Color = Color.white,
    liquidColor: Color = Color.white,
    gasColor: Color = Color.white,
): Triple<Item, Liquid, Liquid> {
    val item = Item(name, itemColor)
    val liquid = Liquid("$name-liquid", liquidColor)
    val gas = Liquid("$name-gas", gasColor).apply { gas = true }
    return Triple(item, liquid, gas)
}

fun createItemWithFluid(name: String) = createItemWithFluid(name, Color.white, Color.white, Color.white)
fun createItemWithFluid(name: String, color: Color) = createItemWithFluid(name, color, color, color)
