let loader = Vars.mods.mainLoader();
let scripts = Vars.mods.scripts;
let NativeJavaClass = Packages.rhino.NativeJavaClass;

function getClass(name){
	return  NativeJavaClass(scripts.scope, loader.loadClass(name));
};

exports.DawnCore = getClass("dawn.DawnCore");
exports.PowerItems = getClass("dawn.PowerItems");
exports.DawnDrill = getClass("dawn.DawnDrill");