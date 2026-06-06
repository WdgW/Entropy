Events.on(EventType.ClientLoadEvent, cons(e => {

    var dialog = new JavaAdapter(BaseDialog, {}, "矩阵能源");
    var icon =new Packages.arc.scene.style.TextureRegionDrawable(Core.atlas.find(" ", Core.atlas.find("clear")));
    dialog.shown(run(() => {
        dialog.cont.table(Tex.button, cons(t => {
            t.defaults().size(120, 88).right();
            t.button("关闭\n界面", icon, Styles.cleart, run(() => {
                dialog.hide();
            }));
        t.add("欢迎游玩，如果\n在游玩的过程中\n发现了bug请\n反馈给作者！！！")
        }));
    }));
    dialog.show();
}));