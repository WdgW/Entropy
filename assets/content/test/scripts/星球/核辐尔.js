var myItem = require("物品");
const lib = require("base/矩阵能源lib");
//实例化一个核心
/*var 核心 = new CoreBlock("核心");
核心.size = 4;
核心.category = Category.effect;
核心.buildVisibility = BuildVisibility.shown;
核心.requirements = ItemStack.with(myItem.碳钢板, 1);
*/
//实例化一个星球
// 创建星球
let 核辐尔 =  new Planet("核辐尔", Planets.sun, 1, 4);
// generator: 区块生成器,这里借用Serpulo的,具体待研究
核辐尔.generator = new ErekirPlanetGenerator();
        
    // public HexSkyMesh(Planet planet, int seed, float speed, float radius, int divisions, Color color, int octaves, float persistence, float scl, float thresh)
    
    // 发射时核心携带资源倍率
核辐尔.launchCapacityMultiplier = 1;
核辐尔.sectorSeed = -1; // 区块生成种子
核辐尔.allowWaves = true; // 是否允许生成区块波次
核辐尔.allowWaveSimulation = true; // 是否允许后台自动挂波次
核辐尔.allowSectorInvasion = true; // 是否允许区块被敌人进攻
核辐尔.allowLaunchSchematics = true; // 是否允许使用核心蓝图
核辐尔.enemyBuildSpeedMultiplier = 1; // 敌人建筑倍率 这里不做处理
 核辐尔.enemyCoreSpawnReplace = true; // 敌人最后的核心被摧毁后是否要生成一个出怪点
核辐尔.allowLaunchLoadout = true; // 是否允许发射时携带物资
核辐尔.clearSectorOnLose = false; // 区块输了是否重置区块(一次过是吧)
核辐尔.tidalLock = false; // 是否潮汐锁定(冷知识: erekir潮汐锁定)
核辐尔.prebuildBase = true; // 是否需要像e星那样帅气的着陆建筑特效
    
    // ruleSetter: 当区块地图加载时会自动将地图规则作以下调整
    /*ruleSetter: r => {
        r.waveTeam = Team.crux;
        r.placeRangeCheck = false; // 恶心的建筑区
        r.showSpawns = true; // 在地图上不显示出怪点
    },*/
    
核辐尔.iconColor = Color.valueOf("146B00FF"); // 星球面板(PlanetDialog)上的颜色
    
    // 环境
核辐尔.atmosphereColor = Color.valueOf("146B00FF"); // 环境色
核辐尔.atmosphereRadIn = 0.01;
核辐尔.atmosphereRadOut = 0.3;
    
    // 光照
核辐尔.updateLighting = true; // 是否有昼夜更替
核辐尔.lightSrcFrom = 0; // 光照数值 这里不做处理
核辐尔.lightSrcTo = 0.8;
核辐尔.lightDstFrom = 0.2;
核辐尔.lightDstTo = 1;
    
核辐尔.startSector = 15; // 开始区块(类似零号地区) (这个的查看可以用我的mod<显示星球区块id>)
    
核辐尔.alwaysUnlocked = true;// 是否默认解锁
    
核辐尔.landCloudColor = Object.assign(Pal.spore.cpy(), {a: 0.5});// 核心着陆烟尘的颜色
    
    //defaultCore: 核心, // 默认的核心 这里不做处理

/*核辐尔.init(){
        this.super$init();
        
        // meshLoader, cloudMeshLoader 渲染作用,这里借用Serpulo的,具体待研究
        this.meshLoader = () => new HexMesh(this, 6);
        this.cloudMeshLoader = () => new MultiMesh(
            new HexSkyMesh(this, 11, 0.15, 0.13, 5, Object.assign(new Color().set(Pal.spore).mul(0.9), {a: 0.75}), 2, 0.45, 0.9, 0.38),
            new HexSkyMesh(this, 1, 0.6, 0.16, 5, Object.assign(Color.white.cpy().lerp(Pal.spore, 0.55), {a: 0.75}), 2, 0.45, 1, 0.41)
        );*/
        
核辐尔.hiddenItems.addAll(
myItem.铁,myItem.碳钢板,myItem.铅碳混合物,
myItem.储液罐,myItem.铀棒,myItem.铀,
myItem.石油罐,myItem.水罐,myItem.矿渣罐,
myItem.TNT,myItem.火药,myItem.硫磺,
myItem.铜矿石,myItem.黄铜,myItem.黄铁,
Items.copper,Items.lead,
), // 在星球上隐藏的物品,会决定该星球上能建造的方块
核辐尔.unlockedOnLand.addAll(); // 着陆该星球就解锁某些物品



核辐尔.meshLoader = prov(() => new SunMesh(核辐尔, 4, 6, 0.3, 1.8, 1.2, 1, 1.2, new Color(0.15, 0.3, 0.75, 0.5), new Color(0.15, 0.3, 0.75, 0.6), new Color(0.15, 0.3, 0.75, 0.7), Color(0.15, 0.3, 0.75, 0.8), new Color(0.15, 0.3, 0.75, 0.9), new Color(0.15, 0.3, 0.75, 1))
);
//核辐尔.meshLoader = () => new HexMesh(核辐尔 6);
/*核辐尔.cloudMeshLoader = () => new MultiMesh(
new HexSkyMesh(this, 11, 0.15, 0.13, 5, Object.assign(new Color().set(Pal.spore).mul(0.9), {a: 0.75}), 2, 0.45, 0.9, 0.38),
new HexSkyMesh(this, 1, 0.6, 0.16, 5, Object.assign(Color.white.cpy().lerp(Pal.spore, 0.55), {a: 0.75}), 2, 0.45, 1, 0.41)
);*/


核辐尔.cloudMeshLoader = prov(() => new MultiMesh(
new HexSkyMesh(核辐尔, 0.9, 0.06, 0.18, 6, new Color(0.25, 0.5, 0.75, 0.5), 1, 0.45, 1.2, 0.45),
new HexSkyMesh(核辐尔, 1, 0.09, 0.21, 3, new Color(0.5, 0.25, 1, 0.5), 2, 0.6, 1.5, 0.6)
));

核辐尔.bloom = 核辐尔.accessible = true;
//是否使用光晕/是否在行星列表显示/
核辐尔.rotateTime = 300*60;//自转时间
核辐尔.orbitRadius = 6; //公转半径
核辐尔.orbitTime = 8000 * 60; //公转一圈时间
核辐尔.lightColor = Color.valueOf("146B00FF");//光色
核辐尔.localizedName = "核辐尔";
exports.核辐尔 = 核辐尔;

// 创建主线区块
/**
    看源码SectorPreset.java:
        public SectorPreset(String name, Planet planet, int sector)
    可以知道SectorPreset的一个构造函数包含以下参数:
        name: 主线区块的name;
            这里的name决定主线区块地图的文件名字
            地图文件见<maps/>
        planet: 所在星球;
        sector: 所在区块id(这个的查看可以用我的mod<显示星球区块id>);
*/

/*let hfe = Object.assign(new SectorPreset("机械迫降", 核辐尔, 15), {
alwaysUnlocked: true, // 是否默认解锁
addStartingItems: true, //
isLastSector: false, // 是否是主线结尾区块 用于提示已经完成星球主线
overrideLaunchDefaults: true, // 可配合allowLaunchSchematics
allowLaunchSchematics: false, // 该区块是否能用核心蓝图 可配合overrideLaunchDefaults
attackAfterWaves: false, // 波次完成后是否启用进攻模式
noLighting: true, // 区块是否关闭昼夜更替
captureWave: 10, // 占领波数
difficulty: 1, // 难度
startWaveTimeMultiplier: 2,// 首波次准备时间倍率
});
hfe.localizedName = "1";
*/
// 3. 创建星球专属科技树 (这里偷懒塞一起了)
const nodeRoot = TechTree.nodeRoot;
const node = TechTree.node;

// public static TechNode nodeRoot(String name, UnlockableContent content, boolean requireUnlock, Runnable children)
/*核辐尔.techTree = nodeRoot("核辐尔", 核心, true, () => {
    node(myGroundZero, Seq.with(
        new Objectives.SectorComplete(SectorPresets.planetaryTerminal),
    ), () => {
    
    });
    
});*/
