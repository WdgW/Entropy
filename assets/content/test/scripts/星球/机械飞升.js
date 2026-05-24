const lib = require("base/矩阵能源lib");

const 机械飞升 = new JavaAdapter(Planet, {
    load() {
        this.meshLoader = prov(() => new HexMesh(机械飞升, 5));
        this.super$load();
    }
}, "机械飞升", Planets.sun, 1);
机械飞升.generator = new SerpuloPlanetGenerator();
const sS = require("sectorSize");
sS.planetGrid(机械飞升, 3.3);
//机械飞升.defaultCore = "";
机械飞升.allowLaunchSchematics = true
机械飞升.atmosphereColor = Color.valueOf("#00b4ff");
机械飞升.atmosphereRadIn = 0.05;
机械飞升.atmosphereRadOut = 0.5;
机械飞升.localizedName = "机械飞升";
机械飞升.visible = true;
机械飞升.bloom = false;
机械飞升.accessible = true;
机械飞升.alwaysUnlocked = true;
机械飞升.startSector = 3;
机械飞升.orbitRadius = 200;

机械飞升.hiddenItems.addAll(
Items.copper,Items.lead,
);

const map = new SectorPreset("机械迫降", 机械飞升, 4);
map.captureWave = 100;
map.alwaysUnlocked = true;
map.difficulty = 3
map.addStartingItems = true;
map.localizedName = "机械迫降"//葱域

const map1 = new SectorPreset("好生存", 机械飞升, 12);
map1.captureWave = 150;
map1.difficulty = 3
map1.addStartingItems = true;
map1.alwaysUnlocked = true;
map1.localizedName = "简单生存"//绿色要塞

exports.机械飞升 = 机械飞升;