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
import arc.scene.ui.Image
import arc.scene.ui.layout.Stack
import arc.scene.ui.layout.Table
import arc.struct.EnumSet
import arc.struct.Seq
import arc.util.Scaling
import arc.util.Time
import arc.util.io.Reads
import arc.util.io.Writes
import entropy.Entropy.Companion.ifTrue
import entropy.EntropyBlock
import entropy.EntropyBuilding
import mindustry.Vars
import mindustry.Vars.content
import mindustry.Vars.renderer
import mindustry.content.Fx
import mindustry.core.UI
import mindustry.ctype.UnlockableContent
import mindustry.entities.Damage
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
import mindustry.type.Item
import mindustry.type.ItemStack
import mindustry.ui.Bar
import mindustry.ui.Styles
import mindustry.world.Block
import mindustry.world.blocks.ExplosionShield
import mindustry.world.blocks.power.PowerGraph
import mindustry.world.blocks.power.PowerNode.PowerNodeBuild
import mindustry.world.consumers.*
import mindustry.world.meta.*
import mindustry.world.meta.StatValues.withTooltip
import kotlin.math.max
import kotlin.math.min

/**
 * 将电力作为护盾
 */
@Suppress("DuplicatedCode")
open class PowerProjector(name: String) : Block(name), EntropyBlock {
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
//    var smoke = Effect(17f) { e ->
//        randLenVectors(e.id.toLong(), 4, e.fin() * 8f) { x, y ->
//            val size = 1f + e.fout() * 5f
//            color(Color.lightGray, Color.gray, e.fin())
//            Fill.circle(e.x + x, e.y + y, size / 2f)
//        }
//    }

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

    /**
     * 护盾被摧毁时的震动
     */
    var shake: Float = 0.1f

    /**
     * 每个节点的消耗比例
     */
    var perNodeConsumption: Float = 0.5f


    var topRegion: TextureRegion? = null

    //TODO json support
    var itemConsumer: ConsumeItems? = null
        set(value) {
            field = value
            consumeBuilder.removeAll { it is ConsumeItemDynamic || it is ConsumeItems || it is ConsumeItemFilter }
            consume(ConsumeItemDynamic { build: PowerProjectorBuild ->
                val items = Seq<ItemStack>()
                value?.items?.forEach {
                    items.add(
                        ItemStack(
                            it.item,
                            it.amount * (build.powerProjectorNodes.size * perNodeConsumption + 1).toInt()
                        )
                    )
                }
                items.toArray(ItemStack::class.java)
            }
            )
        }
    val itemCapacitiesBase = Array(content.items().size) { 0 }

    //TODO json support
    var coolantConsumer: Consume? = null


    init {
        update = true
        solid = true
        group = BlockGroup.projectors
        hasPower = true
//        hasLiquids = true
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
        itemConsumer?.items?.forEach {
            itemFilter[it.item.id.toInt()] = true
            itemCapacitiesBase[it.item.id.toInt()] = it.amount
        }
    }

    override fun setBars() {
        super.setBars()
        removeBar("shield")
        addBar<PowerProjectorBuild>("shield") { build ->
            val batteryStored = build.power.graph.lastPowerStored
            val batteryCapacity = build.power.graph.lastCapacity
            Bar(
                { Core.bundle.format("stat.shieldhealth", batteryStored, "/", batteryCapacity) },
                { Pal.accent }
            ) { ((!build.enabled || build.shouldDisable) ifTrue 0f) ?: (batteryStored / batteryCapacity) }
        }
    }

    override fun outputsItems(): Boolean {
        return false
    }

    fun stack(region: TextureRegion, amount: Int, content: UnlockableContent, tooltip: Boolean): Stack {
        val stack = Stack()

        stack.add(Table { o ->
            o.left()
            o.add(Image(region)).size(32f).scaling(Scaling.fit)
        })

        if (amount != 0) {
            stack.add(Table { t ->
                t.left().bottom()
                t.add(
                    "${
                        if (amount >= 1000) {
                            UI.formatAmount(amount.toLong())
                        } else {
                            amount.toString()
                        }
                    }+${arc.util.Strings.autoFixed(amount * perNodeConsumption, 3)}N"
                ).name("stack amount").style(Styles.outlineLabel)
                t.pack()
            })
        }

        withTooltip(stack, content, tooltip)

        return stack
    }

    fun displayItem(item: Item, amount: Int, timePeriod: Float, showName: Boolean): Table {
        val t = Table()
        t.add(stack(item.uiIcon, amount, item, !showName))
        t.add(
            "${if (showName) item.localizedName + "\n" else ""}[lightgray]${
                arc.util.Strings.autoFixed(
                    amount / (timePeriod / 60f),
                    3
                )
            }+${
                arc.util.Strings.autoFixed(
                    amount * perNodeConsumption / (timePeriod / 60f),
                    3
                )
            }N${StatUnit.perSecond.localized()}"
        ).padLeft(2f).padRight(5f).style(
            Styles.outlineLabel
        )
        return t
    }

    fun displayItemPercent(item: Item, percent: Int, showName: Boolean): Table {
        val t = Table()
        t.add(stack(item.uiIcon, 0, item, !showName))
        t.add((if (showName) item.localizedName + "\n" else "") + "[lightgray]" + percent + "%").padLeft(2f)
            .padRight(5f).style(
                Styles.outlineLabel
            )
        return t
    }

    override fun setStats() {
        val consItems: Boolean = itemConsumer != null

        if (consItems) {
            stats.timePeriod = phaseUseTime
            stats.remove(Stat.input)
            val items = itemConsumer!!.items
            stats.add(
                Stat("n", StatCat.crafting),
                "电力护盾节点数"
            )
            stats.add(
                Stat.input, if (stats.timePeriod < 0) {
                    StatValue { table ->
                        items.forEach { stack ->
                            table.add(displayItemPercent(stack.item, stack.amount, true)).padRight(5f)
                        }
                    }
                } else {
                    StatValue { table ->
                        items.forEach { stack ->
                            table.add(displayItem(stack.item, stack.amount, stats.timePeriod, true)).padRight(5f)
                        }
                    }
                })
//                ((stats.timePeriod < 0) ifTrue {
//                    itemConsumer!!.items.forEach { table ->
//
//                    }
//                }) ?: StatValues.items(
//                    stats.timePeriod,
//                    itemConsumer!!.items
//                )
//            )
        }
        super.setStats()

//        stats.add(booster ? Stat.booster : Stat.input, stats.timePeriod < 0 ? StatValues.items(items) : StatValues.items(stats.timePeriod, items))

//        table.table(Cons {
//                c: arc.scene.ui.layout.Table? -> var i: kotlin.Int = 0
//            for (stack in items) {
//                c.add<ReqImage?>(ReqImage(StatValues.stack(stack.item, java.lang.Math.round(stack.amount * multiplier.get(build))),
//                    Boolp {build.items.has(stack.item, java.lang.Math.round(stack.amount * multiplier.get(build)))})).padRight(8f)
//                if (++i % 4 == 0) c.row()
//            }
//
//        }).left()
//        stats.add(Stat.shieldHealth, shieldHealth, StatUnit.none)
//        stats.add(Stat.regenerationRate, cooldownNormal * 60f, StatUnit.perSecond)
//        stats.add(Stat.cooldownTime, (shieldHealth / cooldownBrokenBase / 60f).toInt().toFloat(), StatUnit.seconds)

        if (consItems && itemConsumer is ConsumeItems) {
            stats.remove(Stat.booster)

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

    inner class PowerProjectorBuild : Building(), Ranged, ExplosionShield, EntropyBuilding {
        var radscl: Float = 0f
            set(value) {
                field = if (value < 0.001f) {
                    0f
                } else if (value > 0.999f) {
                    1f
                } else {
                    value
                }
            }
        var warmup: Float = 0f
            set(value) {
                field = if (value < 0.001f) {
                    0f
                } else if (value > 0.999f) {
                    1f
                } else {
                    value
                }
            }
        var phaseHeat: Float = 0f
            set(value) {
                field = if (value < 0.001f) {
                    0f
                } else if (value > 0.999f) {
                    1f
                } else {
                    value
                }
            }

        var lastRadscl: Float = 0f
        var lastPhaseHeat: Float = 0f
        var lastWarmup: Float = 0f

        var hit: Float = 0f

        // ✅ 添加上次更新时的电力图，用于检测电网变化
        var lastGraph: PowerGraph? = null
        var lastGraphSize: Int = 0
        var lastGraphID: Int = -1

        var needsUpdate: Boolean = false

        var shouldDisable: Boolean = false

        // ✅ 节点缓存，只在电网更新时更新
        val powerProjectorNodes = Seq<PowerProjectorNode.PowerProjectorNodeBuild>(
            false,
            16,
            PowerProjectorNode.PowerProjectorNodeBuild::class.java
        )

        /**
         * 优化的子弹检测回调
         * 先用矩形边界粗检测过滤，再用多边形精确检测
         */
        private val shieldConsumer = shieldConsumer@{ bullet: Bullet ->
            if (bullet.team !== this@PowerProjectorBuild.team &&
                bullet.type.absorbable &&
                !bullet.absorbed
            ) {

                val bx = bullet.x
                val by = bullet.y
                val realRadius = this@PowerProjectorBuild.realRadius()

                if (realRadius > 0) {
                    // 检测本体护盾
                    val selfMinX = this@PowerProjectorBuild.x - realRadius
                    val selfMaxX = this@PowerProjectorBuild.x + realRadius
                    val selfMinY = this@PowerProjectorBuild.y - realRadius
                    val selfMaxY = this@PowerProjectorBuild.y + realRadius

                    if (bx in selfMinX..selfMaxX && by in selfMinY..selfMaxY) {
                        if (Intersector.isInRegularPolygon(
                                this@PowerProjector.sides, this@PowerProjectorBuild.x, this@PowerProjectorBuild.y,
                                realRadius, this@PowerProjector.shieldRotation, bx, by
                            )
                        ) {
                            handleBulletAbsorb(bullet, this@PowerProjectorBuild.power.graph)
                            return@shieldConsumer
                        }
                    }

                    // 检测所有节点护盾

                    for (node in powerProjectorNodes) {
                        val nodeRadius = node.realRadius()

                        if (nodeRadius <= 0f) continue

                        // 先做矩形粗检测，快速过滤掉不在范围内的子弹
                        val minX = node.x - nodeRadius
                        val maxX = node.x + nodeRadius
                        val minY = node.y - nodeRadius
                        val maxY = node.y + nodeRadius
                        if (bx !in minX..maxX || by !in minY..maxY) continue

                        // 矩形内才执行昂贵的多边形检测
                        val inPolygon = Intersector.isInRegularPolygon(
                            this@PowerProjector.sides, node.x, node.y,
                            nodeRadius, this@PowerProjector.shieldRotation, bx, by
                        )
                        if (inPolygon) {
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
            bullet.absorb()
            this@PowerProjector.hitSound.at(
                bullet.x,
                bullet.y,
                1f + Mathf.range(0.1f),
                this@PowerProjector.hitSoundVolume
            )
            this@PowerProjector.absorbEffect.at(bullet)
            this.hit = 1f
            val powerStored = this.power.graph.lastPowerStored
            val damage = bullet.type.shieldDamage(bullet) * this@PowerProjector.shieldDamagedMultiplier
            val surplus = powerStored - damage
            when {
                surplus > 0 -> graph.transferPower(-damage)
                surplus == 0.toFloat() -> {
                    graph.transferPower(-damage)
                    this@PowerProjectorBuild.breakShield()
                    if (this@PowerProjector.isDestroySelf) {
                        kill()
                    }
                    this@PowerProjector.shieldBreakEffect.at(x, y, realRadius(), team.color)
                    this@PowerProjector.breakSound.at(x, y)
                }

                surplus < 0 -> {
                    graph.transferPower(-powerStored)

                    this@PowerProjectorBuild.breakShield()
                    if (this@PowerProjector.isDestroySelf) {
                        kill()
                    }
                    this@PowerProjector.shieldBreakEffect.at(x, y, realRadius(), team.color)
                    this@PowerProjector.breakSound.at(x, y)

                    // 创建爆炸效果
                    Fx.explosion.at(x, y)
                    Fx.shockwave.at(x, y, radius)

                    // 造成伤害
                    Damage.damage(bullet.team, x, y, radius, damage, true)

                    // 可选：添加屏幕震动
                    if (shake > 0) {
                        Effect.shake(shake, shake, x, y)
                    }
                }
            }
        }

        override fun range(): Float = realRadius()

        override fun shouldAmbientSound(): Boolean = enabled && !shouldDisable && realRadius() > 1f

        override fun onRemoved() {
            val radius: Float = realRadius()
            if (enabled && !shouldDisable && radius > 1f) Fx.forceShrink.at(x, y, radius, team.color)
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
            // ✅ 初始化变量，防止被 pickedUp() 残留的 0 值影响
            warmup = 0f
            radscl = 0f
            phaseHeat = 0f
            hit = 0f
            lastGraph = null
            lastGraphSize = 0
            lastGraphID = 0
            needsUpdate = true
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
            if (efficiency <= 0f || (itemConsumer != null && itemConsumer!!.efficiency(this) <= 0f)) {
                radscl = 0f
                phaseHeat = 0f
                warmup = 0f
                return
            }
            if (lastGraphID != power.graph.getID() || lastGraph !== power.graph || lastGraphSize != power.graph.all.size || needsUpdate) {
                lastGraph = power.graph
                lastGraphSize = power.graph.all.size
                lastGraphID = power.graph.getID()
                updateNodeList()
            }

            if (shouldDisable) return

            lastRadscl = radscl
            lastPhaseHeat = phaseHeat
            lastWarmup = warmup

            val phaseValid = itemConsumer != null && itemConsumer!!.efficiency(this) > 0

            phaseHeat = Mathf.lerpDelta(phaseHeat, Mathf.num(phaseValid).toFloat(), 0.1f)

            if (phaseValid && enabled && !shouldDisable && timer(
                    timerUse,
                    phaseUseTime / timeScale
                ) && efficiency > 0 && power.status > 0
            ) {
                consume()
                Fx.reactorsmoke.at(x + Mathf.range(Vars.tilesize), y + Mathf.range(Vars.tilesize))

            }

            radscl = Mathf.lerpDelta(radscl, warmup, 0.05f)
            warmup = Mathf.lerpDelta(warmup, efficiency, 0.1f)

            if (hit > 0f) {
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
                    is PowerProjectorBuild if it != this && it.enabled && !it.shouldDisable && it.realRadius() > 0f -> {
                        shouldDisable = true
                    }

                    is PowerProjectorNode.PowerProjectorNodeBuild if !it.dead -> {
                        powerProjectorNodes.add(it)

                    }
                }
            }
        }

        fun breakShield() {
            this.power.graph.batteries.forEach {
                if (!it.dead) {
                    if (isDestroyBattery) {
                        it.kill()
                    }
                    this.power.graph.batteries.remove(it)
                }
            }

            val needRemove = arrayListOf<Building>()
            this.power.graph.all.forEach {
                if (it is PowerNodeBuild && !it.dead) {
                    if (it is PowerProjectorNode.PowerProjectorNodeBuild) {
                        if (isDestroyProjectorNode) {
                            it.kill()
                            needRemove.add(it)
                            shieldBreakEffect.at(it.x, it.y, realRadius() * it.radiusScl, team.color)
                            breakSound.at(it.x, it.y)
                        }
                    } else {
                        if (isDestroyNode) {
                            it.kill()
                            needRemove.add(it)
                        }
                    }
                }
            }
            needRemove.forEach { this.power.graph.all.remove(it) }

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


        fun deflectBullets() {
            val radius = realRadius()


            if (radius <= 0f) {
                return
            }

            // ✅ 先更新缓存，因为 radius 可能随 warmup 变化
//            updateNodeCache()

            // ✅ 计算全局边界（包含本体和所有节点）
            var minX = x - radius
            var maxX = x + radius
            var minY = y - radius
            var maxY = y + radius




            for (node in powerProjectorNodes) {
                val nodeMinX = node.x - node.radiusScl * radius
                val nodeMaxX = node.x + node.radiusScl * radius
                val nodeMinY = node.y - node.radiusScl * radius
                val nodeMaxY = node.y + node.radiusScl * radius


                // 取更小的min，更大的max

                minX = min(minX, nodeMinX)
                maxX = max(maxX, nodeMaxX)
                minY = min(minY, nodeMinY)
                maxY = max(maxY, nodeMaxY)
            }



            Groups.bullet.intersect(
                minX,
                minY,
                maxX - minX,
                maxY - minY,
                shieldConsumer
            )
        }


        override fun absorbExplosion(ex: Float, ey: Float, damage: Float): Boolean {
            val absorb = Intersector.isInRegularPolygon(sides, x, y, realRadius(), shieldRotation, ex, ey)
            if (absorb) {
                absorbEffect.at(ex, ey)
                hit = 1f
            }
            return absorb
        }

        fun realRadius(): Float {
            val r = (radius + phaseHeat * phaseRadiusBoost) * radscl
            if (r < 0.01f) {
                return 0f
            }
            return r
        }

        override fun sense(sensor: LAccess): Double {
            if (sensor == LAccess.shield) return ((!enabled || shouldDisable) ifTrue 0.0)
                ?: (power.graph.lastPowerStored.toDouble()).coerceAtLeast(0.0)
            return super.sense(sensor)
        }

        override fun draw() {
            super.draw()

            if (enabled && !shouldDisable && efficiency > 0 && power.status > 0) {
                Draw.alpha(power.graph.lastPowerStored / power.graph.lastCapacity * 0.75f)
                Draw.z(Layer.blockAdditive)
                Draw.blend(Blending.additive)
                Draw.rect(topRegion, x, y)
                Draw.blend()
                Draw.z(Layer.block)
                Draw.reset()

                drawShield()
            }

        }


        fun drawShield() {
            val radius = realRadius()

            if (radius > 0.001f) {

                Draw.color(team.color, Color.white, Mathf.clamp(hit))

                if (renderer.animateShields) {
                    // 动画护盾
                    Draw.z(Layer.shields + 0.001f * hit)
                    Fill.poly(x, y, sides, radius, shieldRotation)
                    powerProjectorNodes.forEach {
                        Fill.poly(it.x, it.y, sides, radius * it.radiusScl, shieldRotation)
                    }
                    Draw.reset()
                } else {
                    Draw.z(Layer.shields)
                    Lines.stroke(1.5f)
                    Draw.alpha(0.09f + Mathf.clamp(0.08f * hit))
                    Fill.poly(x, y, sides, radius, shieldRotation)
                    Draw.alpha(1f)
                    Lines.poly(x, y, sides, radius, shieldRotation)
                    powerProjectorNodes.forEach {
                        Lines.stroke(1.5f)
                        Draw.alpha(0.09f + Mathf.clamp(0.08f * hit))
                        Fill.poly(it.x, it.y, sides, radius * it.radiusScl, shieldRotation)
                        Draw.alpha(1f)
                        Lines.poly(it.x, it.y, sides, radius * it.radiusScl, shieldRotation)
                    }
                    Draw.reset()
                }
            }
        }

        override fun acceptItem(source: Building?, item: Item?): Boolean {
            return this.block.consumesItem(item) && this.items.get(item) < this.getMaximumAccepted(item)
        }

        override fun getMaximumAccepted(item: Item?): Int {
            return 3 * itemCapacitiesBase[item?.id?.toInt()
                ?: 0] * (powerProjectorNodes.size * perNodeConsumption + 1).toInt()
        }


        override fun write(write: Writes) {
            super.write(write)
            write.f(radscl)
            write.f(warmup)
            write.f(phaseHeat)
            write.i(powerProjectorNodes.size)
            powerProjectorNodes.forEach {
                write.i(it.pos())
            }
        }

        override fun read(read: Reads, revision: Byte) {
            super.read(read, revision)
            radscl = read.f()
            warmup = read.f()
            phaseHeat = read.f()
            val size = read.i()
            powerProjectorNodes.clear()
            (0 until size).forEach { _ ->
                powerProjectorNodes.add(Vars.world.build(read.i()) as PowerProjectorNode.PowerProjectorNodeBuild)
            }
            // ✅ 读取后重新验证列表
            lastGraph = null
            lastGraphID = -1
        }

        fun PowerProjectorNode.PowerProjectorNodeBuild.realRadius(): Float {
            return this@PowerProjectorBuild.realRadius() * radiusScl
        }
    }


}
