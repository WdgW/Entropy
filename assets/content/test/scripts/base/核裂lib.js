exports.modName = "矩阵能源";
exports.mod = Vars.mods.locateMod(exports.modName);

exports.addToResearch = (content, research) => {
	if (!content) {
		throw new Error('content is null!');
	}
	if (!research.parent) {
		throw new Error('research.parent is empty!');
	}
	let researchName = research.parent;
	let customRequirements = research.requirements;
	let objectives = research.objectives;
	let lastNode = TechTree.all.find(boolf(t => t.content == content));
	if (lastNode != null) {
		lastNode.remove();
	}
	let node = new TechTree.TechNode(null, content, customRequirements !== undefined ? customRequirements : content.researchRequirements());
	let currentMod = exports.mod;
	if (objectives) {
		node.objectives.addAll(objectives);
	}
	if (node.parent != null) {
		node.parent.children.remove(node);
	}
	// find parent node.
	let parent = TechTree.all.find(boolf(t => t.content.name.equals(researchName) || t.content.name.equals(currentMod.name + "-" + researchName)));
	if (parent == null) {
		throw new Error("Content '" + researchName + "' isn't in the tech tree, but '" + content.name + "' requires it to be researched.");
	}
	// add this node to the parent
	if (!parent.children.contains(node)) {
		parent.children.add(node);
	}
	// reparent the node
	node.parent = parent;
}

exports.loadRegion = (name) => {
	return Core.atlas.find(exports.modName + "-" + name)
}

exports.newBlock = (name, con) => {
	const b = extend(Block, name, con);
	b.solid = b.update = b.destructible = b.configurable = true;
	b.drawDisabled = false;
	b.envEnabled = Env.any;
	b.group = BlockGroup.none;
	b.priority = TargetPriority.base;
	b.category = Category.effect;
	b.buildVisibility = BuildVisibility.shown;
	return b
}

exports.setBuilding = (building, block, con) => {
	block.buildType = prov(() => {
		if (building == Building) {
			return extend(building, con);
		} else {
			return extend(building, block, con);
		}
	})
}

exports.bundle = (text, num) => {
	if (num) {
		return Core.bundle.format(text, num);
	} else {
		return Core.bundle.get(text);
	}
}

exports.AngleTrns = (ang, rad, rad2) => {
	if (rad2) {
		return {
			x: Angles.trnsx(ang, rad, rad2),
			y: Angles.trnsy(ang, rad, rad2)
		}
	} else {
		return {
			x: Angles.trnsx(ang, rad),
			y: Angles.trnsy(ang, rad)
		}
	}
}

function Halo(obj) {
	let def = {
		fuc: {},
		mirror: false,
		shapeR: 0,
		shapes: 2,
		radius: 4,
		triL: 20,
		haloRad: 50,
		haloRot: 0,
		haloRS: 0,
		color: Color.valueOf("FF5845"),
		colorTo: Color.valueOf("FF6666")
	}
	for (let k in def) {
		if (obj[k] == undefined) continue
		def[k] = obj[k]
	}
	let p = extend(HaloPart, def.fuc)
	p.mirror = def.mirror;
	p.tri = true;
	p.shapeRotation = def.shapeR;
	p.shapes = def.shapes;
	p.radius = def.radius;
	p.triLength = def.triL;
	p.haloRadius = def.haloRad;
	p.haloRotation = def.haloRot,
	p.haloRotateSpeed = def.haloRS;
	p.color = def.color;
	p.colorTo = def.colorTo;
	p.layer = 110;
	return p
}
exports.Halo = Halo;

exports.DoubleHalo = (unit, obj) => {
	unit.parts.add(
	(() => {
		let h = Halo({
			mirror: obj.mirror,
			shapes: obj.shapes,
			radius: obj.radius,
			triL: obj.triL,
			haloRad: obj.haloRad,
			haloRot: obj.haloRot,
			haloRS: obj.haloRS,
			color: obj.color,
			colorTo: obj.colorTo
		})
		return h
	})(), (() => {
		let h = Halo({
			mirror: obj.mirror,
			shapeR: 180,
			shapes: obj.shapes,
			radius: obj.radius,
			triL: obj.triL / 4,
			haloRad: obj.haloRad,
			haloRot: obj.haloRot,
			haloRS: obj.haloRS,
			color: obj.color,
			colorTo: obj.colorTo
		})
		return h
	})())
}