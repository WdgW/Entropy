const library = require("base/library");
const 矩阵能源 = require("物品");
const 液体倾灌机 = library.MultiCrafter(GenericCrafter, GenericCrafter.GenericCrafterBuild, "液体倾灌机", [
  {
    input: {
      items: ["metaglass/3","titanium/1"],
      power: 5,
    },
    output: {
      items: ["矩阵能源-储液罐/1"],
    },
    craftTime: 60,
  }, 
  {
    input: {
      items: ["矩阵能源-储液罐/1"],
      liquids: ["slag/1"],
      power: 5,
    },
    output: {
      items: ["矩阵能源-矿渣罐/1"],
    },
    craftTime: 60,
  },
  {
    input: {
      items: ["矩阵能源-储液罐/1"],     
      liquids: ["oil/1"],
      power: 5
    },
    output: {
      items: ["矩阵能源-石油罐/1"],
    },
    craftTime: 60,
  },
  {
    input: {
      items: ["矩阵能源-储液罐/1"],     
      liquids: ["water/1"],
      power: 5
    },
    output: {
      items: ["矩阵能源-水罐/1"],
    },
    craftTime: 60,
  },
{
    input: {
      items: ["矩阵能源-水罐/1"],
      power: 5
    },
    output: {
      items: ["矩阵能源-储液罐/1"],
      liquids: ["water/1"],
    },
    craftTime: 60,
  },
{
    input: {
      items: ["矩阵能源-矿渣罐/1"],
      power: 5
    },
    output: {
      items: ["矩阵能源-储液罐/1"],
      liquids: ["slag/1"],
    },
    craftTime: 60,
  },
{
    input: {
      items: ["矩阵能源-石油罐/1"],
      power: 5
    },
    output: {
      items: ["矩阵能源-储液罐/1"],
      liquids: ["oil/1"],
    },
    craftTime: 60,
  },
  
  
]);