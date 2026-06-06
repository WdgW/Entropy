const myItem = require("物品");
require("方块");
require("沙盒");
require("星球");
require("提示");
//require("机械崛起");
//require("base/library");
//require("填充矿物");
//require("environment");

const 导弹 = new MissileUnitType("导弹-子弹");



//———————————————————————
//作者miner 
const defaultMinZoomLim = Vars.renderer.minZoom;
const defaultMaxZoomLim = Vars.renderer.maxZoom;
print("default min zoom: "+defaultMinZoomLim);
print("defaultn max zoom: "+defaultMaxZoomLim);
const minZoomLim = 0.3;
const maxZoomLim = 35;
const minZoom = 0.90;
const maxZoom = 30;
function resetZoomLim(toOriginal){
	if(toOriginal){
		Vars.renderer.minZoom = defaultMinZoomLim;
		Vars.renderer.maxZoom = defaultMaxZoomLim;} else {
		Vars.renderer.minZoom = minZoomLim;
		Vars.renderer.maxZoom = maxZoomLim;}}
function updateZoom(min, max){
	Vars.renderer.minZoom = min;
	Vars.renderer.maxZoom = max;}
if(!Vars.headless){	updateZoom(minZoomLim,maxZoomLim);}
//———————————————————————

let customMapDirectory = Vars.dataDirectory.child("maps/");
Log.info("______________");
Log.info(Json);
Events.on(EventType.ClientLoadEvent, e => {
    let _contentMap = Vars.content.getContentMap();
    for (let i = 0; i < _contentMap.length; i++){
        let _contents = _contentMap[i];
        for (let j = 0; j < _contents.size; j++){
			let content = _contents.get(j);
			Log.info(Json);
			Log.info(content);
			Log.info(Json.toJson(content));
		}
        
       
       
    }
});

