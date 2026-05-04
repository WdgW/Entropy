package entropy

import arc.Core
import arc.func.Cons
import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.math.Mathf
import arc.scene.style.TextureRegionDrawable
import arc.scene.ui.Image
import arc.scene.ui.layout.Stack
import arc.scene.ui.layout.Table
import arc.struct.Seq
import arc.util.*
import arc.util.io.Reads
import arc.util.io.Writes
import mindustry.Vars
import mindustry.content.Fx
import mindustry.core.UI
import mindustry.ctype.UnlockableContent
import mindustry.entities.Effect
import mindustry.entities.units.BuildPlan
import mindustry.gen.Building
import mindustry.gen.Icon
import mindustry.graphics.Pal
import mindustry.logic.LAccess
import mindustry.mod.NoPatch
import mindustry.type.Item
import mindustry.type.ItemStack
import mindustry.type.Liquid
import mindustry.type.LiquidStack
import mindustry.ui.Bar
import mindustry.ui.Styles
import mindustry.world.blocks.ItemSelection
import mindustry.world.blocks.payloads.Payload
import mindustry.world.blocks.production.GenericCrafter
import mindustry.world.consumers.ConsumeItemDynamic
import mindustry.world.consumers.ConsumeLiquidsDynamic
import mindustry.world.consumers.ConsumePowerDynamic
import mindustry.world.draw.DrawBlock
import mindustry.world.draw.DrawDefault
import mindustry.world.meta.BlockStatus
import mindustry.world.meta.Stat
import mindustry.world.meta.StatUnit
import mindustry.world.meta.StatValues
import java.util.*
import kotlin.math.max
import kotlin.math.min

class MultiFactory(name: String) : GenericCrafter(name) {
    var itemCapacities: IntArray = intArrayOf()
    var liquidCapacities: FloatArray = floatArrayOf()

    var plans: Seq<ProductPlan> = Seq<ProductPlan>(4)

    //    public Sound createSound = Sounds.unitCreate;
    //    public float createSoundVolume = 1f;
    init {
        update = true
        hasPower = true
        hasItems = true
        hasLiquids = true
        solid = true
        configurable = true
        clearOnDoubleTap = true
        acceptsItems = true

        //        outputsPayload = true;
//        rotate = true;
//        regionRotated1 = 1;
//        commandable = true;
//        ambientSound = Sounds.loopUnitBuilding;
//        ambientSoundVolume = 0.09f;
        config(Int::class.java) { build: MultiFactoryBuild, i: Int ->
            if (!configurable) return@config
            if (build.currentPlan == i) return@config
            build.currentPlan = (if (i < 0 || i >= plans.size) -1 else i)
            build.progress = 0f
            if (build.currentPlan != -1) {
                plans.get(build.currentPlan).applyPlan(build)
            }
        }

        config(Item::class.java){ build: MultiFactoryBuild, l: Item ->
            if (!configurable) return@config
            val next =
                plans.indexOf { p: ProductPlan -> p.outputItems.isNotEmpty() && p.outputItems[0].item === l }
            if (build.currentPlan == next) return@config
            build.currentPlan = next
            build.progress = 0f
            plans.get(build.currentPlan).applyPlan(build)
        }

        config(Liquid::class.java){ build: MultiFactoryBuild, l: Liquid ->
            if (!configurable) return@config
            val next =
                plans.indexOf { p: ProductPlan -> p.outputLiquids.isNotEmpty() && p.outputLiquids[0].liquid === l }
            if (build.currentPlan == next) return@config
            build.currentPlan = next
            build.progress = 0f
            plans.get(build.currentPlan).applyPlan(build)
        }

//        config(UnitCommand.class, (MultiFactoryBuild build, UnitCommand command) -> build.command = command);
//        configClear((MultiFactoryBuild build) -> build.command = null);

        consume(ConsumeItemDynamic { e: MultiFactoryBuild ->
            if (e.currentPlan == -1) return@ConsumeItemDynamic ItemStack.empty
            val plan = plans.get(min(e.currentPlan, plans.size - 1))
            plan.inputItems
        })

        consume(ConsumeLiquidsDynamic { e: MultiFactoryBuild ->
            if (e.currentPlan == -1) return@ConsumeLiquidsDynamic LiquidStack.empty
            val plan = plans.get(min(e.currentPlan, plans.size - 1))
            plan.inputLiquids
        })
        consume(ConsumePowerDynamic { building: Building ->
            if (building !is MultiFactoryBuild || building.currentPlan == -1) return@ConsumePowerDynamic 0f
            plans.get(min(building.currentPlan, plans.size - 1)).powerPerTick
        })
    }

    override fun init() {
        initCapacities()
        super.init()
        for (plan in plans) {
            if (plan.hasOutputItem) hasItems = true
            if (plan.hasOutputLiquid) hasLiquids = true
            if (plan.hasPower) hasPower = true
        }
    }

    override fun afterPatch() {
        initCapacities()
        super.afterPatch()
    }

    fun initCapacities() {
        itemCapacities = IntArray(Vars.content.items().size)
        itemCapacity = 10 //unit factories can't control their own capacity externally, setting the value does nothing

        liquidCapacities = FloatArray(Vars.content.liquids().size)
        liquidCapacity = 10f

        for (plan in plans) {
            for (stack in plan.outputItems) {
                itemCapacities[stack.item.id.toInt()] = max(itemCapacities[stack.item.id.toInt()], stack.amount * 2)
                itemCapacity = max(itemCapacity, stack.amount * 2)
            }
            for (stack in plan.inputItems) {
                itemCapacities[stack.item.id.toInt()] = max(itemCapacities[stack.item.id.toInt()], stack.amount * 2)
                itemCapacity = max(itemCapacity, stack.amount * 2)
            }

            for (stack in plan.outputLiquids) {
                liquidCapacities[stack.liquid.id.toInt()] =
                    max(liquidCapacities[stack.liquid.id.toInt()], stack.amount * 2)
                liquidCapacity = max(liquidCapacity, stack.amount * 2)
            }
            for (stack in plan.inputLiquids) {
                liquidCapacities[stack.liquid.id.toInt()] =
                    max(liquidCapacities[stack.liquid.id.toInt()], stack.amount * 2)
                liquidCapacity = max(liquidCapacity, stack.amount * 2)
            }
        }

        //        consumeBuilder.each(c -> c.multiplier = b -> state.rules.unitCost(b.team));
    }

    override fun setBars() {
        super.setBars()
        removeBar("liquid")
        removeBar("power")


        if (consPower != null) {
            val buffered = consPower.buffered

            addBar("power"
            ) { entity: Building ->
                Bar(
                    {
                        if (buffered) Core.bundle.format(
                            "bar.poweramount",
                            if ((entity.power.status * consPower.requestedPower(entity)).isNaN()) "<ERROR>" else UI.formatAmount(
                                (entity.power.status * consPower.requestedPower(entity)).toInt().toLong()
                            )
                        ) else Core.bundle.get("bar.power")
                    },
                    { Pal.powerBar },
                    { if (Mathf.zero(consPower.requestedPower(entity)) && entity.power.graph.getPowerProduced() + entity.power.graph.getBatteryStored() > 0f) 1f else entity.power.status })
            }
        }

        //        for (ProductPlan plan : plans) {
//            if (plan.hasPower){
//                addBar("power", (MultiFactoryBuild entity) -> {
//                    ProductPlan p = plans.get(Math.max(Math.min(entity.currentPlan, plans.size - 1),0));
//                    return new Bar(
//                            () -> p.hasPower Core.bundle.format("bar.poweramount", Float.isNaN(entity.power.status*p.powerPerTick) ? "<ERROR>" : UI.formatAmount((int) (entity.power.status*p.powerPerTick))) :
//                                    Core.bundle.get("bar.power"),
//                            () -> Pal.powerBar,
//                            () -> Mathf.zero(consPower.requestedPower(entity)) && entity.power.graph.getPowerProduced() + entity.power.graph.getBatteryStored() > 0f ? 1f : entity.power.status);
//                });
//            }
//        }

//        addBar("power", entity -> {
//            if (entity instanceof MultiFactoryBuild e && e.currentPlan != -1){
//                ProductPlan plan = plans.get(Math.max(Math.min(e.currentPlan, plans.size - 1),0));
//                return new Bar(
//                        () -> plan.hasPower? Core.bundle.format("bar.poweramount", Float.isNaN(entity.power.status) ? "<ERROR>" : UI.formatAmount((int) (entity.power.status))) :
//                                Core.bundle.get("bar.power"),
//                        () -> Pal.powerBar,
//                        () -> Mathf.zero(consPower.requestedPower(entity)) && entity.power.graph.getPowerProduced() + entity.power.graph.getBatteryStored() > 0f ? 1f : entity.power.status);
//            } else {
//                boolean buffered = consPower.buffered;
//                float capacity = consPower.capacity;
//
//                return new Bar(
//                        () -> buffered ? Core.bundle.format("bar.poweramount", Float.isNaN(entity.power.status * capacity) ? "<ERROR>" : UI.formatAmount((int)(entity.power.status * capacity))) :
//                                Core.bundle.get("bar.power"),
//                        () -> Pal.powerBar,
//                        () -> Mathf.zero(consPower.requestedPower(entity)) && entity.power.graph.getPowerProduced() + entity.power.graph.getBatteryStored() > 0f ? 1f : entity.power.status);
//            }
//        });
        for (plan in plans) {
            for (stack in plan.outputLiquids) {
                addLiquidBar(stack.liquid)
            }
            for (stack in plan.inputLiquids) {
                addLiquidBar(stack.liquid)
            }
        }

//        if (outputLiquids != null && outputLiquids.size > 0) {
//            //no need for dynamic liquid bar
//
//            //then display output buffer
//        }

        //        addBar("progress", (MultiFactoryBuild e) -> new Bar("bar.progress", Pal.ammo, e::fraction));

//        addBar("products", (MultiFactoryBuild e) ->
//                new Bar(
//                        () -> e.outItem() == null ? "[lightgray]" + Iconc.cancel :
//                                Core.bundle.format("bar.unitcap",
//                                        Fonts.getUnicodeStr(e.outItem().name),
//                                        e.team.data().countType(e.outItem()),
//                                        e.outItem() == null ? Units.getStringCap(e.team) : (e.unit().useUnitCap ? Units.getStringCap(e.team) : "∞")
//                                ),
//                        () -> Pal.power,
//                        () -> e.unit() == null ? 0f : (e.unit().useUnitCap ? (float)e.team.data().countType(e.unit()) / Units.getCap(e.team) : 1f)
//                ));
    }

    override fun outputsItems(): Boolean {
        return true
    }

    override fun setStats() {
        super.setStats()
        stats.remove(Stat.productionTime)

        stats.add(Stat.output) { table: Table ->
            table.row()
            for (plan in plans) {
                // 跳过含有禁用内容的计划，避免显示空面板
                if (containsBannedContent(plan)) continue

                table.table(Styles.grayPanel) { t: Table ->
                    // 左侧：输出
                    t.table { tt: Table -> buildOutputSection(tt, plan) }.left().pad(10f).padLeft(20f)

                    t.table { p: Table ->
                        StatValues.number(plan.powerPerTick * 60f, StatUnit.powerSecond).display(p)
                    }

                    // 右侧：输入
                    t.table { req -> buildInputSection(req, plan) }.growX().right().pad(10f)

                    t.row()
                    // 生产时间
                    t.add(
                        (Core.bundle.get("stat.productiontime") + ": "
                                + Strings.autoFixed(plan.craftTime / 60f, 3) + " "
                                + Core.bundle.get("unit.seconds"))
                    ).left()
                        .color(Color.lightGray).pad(2f)
                }.left().growX().left().pad(5f)
                table.row()
            }
        }
    }

    /**
     * 判断计划中是否包含当前不可用的（被禁用的）物品/液体
     */
    private fun containsBannedContent(plan: ProductPlan): Boolean {
        if (plan.hasOutputItem) {
            for (stack in plan.outputItems) {
                if (stack.item.isBanned) return true
            }
        }
        if (plan.hasOutputLiquid) {
            for (stack in plan.outputLiquids) {
                if (stack.liquid.isBanned) return true
            }
        }
        return false
    }

    /**
     * 填充输出物品/液体到左侧表格
     */
    private fun buildOutputSection(tt: Table, plan: ProductPlan) {
        if (plan.hasOutputItem) {
            for (stack in plan.outputItems) {
                if (stack.item == null) continue
                if (stack.item.unlockedNow()) {
                    tt.left()
                    tt.add(displayItem(stack.item, stack.amount, plan.craftTime, true)).left().padLeft(10f)
                } else {
                    addLockIcon(tt)
                }
            }
        }
        if (plan.hasOutputLiquid) {
            for (stack in plan.outputLiquids) {
                if (stack.liquid == null) continue
                if (stack.liquid.unlockedNow()) {
                    tt.left()
                    tt.add(displayLiquid(stack.liquid, stack.amount * (60f / plan.craftTime), true)).left()
                        .pad(10f).padLeft(10f)
                } else {
                    addLockIcon(tt)
                }
            }
        }
    }

    /**
     * 填充输入物品/液体到右侧表格（每行最多6个）
     */
    private fun buildInputSection(req: Table, plan: ProductPlan) {
        if (plan.hasInputItem) {
            for (i in plan.inputItems.indices) {
                if (i > 0 && i % 6 == 0) req.row() // 每行6个

                val stack = plan.inputItems[i]
                if (stack.item == null) continue
                if (stack.item.unlockedNow()) {
                    req.right()
                    req.add(displayItem(stack.item, stack.amount, plan.craftTime, true).right())
                        .right()
                } else {
                    addLockIcon(req)
                }
            }
        }
        if (plan.hasInputLiquid) {
            for (i in plan.inputLiquids.indices) {
                if (i > 0 && i % 6 == 0) req.row()
                val stack = plan.inputLiquids[i]
                if (stack.liquid == null) continue
                if (stack.liquid.unlockedNow()) {
                    req.right()
                    req.add(
                        displayLiquid(
                            stack.liquid,
                            stack.amount * (60f / plan.craftTime), true
                        ).right()
                            .right()
                    ).right()
                } else {
                    addLockIcon(req)
                }
            }
        }
    }

    /**
     * 统一添加锁图标（未解锁）
     */
    private fun addLockIcon(parent: Table) {
        parent.image(Icon.lock).color(Pal.darkerGray).size(40f)
    }

    //    @Override
    //    public TextureRegion[] icons(){
    //        return new TextureRegion[]{region, outRegion, topRegion};
    //    }
    override fun drawPlanRegion(plan: BuildPlan, list: Eachable<BuildPlan?>?) {
        Draw.rect(region, plan.drawx(), plan.drawy())
        //        Draw.rect(outRegion, plan.drawx(), plan.drawy(), plan.rotation * 90);
//        Draw.rect(topRegion, plan.drawx(), plan.drawy());
    }

    override fun getPlanConfigs(options: Seq<UnlockableContent?>) {
        for (plan in plans) {
            plan.eachOutput { o: UnlockableContent ->
                if (!o.isBanned) {
                    options.add(o)
                }
            }
        }
    }

    class ProductPlan() {
        var outputItems: Array<ItemStack> = ItemStack.empty
        var outputLiquids: Array<LiquidStack> = LiquidStack.empty
        var inputItems: Array<ItemStack> = ItemStack.empty
        var inputLiquids: Array<LiquidStack> = LiquidStack.empty
        var liquidOutputDirections: IntArray = intArrayOf(-1)
        var dumpExtraLiquid: Boolean = true
        var ignoreLiquidFullness: Boolean = false
        var craftTime: Float = 80f
        var craftEffect: Effect = Fx.none
        var updateEffect: Effect = Fx.none
        var updateEffectChance: Float = 0.04f
        var updateEffectSpread: Float = 4f
        var warmupSpeed: Float = 0.019f

        @NoPatch
        var legacyReadWarmup: Boolean = false

        var drawer: DrawBlock = DrawDefault()

        var powerPerTick: Float = 0f

        var hasOutputItem: Boolean = false
        var hasOutputLiquid: Boolean = false
        var hasInputItem: Boolean = false
        var hasInputLiquid: Boolean = false
        var hasPower: Boolean = false
        var output: UnlockableContent? = null


        override fun toString(): String {
            return "ProductPlan{" +
                    "outputItems=" + outputItems.contentToString() +
                    ", outputLiquids=" + outputLiquids.contentToString() +
                    ", inputItems=" + inputItems.contentToString() +
                    ", inputLiquids=" + inputLiquids.contentToString() +
                    ", liquidOutputDirections=" + liquidOutputDirections.contentToString() +
                    ", dumpExtraLiquid=" + dumpExtraLiquid +
                    ", ignoreLiquidFullness=" + ignoreLiquidFullness +
                    ", craftTime=" + craftTime +
                    ", craftEffect=" + craftEffect +
                    ", updateEffect=" + updateEffect +
                    ", updateEffectChance=" + updateEffectChance +
                    ", updateEffectSpread=" + updateEffectSpread +
                    ", warmupSpeed=" + warmupSpeed +
                    ", drawer=" + drawer +
                    ", powerPerTick=" + powerPerTick +
                    '}'
        }

        constructor(
            outputItem: ItemStack,
            time: Float,
            inputItems: Array<ItemStack>,
            power: Float
        ) : this() {
            this.outputItems = ItemStack.with(outputItem)
            this.craftTime = time
            this.inputItems = inputItems
            this.powerPerTick = power
            cache()
        }

        constructor(
            outputItems: Array<ItemStack>,
            time: Float,
            inputItems: Array<ItemStack>,
            power: Float
        ) : this() {
            this.outputItems = outputItems
            this.craftTime = time
            this.inputItems = inputItems
            this.powerPerTick = power
            cache()
        }

        constructor(
            outputLiquid: LiquidStack,
            time: Float,
            inputLiquids: Array<LiquidStack>,
            power: Float
        ) : this() {
            this.outputLiquids = LiquidStack.with(outputLiquid)
            this.craftTime = time
            this.inputLiquids = inputLiquids
            this.powerPerTick = power
            cache()
        }

        constructor(
            outputLiquids: Array<LiquidStack>,
            time: Float,
            inputLiquids: Array<LiquidStack>,
            power: Float
        ) : this() {
            this.outputLiquids = outputLiquids
            this.craftTime = time
            this.inputLiquids = inputLiquids
            this.powerPerTick = power
            cache()
        }

        fun cache() {
            this.hasOutputItem = outputItems.isNotEmpty()
            this.hasOutputLiquid = outputLiquids.isNotEmpty()
            this.hasInputItem = inputItems.isNotEmpty()
            this.hasInputLiquid = inputLiquids.isNotEmpty()
            this.output =
                if (hasOutputItem) outputItems[0].item else if (hasOutputLiquid) outputLiquids[0].liquid else null
            this.hasPower = powerPerTick > 0 // TODO 支持发电
        }

        fun eachOutput(func: Cons<UnlockableContent>) {
            if (hasOutputItem) {
                for (outputItem in outputItems) {
                    func.get(outputItem.item)
                }
            }
            if (hasOutputLiquid) {
                for (outputLiquid in outputLiquids) {
                    func.get(outputLiquid.liquid)
                }
            }
        }

        fun eachInput(func: Cons<UnlockableContent>) {
            if (hasInputItem) {
                for (outputItem in inputItems) {
                    func.get(outputItem.item)
                }
            }
            if (hasInputLiquid) {
                for (outputLiquid in inputLiquids) {
                    func.get(outputLiquid.liquid)
                }
            }
        }

        fun applyPlan(build: MultiFactoryBuild) {
            build.outputItems = outputItems
            build.outputLiquids = outputLiquids
            build.liquidOutputDirections = liquidOutputDirections
            build.dumpExtraLiquid = dumpExtraLiquid
            build.ignoreLiquidFullness = ignoreLiquidFullness
            build.craftTime = craftTime
            build.craftEffect = craftEffect
            build.updateEffect = updateEffect
            build.updateEffectChance = updateEffectChance
            build.updateEffectSpread = updateEffectSpread
            build.warmupSpeed = warmupSpeed
            build.legacyReadWarmup = legacyReadWarmup
            build.drawer = drawer
        }
    }

    inner class MultiFactoryBuild : GenericCrafterBuild() {
//        @Nullable
        lateinit var outputItems: Array<ItemStack>

//        @Nullable
        lateinit var outputLiquids: Array<LiquidStack>
        var liquidOutputDirections: IntArray = intArrayOf(-1)
        var dumpExtraLiquid: Boolean = true
        var ignoreLiquidFullness: Boolean = false
        var craftTime: Float = 80f
        var craftEffect: Effect = Fx.none
        var updateEffect: Effect = Fx.none
        var updateEffectChance: Float = 0.04f
        var updateEffectSpread: Float = 4f
        var warmupSpeed: Float = 0.019f

        @NoPatch
        var legacyReadWarmup: Boolean = false
        var drawer: DrawBlock = DrawDefault()

        //        public @Nullable Vec2 commandPos;
        //        public @Nullable UnitCommand command;
        var time: Float = 0f
        var speedScl: Float = 0f
        var currentPlan: Int = -1

        fun fraction(): Float {
            return if (currentPlan == -1) 0f else progress / plans.get(currentPlan).craftTime
        }

        //        public boolean canSetCommand(){
        //            var output = unit();
        //            return output != null && output.commands.size > 1 && output.allowChangeCommands &&
        //                    //to avoid cluttering UI, don't show command selection for "standard" units that only have two commands.
        //                    !(output.commands.size == 2 && output.commands.get(1) == UnitCommand.enterPayloadCommand);
        //        }
        override fun created() {
            //auto-set to the first plan, it's better than nothing.
            if (currentPlan == -1) {
                currentPlan = plans.indexOf { u ->
                    if (u.hasOutputItem) {
                        for (stack in u.outputItems) {
                            if (!stack.item.unlockedNow()) return@indexOf false
                        }
                    }
                    if (u.hasOutputLiquid) {
                        for (stack in u.outputLiquids) {
                            if (!stack.liquid.unlockedNow()) return@indexOf false
                        }
                    }
                    if (u.hasInputItem) {
                        for (stack in u.inputItems) {
                            if (!stack.item.unlockedNow()) return@indexOf false
                        }
                    }
                    if (u.hasInputLiquid) {
                        for (stack in u.inputLiquids) {
                            if (!stack.liquid.unlockedNow()) return@indexOf false
                        }
                    }
                    true
                }
            }
        }


        override fun drawSelect() {
            super.drawSelect()
            val options = Seq<UnlockableContent>()
            for (plan in plans) {
                plan.eachOutput { o: UnlockableContent ->
                    if (!o.isBanned) {
                        options.add(o)
                    }
                }
            }
            if (plans.size > 0 && currentPlan != -1 && currentPlan < plans.size) {
                drawItemSelection(plans.get(currentPlan).output)
            }
        }

        //        @Override
        //        public Vec2 getCommandPosition(){
        //            return commandPos;
        //        }
        //
        //        @Override
        //        public void onCommand(Vec2 target){
        //            commandPos = target;
        //        }
        override fun senseObject(sensor: LAccess): Any? {
            if (sensor == LAccess.config) return if (currentPlan == -1) null else plans.get(currentPlan).output
            return super.senseObject(sensor)
        }

        override fun sense(sensor: LAccess): Double {
            if (sensor == LAccess.progress) return Mathf.clamp(fraction()).toDouble()
            if (sensor == LAccess.itemCapacity) return Mathf.round(itemCapacity * Vars.state.rules.unitCost(team))
                .toDouble()
            return super.sense(sensor)
        }

        override fun buildConfiguration(table: Table) {
            val units = Seq.with(plans)
                .map<UnlockableContent> { p -> p.output }
                .select { obj -> Objects.nonNull(obj) } // 先过滤空值
                .retainAll { u -> u.unlockedNow() && !u.isBanned }

            if (units.any()) {
                ItemSelection.buildTable(
                    this@MultiFactory, table, units,
                    { if (currentPlan == -1) null else plans.get(currentPlan).output },
                    { value -> this.configure(value) },
                    selectionRows, selectionColumns
                )
                table.row()

                val commands = Table()
                commands.top().left()

                //                Runnable rebuildCommands = () -> {
//                    commands.clear();
//                    commands.background(null);
//                    var unit = unit();
//                    if(unit != null && canSetCommand()){
//                        commands.background(Styles.black6);
//                        var group = new ButtonGroup<ImageButton>();
//                        group.setMinCheckCount(0);
//                        int i = 0, columns = Mathf.clamp(units.size, 2, selectionColumns);
//                        var list = unit.commands;
//
//                        commands.image(Tex.whiteui, Pal.gray).height(4f).growX().colspan(columns).row();
//
//                        for(var item : list){
//                            ImageButton button = commands.button(item.getIcon(), Styles.clearNoneTogglei, 40f, () -> {
//                                configure(item);
//                            }).tooltip(item.localized()).group(group).get();
//
//                            button.update(() -> button.setChecked(command == item || (command == null && unit.defaultCommand == item)));
//
//                            if(++i % columns == 0){
//                                commands.row();
//                            }
//                        }
//
//                        if(list.size < columns){
//                            for(int j = 0; j < (columns - list.size); j++){
//                                commands.add().size(40f);
//                            }
//                        }
//                    }
//                };
//
//                rebuildCommands.run();

                //Since the menu gets hidden when a new unit is selected, this is unnecessary.
                /*
                UnitType[] lastUnit = {unit()};

                commands.update(() -> {
                    if(lastUnit[0] != unit()){
                        lastUnit[0] = unit();
                        rebuildCommands.run();
                    }
                });*/
                table.row()

                table.add(commands).fillX().left()
            } else {
                table.table(Styles.black3) { t -> t.add("@none").color(Color.lightGray) }
            }
        }


        override fun acceptPayload(source: Building, payload: Payload): Boolean {
            return false
        }

        override fun display(table: Table) {
            super.display(table)

            val reg = TextureRegionDrawable()

            table.row()
            table.table { t ->
                t.left()
                t.image().update { i ->
                    i.setDrawable(if (currentPlan == -1) Icon.cancel else plans.get(currentPlan).output?.let { reg.set(it.uiIcon) })
                    i.setScaling(Scaling.fit)
                    i.setColor(if (currentPlan == -1) Color.lightGray else Color.white)
                }.size(32f).padBottom(-4f).padRight(2f)
                t.label { if (currentPlan == -1) "@none" else plans.get(currentPlan).output?.localizedName ?: "@none" }
                    .wrap().width(230f).color(
                        Color.lightGray
                    )
            }.left()
        }

        override fun config(): Any {
            return currentPlan
        }

        override fun draw() {
            super.draw()
            Draw.rect(region, x, y)

            //            Draw.rect(outRegion, x, y, rotdeg());
//
//            if(currentPlan != -1){
//                ProductPlan plan = plans.get(currentPlan);
//                Draw.draw(Layer.blockOver, () -> Drawf.construct(this, plan.output, rotdeg() - 90f, progress / plan.craftTime, speedScl, time));
//            }

//            Draw.z(Layer.blockOver);

//            payRotation = rotdeg();
//            drawPayload();

//            Draw.z(Layer.blockOver + 0.1f);

//            Draw.rect(topRegion, x, y);
        }

        override fun updateTile() {
//            super.updateTile();
            if (!configurable) {
                currentPlan = 0
            }

            if (currentPlan < 0 || currentPlan >= plans.size) {
//                currentPlan = -1;
                currentPlan = 0
            }

            val plan = plans.get(currentPlan)

            if (efficiency > 0 && currentPlan != -1) {
                time += edelta() * speedScl
                progress += 1.0f / plan.craftTime * this.edelta()
                warmup = Mathf.approachDelta(warmup, warmupTarget(), warmupSpeed)
                speedScl = Mathf.lerpDelta(speedScl, 1f, 0.05f)

                //                //continuously output based on efficiency
//                if(outputLiquids != null){
//                    float inc = getProgressIncrease(1f);
//                    for(var output : outputLiquids){
//                        handleLiquid(this, output.liquid, Math.min(output.amount * inc, liquidCapacity - liquids.get(output.liquid)));
//                    }
//                }
                if (wasVisible && Mathf.chanceDelta(updateEffectChance.toDouble())) {
                    updateEffect.at(
                        x + Mathf.range(size * updateEffectSpread),
                        y + Mathf.range(size * updateEffectSpread)
                    )
                }
            } else {
                speedScl = Mathf.lerpDelta(speedScl, 0f, 0.05f)
                warmup = Mathf.approachDelta(warmup, 0f, warmupSpeed)
            }

            //TODO may look bad, revert to edelta() if so
            totalProgress += warmup * Time.delta


            if (currentPlan != -1) {
                //make sure to reset plan when the unit got banned after placement
                plan.output?.let {
                    if (it.isBanned) {
                        currentPlan = -1
                        return
                    }
                }

                if (progress >= 1f) {
                    consume()
                    if (plan.hasOutputItem) {
                        for (output in plan.outputItems) {
                            for (i in 0..<output.amount) {
                                if (items.get(output.item) < itemCapacities[output.item.id.toInt()]) {
                                    items.add(output.item, 1)
                                    dump(output.item)
                                }
                            }
                        }
                    }
                    if (plan.hasOutputLiquid) {
                        for (i in plan.outputLiquids.indices) {
                            val liquid = plan.outputLiquids[i].liquid
                            val amount = plan.outputLiquids[i].amount

                            handleLiquid(this, liquid, amount)

                            val outputDir =
                                if (plan.liquidOutputDirections.size > i) plan.liquidOutputDirections[i] else -1


                            if (timer(timerDump, dumpTime / timeScale)) {
                                dumpLiquid(liquid, 0.9f, outputDir)
                            }
                        }
                    }

                    if (wasVisible) {
                        craftEffect.at(x, y)
                    }
                    progress %= 1f


                    //                    if(outputItems != null){
//                        for(var output : outputItems){
//                            for(int i = 0; i < output.amount; i++){
//                                offload(output.item);
//                            }
//                        }
//                    }

//                    if (outputLiquids != null){
//                        for(var output : outputLiquids){
//                            handleLiquid(this, output.liquid, -output.amount);
//                        }
//                    }


//                    if(outputItems != null && timer(timerDump, dumpTime / timeScale)){
//                        for(ItemStack output : outputItems){
//                            dump(output.item);
//                        }
//                    }
//
//                    if(outputLiquids != null){
//                        for(int i = 0; i < outputLiquids.length; i++){
//                            int outputDir = liquidOutputDirections.length > i ? liquidOutputDirections[i] : -1;
//
                    /**                            dumpLiquid(outputLiquids[i].liquid, 1f, dir); */
//                            Liquid liquid = outputLiquids[i].liquid;
//
//                            int dump = this.cdump;
//                            if (!(this.liquids.get(liquid) <= 1.0E-4F)) {
//                                if (!Vars.net.client() && Vars.state.isCampaign() && this.team == Vars.state.rules.defaultTeam) {
//                                    liquid.unlock();
//                                }
//
//                                for(int j = 0; j < this.proximity.size; ++j) {
//                                    this.incrementDump(this.proximity.size);
//                                    Building other = (Building)this.proximity.get((j + dump) % this.proximity.size);
//                                    if (outputDir == -1 || (outputDir + this.rotation) % 4 == this.relativeTo(other)) {
//                                        other = other.getLiquidDestination(this, liquid);
//                                        if (other != null && other.block.hasLiquids && this.canDumpLiquid(other, liquid) && other.liquids != null) {
//                                            float ofract = other.liquids.get(liquid) / other.block.liquidCapacity;
//                                            float fract = this.liquids.get(liquid) / this.block.liquidCapacity;
//                                            this.transferLiquid(other, (fract - ofract) * this.block.liquidCapacity, liquid);
//
//                                        }
//                                    }
//                                }
//
//                            }
//                        }
//                    }
                }

                //                progress = Mathf.clamp(progress, 0, plan.craftTime);
            } else {
                progress = 0f
            }

            dumpOutputs()
        }

        override fun shouldConsume(): Boolean {
            if (currentPlan == -1) return false
            dumpOutputs()
            val plan = plans.get(currentPlan)

            if (plan.hasOutputItem) {
                for (output in plan.outputItems) {
                    if (items.get(output.item) + output.amount > itemCapacities[output.item.id.toInt()]) {
                        return false
                    }
                }
            }

            if (plan.hasOutputLiquid && !ignoreLiquidFullness) {
                var allFull = true
                for (output in plan.outputLiquids) {
                    if (liquids.get(output.liquid) >= liquidCapacity - 0.001f) {
                        if (!dumpExtraLiquid) {
                            return false
                        }
                    } else {
                        //if there's still space left, it's not full for all liquids
                        allFull = false
                    }
                }

                //if there is no space left for any liquid, it can't reproduce
                if (allFull) {
                    return false
                }
            }

            return enabled
        }

        override fun dumpOutputs() {
            val plan = plans.get(min(currentPlan, plans.size - 1))
            if (plan.hasOutputItem && timer(timerDump, dumpTime / timeScale)) {
                for (output in plan.outputItems) {
                    dump(output.item)
                }
            }

            if (plan.hasOutputLiquid) {
                for (i in plan.outputLiquids.indices) {
                    val dir = if (plan.liquidOutputDirections.size > i) plan.liquidOutputDirections[i] else -1

                    dumpLiquid(plan.outputLiquids[i].liquid, 2f, dir)
                }
            }
        }

        override fun status(): BlockStatus {
            if (!team.activateUnitFactories()) return BlockStatus.inactive
            return super.status()
        }

        override fun getMaximumAccepted(item: Item): Int {
            return Mathf.round(itemCapacities[item.id.toInt()].toFloat())
        }

        override fun acceptItem(source: Building, item: Item): Boolean {
            return hasItems && currentPlan != -1 && plans.get(currentPlan).hasInputItem && items.get(item) < getMaximumAccepted(
                item
            ) &&
                    Structs.contains(
                        plans.get(currentPlan).inputItems
                    ) { stack: ItemStack -> stack.item === item }
        }

        override fun acceptLiquid(source: Building, liquid: Liquid): Boolean {
            return hasLiquids && currentPlan != -1 && plans.get(currentPlan).hasInputLiquid && liquids.get(liquid) < liquidCapacities[liquid.id.toInt()] &&
                    Structs.contains(
                        plans.get(currentPlan).inputLiquids
                    ) { stack: LiquidStack -> stack.liquid === liquid }
        }

        override fun version(): Byte {
            return 4
        }

        override fun write(write: Writes) {
            super.write(write)
            write.f(progress)
            write.s(currentPlan)
            //            TypeIO.writeVecNullable(write, commandPos);
//            TypeIO.writeCommand(write, command);
        }

        override fun read(read: Reads, revision: Byte) {
            super.read(read, revision)
            progress = read.f()
            currentPlan = read.s().toInt()
            if (revision >= 4) {
                plans.get(min(currentPlan, plans.size - 1)).applyPlan(this)
            }

            //            if(revision >= 2){
//                commandPos = TypeIO.readVecNullable(read);
//            }
//
//            if(revision >= 3){
//                command = TypeIO.readCommand(read);
//            }
        }
    }

    companion object {
        fun displayItem(item: Item, amount: Int, timePeriod: Float, showName: Boolean): Table {
            val t = Table()
            t.add(StatValues.stack(item, amount, !showName))
            t.add(
                (if (showName) item.localizedName + "\n" else "") + "[lightgray]" + Strings.autoFixed(
                    amount / (timePeriod / 60f),
                    1
                ) + StatUnit.perSecond.localized()
            ).padLeft(2f).padRight(
                (3 + (if (amount != 0) (Strings.autoFixed(
                    amount.toFloat(),
                    1
                ).length - 1) * 5 else 0)).toFloat()
            ).style(
                Styles.outlineLabel
            )
            return t
        }

        fun displayLiquid(liquid: Liquid, amount: Float, perSecond: Boolean): Table {
            val t = Table()

            t.add(object : Stack() {
                init {
                    add(Image(liquid.uiIcon).setScaling(Scaling.fit))

                    if (amount != 0f) {
                        val t = Table().left().bottom()
                        t.add(Strings.autoFixed(amount, 1)).style(Styles.outlineLabel)
                        add(t)
                    }
                }
            }).size(Vars.iconMed)
                .padRight((3 + (if (amount != 0f) (Strings.autoFixed(amount, 1).length - 1) * 5 else 0)).toFloat())
                .with { s: Stack -> StatValues.withTooltip(s, liquid, false) }

            t.add(liquid.localizedName + "\n" + "[lightgray]" + if (perSecond) StatUnit.perSecond.localized() else "" ).padLeft(2f)
                .padRight((3 + (if (amount != 0f) (Strings.autoFixed(amount, 1).length - 1) * 5 else 0)).toFloat())
                .style(
                    Styles.outlineLabel
                )

            //        if(perSecond && amount != 0){
//
//            t.add(StatUnit.perSecond.localized()).padLeft(2).padRight(5).color(Color.lightGray).style(Styles.outlineLabel);
//        }

//        t.add(liquid.localizedName);
            return t
        }
    }
}
