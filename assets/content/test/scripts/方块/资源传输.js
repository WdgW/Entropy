const range = 80;

function newUnloader(name) {
	const u = extend(Unloader, name, {
		drawPlace(x, y, rotation, valid) {
			this.super$drawPlace(x, y, rotation, valid);
			Drawf.dashCircle(x * Vars.tilesize + this.offset, y * Vars.tilesize + this.offset, range, Pal.accent);
		},
		outputsItems() {
			return false;
		},
		setStats() {
			this.super$setStats();
			this.stats.remove(Stat.speed);
			this.stats.add(Stat.range, range / Vars.tilesize, StatUnit.blocks);
		}
	});
	Object.assign(u, {
		size: 2,
		itemCapacity: 200,
		acceptsItems: true,
	});
	
	u.buildVisibility = BuildVisibility.shown;
	u.category = Category.effect;
	exports[name] = u;
	return u
}


let 范围传输器 = newUnloader("范围传输器");
范围传输器.buildType = prov(() => {
	let items = null, sortItem = null, core = null;
	return new JavaAdapter(Unloader.UnloaderBuild, {
		updateTile() {
			let items = this.items;
				sortItem = this.sortItem;
				core = this.team.core();
			if (sortItem != null && items.get(sortItem) < this.getMaximumAccepted(sortItem) && core.items.get(sortItem) > 0) {
				core.items.remove(sortItem, 1)
				items.add(sortItem, 1)
			}
			Vars.indexer.eachBlock(this, range, boolf(other => other.block instanceof GenericCrafter || other.block instanceof ItemTurret || other.block instanceof PowerTurret || other.block instanceof UnitFactory || other.block instanceof Reconstructor), cons(other => {
				if (sortItem == null) {
					let arr = Vars.content.items()
					for (let i = 0; i < arr.length; i++) {
						other.handleItem(this, arr[i]);
						Fx.itemTransfer.at(this.x, this.y, 2, arr[i].color, other);
						this.items.remove(arr[i], 1)
					}
				} else if (sortItem != null && items.get(sortItem) > 0 && other.acceptItem(this, sortItem)) {
					other.handleItem(this, sortItem);
					Fx.itemTransfer.at(this.x, this.y, 2, sortItem.color, other);
					this.items.remove(sortItem, 1)
				}
			}));
		},
		acceptItem(source, item) {
			return item == this.sortItem && this.items.get(item) < this.getMaximumAccepted(item)
		},
		drawConfigure() {
			sortItem = this.sortItem;
			this.super$drawConfigure();
			Vars.indexer.eachBlock(this, range, boolf(other => other.block instanceof GenericCrafter || other.block instanceof ItemTurret || other.block instanceof PowerTurret || other.block instanceof UnitFactory || other.block instanceof Reconstructor), cons(other => {
				if (sortItem != null && other.acceptItem(this, sortItem)) {
					Draw.color(sortItem.color);
					Lines.square(other.x, other.y, other.block.size * Vars.tilesize / 2 + 1, 45)
				}
			}));
			Drawf.dashCircle(this.x, this.y, range, Pal.accent);
		},
		draw() {
			this.super$draw();
			this.sortItem == null ? Draw.color(Color.white) : Draw.color(sortItem.color)
			Draw.alpha(Mathf.sin(0.05 * Time.time));
			Lines.square(this.x, this.y, 8 * Mathf.sin(0.025 * Time.time), 45);
		}
	}, 范围传输器);
});
let 范围收集器 = newUnloader("范围收集器");
范围收集器.buildType = prov(() => {
	let items = null, sortItem = null, core = null;
	return new JavaAdapter(Unloader.UnloaderBuild, {
		updateTile() {
			items = this.items;
			sortItem = this.sortItem;
			core = this.team.core();
			if (sortItem != null && items.get(sortItem) > 0 && core.items.get(sortItem) < core.getMaximumAccepted(sortItem)) {
				core.items.add(sortItem, 1)
				items.remove(sortItem, 1)
				//Fx.itemTransfer.at(this.x, this.y, 2, sortItem.color, core);
			}
			Vars.indexer.eachBlock(this, range, boolf(other => other.block instanceof GenericCrafter || other.block instanceof Drill || other.block instanceof BeamDrill || other.block instanceof WallCrafter), cons(other => {
				if (sortItem != null && other.items.get(sortItem) > 0 && this.acceptItem(other, sortItem)) {
					this.handleItem(other, sortItem);
					Fx.itemTransfer.at(other.x, other.y, 2, sortItem.color, this);
					other.items.remove(sortItem, 1);
				}
			}));
		},
		acceptItem(source, item) {
			return item == this.sortItem && this.items.get(item) < this.getMaximumAccepted(item)
		},
		drawConfigure() {
			sortItem = this.sortItem;
			this.super$drawConfigure();
			Vars.indexer.eachBlock(this, range, boolf(other => other.block instanceof GenericCrafter || other.block instanceof Drill || other.block instanceof BeamDrill || other.block instanceof WallCrafter), cons(other => {
				if (sortItem != null && this.items.get(sortItem) != null) {
					Draw.color(sortItem.color);
					Lines.square(other.x, other.y, other.block.size * Vars.tilesize / 2 + 1, 45)
				}
			}));
			Drawf.dashCircle(this.x, this.y, range, Pal.accent);
		},
		draw() {
			this.super$draw();
			this.sortItem == null ? Draw.color(Color.white) : Draw.color(sortItem.color)
			Draw.alpha(-Mathf.sin(0.05 * Time.time));
			Lines.square(this.x, this.y, 8 * Mathf.sin(0.025 * Time.time), 45);
		}
	}, 范围收集器);
});
//搬运自血肉诅咒
//搬运自血肉诅咒
//搬运自血肉诅咒
//搬运自血肉诅咒
//搬运自血肉诅咒