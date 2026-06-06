function Colour(R, G, B, A) {
	if (G) {
		return new Color(R, G, B, A || 1);
	} else {
		return Color.valueOf(R);
	};
};
exports.Color = Colour;

function newItem(n, c, r, obj){
if(c) {
return exports[n] = new Item(n, Color.valueOf(c));
Object.assign([n], obj)
}else{
return exports[n] = extend(Item, n, {});
 }
}
exports.newItem = newItem;
/*
radioactivity : r,//放射性
flammability : f,//易燃性
explosiveness : e,//爆炸性
charge : ch,//放点性
cost : co,//一个物品增加建造时间
hardness: h,//硬度
*/