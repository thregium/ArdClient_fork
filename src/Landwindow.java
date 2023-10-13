/* Preprocessed source code */
/* -*- Java -*- */

import haven.BuddyWnd;
import haven.Button;
import haven.CheckBox;
import haven.Coord;
import haven.GOut;
import haven.GameUI;
import haven.Label;
import haven.MCache;
import haven.RichText;
import haven.Tex;
import haven.TexI;
import haven.Text;
import haven.UI;
import haven.Utils;
import haven.Widget;
import haven.Window;

import java.awt.Color;

public class Landwindow extends Window {
    Widget bn, be, bs, bw, refill, buy, reset, dst, rebond;
    BuddyWnd.GroupSelector group;
    Label area, cost;
    Widget authmeter;
    int auth, acap, adrain;
    boolean offline;
    Coord c1, c2, cc1, cc2;
    MCache.Overlay ol;
    MCache map;
    int bflags[] = new int[8];
    PermBox perms[] = new PermBox[4];
    CheckBox homeck;
    private static final String fmt = "Area: %d m" + ((char) 0xB2);

    public static Widget mkwidget(UI ui, Object... args) {
        Coord c1 = (Coord) args[0];
        Coord c2 = (Coord) args[1];
        return (new Landwindow(c1, c2));
    }

    private void fmtarea() {
        area.settext(String.format(fmt, (c2.x - c1.x + 1) * (c2.y - c1.y + 1)));
    }

    private void updatecost() {
        cost.settext(String.format("Cost: %d", 10 * (((cc2.x - cc1.x + 1) * (cc2.y - cc1.y + 1)) - ((c2.x - c1.x + 1) * (c2.y - c1.y + 1)))));
    }

    private void updflags() {
        int fl = bflags[group.group];
        for (PermBox w : perms)
            w.a = (fl & w.fl) != 0;
    }

    private class PermBox extends CheckBox {
        int fl;

        PermBox(String lbl, int fl) {
            super(lbl);
            this.fl = fl;
        }

        public void changed(boolean val) {
            int fl = 0;
            for (PermBox w : perms) {
                if (w.a)
                    fl |= w.fl;
            }
            Landwindow.this.wdgmsg("shared", group.group, fl);
            bflags[group.group] = fl;
        }
    }

    private Tex rauth = null;

    public Landwindow(Coord c1, Coord c2) {
        super(new Coord(0, 0), "Stake", false);
        this.cc1 = this.c1 = c1;
        this.cc2 = this.c2 = c2;
        int y = 0;
        area = add(new Label(""), new Coord(0, y)); y += UI.scale(15);
        authmeter = add(new Widget(UI.scale(300, 20)) {
            public void draw(GOut g) {
                int auth = Landwindow.this.auth;
                int acap = Landwindow.this.acap;
                if (acap > 0) {
                    g.chcolor(0, 0, 0, 255);
                    g.frect(Coord.z, sz);
                    g.chcolor(128, 0, 0, 255);
                    Coord isz = sz.sub(2, 2);
                    isz.x = (auth * isz.x) / acap;
                    g.frect(new Coord(1, 1), isz);
                    g.chcolor();
                    if (rauth == null) {
                        Color col = offline ? Color.RED : Color.WHITE;
                        rauth = new TexI(Utils.outline2(Text.render(String.format("%s/%s", auth, acap), col).img, Utils.contrast(col)));
                    }
                    g.aimage(rauth, sz.div(2), 0.5, 0.5);
                }
            }
        }, new Coord(0, y)); y += UI.scale(25);
        refill = add(new Button(UI.scale(140), "Refill"), new Coord(0, y));
        refill.tooltip = RichText.render("Refill this claim's presence immediately from your current pool of learning points.", UI.scale(300));
        y += UI.scale(40);
        cost = add(new Label("Cost: 0"), new Coord(0, y)); y += UI.scale(25);
        fmtarea();
        bn = add(new Button(UI.scale(120), "Extend North"), UI.scale(90), y);
        be = add(new Button(UI.scale(120), "Extend East"), UI.scale(180), y + UI.scale(25));
        bs = add(new Button(UI.scale(120), "Extend South"), UI.scale(90), y + UI.scale(50));
        bw = add(new Button(UI.scale(120), "Extend West"), 0, y + UI.scale(25));
        y += UI.scale(100);
        buy = add(new Button(UI.scale(140), "Buy"), 0, y);
        reset = add(new Button(UI.scale(140), "Reset"), UI.scale(160), y);
        dst = add(new Button(UI.scale(140), "Declaim"), 0, y + UI.scale(35));
        rebond = add(new Button(UI.scale(140), "Renew bond"), UI.scale(160), y + UI.scale(35));
        rebond.tooltip = RichText.render("Create a new bond for this claim, destroying the old one. Costs half of this claim's total presence.", UI.scale(300));
        y += UI.scale(80);
        add(new Label("Assign permissions to memorized people:"), 0, y); y += UI.scale(15);
        group = add(new BuddyWnd.GroupSelector(0) {
            protected void changed(int g) {
                super.changed(g);
                updflags();
            }
        }, 0, y);
        y += UI.scale(30);
        perms[0] = add(new PermBox("Trespassing", 1), UI.scale(10), y); y += UI.scale(20);
        perms[3] = add(new PermBox("Rummaging", 8), UI.scale(10), y); y += UI.scale(20);
        perms[1] = add(new PermBox("Theft", 2), UI.scale(10), y); y += UI.scale(20);
        perms[2] = add(new PermBox("Vandalism", 4), UI.scale(10), y); y += UI.scale(20);
        add(new Label("White permissions also apply to non-memorized people."), 0, y); y += UI.scale(15);
        pack();
    }

    protected void added() {
        super.added();
        map = ui.sess.glob.map;
        getparent(GameUI.class).map.enol(0, 1, 16);
        ol = map.new Overlay(cc1, cc2, 65536);
    }

    public void destroy() {
        getparent(GameUI.class).map.disol(0, 1, 16);
        ol.destroy();
        super.destroy();
    }

    public void uimsg(String msg, Object... args) {
        if (msg == "upd") {
            Coord c1 = (Coord) args[0];
            Coord c2 = (Coord) args[1];
            this.c1 = c1;
            this.c2 = c2;
            fmtarea();
            updatecost();
        } else if (msg == "shared") {
            int g = (Integer) args[0];
            int fl = (Integer) args[1];
            bflags[g] = fl;
            if (g == group.group)
                updflags();
        } else if (msg == "auth") {
            auth = (Integer) args[0];
            acap = (Integer) args[1];
            adrain = (Integer) args[2];
            offline = (Integer) args[3] != 0;
            rauth = null;
        } else if (msg == "entime") {
            int entime = (Integer) args[0];
            authmeter.tooltip = Text.render(String.format("%d:%02d until enabled", entime / 3600, (entime % 3600) / 60));
        }
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender == bn) {
            cc1 = cc1.add(0, -1);
            ol.update(cc1, cc2);
            updatecost();
            return;
        } else if (sender == be) {
            cc2 = cc2.add(1, 0);
            ol.update(cc1, cc2);
            updatecost();
            return;
        } else if (sender == bs) {
            cc2 = cc2.add(0, 1);
            ol.update(cc1, cc2);
            updatecost();
            return;
        } else if (sender == bw) {
            cc1 = cc1.add(-1, 0);
            ol.update(cc1, cc2);
            updatecost();
            return;
        } else if (sender == buy) {
            wdgmsg("take", cc1, cc2);
            return;
        } else if (sender == reset) {
            ol.update(cc1 = c1, cc2 = c2);
            updatecost();
            return;
        } else if (sender == dst) {
            wdgmsg("declaim");
            return;
        } else if (sender == rebond) {
            wdgmsg("bond");
            return;
        } else if (sender == refill) {
            wdgmsg("refill");
            return;
        }
        super.wdgmsg(sender, msg, args);
    }
}
