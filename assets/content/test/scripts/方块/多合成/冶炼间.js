const library = require("base/library");
const myItems = require("物品");
const ylj = library.MultiCrafter(GenericCrafter, GenericCrafter.GenericCrafterBuild, "冶炼间", [
{input: {
items: ["矩阵能源-铜矿石/1"],
power: 1.5,},
output: {
items: ["矩阵能源-黄铜/2","矩阵能源-黄铁/1"],
},craftTime: 55,}, 



]);