/**
 * @author guiY<guiYMOUR>
 * @Extra mod <https://github.com/guiYMOUR/mindustry-Extra-Utilities-mod>
 */
let urlLoader = Packages.java.net.URLClassLoader([Vars.mods.getMod(modName).file.file().toURI().toURL()], Vars.mods.mainLoader());
let getClass = function (name){
    return Packages.rhino.NativeJavaClass(Vars.mods.scripts.scope, urlLoader.loadClass(name));
}

const LiquidUnloader = getClass("lib.worlds.blocks.liquid.LiquidUnloader");

//use Java
const lu = new LiquidUnloader("liquid-unloader");
lu.speed = 6;
lu.health = 70;
lu.liquidCapacity = 10;
lu.requirements = ItemStack.with(
    Items.copper, 1
);
lu.buildVisibility = BuildVisibility.shown;
lu.category = Category.effect;
exports.lu = lu;