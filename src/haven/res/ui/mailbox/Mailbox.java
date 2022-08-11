package haven.res.ui.mailbox;

import haven.Button;
import haven.Coord;
import haven.GOut;
import haven.Inventory;
import haven.Loading;
import haven.Resource;
import haven.Tex;
import haven.TexI;
import haven.Text;
import haven.UI;
import haven.Utils;
import haven.WItem;
import haven.Widget;
import java.awt.Color;

public class Mailbox extends Widget {
    public static final Tex bg = Resource.remote().loadwait("ui/mailbox").layer(Resource.imgc, 0).tex();
    public static final Tex sq = Inventory.invsq;
    public static final int itemmarg = (bg.sz().y - sq.sz().y) / 2,
            bbtnw = UI.scale(70);
    public static final Text.Foundry nfnd = new Text.Foundry(Text.sans, 12);
    public static final Coord sqc = new Coord(itemmarg, itemmarg),
            itemc = sqc.add(1, 1),
            bbtnc = bg.sz().sub(UI.scale(5, 2)),
            nmc = new Coord(itemc.x + sq.sz().x + UI.scale(5), itemc.y);
    public final Button rbtn;
    public final Mail spec;
    private Text rnm;
    private int num;
    private Tex rnum;

    public static Widget mkwidget(UI ui, Object... args) {
        int resid = (Integer) args[0];
        int num = (Integer) args[1];
        return (new Mailbox(new Mail(ui.sess.getres(resid), num)));
    }

    private static Tex rnum(int num) {
        return (new TexI(Utils.outline2(Text.render(Integer.toString(num), Color.WHITE).img, Utils.contrast(Color.WHITE))));
    }

    public Mailbox(Mail spec) {
        super(bg.sz());
        this.spec = spec;
        this.rbtn = adda(new Button(bbtnw, "Receive"), bbtnc, 1, 1);
    }

    public void draw(GOut g) {
        if ((rnum == null) || (num != spec.num))
            rnum = rnum(num = spec.num);
        g.image(bg, Coord.z);
        g.image(sq, sqc);
        draw:
        {
            if (rnm == null) {
                try {
                    rnm = nfnd.render(spec.res.get().layer(Resource.tooltip).t);
                } catch (Loading e) {
                    g.image(WItem.missing.layer(Resource.imgc).tex(), itemc, Inventory.sqsz);
                    break draw;
                }
            }
            g.image(spec.res.get().layer(Resource.imgc).tex(), itemc);
            g.image(rnm.tex(), nmc);
        }
        g.aimage(rnum, itemc.add(Inventory.sqsz).sub(UI.scale(1, 1)), 1, 1);
        super.draw(g);
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender == rbtn) {
            wdgmsg("recv");
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    public void uimsg(String msg, Object... args) {
        if (msg == "num") {
            spec.num = (Integer) args[0];
        } else {
            super.uimsg(msg, args);
        }
    }
}
