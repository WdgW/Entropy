const lib = require("base/lib");
const 核裂 = extend(itemTurret, "核裂", {});

function DoubleTri(x, y, width, length, angle) {
	Drawf.tri(x, y, width, length, angle + 180);
	Drawf.tri(x, y, width, length / 4, angle);
}
lib.setBuilding(PowerTurret.PowerTurretBuild, 核裂, {
	draw() {
		this.super$draw();
		let tris = 3
		Draw.color(Color.valueOf("FF000040"));
		Draw.z(0.0001);
		Fill.circle(this.targetPos.getX(), this.targetPos.getY(), 400);
		Draw.color(Color.valueOf("FF5845"));
		Draw.z(Layer.effect);
		for (let i = 0; i < tris; i++) {
			let ang = i * 360 / tris + Time.time;
			let ang2 = ang - Time.time * 2 + 360 / tris / 2;
			let xy = lib.AngleTrns(ang, 32);
			let xy2 = lib.AngleTrns(ang2, 16);
			Fill.circle(this.targetPos.getX() + xy.x, this.targetPos.getY() + xy.y, 1);
		}
	}
})