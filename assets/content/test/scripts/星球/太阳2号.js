
//实例化一个星球
// 创建星球
let 太阳2号 =  new Planet("太阳2号", null, 10, 0);
// generator: 区块生成器,这里借用Serpulo的,具体待研究
//太阳2号.generator = new SerpuloPlanetGenerator(),
        
    // public HexSkyMesh(Planet planet, int seed, float speed, float radius, int divisions, Color color, int octaves, float persistence, float scl, float thresh)
   
    
太阳2号.iconColor = Color.valueOf("F7FF00FF"), // 星球面板(PlanetDialog)上的颜色
    
    // 环境
太阳2号.atmosphereColor = Color.valueOf("F7FF00FF"), // 环境色
太阳2号.atmosphereRadIn = 0.01,
太阳2号.atmosphereRadOut = 0.3,
    
    // 光照
太阳2号.updateLighting = false, // 是否有昼夜更替
太阳2号.lightSrcFrom = 0, // 光照数值 这里不做处理
太阳2号.lightSrcTo = 0.8,
太阳2号.lightDstFrom = 0.2,
太阳2号.lightDstTo = 1,




太阳2号.meshLoader = prov(() => new SunMesh(太阳2号, 4, 6, 0.3, 1.8, 1.2, 1, 1.2, new Color(0.15, 0.3, 0.75, 0.5), new Color(0.15, 0.3, 0.75, 0.6), new Color(0.15, 0.3, 0.75, 0.7), Color(0.15, 0.3, 0.75, 0.8), new Color(0.15, 0.3, 0.75, 0.9), new Color(0.15, 0.3, 0.75, 1))
);
太阳2号.cloudMeshLoader = prov(() => new MultiMesh(
	new HexSkyMesh(太阳2号, 0.9, 0.06, 0.18, 6, new Color(0.25, 0.5, 0.75, 0.5), 1, 0.45, 1.2, 0.45),
	new HexSkyMesh(太阳2号, 1, 0.09, 0.21, 3, new Color(0.5, 0.25, 1, 0.5), 2, 0.6, 1.5, 0.6)
));

太阳2号.bloom = true;
太阳2号.accessible = true;
//是否使用光晕/是否在行星列表显示/
太阳2号.orbitRadius = 1000; //公转半径
太阳2号.lightColor = Color.valueOf("F7FF00FF");//光色
太阳2号.localizedName = "太阳2号";
exports.太阳2号 = 太阳2号;
