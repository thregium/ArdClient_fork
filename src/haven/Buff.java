/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import haven.ItemInfo.AttrCache;
import modification.dev;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Buff extends Widget implements ItemInfo.ResOwner, Bufflist.Managed {
    public static final Text.Foundry nfnd = new Text.Foundry(Text.dfont, 10);
    public static final Tex frame = Resource.loadtex("gfx/hud/buffs/frame");
    public static final Tex cframe = Resource.loadtex("gfx/hud/buffs/cframe");
    public static final Tex scframe = Theme.tex("scframe");
    public static final Coord imgoff = new Coord(3, 3);
    public static final Coord ameteroff = new Coord(3, 37), ametersz = new Coord(32, 3);
    public static final Coord sameteroff = new Coord(3, 27), sametersz = new Coord(22, 2);
    private static final Pattern travelpat = Pattern.compile("Travel weariness: ([0-9\\.]+)/([0-9\\.]+) \\(([0-9\\.]+)\\%\\)");
    public Indir<Resource> res;
    public double cmeter = -1;
    public double cmrem = -1;
    public double gettime;
    protected int a = 255;
    protected boolean dest = false;
    private ItemInfo.Raw rawinfo = null;
    private List<ItemInfo> info = Collections.emptyList();
    /* Deprecated */
    String tt = null;
    public int ameter = -1;
    int nmeter = -1;
    Tex ntext = null;
    public Tex atex;

    @RName("buff")
    public static class $_ implements Factory {
        public Widget create(UI ui, Object[] args) {
            Indir<Resource> res = ui.sess.getres((Integer) args[0]);
            return (new Buff(res));
        }
    }

    public Buff(Indir<Resource> res) {
        super(cframe.sz());
        this.res = res;
    }

    public Resource resource() {
        return (res.get());
    }

    private static final OwnerContext.ClassResolver<Buff> ctxr = new OwnerContext.ClassResolver<Buff>()
            .add(Buff.class, wdg -> wdg)
            .add(Glob.class, wdg -> wdg.ui.sess.glob)
            .add(Session.class, wdg -> wdg.ui.sess);

    public <T> T context(Class<T> cl) {
        return (ctxr.context(cl, this));
    }

    public List<ItemInfo> info() {
        if (info == null)
            info = ItemInfo.buildinfo(this, rawinfo);
        return (info);
    }

    private Tex nmeter() {
        if (ntext == null)
            ntext = new TexI(Utils.outline2(nfnd.render(Integer.toString(nmeter), Color.WHITE).img, Color.BLACK));
        return (ntext);
    }

    public interface AMeterInfo {
        public double ameter();
    }

    public static abstract class AMeterTip extends ItemInfo.Tip implements AMeterInfo {
        public AMeterTip(Owner owner) {
            super(owner);
        }

        public void layout(Layout l) {
            int n = (int) Math.floor(ameter() * 100);
            l.cmp.add(Text.render(" (" + n + "%)").img, new Coord(l.cmp.sz.x, 0));
        }

        public int order() {
            return (10);
        }

        public Tip shortvar() {
            return (this);
        }
    }

    private final AttrCache<Double> ameteri = new AttrCache<>(this::info, AttrCache.map1(AMeterInfo.class, minf -> minf::ameter));
    private final AttrCache<Tex> nmeteri = new AttrCache<>(this::info, AttrCache.map1s(GItem.NumberInfo.class, ninf -> new TexI(GItem.NumberInfo.numrender(ninf.itemnum(), ninf.numcolor()))));
    private final AttrCache<Double> cmeteri = new AttrCache<>(this::info, AttrCache.map1(GItem.MeterInfo.class, minf -> minf::meter));

    public static final Map<String, Color> openings = new HashMap<String, Color>(4) {{
        put("paginae/atk/dizzy", new Color(8, 103, 136));
        put("paginae/atk/offbalance", new Color(8, 103, 1));
        put("paginae/atk/cornered", new Color(221, 28, 26));
        put("paginae/atk/reeling", new Color(203, 168, 6));
    }};
    private final Coord simpleOpeningSz = new Coord(32, 32);
    private final Coord simpleOpeningSmallSz = new Coord(22, 21);

    public boolean isOpening() {
        try {
            final Resource res = this.res.get();
            final Color clr = openings.get(res.name);
            return clr != null;
        } catch (Loading l) {
            return false;
        }
    }

    /**
     * Only for Fightview to see the buffs in the list, nothing fancy.
     * Fight Buffs don't have meters aside from the ameter.
     * Fight buffs also use the cframe always
     */
    public void fightdraw(final GOut g, float scale) {
        final Coord sz = scframe.sz();

        try {
            final Resource res = this.res.get();
            final Color clr = openings.get(res.name);
            if (clr != null) {
                g.chcolor(255, 255, 255, a);
                Double ameter = (this.ameter >= 0) ? (this.ameter / 100.0) : ameteri.get();
                if (ameter != null) {
                    g.image(scframe, Coord.z, sz.mul(scale));
                    g.chcolor(0, 0, 0, a);
                    g.frect(sameteroff.mul(scale), sametersz.mul(scale));
                    g.chcolor(255, 255, 255, a);
                    g.frect(sameteroff.mul(scale), new Coord((int) Math.floor(ameter * sametersz.x), sametersz.y).mul(scale));
                }

                g.chcolor(clr);
                g.frect(imgoff.mul(scale), simpleOpeningSmallSz.mul(scale));
                g.chcolor();

                if (ameter != null) {
                    final Coord size = FastText.size(this.ameter + "");
                    final Coord c = sz.mul(scale).div(2);
                    g.chcolor(new Color(64, 64, 64, 215));
                    g.frect(c.sub(size.div(2)), size.sub(0, 1));
                    g.chcolor();
                    FastText.aprintf(g, c, 0.5, 0.5, "%d", this.ameter);
                }
            }
        } catch (Loading l) {
            //do nothing
        }
    }

    public void fightdraw(final GOut g) {
        fightdraw(g, 1.0f);
    }

    public void stancedraw(final GOut g, float scale) {
        final Coord sz = scframe.sz();
        try {
            final Resource res = this.res.get();
            Tex img = res.layer(Resource.imgc).tex();
            if (img != null) {
                g.image(img, Coord.z, sz.mul(scale));
                g.chcolor(new Color(64, 64, 64, 215));
                g.rect(Coord.z, sz.mul(scale));
                g.chcolor();
                if (this.ameter > 0) {
                    final Coord size = FastText.size(this.ameter + "");
                    final Coord c = sz.mul(scale).div(2);
                    g.chcolor(new Color(64, 64, 64, 215));
                    g.frect(c.sub(size.div(2)), size.sub(0, 1));
                    g.chcolor();
                    FastText.aprintf(g, c, 0.5, 0.5, "%d", this.ameter);
                }
            }
        } catch (Loading l) {
            //do nothing
        }
    }

    public void stancedraw(final GOut g) {
        stancedraw(g, 1.0f);
    }

    public void sessdraw(final GOut g, Coord bc) {
        try {
            Resource res = this.res.get();
            Color clr = openings.get(res.name);
            if (clr == null) {
                draw(g.reclip(bc, sz));
                return;
            }

            if (ameter >= 0) {
                g.image(Buff.cframe, bc);
                g.chcolor(Color.BLACK);
                g.frect(bc.add(Buff.ameteroff), Buff.ametersz);
                g.chcolor(Color.WHITE);
                g.frect(bc.add(Buff.ameteroff), new Coord((ameter * Buff.ametersz.x) / 100, Buff.ametersz.y));
            } else {
                g.image(Buff.frame, bc);
            }

            bc.x += 3;
            bc.y += 3;

            g.chcolor(clr);
            g.frect(bc, simpleOpeningSz);

            g.chcolor(Color.WHITE);
            if (ameter() >= 0) {
                final Coord asz = FastText.sizes(ameter + "");
                bc.x = bc.x + simpleOpeningSz.x / 2 - asz.x / 2;
                bc.y = bc.y + simpleOpeningSz.y / 2 - asz.y / 2;
                FastText.printsf(g, bc, "%d", ameter);
            }
            g.chcolor();
        } catch (Loading l) {
            draw(g.reclip(bc, sz));
        }
    }

    public void draw(GOut g) {
        if (TexGL.disableall)
            return;
        g.chcolor(255, 255, 255, a);
        Double ameter = (this.ameter >= 0) ? Double.valueOf(this.ameter / 100.0) : ameteri.get();
        if (ameter != null) {
            g.image(cframe, Coord.z);
            g.chcolor(0, 0, 0, a);
            g.frect(ameteroff, ametersz);
            g.chcolor(255, 255, 255, a);
            g.frect(ameteroff, new Coord((int) Math.floor(ameter * ametersz.x), ametersz.y));
        } else {
            g.image(frame, Coord.z);
        }
        try {
            Tex img = res.get().layer(Resource.imgc).tex();
            g.image(img, imgoff);
            Tex nmeter = (this.nmeter >= 0) ? nmeter() : nmeteri.get();
            if (nmeter != null)
                g.aimage(nmeter, imgoff.add(img.sz()).sub(1, 1), 1, 1);
            Double cmeter;
            if (this.cmeter >= 0) {
                double m = this.cmeter;
                if (cmrem >= 0) {
                    double ot = cmrem;
                    double pt = Utils.rtime() - gettime;
                    m *= (ot - pt) / ot;
                }
                cmeter = m;
            } else {
                cmeter = cmeteri.get();
            }
            if (cmeter != null) {
                double m = Utils.clip(cmeter, 0.0, 1.0);
                g.chcolor(255, 255, 255, a / 2);
                Coord ccc = img.sz().div(2);
                g.prect(imgoff.add(ccc), ccc.inv(), img.sz().sub(ccc), Math.PI * 2 * m);
                g.chcolor(255, 255, 255, a);
                g.atextstroked(Utils.odformat2((1 - cmeter) * 100, 2), Coord.of(sz.div(2).x, 0), 0.5, 0, Color.WHITE, Color.BLACK, Button.tf);
            }
        } catch (Loading e) {
        }
        if (this.ameter >= 0) {
            final int width = FastText.textw(this.ameter + "");
            final Coord c = new Coord(sz.x / 2 - width / 2, sz.y / 2 - 5);
            final Coord tsz = new Coord(width, 10);
            g.chcolor(new Color(64, 64, 64, 215));
            g.frect(c, c.add(tsz.x, 0), c.add(tsz), c.add(0, tsz.y));
            g.chcolor();
            FastText.aprintf(g, sz.div(2), 0.5, 0.5, "%s", this.ameter);
//            g.atextstroked(Utils.odformat2(this.ameter, 2), Coord.of(sz.div(2).x, sz.y - 7), 0.5, 1, Color.WHITE, Color.BLACK, Button.tf);
        }
    }

    public void draw(GOut g, int size) {
        Coord sz = new Coord(size, size);
        Coord metSz = new Coord(size, 3);
        g.chcolor(255, 255, 255, this.a);

        try {
            Tex img = ((this.res.get()).layer(Resource.imgc)).tex();
            g.image(img, imgoff);
            Tex nmeter = this.nmeter >= 0 ? this.nmeter() : this.nmeteri.get();
            if (nmeter != null) {
                g.aimage(nmeter, imgoff.add(sz).sub(1, 1), 1.0D, 1.0D);
            }
        } catch (Loading e) {
        }

        Double ameter = (this.ameter >= 0) ? Double.valueOf(this.ameter / 100.0) : ameteri.get();
        if (ameter != null) {
            g.chcolor(0, 0, 0, this.a);
            g.frect(ameteroff, metSz);
            g.chcolor(255, 255, 255, this.a);
            g.frect(ameteroff, new Coord((int) Math.floor(ameter * (double) metSz.x), metSz.y));
        }
    }


    private BufferedImage shorttip() {
        if (rawinfo != null)
            return (ItemInfo.shorttip(info()));
        if (tt != null)
            return (Text.render(tt).img);
        String ret = res.get().layer(Resource.tooltip).t;
        if (ameter >= 0)
            ret = ret + " (" + ameter + "%)";
        return (Text.render(ret).img);
    }

    private BufferedImage longtip() {
        BufferedImage img;
        if (rawinfo != null)
            img = ItemInfo.longtip(info());
        else
            img = shorttip();
        Resource.Pagina pag = res.get().layer(Resource.pagina);
        if (pag != null)
            img = ItemInfo.catimgs(0, img, RichText.render("\n" + pag.text, 200).img);
        return (img);
    }

    private double hoverstart;
    private Tex shorttip, longtip;
    private List<ItemInfo> ttinfo = null;

    public Object tooltip(Coord c, Widget prev) {
        double now = Utils.rtime();
        if (prev != this)
            hoverstart = now;
        try {
            List<ItemInfo> info = info();
            if (now - hoverstart < 1.0 && !Config.longtooltips) {
                if (shorttip == null)
                    shorttip = new TexI(shorttip());
                return (shorttip);
            } else {
                if (longtip == null)
                    longtip = new TexI(longtip());
                return (longtip);
            }
        } catch (Loading e) {
            return ("...");
        }
    }

    public int ameter() {
        return ameter;
    }

    public Optional<Resource> res() {
        try {
            if (res != null) {
                return Optional.of(res.get());
            } else {
                return Optional.empty();
            }
        } catch (Loading e) {
            return Optional.empty();
        }
    }

    public void reqdestroy() {
        anims.clear();
        final Coord o = this.c;
        dest = true;
        new NormAnim(0.35) {
            public void ntick(double a) {
                Buff.this.a = 255 - (int) (255 * a);
                Buff.this.c = o.add(0, (int) (a * a * cframe.sz().y));
                if (a == 1.0)
                    destroy();
            }
        };
    }

    public void move(Coord c, double off) {
        if (dest)
            return;
        double ival = 0.8;
        double foff = off * (1.0 - 0.8);
        final Coord o = this.c;
        final Coord d = c.sub(o);
        new NormAnim(0.5) {
            public void ntick(double a) {
                a = Utils.clip((a - foff) * (1.0 / ival), 0, 1);
                Buff.this.c = o.add(d.mul(Utils.smoothstep(a)));
            }
        };
    }

    public void move(Coord c) {
        move(c, 0);
    }

    public void uimsg(String msg, Object... args) {
        if (msg == "ch") {
            this.res = ui.sess.getres((Integer) args[0]);
        } else if (msg == "tt") {
            info = null;
            rawinfo = new ItemInfo.Raw(args);
            shorttip = longtip = null;
        } else if (msg == "tip") {
            String tt = (String) args[0];
            this.tt = tt.isEmpty() ? null : tt;
            Matcher mat = travelpat.matcher(tt);
            if (mat.find()) {
                dev.simpleLog(tt.replaceAll(travelpat.pattern(), "$1/$2 - $3"));
            }
            shorttip = longtip = null;
        } else if (msg == "am") {
            this.ameter = (Integer) args[0];
            shorttip = longtip = null;
            if (atex != null)
                atex.dispose();
            atex = null;
        } else if (msg == "nm") {
            this.nmeter = (Integer) args[0];
            ntext = null;
        } else if (msg == "cm") {
            this.cmeter = ((Number) args[0]).doubleValue() / 100.0;
            this.cmrem = (args.length > 1) ? (((Number) args[1]).doubleValue() * 0.06) : -1;
            gettime = Utils.rtime();
        } else {
            super.uimsg(msg, args);
        }
    }


    public boolean mousedown(Coord c, int btn) {
        if (parent != null && parent instanceof Bufflist && ((Bufflist) parent).moveHit(c, btn)) {
            return false;
        } else {
            wdgmsg("cl", c.sub(imgoff), btn, ui.modflags());
            return (true);
        }
    }
}
