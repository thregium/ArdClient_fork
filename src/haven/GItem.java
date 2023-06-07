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

import haven.purus.pbot.PBotUtils;
import haven.purus.pbot.PBotWindowAPI;
import haven.res.ui.tt.q.qbuff.QBuff;
import haven.resutil.Curiosity;
import haven.resutil.FoodInfo;
import integrations.food.FoodService;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import modification.configuration;
import modification.dev;


import static haven.Text.num10Fnd;
import static haven.Text.num12boldFnd;

public class GItem extends AWidget implements ItemInfo.SpriteOwner, GSprite.Owner {
    public Indir<Resource> res;
    private static ItemFilter filter;
    private static long lastFilter = 0;
    public MessageBuf sdt;
    public int meter = 0;
    public int num = -1;
    public Widget contents = null;
    public String contentsnm = null;
    public Object contentsid = null;
    public int infoseq;
    public Widget hovering;
    private GSprite spr;
    private ItemInfo.Raw rawinfo;
    public List<ItemInfo> info = Collections.emptyList();
    private QBuff quality;
    public Tex metertex;
    public double studytime = 0.0;
    public volatile boolean drop = false;
    private double dropTimer = 0;
    public boolean matches = false;
    public boolean sendttupdate = false;
    private long filtered = 0;
    private boolean postProcessed = false;

    public static void setFilter(ItemFilter filter) {
        GItem.filter = filter;
        lastFilter = System.currentTimeMillis();
    }

    @RName("item")
    public static class $_ implements Factory {
        public Widget create(UI ui, Object[] args) {
            int res = (Integer) args[0];
            Message sdt = (args.length > 1) ? new MessageBuf((byte[]) args[1]) : Message.nil;
            return (new GItem(ui, ui.sess.getres(res), sdt));
        }
    }

    public interface ColorInfo {
        Color olcol();
    }

    public interface OverlayInfo<T> {
        T overlay();

        void drawoverlay(GOut g, T data);
    }

    public static class InfoOverlay<T> {
        public final OverlayInfo<T> inf;
        public final T data;

        public InfoOverlay(OverlayInfo<T> inf) {
            this.inf = inf;
            this.data = inf.overlay();
        }

        public void draw(GOut g) {
            if (!inf.getClass().getName().equals("Level") || configuration.newmountbar)
                inf.drawoverlay(g, data);
        }

        public static <S> InfoOverlay<S> create(OverlayInfo<S> inf) {
            return (new InfoOverlay<S>(inf));
        }
    }

    public interface NumberInfo extends OverlayInfo<Tex> {
        int itemnum();

        default Color numcolor() {
            return (Color.WHITE);
        }

        default Tex overlay() {
            return (new TexI(GItem.NumberInfo.numrender(itemnum(), numcolor())));
        }

        default void drawoverlay(GOut g, Tex tex) {
            if (configuration.shownumeric) {
                Coord btm = configuration.infopos(configuration.numericpos, g.sz, tex.sz());
                g.image(tex, btm);
            }
        }

        static BufferedImage numrender(int num, Color col) {
            if (!Config.largeqfont)
                return Text.renderstroked(num + "", col, Color.BLACK).img;
            else
                return Text.renderstroked(num + "", col, Color.BLACK, num12boldFnd).img;
        }
    }

    public interface MeterInfo {
        double meter();
    }


    public static class Amount extends ItemInfo implements NumberInfo {
        private final int num;

        public Amount(Owner owner, int num) {
            super(owner);
            this.num = num;
        }

        public int itemnum() {
            return (num);
        }
    }

    public GItem(UI ui, Indir<Resource> res, Message sdt) {
        this.ui = ui;
        this.res = res;
        this.sdt = new MessageBuf(sdt);
        waitforinit();
    }

    public GItem(Indir<Resource> res, Message sdt) {
        this(null, res, sdt);
    }

    public GItem(Indir<Resource> res) {
        this(res, Message.nil);
    }

    public String getname() {
        if (rawinfo == null) {
            return "";
        }

        try {
            return ItemInfo.find(ItemInfo.Name.class, info()).str.text;
        } catch (Exception ex) {
            return "";
        }
    }

    private Random rnd = null;

    public Random mkrandoom() {
        if (rnd == null)
            rnd = new Random();
        return (rnd);
    }

    public Resource getres() {
        return (res.get());
    }

    private static final OwnerContext.ClassResolver<GItem> ctxr = new OwnerContext.ClassResolver<GItem>()
            .add(GItem.class, wdg -> wdg)
            .add(Glob.class, wdg -> wdg.ui.sess.glob)
            .add(Session.class, wdg -> wdg.ui.sess);

    public <T> T context(Class<T> cl) {
        return (ctxr.context(cl, this));
    }

    @Deprecated
    public Glob glob() {
        return (ui.sess.glob);
    }

    protected volatile boolean inited;
    protected Loading error;

    private void init() {
        if (spr != null) return;
        this.spr = GSprite.create(this, res.get(), sdt.clone());
    }

    private void waitforinit() {
        if (inited) return;
        try {
            init();
            inited = true;
        } catch (Loading l) {
            error = l;
            l.waitfor(this::waitforinit, waiting -> {});
        }
    }

    private double dropCooldown = Utils.rtime();
    private static final double dropDelay = 0.25;

    public GSprite spr() {
        if (!inited) return (null);
        GSprite spr = this.spr;
        if (!postProcessed && Utils.rtime() - dropCooldown > dropDelay) {
            try {
                if (dropItMaybe()) postProcessed = true;
            } catch (Exception l) {
                dev.simpleLog(l.getMessage());
            }
            dropCooldown = Utils.rtime();
        }
        return (spr);
    }

    public String resname() {
        Resource res = resource();
        if (res != null) {
            return res.name;
        }
        return "";
    }

    private int delay = 0;

    public void tick(double dt) {
        if (!inited) {
            delay += dt;
            return;
        }
        super.tick(dt);
        if (drop) {
            dropTimer += dt;
            if (dropTimer > 0.1) {
                dropTimer = 0;
                wdgmsg("drop", Coord.z);
            }
        }
        GSprite spr = spr();
        if (spr != null) {
            spr.tick(delay + dt);
            delay = 0;
        } else delay += dt;
        testMatch();
        updcontinfo();
        ckconthover();
    }

    public void testMatch() {
        try {
            if (filtered < lastFilter && spr != null) {
                matches = filter != null && filter.matches(info());
                filtered = lastFilter;
            }
        } catch (Loading ignored) {
        }
    }

    public static BufferedImage longtip(GItem item, List<ItemInfo> info) {
        synchronized (Collections.unmodifiableList(info)) {
            BufferedImage img = ItemInfo.longtip(info);
            if (img == null) {
                img = ItemInfo.shorttip(info);
            } else {
                if (info.stream().anyMatch(i -> i instanceof Curiosity || i.getClass().toString().contains("ISlots") || i instanceof FoodInfo)) {
                    UI ui = item.glob().ui.get();
                    if (ui != null && ui.modflags() != UI.MOD_SHIFT) {
                        img = ItemInfo.catimgs_center(5, img, RichText.render("[Shift for details]", new Color(150, 150, 150)).img);
                    }
                }
            }
            Resource.Pagina pg = item.res.get().layer(Resource.pagina);
            if (pg != null)
                img = ItemInfo.catimgs(0, img, RichText.render("\n" + pg.text, 200).img);
            return (img);
        }
    }

    public List<ItemInfo> info() {
        if (info == null) {
            info = ItemInfo.buildinfo(this, rawinfo);
            Resource.Pagina pg = res.get().layer(Resource.pagina);
            if (pg != null)
                info.add(new ItemInfo.Pagina(this, pg.text));
            try {
                // getres() can throw Loading, ignore it
                FoodService.checkFood(info, getres());
            } catch (Exception ex) {
            }
        }
        return (info);
    }

    public <T> Optional<T> getinfo(Class<T> type) {
        try {
            for (final ItemInfo info : info()) {
                if (type.isInstance(info)) {
                    return Optional.of(type.cast(info));
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public <T> Optional<T> getinfo(Class<T> type, List<ItemInfo> infolst) {
        try {
            for (final ItemInfo info : infolst) {
                if (type.isInstance(info)) {
                    return Optional.of(type.cast(info));
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public <T> List<T> getinfos(Class<T> type) {
        final List<T> infos = new ArrayList<>();
        try {
            for (final ItemInfo info : info()) {
                if (type.isInstance(info)) {
                    infos.add(type.cast(info));
                }
            }
            return infos;
        } catch (Exception e) {
            return infos;
        }
    }

    public Optional<String> name() {
        final ItemInfo.Name name = getinfo(ItemInfo.Name.class).orElse(null);
        if (name != null) {
            return Optional.of(name.str.text);
        } else {
            return Optional.empty();
        }
    }

    public Resource resource() {
        return (res.get());
    }

    public GSprite sprite() {
        if (spr == null)
            throw (new Loading("Still waiting for sprite to be constructed"));
        return (spr);
    }

    public void uimsg(String name, Object... args) {
        if (name == "num") {
            num = (Integer) args[0];
        } else if (name == "chres") {
            synchronized (this) {
                res = ui.sess.getres((Integer) args[0]);
                sdt = (args.length > 1) ? new MessageBuf((byte[]) args[1]) : MessageBuf.nil;
                spr = null;
                inited = false;
                waitforinit();
            }
        } else if (name == "tt") {
            info = null;
            if (rawinfo != null)
                quality = null;
            rawinfo = new ItemInfo.Raw(args);
            filtered = 0;
            if (sendttupdate) {
                wdgmsg("ttupdate");
            }
            infoseq++;
        } else if (name == "meter") {
            meter = (int) ((Number) args[0]).doubleValue();
            metertex = Text.renderstroked(String.format("%d%%", meter), Color.WHITE, Color.BLACK, num10Fnd).tex();
        } else if (name == "contopen") {
            boolean nst;
            if (args[0] == null)
                nst = contentswnd == null;
            else
                nst = ((Integer) args[0]) != 0;
            showcontwnd(nst); //FIXME WTF null ebat'
        } else {
            super.uimsg(name, args);
        }
    }

    public void addchild(Widget child, Object... args) {
        /* XXX: Update this to use a checkable args[0] once a
         * reasonable majority of clients can be expected to not crash
         * on that. */
        if (true || ((String) args[0]).equals("contents")) {
            contents = add(child);
            contentsnm = (String) args[1];
            contentsid = null;
            if (args.length > 2)
                contentsid = args[2];
        }
    }

    public void cdestroy(Widget w) {
        super.cdestroy(w);
        if (w == contents) {
            contents = null;
            contentsid = null;
        }
    }

    public interface ContentsInfo {
        void propagate(List<ItemInfo> buf, ItemInfo.Owner outer);
    }

    /* XXX: Please remove me some time, some day, when custom clients
     * can be expected to have merged ContentsInfo. */
    private static void propagate(ItemInfo inf, List<ItemInfo> buf, ItemInfo.Owner outer) {
        try {
            java.lang.reflect.Method mth = inf.getClass().getMethod("propagate", List.class, ItemInfo.Owner.class);
            Utils.invoke(mth, inf, buf, outer);
        } catch (NoSuchMethodException e) {
        }
    }

    private int lastcontseq;
    private List<Pair<GItem, Integer>> lastcontinfo = null;

    private void updcontinfo() {
        if (info == null)
            return;
        Widget contents = this.contents;
        if (contents != null) {
            boolean upd = false;
            if ((lastcontinfo == null) || (lastcontseq != contents.childseq)) {
                lastcontinfo = new ArrayList<>();
                for (Widget ch : contents.children()) {
                    if (ch instanceof GItem) {
                        GItem item = (GItem) ch;
                        lastcontinfo.add(new Pair<>(item, item.infoseq));
                    }
                }
                lastcontseq = contents.childseq;
                upd = true;
            } else {
                for (ListIterator<Pair<GItem, Integer>> i = lastcontinfo.listIterator(); i.hasNext(); ) {
                    Pair<GItem, Integer> ch = i.next();
                    if (ch.b != ch.a.infoseq) {
                        i.set(new Pair<>(ch.a, ch.a.infoseq));
                        upd = true;
                    }
                }
            }
            if (upd) {
                info = null;
                infoseq++;
            }
        } else {
            lastcontinfo = null;
        }
    }

    private void addcontinfo(List<ItemInfo> buf) {
        Widget contents = this.contents;
        if (contents != null) {
            for (Widget ch : contents.children()) {
                if (ch instanceof GItem) {
                    for (ItemInfo inf : ((GItem) ch).info()) {
                        if (inf instanceof ContentsInfo)
                            ((ContentsInfo) inf).propagate(buf, this);
                        else
                            propagate(inf, buf, this);
                    }
                }
            }
        }
    }

    private Widget contparent() {
        /* XXX: This is a bit weird, but I'm not sure what the alternative is... */
        Widget cont = getparent(GameUI.class);
        return ((cont == null) ? cont = ui.root : cont);
    }

    public void destroy() {
        if (contents != null) {
            contents.reqdestroy();
            contents = null;
        }
        if (contentswnd != null) {
            contentswnd.reqdestroy();
            contentswnd = null;
        }
        super.destroy();
    }

    private Widget lcont = null;
    public Contents contentswdg;
    public Window contentswnd;

    private void ckconthover() {
        if (lcont != this.contents) {
            if ((this.contents != null) && (this.contentsid != null) && (contentswdg == null) && (contentswnd == null) &&
                    Utils.getprefb(String.format("cont-wndvis/%s", this.contentsid), false)) {
                Coord c = Utils.getprefc(String.format("cont-wndc/%s", this.contentsid), null);
                if (c != null) {
                    this.contents.unlink();
                    contentswnd = contparent().add(new ContentsWindow(this, this.contents), c);
                }
            }
            lcont = this.contents;
        }
        if (hovering != null) {
            if (contentswdg == null) {
                if ((this.contents != null) && (contentswnd == null)) {
                    Widget cont = contparent();
                    ckparent:
                    for (Widget prev : cont.children()) {
                        if (prev instanceof Contents) {
                            for (Widget p = hovering; p != null; p = p.parent) {
                                if (p == prev)
                                    break ckparent;
                                if (p instanceof Contents)
                                    break;
                            }
                            return;
                        }
                    }
                    this.contents.unlink();
                    contentswdg = cont.add(new Contents(this, this.contents), ui.mc); //hovering.parentpos(cont, hovering.sz.sub(UI.scale(5, 5)).sub(Contents.hovermarg))
                }
            }
        } else {
            if ((contentswdg != null) && !contentswdg.hovering && !contentswdg.hasmore()) {
                contentswdg.reqdestroy();
                contentswdg = null;
            }
        }
        hovering = null;
    }

    private final Object subsync = new Object();

    public void showcontwnd(boolean show) {
        if (show && (contentswnd == null)) {
            Widget cont = contparent();
            Coord wc = null;
            if (this.contentsid != null)
                wc = Utils.getprefc(String.format("cont-wndc/%s", this.contentsid), null);
            if (wc == null)
                wc = cont.rootxlate(ui.mc).add(UI.scale(5, 5));
            contents.unlink();
            if (contentswdg != null) {
                contentswdg.invdest = true;
                contentswdg.reqdestroy();
                contentswdg = null;
            }

            Window anotherWnd = PBotWindowAPI.getWindow(ui, contentsnm);
            ContentsWindow wnd = new ContentsWindow(this, this.contents, anotherWnd == null || Config.stackwindows);

            GameUI gui = getparent(GameUI.class);
            if (!Config.stackwindows && anotherWnd != null && gui != null) wc = gui.optplacement(wnd, wc);

            contentswnd = cont.add(wnd, wc);
            if (this.contentsid != null) {
                Utils.setprefb(String.format("cont-wndvis/%s", this.contentsid), true);
                Utils.setprefc(String.format("cont-wndc/%s", this.contentsid), wc);
            }
        } else if (!show && (contentswnd != null)) {
            contentswnd.reqdestroy();
            contentswnd = null;
        }

        /*if (item.contentswdg != null && item.contentswnd == null) {
//                    Coord c = Utils.getprefc(String.format("cont-wndc/%s", item.contentsid), null);
            Coord off = item.contentswdg.inv.c;
            item.contentswdg.inv.unlink();
            Window anotherWnd = PBotWindowAPI.getWindow(ui, item.contentswdg.cont.contentsnm);
            GItem.ContentsWindow wnd = new GItem.ContentsWindow(item.contentswdg.cont, item.contentswdg.inv, anotherWnd == null || Config.stackwindows);
            off = off.sub(wnd.xlate(wnd.inv.c, true));
            GameUI gui = getparent(GameUI.class);
            Coord toc = item.contentswdg.c.add(off);
            if (!Config.stackwindows && anotherWnd != null && gui != null) toc = gui.optplacement(wnd, toc);
            item.contentswdg.cont.contentswnd = item.contentswdg.parent.add(wnd, toc);
            item.contentswdg.invdest = true;
            item.contentswdg.destroy();
            item.contentswdg.cont.contents = null;
        }*/
    }

    public static class Contents extends Widget {
        public static final Coord hovermarg = UI.scale(8, 8);
        public static final Tex bg = Window.bg;
        public static final IBox obox = Window.wbox;
        public final GItem cont;
        public final Widget inv;
        public boolean invdest;
        private boolean hovering;
        private UI.Grab dm = null;
        private Coord doff;

        public Contents(GItem cont, Widget inv) {
            z(90);
            this.cont = cont;
            /* XXX? This whole movement of the inv widget between
             * various parents is kind of weird, but it's not
             * obviously incorrect either. A proxy widget was tried,
             * but that was even worse, due to rootpos and similar
             * things being unavoidable wrong. */
            this.inv = add(inv, hovermarg.add(obox.ctloff()));
            this.tick(0);
        }

        public void draw(GOut g) {
            Coord bgc = new Coord();
            Coord ctl = hovermarg.add(obox.btloff());
            Coord cbr = sz.sub(obox.cisz()).add(ctl);
            for (bgc.y = ctl.y; bgc.y < cbr.y; bgc.y += bg.sz().y) {
                for (bgc.x = ctl.x; bgc.x < cbr.x; bgc.x += bg.sz().x)
                    g.image(bg, bgc, ctl, cbr);
            }
            obox.draw(g, hovermarg, sz.sub(hovermarg));
            super.draw(g);
        }

        public void tick(double dt) {
            super.tick(dt);
            resize(inv.c.add(inv.sz).add(obox.btloff()));
            hovering = false;
        }

        public void destroy() {
            if (!invdest) {
                inv.unlink();
                cont.add(inv);
            }
            super.destroy();
        }

        public boolean hasmore() {
            for (GItem item : children(GItem.class)) {
                if (item.contentswdg != null)
                    return (true);
            }
            return (false);
        }

        public void cdestroy(Widget w) {
            super.cdestroy(w);
            if (w == inv) {
                cont.cdestroy(w);
                invdest = true;
                this.destroy();
                cont.contentswdg = null;
            }
        }

        public boolean mousedown(Coord c, int btn) {
            if (super.mousedown(c, btn))
                return (true);
            if (btn == 1) {
                dm = ui.grabmouse(this);
                doff = c;
                return (true);
            }
            return (false);
        }

        public boolean mouseup(Coord c, int btn) {
            if ((dm != null) && (btn == 1)) {
                dm.remove();
                dm = null;
                return (true);
            }
            return (super.mouseup(c, btn));
        }

        public void mousemove(Coord c) {
            if (dm != null) {
                if (c.dist(doff) > 10) {
                    dm.remove();
                    dm = null;
                    Coord off = inv.c;
                    inv.unlink();
                    ContentsWindow wnd = new ContentsWindow(cont, inv);
                    off = off.sub(wnd.xlate(wnd.inv.c, true));
                    cont.contentswnd = parent.add(wnd, this.c.add(off));
                    wnd.drag(doff.sub(off));
                    invdest = true;
                    destroy();
                    cont.contentswdg = null;
                }
            } else {
                super.mousemove(c);
            }
        }

        public boolean mousehover(Coord c) {
            super.mousehover(c);
            hovering = true;
            return (true);
        }
    }

    public static class ContentsWindow extends Window {
        public final GItem cont;
        public final Widget inv;
        private boolean invdest;
        private Coord psz = null;
        private Object id;

        public ContentsWindow(GItem cont, Widget inv) {
            this(cont, inv, false);
        }

        public ContentsWindow(GItem cont, Widget inv, boolean loadPosition) {
            super(Coord.z, cont.contentsnm, cont.contentsnm, loadPosition, false);
            this.cont = cont;
            this.inv = add(inv, Coord.z);
            this.id = cont.contentsid;
            this.tick(0);
        }

        private Coord lc = null;

        public void tick(double dt) {
            if (cont.contents != inv) {
                destroy();
                cont.contentswnd = null;
                return;
            }
            super.tick(dt);
            if (!Utils.eq(inv.sz, psz))
                resize(inv.c.add(psz = inv.sz));
            if (!Utils.eq(lc, this.c) && (cont.contentsid != null)) {
                Utils.setprefc(String.format("cont-wndc/%s", cont.contentsid), lc = this.c);
                Utils.setprefb(String.format("cont-wndvis/%s", cont.contentsid), true);
            }
        }

        public void wdgmsg(Widget sender, String msg, Object... args) {
            if ((sender == this) && (msg == "close")) {
                reqdestroy();
                cont.contentswnd = null;
                if (cont.contentsid != null)
                    Utils.setprefb(String.format("cont-wndvis/%s", cont.contentsid), false);
            } else {
                super.wdgmsg(sender, msg, args);
            }
        }

        public void destroy() {
            if (!invdest) {
                inv.unlink();
                cont.add(inv);
            }
            super.destroy();
        }

        public void cdestroy(Widget w) {
            super.cdestroy(w);
            if (w == inv) {
                cont.cdestroy(w);
                invdest = true;
                this.destroy();
                cont.contentswnd = null;
            }
        }
    }

    public void qualitycalc(List<ItemInfo> infolist) {
        for (ItemInfo info : infolist) {
            if (info instanceof QBuff) {
                this.quality = (QBuff) info;
                break;
            }
        }
    }

    public QBuff quality() {
        if (quality == null) {
            try {
                for (ItemInfo info : info()) {
                    if (info instanceof ItemInfo.Contents) {
                        qualitycalc(((ItemInfo.Contents) info).sub);
                        return quality;
                    }
                }
                qualitycalc(info());
            } catch (Loading l) {
            }
        }
        return quality;
    }

    public ItemInfo.Contents getcontents() {
        try {
            for (ItemInfo info : info()) {
                if (info instanceof ItemInfo.Contents)
                    return (ItemInfo.Contents) info;
            }
        } catch (Exception e) { // fail silently if info is not ready
        }
        return null;
    }

    private boolean dropItMaybe() {
        Resource curs = ui.root.getcurs(Coord.z);
        Resource res = this.resource();
        String name = res.basename();
        String resname = res.name;
        ItemInfo.Name infoname = ItemInfo.find(ItemInfo.Name.class, info());
        if (infoname != null) {
            String invname = infoname.str.text;
            for (Map.Entry<String, Boolean> entry : Config.autodroplist.entrySet()) {
                if (entry.getValue() && (invname.equals(entry.getKey()) || resname.equals(entry.getKey()))) {
                    drop = true;
                    return (true);
//                this.wdgmsg("drop", Coord.z);
                }
            }
            if (curs != null && curs.name.equals("gfx/hud/curs/mine")) {
                if (PBotUtils.getStamina(ui) < 40) {
                    PBotUtils.drink(ui, false);
                }
                if (Config.dropMinedStones && Config.mineablesStone.contains(name) ||
                        Config.dropMinedOre && Config.mineablesOre.contains(name) ||
                        Config.dropMinedOrePrecious && Config.mineablesOrePrecious.contains(name) ||
                        Config.dropMinedCatGold && invname.contains("Cat Gold") ||
                        Config.dropMinedCrystals && invname.contains("Strange Crystal") ||
                        Config.dropMinedSeaShells && invname.contains("Petrified Seashell") ||
                        Config.dropMinedQuarryquartz && invname.contains("quarryquartz")) {
                    drop = true;
                    return (true);
                }
//                this.wdgmsg("drop", Coord.z);
            }
        } else {
            return (false);
        }
        return (true);
    }

    public Coord size() {
        GSprite spr = spr();
        if (spr != null) {
            return spr.sz().div(30);
        } else {
            return new Coord(0, 0);
        }
//        try {
//            Indir<Resource> res = getres().indir();
//            if (res.get() != null && res.get().layer(Resource.imgc) != null) {
//                Tex tex = res.get().layer(Resource.imgc).tex();
//                if (tex == null)
//                    return new Coord(1, 1);
//                else
//                    return tex.sz().div(30);
//            } else {
//                return new Coord(1, 1);
//            }
//        } catch (Loading l) {
//
//        }
//        return new Coord(1, 1);
    }
}
