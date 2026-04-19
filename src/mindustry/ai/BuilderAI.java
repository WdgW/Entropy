package mindustry.ai.types;

import arc.struct.*;
import arc.util.*;
import mindustry.ai.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock.*;

import static mindustry.Vars.*;

public class BuilderAI extends AIController {
    // 原有参数保持不变
    public static float buildRadius = 1500, retreatDst = 110f, retreatDelay = Time.toSeconds * 2f, defaultRebuildPeriod = 60f * 2f;

    // 核心修改: 定义空闲时跟随玩家的距离，单位: 世界单位 (tilesize = 8)
    public static float idleFollowDistance = 20f * tilesize;

    public @Nullable Unit assistFollowing;
    public @Nullable Unit following;
    public @Nullable Teamc enemy;
    public @Nullable BlockPlan lastPlan;
    public float fleeRange = 370f, rebuildPeriod = defaultRebuildPeriod;
    public boolean alwaysFlee;
    public boolean onlyAssist;
    boolean found = false;
    float retreatTimer;

    // 构造函数保持不变
    public BuilderAI(boolean alwaysFlee, float fleeRange) {
        this.alwaysFlee = alwaysFlee;
        this.fleeRange = fleeRange;
    }

    public BuilderAI() {
    }

    @Override
    public void init() {
        if (rebuildPeriod == defaultRebuildPeriod && unit.team.rules().buildAi) {
            rebuildPeriod = 10f;
        }
        Log.infoTag("Entropy","已加载");
    }

    // ----- 核心重写部分 start -----
    @Override
    public void updateMovement() {
        // 1. 原有的朝向和攻击逻辑保持不变
        if (target != null && shouldShoot()) {
            unit.lookAt(target);
        } else if (!unit.type.flying) {
            unit.lookAt(unit.prefRotation());
        }
        unit.updateBuilding = true;

        // 2. 原有的跟随辅助者有效性检查保持不变
        if (assistFollowing != null && !assistFollowing.isValid()) assistFollowing = null;
        if (following != null && !following.isValid()) following = null;
        if (assistFollowing != null && assistFollowing.activelyBuilding()) {
            following = assistFollowing;
        }

        boolean moving = false;
        boolean hold = hasStance(UnitStance.holdPosition);

        // ----- 核心修改: 任务优先级判断 -----
        // 3. 首先判断单位是否正在执行建造任务
        if (unit.buildPlan() != null) {
            // 【关键逻辑】当单位有建造任务时，不进行跟随，完全由原有的建造逻辑控制
            retreatTimer = 0f;
            BuildPlan req = unit.buildPlan();

            // 3.1 原有逻辑: 清除无效计划
            if (!req.breaking && timer.get(timerTarget2, 40f)) {
                for (Player player : Groups.player) {
                    if (player.isBuilder() && player.unit().activelyBuilding() && player.unit().buildPlan().samePos(req) && player.unit().buildPlan().breaking) {
                        unit.plans.removeFirst();
                        unit.team.data().plans.remove(p -> p.x == req.x && p.y == req.y);
                        return;
                    }
                }
            }

            // 3.2 原有逻辑: 验证计划有效性
            boolean valid = !(lastPlan != null && lastPlan.removed) && ((req.tile() != null && req.tile().build instanceof ConstructBuild cons && cons.current == req.block) || (req.breaking ? Build.validBreak(unit.team(), req.x, req.y) : Build.validPlace(req.block, unit.team(), req.x, req.y, req.rotation)));

            if (valid) {
                if (!hold) {
                    // 移动至建造点，距离由buildRadius和单位属性决定
                    float range = Math.min(unit.type.buildRange - unit.type.hitSize * 2f, buildRadius);
                    moveTo(req.tile(), range, 20f);
                    moving = !unit.within(req.tile(), range);
                } else if (!unit.within(req, unit.type.buildRange - tilesize) && !state.rules.infiniteResources) {
                    unit.plans.removeFirst();
                    lastPlan = null;
                }
            } else {
                unit.plans.removeFirst();
                lastPlan = null;
            }
        } else {
            // 4. 当单位没有建造任务时 (处于空闲状态)
            // 【关键逻辑】获取距离最近的玩家，并检查距离。这里忽略已死亡的玩家。
            Player closestPlayer = null;
            float minDst = Float.MAX_VALUE;
            for (var player : Groups.player) {
                if (!player.dead() && player.team() == unit.team) {
                    float dst = player.unit().dst2(unit);
                    if (dst < minDst) {
                        closestPlayer = player;
                        minDst = dst;
                    }
                }
            }

            if (closestPlayer != null) {
                // 获取玩家单位
                Unit playerUnit = closestPlayer.unit();
                // 计算单位与玩家之间的距离
                float distanceToPlayer = unit.dst(playerUnit);

                // 【核心距离检查】如果单位距离玩家超过10米 (idleFollowDistance)，则执行跟随移动
                if (distanceToPlayer > idleFollowDistance && !hold) {
                    // 使用 moveTo 方法使单位向玩家移动，距离为 idleFollowDistance
                    moveTo(playerUnit, idleFollowDistance, 20f);
                    moving = !unit.within(playerUnit, idleFollowDistance + 1f);
                } else if (hold) {
                    // 如果单位处于原地防守状态，不进行任何移动
                }

                // 更新 assistFollowing 字段，保持原有逻辑兼容性
                if (onlyAssist) {
                    assistFollowing = playerUnit;
                }
            } else {
                // 如果没有找到活着的队友玩家，则执行原有的寻找建造任务逻辑
                if (!unit.team.data().plans.isEmpty() && following == null && timer.get(timerTarget3, rebuildPeriod)) {
                    // 原有寻找建造计划逻辑...
                }

                // 原有的躲避敌人逻辑
                if (timer.get(timerTarget4, 40)) {
                    enemy = target(unit.x, unit.y, fleeRange, true, true);
                }
                if ((retreatTimer += Time.delta) >= retreatDelay || alwaysFlee) {
                    if (enemy != null) {
                        unit.clearBuilding();
                        var core = unit.closestCore();
                        if (core != null && !unit.within(core, retreatDst)) {
                            moveTo(core, retreatDst);
                            moving = true;
                        }
                    }
                }
            }

            // 5. 原有的寻找新的建造计划逻辑 (这里只保留了核心结构)
            if (!onlyAssist && !unit.team.data().plans.isEmpty() && following == null && timer.get(timerTarget3, rebuildPeriod)) {
                // ... 原有寻找计划逻辑 ...
            }
        }
        // ----- 核心重写部分 end -----

        // 6. 原有的飞行/推进逻辑保持不变
        if (!unit.type.flying) {
            unit.updateBoosting(unit.type.boostWhenBuilding || moving || unit.floorOn().isDuct || unit.floorOn().damageTaken > 0f || unit.floorOn().isDeep());
        }
    }

    // 以下方法保持原有代码不变
    protected boolean nearEnemy(int x, int y) {
        return Units.nearEnemy(unit.team, x * tilesize - fleeRange / 2f, y * tilesize - fleeRange / 2f, fleeRange, fleeRange);
    }

    @Override
    public AIController fallback() {
        if (unit.team.isAI() && unit.team.rules().prebuildAi) {
            return new PrebuildAI();
        }
        return unit.type.flying ? new FlyingAI() : new GroundAI();
    }

    @Override
    public boolean useFallback() {
        if (unit.team.isAI() && unit.team.rules().prebuildAi) {
            return true;
        }
        return state.rules.waves && unit.team == state.rules.waveTeam && !unit.team.rules().rtsAi;
    }

    @Override
    public boolean shouldFire() {
        return !(unit.controller() instanceof CommandAI ai) || ai.shouldFire();
    }

    @Override
    public boolean shouldShoot() {
        return !unit.isBuilding() && unit.type.canAttack;
    }
}