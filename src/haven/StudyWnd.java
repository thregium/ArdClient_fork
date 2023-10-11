package haven;

import haven.resutil.Curiosity;
import java.text.DecimalFormat;

public class StudyWnd extends GameUI.Hidewnd {
    InventoryProxy study;
    StudyInfo info;

    StudyWnd() {
        super(Coord.z, (Resource.getLocString(Resource.BUNDLE_WINDOW, "Study")));
    }

    public void setStudy(Inventory inventory) {
        if (study != null) {
            study.reqdestroy();
            info.reqdestroy();
        }
        final InventoryProxy invp = new InventoryProxy(inventory);
        study = add(invp, 0, 0);
        info = add(new StudyInfo(new Coord(study.sz.x, 66), inventory), 0, study.c.y + study.sz.y + 5);
        pack();
    }

    private static class StudyInfo extends Widget {
        public Widget study;
        public int texp, tw, tenc;
        public double tlph;
        private final Text.UTex<?> texpt = new Text.UTex<>(() -> texp, s -> PUtils.strokeTex(Text.std.render(Utils.thformat(s))));
        private final Text.UTex<?> twt = new Text.UTex<>(() -> tw + "/" + ui.sess.glob.getcattr("int").comp, s -> PUtils.strokeTex(Text.std.render(s)));
        private final Text.UTex<?> tenct = new Text.UTex<>(() -> tenc, s -> PUtils.strokeTex(Text.std.render(Integer.toString(s))));
        private final DecimalFormat f = new DecimalFormat("##.##");
        private final Text.UTex<?> tlphr = new Text.UTex<>(() -> tlph, s -> PUtils.strokeTex(Text.std.render(String.format("%s", !Utils.getprefb("tooltipapproximatert", false) ? f.format(tlph) : f.format(tlph * ui.sess.glob.getTimeFac())))));

        private StudyInfo(Coord sz, Widget study) {
            super(sz);
            this.study = study;
            add(new Label("Attention:"), UI.scale(2, 2));
            add(new Label("Experience cost:"), UI.scale(2, 18));
            add(new Label("Learning points:"), UI.scale(2, 34));
            add(new Label("LP/H:"), UI.scale(2, 50));
        }

        private void upd() {
            int texp = 0, tw = 0, tenc = 0;
            double tlph = 0;
            for (GItem item : study.children(GItem.class)) {
                try {
                    Curiosity ci = ItemInfo.find(Curiosity.class, item.info());
                    if (ci != null) {
                        texp += ci.exp;
                        tw += ci.mw;
                        tenc += ci.enc;
                        tlph += (ci.exp / (ci.time / 60));
                    }
                } catch (Loading ignored) {
                }
            }
            this.texp = texp;
            this.tw = tw;
            this.tenc = tenc;
            this.tlph = tlph;
        }

        public void draw(GOut g) {
            upd();
            super.draw(g);
            g.chcolor(255, 192, 255, 255);
            g.aimage(twt.get(), new Coord(sz.x - 4, 2), 1.0, 0.0);
            g.chcolor(255, 255, 192, 255);
            g.aimage(tenct.get(), new Coord(sz.x - 4, 18), 1.0, 0.0);
            g.chcolor(192, 192, 255, 255);
            g.aimage(texpt.get(), new Coord(sz.x - 4, 34), 1.0, 0.0);
            g.chcolor(192, 192, 255, 255);
            g.aimage(tlphr.get(), new Coord(sz.x - 4, 50), 1.0, 0.0);
        }
    }
}