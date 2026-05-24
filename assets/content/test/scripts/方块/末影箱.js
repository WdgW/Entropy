const invincibleCore = extend(CoreBlock, "末影箱", {
//搬自末影箱模组。
    canBreak(tile) { return Vars.state.teams.cores(tile.team()).size > 1; },
    canReplace(other) { return other.alwaysReplace; },
    canPlaceOn(tile, team, rotation) { return Vars.state.teams.cores(team).size < 100; },
    placeBegan(tile, previous) {},
    beforePlaceBegan(tile, previous) {},

    drawPlace(x, y, rotation, valid) {}
});
invincibleCore.buildType = prov(() => new JavaAdapter(CoreBlock.CoreBuild, {
    onRemoved() { Vars.state.teams.unregisterCore(this) },
}, invincibleCore));
//exports.invincibleCore = 末影箱;