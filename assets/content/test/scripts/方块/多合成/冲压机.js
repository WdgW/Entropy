const library = require("base/library");
const myItems = require("物品");
const cyj = library.MultiCrafter(GenericCrafter, GenericCrafter.GenericCrafterBuild, "冲压机", [
{input: {
items: ["矩阵能源-铅碳混合物/2","矩阵能源-铁/1",],
power: 2,},
output: {
items: ["矩阵能源-碳钢板/1"],
},craftTime: 60,}, 



]);