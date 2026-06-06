/**
 * @author guiY<guiYMOUR>
 * @Extra mod <https://github.com/guiYMOUR/mindustry-Extra-Utilities-mod>
 */
let urlLoader = Packages.java.net.URLClassLoader([Vars.mods.getMod(modName).file.file().toURI().toURL()], Vars.mods.mainLoader());
let getClass = function (name){
    return Packages.rhino.NativeJavaClass(Vars.mods.scripts.scope, urlLoader.loadClass(name));
}

const LiquidMassDriver = getClass("lib.worlds.blocks.liquid.LiquidMassDriver");

const driver = new LiquidMassDriver("ld");
driver.range = 55 * 8;
driver.reload = 150;
driver.knockback = 3;
driver.hasPower = true;
driver.consumePower(1.8);
driver.size = 2;
driver.liquidCapacity = 300;
driver.requirements = ItemStack.with(
    Items.copper, 1
);
driver.buildVisibility = BuildVisibility.shown;
driver.category = Category.liquid;

exports.driver = driver;