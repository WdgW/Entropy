package entropy.world

import arc.Core
import arc.Events
import arc.audio.Sound
import arc.graphics.Blending
import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import arc.graphics.g2d.Lines
import arc.graphics.g2d.TextureRegion
import arc.math.Mathf
import arc.math.geom.Intersector
import arc.struct.EnumSet
import arc.struct.Seq
import arc.util.Time
import arc.util.io.Reads
import arc.util.io.Writes
import entropy.Entropy.Companion.ifTrue
import entropy.Entropy.Companion.log
import entropy.EntropyBlock
import entropy.EntropyBuilding
import mindustry.Vars
import mindustry.Vars.renderer
import mindustry.content.Fx
import mindustry.entities.Effect
import mindustry.game.EventType
import mindustry.game.Team
import mindustry.gen.Building
import mindustry.gen.Bullet
import mindustry.gen.Groups
import mindustry.gen.Sounds
import mindustry.graphics.Layer
import mindustry.graphics.Pal
import mindustry.logic.LAccess
import mindustry.logic.Ranged
import mindustry.ui.Bar
import mindustry.world.Block
import mindustry.world.blocks.ExplosionShield
import mindustry.world.blocks.defense.ForceProjector
import mindustry.world.blocks.power.PowerGraph
import mindustry.world.blocks.power.PowerNode.PowerNodeBuild
import mindustry.world.consumers.Consume
import mindustry.world.consumers.ConsumeCoolant
import mindustry.world.consumers.ConsumeItems
import mindustry.world.meta.BlockFlag
import mindustry.world.meta.BlockGroup
import mindustry.world.meta.Env
import mindustry.world.meta.Stat

/**
 * 将电力作为护盾
 */
@Suppress("DuplicatedCode")
open class PowerProjector(name: String): Block(name), EntropyBlock {
    val timerUse: Int = timers++
    var phaseUseTime: Float = 350f

    /**
     * 护盾半径提升
     */
    var phaseRadiusBoost: Float = 80f
//    var phaseShieldBoost: Float = 400f
    /**
     * 护盾半径
     */
    var radius: Float = 101.7f

    /**
     * 护盾边数
     */
    var sides: Int = 6

    /**
     * 护盾旋转角度
     */
    var shieldRotation: Float = 0f

    var coolantConsumption: Float = 0.1f
    var consumeCoolant: Boolean = true
    /**
     * 护盾被伤害时的倍率
     */
    var shieldDamagedMultiplier: Float = 1.1f

    /**
     * 护盾被伤害时的音效
     */
    var hitSound: Sound = Sounds.shieldHit

    /**
     * 护盾被伤害时的音效音量
     */
    var hitSoundVolume: Float = 0.12f

    /**
     * 护盾被伤害时的特效
     */
    var absorbEffect: Effect = Fx.absorb
    /**
     * 护盾被伤害时的特效音效
     */
    var shieldBreakEffect: Effect = Fx.shieldBreak

    /**
     * 护盾被摧毁是否销毁电池
     */
    var isDestroyBattery: Boolean = true

    /**
     * 护盾被摧毁是否销毁节点
     */
    var isDestroyNode: Boolean = true

    /**
     * 护盾被摧毁是否销毁投影节点
     */
    var isDestroyProjectorNode: Boolean = true

    /**
     * 护盾被摧毁是否销毁本体
     */
    var isDestroySelf: Boolean = false


    var topRegion: TextureRegion? = null

    //TODO json support
    var itemConsumer: Consume? = null
    //TODO json support
    var coolantConsumer: Consume? = null

    companion object{
        var paramBlock: PowerProjector? = null
        var paramEntity: PowerProjectorBuild? = null

        // 使用三个并行数组替代 Triple，减少对象创建
        var nodeCount: Int = 0
        var nodePosArr: IntArray? = null
        var nodeRadiusArr: FloatArray? = null
        var nodeBoundsArr: FloatArray? = null // [minX, maxX, minY, maxY] * nodeCount

        /**
         * 优化的子弹检测回调
         * 先用矩形边界粗检测过滤，再用多边形精确检测
         */
        protected val shieldConsumer = shieldConsumer@{ bullet: Bullet ->
            if (paramBlock == null || paramEntity == null) {
                return@shieldConsumer
            }
            val paramEntity = paramEntity!!
            val paramBlock = paramBlock!!

            if (bullet.team !== paramEntity.team &&
                bullet.type.absorbable &&
                !bullet.absorbed) {

                val bx = bullet.x
                val by = bullet.y
                val realRadius = paramEntity.realRadius()

                // 检测本体护盾
                if (realRadius > 0) {
                    val selfMinX = paramEntity.x - realRadius
                    val selfMaxX = paramEntity.x + realRadius
                    val selfMinY = paramEntity.y - realRadius
                    val selfMaxY = paramEntity.y + realRadius

                    // 先做矩形粗检测
                    if (bx in selfMinX..selfMaxX && by in selfMinY..selfMaxY) {
                        if (Intersector.isInRegularPolygon(
                                paramBlock.sides, paramEntity.x, paramEntity.y,
                                realRadius, paramBlock.shieldRotation, bx, by)) {
                            handleBulletAbsorb(bullet, paramEntity.power.graph)
                            return@shieldConsumer
                        }
                    }

                    // 检测所有节点护盾
                    val count = nodeCount
                    if (count <= 0) return@shieldConsumer
                    val posArr = nodePosArr!!
                    val radiusArr = nodeRadiusArr!!
                    val boundsArr = nodeBoundsArr!!

                    for (i in 0 until count) {
                        val node = Vars.world.build(posArr[i]) as? PowerProjectorNode.PowerProjectorNodeBuild ?: continue
                        val nodeRadius = radiusArr[i]

                        if (nodeRadius <= 0f) continue

                        val boundsIdx = i * 4
                        // 先做矩形粗检测，快速过滤掉不在范围内的子弹
                        if (bx !in boundsArr[boundsIdx]..boundsArr[boundsIdx + 1] ||
                            by !in boundsArr[boundsIdx + 2]..boundsArr[boundsIdx + 3]) continue

                        // 矩形内才执行昂贵的多边形检测
                        if (Intersector.isInRegularPolygon(
                                paramBlock.sides, node.x, node.y,
                                nodeRadius, paramBlock.shieldRotation, bx, by)) {
                            handleBulletAbsorb(bullet, node.power.graph)
                            return@shieldConsumer
                        }
                    }
                }
            }
        }

        /**
         * 处理子弹吸收的公共逻辑
         */
        private fun handleBulletAbsorb(bullet: Bullet, graph: PowerGraph) {
            if (paramEntity == null || paramBlock == null) {
                return
            }
            val paramEntity = paramEntity!!
            val paramBlock = paramBlock!!
            bullet.absorb()
            paramBlock.hitSound.at(bullet.x, bullet.y, 1f + Mathf.range(0.1f), paramBlock.hitSoundVolume)
            paramBlock.absorbEffect.at(bullet)
            paramEntity.hit = 1f
            graph.transferPower(-bullet.type.shieldDamage(bullet)*paramBlock.shieldDamagedMultiplier)

            if (graph.lastPowerStored <= 0f) {
                paramEntity.breakShield()
                if (paramBlock.isDestroySelf) {
                    paramEntity.kill()
                }
                paramBlock.shieldBreakEffect.at(paramEntity.x, paramEntity.y, paramEntity.realRadius(), paramEntity.team.color)
                paramBlock.breakSound.at(paramEntity.x, paramEntity.y)
            }
        }


    }


    init{
        update = true
        solid = true
        group = BlockGroup.projectors
        hasPower = true
        hasLiquids = true
        hasItems = true
        envEnabled = envEnabled or Env.space
        ambientSound = Sounds.loopShield
        ambientSoundVolume = 0.1f
        flags = EnumSet.of(BlockFlag.shield)
        breakSound = Sounds.shieldBreak

        if (consumeCoolant) {
            consume<Consume>(ConsumeCoolant(coolantConsumption).also { coolantConsumer = it }).boost().update(false)
        }
    }
//    @Load("@-top")
    override fun loadIcon() {
        super.loadIcon()
        topRegion = Core.atlas.find("$name-top")
    }

    override fun init() {
        updateClipRadius(radius + phaseRadiusBoost + 3f)
        super.init()
    }

    override fun setBars() {
        super.setBars()
        removeBar("shield")
        addBar<PowerProjectorBuild>("shield"){ build ->
            val batteryStored = build.power.graph.lastPowerStored
            val batteryCapacity = build.power.graph.lastCapacity
            Bar({ Core.bundle.format("stat.shieldhealth", batteryStored,"/", batteryCapacity) },
                { Pal.accent }
            ){ (!build.enabled ifTrue 0f) ?: (batteryStored / batteryCapacity) }
        }
    }
    override fun outputsItems(): Boolean {
        return false
    }

    override fun setStats() {
        val consItems: Boolean = itemConsumer != null

        if (consItems) stats.timePeriod = phaseUseTime
        super.setStats()
//        stats.add(Stat.shieldHealth, shieldHealth, StatUnit.none)
//        stats.add(Stat.regenerationRate, cooldownNormal * 60f, StatUnit.perSecond)
//        stats.add(Stat.cooldownTime, (shieldHealth / cooldownBrokenBase / 60f).toInt().toFloat(), StatUnit.seconds)

        if (consItems && itemConsumer is ConsumeItems) {
            stats.remove(Stat.booster)
//            stats.add(Stat.booster, StatValues.itemBoosters("+{0} " + StatUnit.shieldHealth.localized(), stats.timePeriod, phaseShieldBoost, phaseRadiusBoost, (itemConsumer as ConsumeItems).items))

//            stats.add(Stat.booster,  {
//                    table: arc.scene.ui.layout.Table -> table.row()
//                table.table { c: arc.scene.ui.layout.Table ->
//                    for (liquid in Vars.content.liquids()) {
//                        if (!consumesLiquid(liquid)) continue
//
//                        c.table(mindustry.ui.Styles.grayPanel) { b: arc.scene.ui.layout.Table ->
//                            b.image(liquid.uiIcon).size(40f).pad(10f).left().scaling(Scaling.fit)
//                                .with(Cons { i: arc.scene.ui.Image? ->
//                                    StatValues.withTooltip<arc.scene.ui.Image?>(
//                                        i,
//                                        liquid,
//                                        false
//                                    )
//                                })
//                            b.table { info: arc.scene.ui.layout.Table ->
//                                info.add(liquid.localizedName).left().row()
//                                info.add(
//                                    arc.util.Strings.autoFixed(
//                                        coolantConsumption * 60f,
//                                        2
//                                    ) + StatUnit.perSecond.localized()
//                                ).left().color(arc.graphics.Color.lightGray)
//
//                            }
//
//                            val liquidHeat: kotlin.Float = (1f + (liquid.heatCapacity - 0.4f) * 0.9f)
////                            val regenBoost: kotlin.Float =
////                                ((cooldownNormal * (cooldownLiquid * liquidHeat)) - cooldownNormal) * 60f
////                            val cooldownBoost: kotlin.Float =
////                                (shieldHealth / (cooldownBrokenBase * (cooldownLiquid * liquidHeat)) - shieldHealth / cooldownBrokenBase) / 60f
//
////                            b.table { bt: arc.scene.ui.layout.Table ->
////                                bt.right().defaults().padRight(3f).left()
////                                bt.add(
////                                    "[lightgray]+" + Core.bundle.format(
////                                        "bar.regenerationrate",
////                                        arc.util.Strings.autoFixed(regenBoost, 2)
////                                    )
////                                ).pad(5f).row()
////                                bt.add(
////                                    Core.bundle.format(
////                                        "ability.stat.cooldown",
////                                        arc.util.Strings.autoFixed(cooldownBoost, 2)
////                                    )
////                                ).pad(5f)
////
////                            }.right().grow().pad(10f).padRight(15f)
//
//                        }.growX().pad(5f).row()
//                    }
//                }.growX().colspan(table.getColumns())
//                table.row()
//
//            })
        }
    }
    override fun drawPlace(x: Int, y: Int, rotation: Int, valid: Boolean) {
        super.drawPlace(x, y, rotation, valid)

        Draw.color(Pal.gray)
        Lines.stroke(3f)
        Lines.poly(x * Vars.tilesize + offset, y * Vars.tilesize + offset, sides, radius, shieldRotation)
        Draw.color(Vars.player.team().color)
        Lines.stroke(1f)
        Lines.poly(x * Vars.tilesize + offset, y * Vars.tilesize + offset, sides, radius, shieldRotation)
        Draw.color()
    }

    inner class PowerProjectorBuild: Building(), Ranged, ExplosionShield, EntropyBuilding {
        var radscl: Float = 0f
        var warmup: Float = 0f
        var phaseHeat: Float = 0f
        var hit: Float = 0f

        // ✅ 添加上次更新时的电力图，用于检测电网变化
        var lastGraph: PowerGraph? = null
        var lastGraphSize: Int =0
        var lastGraphID: Int = 0
        // ✅ 只在必要时更新节点列表
        val powerProjectorNodes = Seq<PowerProjectorNode.PowerProjectorNodeBuild>(false, 16, PowerProjectorNode.PowerProjectorNodeBuild::class.java)
        var needsUpdate: Boolean = false


        var shouldDisable: Boolean = false

        override fun range(): Float = realRadius()

        override fun shouldAmbientSound(): Boolean = enabled && realRadius() > 1f

        override fun onRemoved() {
            val radius: Float = realRadius()
            if (enabled && radius > 1f) Fx.forceShrink.at(x, y, radius, team.color)
            super.onRemoved()
        }

        override fun pickedUp() {
            super.pickedUp()
            warmup = 0f
            radscl = warmup
            lastGraph = null
            powerProjectorNodes.clear()
            shouldDisable = false
        }

        override fun inFogTo(viewer: Team?): Boolean = false

        val buildEndEventListener = { it: EventType.BlockBuildEndEvent ->
            if (it.team == team && it.tile.build is PowerProjectorNode.PowerProjectorNodeBuild && it.tile.build?.power?.graph === power.graph) {
                needsUpdate = true
            }
        }
        val destroyEventListener = { it: EventType.BlockDestroyEvent ->
            if (it.tile.team() == team && it.tile.build is PowerProjectorNode.PowerProjectorNodeBuild && it.tile.build?.power?.graph === power.graph) {
                needsUpdate = true
            }
        }

        override fun created() {
            super.created()
            // ✅ 监听建筑添加/移除事件
            Events.on(EventType.BlockBuildEndEvent::class.java, buildEndEventListener)
            Events.on(EventType.BlockDestroyEvent::class.java, destroyEventListener)
        }

        override fun remove() {
            super.remove()
            // ✅ 移除所有监听
            Events.remove(EventType.BlockBuildEndEvent::class.java, buildEndEventListener)
            Events.remove(EventType.BlockDestroyEvent::class.java, destroyEventListener)
        }

        override fun updateTile() {
            // ✅ 检查电网是否变化，只在变化时更新
            if (lastGraphID != power.graph.getID() || lastGraph !== power.graph || lastGraphSize != power.graph.all.size || needsUpdate) {
                lastGraph = power.graph
                lastGraphSize = power.graph.all.size
                lastGraphID = power.graph.getID()
                updateNodeList()
            }

            if (shouldDisable) {
                enabled = false
                return
            }

            val phaseValid = itemConsumer != null && itemConsumer!!.efficiency(this) > 0

            phaseHeat = Mathf.lerpDelta(phaseHeat, Mathf.num(phaseValid).toFloat(), 0.1f)

            if(phaseValid && enabled && timer(timerUse, phaseUseTime / timeScale) && efficiency > 0){
                consume()
            }

            radscl = Mathf.lerpDelta(radscl, warmup, 0.05f)

            warmup = Mathf.lerpDelta(warmup, efficiency, 0.1f)

            if(hit > 0f){
                hit -= 1f / 5f * Time.delta
            }

            deflectBullets()
        }

        // ✅ 单独的方法，只在电网变化时调用
        private fun updateNodeList() {
            powerProjectorNodes.clear()
            shouldDisable = false

            power.graph.all.forEach {
                when (it) {
                    is PowerProjectorBuild if it != this && it.enabled && it.realRadius() > 0f -> {
                        shouldDisable = true
                    }
                    is PowerProjectorNode.PowerProjectorNodeBuild if !it.dead -> {
                        powerProjectorNodes.add(it)

                    }
                }
            }
        }

        fun breakShield(){
            this.power.graph.batteries.forEach { if (!it.dead) {
                if (isDestroyBattery) {
                    it.kill()
                }
                this.power.graph.batteries.remove(it)
            }  }
            this.power.graph.all.forEach { if (it is PowerNodeBuild && !it.dead) {
                if (it is PowerProjectorNode.PowerProjectorNodeBuild){
                    if (isDestroyProjectorNode) {
                        it.kill()
                        this.power.graph.all.remove(it)
                        shieldBreakEffect.at(it.x, it.y, realRadius() * it.radiusScl, team.color)
                        breakSound.at(it.x, it.y)
                    }
                }else {
                    if (isDestroyNode) {
                        it.kill()
                        this.power.graph.all.remove(it)
                    }
                }

            } }
//            val queue = ArrayDeque<Building>()
//            val visited = HashSet<Building>()
//            queue.add(start)
//            visited.add(start)
//
//            while (queue.isNotEmpty()) {
//                val current = queue.removeFirst()
//                val links = current.power.links
//
//                while (!links.isEmpty) {
//                    val build = Vars.world.build(links.first())
//                    links.removeIndex(0)
//
//                    if (build != null && build is PowerNodeBuild && visited.add(build)) {
//                        queue.add(build)
//                        breakShield(build)
//                        build.kill()
//                    }
//                }
//            }

        }



        fun deflectBullets(){
            val radius = realRadius()
            if (radius <= 0) {
                return
            }

            var minX = x - radius
            var maxX = x + radius
            var minY = y - radius
            var maxY = y + radius

            val nodeSize = powerProjectorNodes.size
            val posArr = IntArray(nodeSize)
            val radiusArr = FloatArray(nodeSize)
            val boundsArr = FloatArray(nodeSize * 4)

            var idx = 0
            for (node in powerProjectorNodes) {
                val nodeRadius = radius * node.radiusScl
                posArr[idx] = node.pos()
                radiusArr[idx] = nodeRadius

                val boundsIdx = idx * 4
                boundsArr[boundsIdx] = node.x - nodeRadius
                boundsArr[boundsIdx + 1] = node.x + nodeRadius
                boundsArr[boundsIdx + 2] = node.y - nodeRadius
                boundsArr[boundsIdx + 3] = node.y + nodeRadius

                // 更新全局边界
                if (nodeRadius > 0f) {
                    minX = minX.coerceAtMost(boundsArr[boundsIdx])
                    maxX = maxX.coerceAtLeast(boundsArr[boundsIdx + 1])
                    minY = minY.coerceAtLeast(boundsArr[boundsIdx + 2])
                    maxY = maxY.coerceAtMost(boundsArr[boundsIdx + 3])
                }
                idx++
            }

            paramBlock = this@PowerProjector
            paramEntity = this
            nodeCount = nodeSize
            nodePosArr = posArr
            nodeRadiusArr = radiusArr
            nodeBoundsArr = boundsArr

            Groups.bullet.intersect(
                minX,
                minY,
                maxX - minX,
                maxY - minY,
                shieldConsumer
            )

            // 清理，防止内存泄漏
            nodeCount = 0
            nodePosArr = null
            nodeRadiusArr = null
            nodeBoundsArr = null
        }

        override fun absorbExplosion(ex: Float, ey: Float, damage: Float): Boolean {
            val absorb = enabled && Intersector.isInRegularPolygon(sides, x, y, realRadius(), shieldRotation, ex, ey)
            if(absorb){
                absorbEffect.at(ex, ey)
                hit = 1f
            }
            return absorb
        }

        fun realRadius(): Float =(radius + phaseHeat * phaseRadiusBoost) * radscl

        override fun sense(sensor: LAccess): Double {
            if(sensor == LAccess.shield) return (!enabled ifTrue 0.0)
                ?: (power.graph.lastPowerStored.toDouble()).coerceAtLeast(0.0)
            return super.sense(sensor)
        }

        override fun draw() {
            super.draw()

            if(enabled){
                Draw.alpha(power.graph.lastPowerStored / power.graph.lastCapacity * 0.75f)
                Draw.z(Layer.blockAdditive)
                Draw.blend(Blending.additive)
                Draw.rect(topRegion, x, y)
                Draw.blend()
                Draw.z(Layer.block)
                Draw.reset()
            }

            drawShield()
        }

        fun drawShield(){
            if(enabled){
                val radius = realRadius()

                if(radius > 0.001f){
                    Draw.color(team.color, Color.white, Mathf.clamp(hit))

                    if(renderer.animateShields){
                        Draw.z(Layer.shields + 0.001f * hit)
                        Fill.poly(x, y, sides, radius, shieldRotation)
                        powerProjectorNodes.forEach {
                            Fill.poly(it.x, it.y, sides, radius*it.radiusScl, shieldRotation)
                        }
                    }else{
                        Draw.z(Layer.shields)
                        Lines.stroke(1.5f)
                        Draw.alpha(0.09f + Mathf.clamp(0.08f * hit))
                        Fill.poly(x, y, sides, radius, shieldRotation)
                        Draw.alpha(1f)
                        Lines.poly(x, y, sides, radius, shieldRotation)
                        powerProjectorNodes.forEach {
                            Lines.stroke(1.5f)
                            Draw.alpha(0.09f + Mathf.clamp(0.08f * hit))
                            Fill.poly(it.x, it.y, sides, radius*it.radiusScl, shieldRotation)
                            Draw.alpha(1f)
                            Lines.poly(it.x, it.y, sides, radius*it.radiusScl, shieldRotation)
                        }
                        Draw.reset()
                    }
                }
            }

            Draw.reset()
        }


        override fun write(write: Writes){
            super.write(write)
            write.f(radscl)
            write.f(warmup)
            write.f(phaseHeat)
            write.i(powerProjectorNodes.size)
            powerProjectorNodes.forEach {
                write.i(it.pos())
            }
        }

        override fun read(read: Reads, revision: Byte){
            super.read(read, revision)
            radscl = read.f()
            warmup = read.f()
            phaseHeat = read.f()
            val size = read.i()
            powerProjectorNodes.clear()
            (0 until size).forEach { _ ->
                powerProjectorNodes.add(Vars.world.build(read.i())as PowerProjectorNode.PowerProjectorNodeBuild)
            }
            // ✅ 读取后重新验证列表
            lastGraph = null
            lastGraphID =0
        }
    }

}
