/*
* @author guiY
* @Extra mod <https://github.com/guiYMOUR/mindustry-Extra-Utilities-mod>
*/
const speed = 2;

const lu = extendContent(LiquidSource, "lu", {
    drawRequestConfig(req, list){
        this.drawRequestConfigCenter(req, req.config, Core.atlas.find("sc-lu-centre"), true);
    },
});
lu.localizedName = "流体装卸器";
lu.buildType = prov(() => {
    var dumpingTo = null;
    var offset = 0;
    var liquidBegin = null;
    return new JavaAdapter(LiquidSource.LiquidSourceBuild, {
        updateTile(){
            if(liquidBegin != this.source){
                this.liquids.clear();
                liquidBegin = this.source;
            }
            for(var i = 0; i < this.proximity.size; i++){
                var pos = (offset + i) % this.proximity.size;
                var other = this.proximity.get(pos);

                if(other.interactable(this.team) && other.block.hasLiquids && this.source != null && other.liquids.get(this.source) > 0){
                    dumpingTo = other;
                    if(this.liquids.total() < this.block.liquidCapacity){
                        var amount = Math.min(speed, other.liquids.get(this.source));
                        this.liquids.add(this.source, amount);
                        other.liquids.remove(this.source, amount);
                    }
                }
            }
            if(this.proximity.size > 0){
                offset ++;
                offset %= this.proximity.size;
            }
            this.dumpLiquid(this.liquids.current());
        },
        canDumpLiquid(to, liquid){
            return to != dumpingTo;
        },
        draw(){
            Draw.rect(Core.atlas.find("sc-lu"), this.x, this.y);
            if(this.source == null){
                Draw.rect("cross", this.x, this.y);
            }else{
                Draw.color(this.source.color);
                Draw.rect(Core.atlas.find("sc-lu-centre"), this.x, this.y);
                Draw.color();
            }
        },
    }, lu);
});
lu.health = 50;
lu.liquidCapacity = 64;
lu.requirements = ItemStack.with(
    Items.metaglass, 20,
);
lu.buildVisibility = BuildVisibility.shown;
lu.category = Category.liquid;