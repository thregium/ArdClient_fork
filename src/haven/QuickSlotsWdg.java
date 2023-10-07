package haven;

import haven.res.ui.tt.q.qbuff.QBuff;
import haven.sloth.gui.MovableWidget;
import modification.configuration;

import java.awt.Color;

public class QuickSlotsWdg extends MovableWidget implements DTarget {
    private static final Tex sbg = Resource.loadtex("gfx/hud/slots");
    public static final Coord lc = UI.scale(6, 6);
    public static final Coord rc = UI.scale(56, 6);
    private static final Coord ssz = UI.scale(44, 44);

    private static final Color qualitybg() {
        return new Color(20, 20, 20, 255 - Config.qualitybgtransparency);
    }

    public QuickSlotsWdg() {
        super(UI.scale(44 + 44 + 6, 44), "QuickSlotsWdg");
    }

    @Override
    public void draw(GOut g) {
        Equipory e = null;
        if (ui.gui != null) e = ui.gui.getequipory();
        if (e != null) {
            g.image(sbg, Coord.z);
            WItem left = e.quickslots[6];
            if (left != null) {
                drawitem(g.reclipl(lc, g.sz), left);
                drawamountbar(g, left.item, UI.scale(44 + 6));
                if (Config.showquality)
                    drawQualityLeft(g, left);
            }
            WItem right = e.quickslots[7];
            if (right != null) {
                drawitem(g.reclipl(rc, g.sz), right);
                drawamountbar(g, right.item, 0);
                if (Config.showquality)
                    drawQualityRight(g, right);
            }
        }
        super.draw(g);
    }

    private void drawitem(GOut g, WItem witem) {
        GItem item = witem.item;
        GSprite spr = item.spr();
        if (spr != null) {
            g.defstate();
            witem.drawmain(g, spr);
            g.defstate();
        } else {
            g.image(WItem.missing.layer(Resource.imgc).tex(), Coord.z, ssz);
        }
    }

    private void drawQualityLeft(GOut g, WItem witem) {
        QBuff quality = witem.item.quality();
        if (Config.showquality) {
            if (quality != null && quality.qtex != null) {
                Tex t = Config.qualitywhole ? quality.qwtex : quality.qtex;
                Coord btm = configuration.infopos(configuration.qualitypos, sz.div(2, 1), t.sz());
                if (Config.qualitybg) {
                    g.chcolor(qualitybg());
                    g.frect(btm, t.sz().add(UI.scale(1, -1)));
                    g.chcolor();
                }
                g.image(t, btm);
            }
        }
    }

    private void drawQualityRight(GOut g, WItem witem) {
        QBuff quality = witem.item.quality();
        if (Config.showquality) {
            if (quality != null && quality.qtex != null) {
                Tex t = Config.qualitywhole ? quality.qwtex : quality.qtex;
                Coord btm = configuration.infopos(configuration.qualitypos, sz.div(2, 1), t.sz()).add(UI.scale(50, 0));
                if (Config.qualitybg) {
                    g.chcolor(qualitybg());
                    g.frect(btm, t.sz().add(UI.scale(1, -1)));
                    g.chcolor();
                }
                g.image(t, btm);
            }
        }
    }

    public void drawamountbar(GOut g, GItem item, int offset) {
        if (item.spr() != null) {
            try {
                for (ItemInfo info : item.info()) {
                    if (info instanceof ItemInfo.Contents) {
                        ItemInfo.Contents imtcnt = (ItemInfo.Contents) info;
                        if (imtcnt.content > 0) {
                            double capacity;
                            if (item.getname().contains("Bucket"))
                                capacity = imtcnt.isseeds ? 1000D : 10.0D;
                            else
                                return;
                            double content = imtcnt.content;
                            int height = sz.y - UI.scale(2);
                            int h = (int) (content / capacity * height);
                            g.chcolor(WItem.famountclr);
                            g.frect(new Coord(sz.x - UI.scale(4) - offset, height - UI.scale(h) + UI.scale(1)), UI.scale(3, h));
                            g.chcolor();
                            return;
                        }
                    }
                }
            } catch (Exception ex) { // fail silently if info is not ready
            }
        }
    }

    @Override
    public boolean drop(Coord cc, Coord ul) {
        Equipory e = ui.gui.getequipory();
        if (e != null) {
            e.wdgmsg("drop", cc.x <= UI.scale(47) ? 6 : 7);
            return true;
        }
        return false;
    }

    @Override
    public boolean iteminteract(Coord cc, Coord ul) {
        Equipory e = ui.gui.getequipory();
        if (e != null) {
            WItem w = e.quickslots[cc.x <= UI.scale(47) ? 6 : 7];
            if (w != null) {
                return w.iteminteract(cc, ul);
            }
        }
        return false;
    }

    @Override
    public boolean mousedown(Coord c, int button) {
        if (super.mousedown(c, button))
            return true;
        if (ui.modmeta)
            return true;
        if (ui.modctrl && button == 1 && Config.disablequickslotdrop)
            return true;
        Equipory e = ui.gui.getequipory();
        if (e != null) {
            WItem w = e.quickslots[c.x <= UI.scale(47) ? 6 : 7];
            if (w != null) {
                w.mousedown(new Coord(w.sz.x / 2, w.sz.y / 2), button);
                return true;
            }
        }
        return false;
    }

    public void simulateclick(Coord c) {
        Equipory e = ui.gui.getequipory();
        if (e != null) {
            WItem w = e.quickslots[c.x <= UI.scale(47) ? 6 : 7];
            if (w != null)
                w.item.wdgmsg("take", new Coord(w.sz.x / 2, w.sz.y / 2));
        }
    }

//    @Override
//    public boolean mouseup(Coord c, int button) {
//        if (dragging != null) {
//            dragging.remove();
//            dragging = null;
////            Utils.setprefc("quickslotsc", this.c);
//            return true;
//        }
//        return super.mouseup(c, button);
//    }

//    @Override
//    public void mousemove(Coord c) {
//        if (dragging != null) {
//            this.c = this.c.add(c.x, c.y).sub(dc);
//            return;
//        }
//        super.mousemove(c);
//    }
}