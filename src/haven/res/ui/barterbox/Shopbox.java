package haven.res.ui.barterbox;

import haven.Button;
import haven.Coord;
import haven.GItem;
import haven.GOut;
import haven.GSprite;
import haven.GSprite.Owner;
import haven.Glob;
import haven.Indir;
import haven.ItemInfo;
import haven.ItemInfo.SpriteOwner;
import haven.Label;
import haven.Loading;
import haven.Message;
import haven.MessageBuf;
import haven.PUtils;
import haven.ResData;
import haven.Resource;
import haven.Resource.Pagina;
import haven.RichText;
import haven.Tex;
import haven.TexI;
import haven.Text;
import haven.TextEntry;
import haven.UI;
import haven.Utils;
import haven.WItem;
import haven.Widget;
import haven.res.lib.tspec.Spec;
import haven.res.ui.tt.q.qbuff.QBuff;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;

import static haven.Inventory.invsq;
import static haven.Inventory.sqsz;

// ui/barterstand
public class Shopbox extends Widget implements SpriteOwner, Owner {
    public static final Tex any = PUtils.strokeTex(Text.render(Resource.getLocString(Resource.BUNDLE_LABEL, "Any")));
    public static final Tex qlbl = PUtils.strokeTex(Text.render(Resource.getLocString(Resource.BUNDLE_LABEL, "Quality:")));
    public static final Tex bg = Resource.loadtex("ui/shopbox");
    public static final Coord itemc = UI.scale(5, 5);
    public static final Coord buyc = UI.scale(5, 43);
    public static final Coord buyca = UI.scale(5, 66);
    public static final Coord pricec = UI.scale(200, 5);
    public static final Coord qualc = (UI.scale(200, 5)).add(invsq.sz()).add(UI.scale(40), 0);
    public static final Coord cbtnc = UI.scale(200, 66);
    public static final Coord spipec = UI.scale(85, 43);
    public static final Coord bpipec = UI.scale(280, 66);
    public ResData res;
    public Spec price;
    public Tex num;
    public int pnum;
    public int pq;
    private Tex pnumt;
    private Tex pqt;
    private GSprite spr;
    private Object[] info = new Object[0];
    private Tex quality;
    private Button spipe;
    private Button bpipe;
    private Button bbtn, bbtn100;
    private TextEntry tbuy;
    private Button cbtn;
    private TextEntry pnume;
    private TextEntry pqe;
    public final boolean admin;
    public final AttrCache<Tex> itemnum = new One(this);
    private List<ItemInfo> cinfo;
    private ItemTip longtip = null, fulltip = null;
    private Tex pricetip = null;
    private Random rnd = null;
    private int count = 0;

    public static Widget mkwidget(UI ui, Object... args) {
        boolean adm = (Integer) args[0] != 0;
        return (new Shopbox(adm));
    }

    public Shopbox(boolean admin) {
        super(bg.sz());
        if (this.admin = admin) {
            spipe = add(new Button(UI.scale(75), "Connect"), spipec);
            bpipe = add(new Button(UI.scale(75), "Connect"), bpipec);
            cbtn = add(new Button(UI.scale(75), "Change"), cbtnc);
            pnume = adda(new TextEntry(UI.scale(30), ""), pricec.add(invsq.sz()).add(UI.scale(5), 0), 0, 1);
            pnume.canactivate = true;
            pnume.dshow = true;
            adda(new Label("Quality:"), qualc.add(0, 0), 0, 1);
            pqe = adda(new TextEntry(40, ""), qualc.add(UI.scale(40), 0), 0, 1);
            pqe.canactivate = true;
            pqe.dshow = true;
        }
    }

    public void draw(GOut g) {
        g.image(bg, Coord.z);
        ResData res = this.res;
        GOut sg;
        if (res != null) {
            sprite:
            {
                sg = g.reclip(itemc, invsq.sz());
                sg.image(invsq, Coord.z);
                GSprite var4 = this.spr;
                if (var4 == null) {
                    try {
                        var4 = this.spr = GSprite.create(this, res.res.get(), res.sdt.clone());
                    } catch (Loading var7) {
                        sg.image((WItem.missing.layer(Resource.imgc)).tex(), Coord.z, sqsz);
                        break sprite;
                    }
                }

                var4.draw(sg);
                if (itemnum.get() != null) {
                    sg.aimage(itemnum.get(), sqsz, 1, 1);
                }

                if (num != null) {
                    g.aimage(num, itemc.add(invsq.sz()).add(UI.scale(5), 0), 0, 2.3);
                }

                if (quality != null) {
                    g.aimage(qlbl, itemc.add(invsq.sz()).add(UI.scale(5), 0), 0, 1);
                    g.aimage(quality, itemc.add(invsq.sz()).add(UI.scale(8) + qlbl.sz().x, 0), 0, 1);
                }
            }
        }

        Spec price = this.price;
        if (price != null) {
            sg = g.reclip(pricec, invsq.sz());
            sg.image(invsq, Coord.z);

            try {
                price.spr().draw(sg);
            } catch (Loading l) {
                sg.image(WItem.missing.layer(Resource.imgc).tex(), Coord.z, sqsz);
            }

            if (!admin && pnumt != null) {
                g.aimage(pnumt, pricec.add(invsq.sz()), 0, 1);
            }

            if (!admin && pqt != null) {
                g.aimage(qlbl, qualc, 0, 1);
                g.aimage(pqt, qualc.add(qlbl.sz().x + UI.scale(4), 0), 0, 1);
            }
        }

        super.draw(g);
    }

    public List<ItemInfo> info() {
        if (cinfo == null) {
            cinfo = ItemInfo.buildinfo(this, info);
            QBuff qb = quality();
            if (qb != null)
                quality = PUtils.strokeTex(Text.render((int) qb.q + ""));
        }
        return (cinfo);
    }

    private QBuff getQBuff(List<ItemInfo> infolist) {
        for (ItemInfo info : infolist) {
            if (info instanceof QBuff)
                return ((QBuff) info);
        }
        return (null);
    }

    private QBuff quality() {
        try {
            for (ItemInfo info : info()) {
                if (info instanceof ItemInfo.Contents)
                    return (getQBuff(((ItemInfo.Contents) info).sub));
            }
            return getQBuff(info());
        } catch (Loading l) {
        }
        return (null);
    }

    public class ItemTip implements Indir<Tex>, ItemInfo.InfoTip {
        private final List<ItemInfo> info;
        private final TexI tex;

        public ItemTip(List<ItemInfo> info, BufferedImage img) {
            this.info = info;
            if (img == null)
                throw (new Loading());
            tex = new TexI(img);
        }

        public List<ItemInfo> info() {return (info);}

        public Tex get() {
            return (tex);
        }
    }

    public Object tooltip(Coord c, Widget prev) {
        ResData res = this.res;
        if (c.isect(itemc, sqsz) && (res != null)) {
            try {
                if (ui.modflags() == UI.MOD_SHIFT) {
                    if (longtip == null) {
                        BufferedImage ti = ItemInfo.longtip(info());
                        Pagina pg = res.res.get().layer(Resource.pagina);
                        if (pg != null)
                            ti = ItemInfo.catimgs(0, ti, RichText.render("\n" + pg.text, UI.scale(200)).img);
                        longtip = new ItemTip(info(), ti);
                    }
                    return (longtip);
                } else {
                    if (fulltip == null) {
                        BufferedImage ti = ItemInfo.longtip(info());
                        Pagina pg = res.res.get().layer(Resource.pagina);
                        if (pg != null)
                            ti = ItemInfo.catimgs(0, ti, RichText.render("\n" + pg.text, UI.scale(200)).img);
                        fulltip = new ItemTip(info(), ti);
                    }
                    return (fulltip);
                }
            } catch (Loading l) {
                return ("...");
            }
        }
        if (c.isect(pricec, sqsz) && (price != null)) {
            try {
                if (pricetip == null)
                    pricetip = price.longtip();
                return (pricetip);
            } catch (Loading l) {
                return ("...");
            }
        }
        return (super.tooltip(c, prev));
    }

    @Deprecated
    public Glob glob() {
        return (ui.sess.glob);
    }

    public Resource resource() {
        return (res.res.get());
    }

    public GSprite sprite() {
        if (spr == null)
            throw (new Loading("Still waiting for sprite to be constructed"));
        return (spr);
    }

    public Resource getres() {
        return (res.res.get());
    }

    public Random mkrandoom() {
        if (rnd == null)
            rnd = new Random();
        return (rnd);
    }

    private static Integer parsenum(TextEntry e) {
        try {
            if (e.buf.line().isEmpty())
                return (0);
            return (Integer.parseInt(e.buf.line()));
        } catch (NumberFormatException exc) {
            return (null);
        }
    }

    public boolean mousedown(Coord c, int button) {
        if ((button == 3) && c.isect(pricec, sqsz) && (price != null)) {
            wdgmsg("pclear");
            return (true);
        }
        return (super.mousedown(c, button));
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender == bbtn) {
            if (ui.modshift && !ui.modctrl) {
                for (int i = 0; i <= 5; i++)
                    wdgmsg("buy");
            } else if (ui.modshift && ui.modctrl) {
                for (int i = 0; i <= 20; i++)
                    wdgmsg("buy");
            } else {
                wdgmsg("buy");
            }
        } else if (sender == bbtn100) {
            if (count > 0) {
                for (int i = 0; i < count; i++)
                    wdgmsg("buy");
            } else {
                ui.gui.msg("You can't buy 0 items.");
            }
        } else if (sender == spipe) {
            wdgmsg("spipe");
        } else if (sender == bpipe) {
            wdgmsg("bpipe");
        } else if (sender == cbtn) {
            wdgmsg("change");
        } else if ((sender == pnume) || (sender == pqe)) {
            wdgmsg("price", parsenum(pnume), parsenum(pqe));
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    private void updbtn() {
        boolean canbuy = price != null && pnum > 0;
        if (canbuy && bbtn == null || bbtn100 == null) {
            bbtn = add(new Button(75, "Buy"), buyc);
            bbtn.tooltip = "Left click to buy 1, Shift left click to buy 5, ctrl shift left click to buy 20.";
            bbtn100 = add(new Button(75, "Buy x"), buyca);
            bbtn100.tooltip = "Type the number in box press enter and press this button.";

            tbuy = add(new TextEntry(40, "") {
                String backup = text();

                @Override
                public boolean keydown(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        try {
                            count = Integer.parseInt(dtext());
                            return true;
                        } catch (NumberFormatException ex) {
                        }
                    }
                    backup = text();
                    boolean b = super.keydown(e);
                    try {
                        if (!text().isEmpty())
                            Integer.parseInt(text());
                        if (text().length() > 2)
                            settext(backup);
                    } catch (Exception ex) {
                        settext(backup);
                    }
                    return (b);
                }
            }, UI.scale(82, 66));
        } else if (!canbuy && this.bbtn != null) {
            this.bbtn.reqdestroy();
            this.bbtn = null;
            this.bbtn100 = null;
        }

    }

    private static Tex rnum(String fmt, int n) {
        if (n < 1)
            return (null);
        return (PUtils.strokeTex(Text.render(String.format(fmt, n))));
    }

    public void uimsg(String name, Object... args) {
        if (name == "res") {
            this.res = null;
            this.spr = null;
            if (args.length > 0) {
                ResData res = new ResData(ui.sess.getres((Integer) args[0]), Message.nil);
                if (args.length > 1) {
                    res.sdt = new MessageBuf((byte[]) args[1]);
                }
                this.res = res;
            }
            updbtn();
        } else if (name == "tt") {
            info = args;
            cinfo = null;
            longtip = null;
            fulltip = null;
        } else if (name == "n") {
            int num = (Integer) args[0];
            this.num = PUtils.strokeTex(Text.render(String.format("%d left", num)));
        } else if (name == "price") {
            int a = 0;
            if (args[a] == null) {
                a++;
                price = null;
            } else {
                Indir<Resource> res = ui.sess.getres((Integer) args[a++]);
                Message sdt = Message.nil;
                if (args[a] instanceof byte[])
                    sdt = new MessageBuf((byte[]) args[a++]);
                Object[] info = null;
                if (args[a] instanceof Object[]) {
                    info = new Object[0][];
                    while (args[a] instanceof Object[])
                        info = Utils.extend(info, args[a++]);
                }
                price = new Spec(new ResData(res, sdt), Spec.uictx(ui), info);
            }
            pricetip = null;
            pnum = (Integer) args[a++];
            pq = (Integer) args[a++];
            if (!admin) {
                pnumt = rnum("\u00d7%d", pnum);
                pqt = (pq > 0) ? rnum("%d+", pq) : any;
            } else {
                pnume.settext((pnum > 0) ? Integer.toString(pnum) : "");
                pnume.commit();
                pqe.settext((pq > 0) ? Integer.toString(pq) : "");
                pqe.commit();
            }
            updbtn();
        } else {
            super.uimsg(name, args);
        }
    }

    @Override
    public <C> C context(Class<C> con) {
        return Spec.uictx.context(con, this.ui);
    }

    public abstract class AttrCache<T> {
        private List<ItemInfo> forinfo;
        private T save;

        public AttrCache(Shopbox shopbox) {
            this.forinfo = null;
            this.save = null;
        }

        public T get() {
            try {
                List<ItemInfo> info = info();
                if (info != forinfo) {
                    save = find(info);
                    forinfo = info;
                }
            } catch (Loading l) {
                return (null);
            }

            return (save);
        }

        protected abstract T find(List<ItemInfo> info);
    }


    class One extends AttrCache<Tex> {
        One(Shopbox shopbox) {
            super(shopbox);
        }

        protected Tex find(List<ItemInfo> info) {
            GItem.NumberInfo numberInfo = ItemInfo.find(GItem.NumberInfo.class, info);
            return numberInfo == null ? null : new TexI(Utils.outline2(Text.render(Integer.toString(numberInfo.itemnum()), Color.WHITE).img, Utils.contrast(Color.WHITE)));
        }
    }
}
