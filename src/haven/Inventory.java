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

import haven.purus.pbot.PBotInventory;
import haven.purus.pbot.PBotItem;
import haven.purus.pbot.PBotUtils;
import haven.res.ui.tt.q.qbuff.QBuff;
import modification.configuration;

import java.awt.Color;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Inventory extends Widget implements DTarget {
    public static final Coord sqsz = UI.scale(new Coord(33, 33));
    public static final Tex invsq/* = Resource.loadtex("gfx/hud/invsq")*/;
    public boolean dropul = true;
    public Coord isz;
    public boolean[] sqmask = null;
    public static final Comparator<WItem> ITEM_COMPARATOR_ASC = new Comparator<WItem>() {
        @Override
        public int compare(WItem o1, WItem o2) {
            QualityList ql1 = o1.itemq.get();
            double q1 = (ql1 != null && !ql1.isEmpty()) ? ql1.single(QualityList.SingleType.Average).value : 0;

            QualityList ql2 = o2.itemq.get();
            double q2 = (ql2 != null && !ql2.isEmpty()) ? ql2.single(QualityList.SingleType.Average).value : 0;

            return Double.compare(q1, q2);
        }
    };
    public static final Comparator<WItem> ITEM_COMPARATOR_DESC = new Comparator<WItem>() {
        @Override
        public int compare(WItem o1, WItem o2) {
            return ITEM_COMPARATOR_ASC.compare(o2, o1);
        }
    };

    public boolean locked = false;
    public Map<GItem, WItem> wmap = new HashMap<GItem, WItem>();

    static {
        Coord sz = sqsz.add(1, 1);
        WritableRaster buf = PUtils.imgraster(sz);
        for (int i = 1, y = sz.y - 1; i < sz.x - 1; i++) {
            buf.setSample(i, 0, 0, 20);
            buf.setSample(i, 0, 1, 28);
            buf.setSample(i, 0, 2, 21);
            buf.setSample(i, 0, 3, 167);
            buf.setSample(i, y, 0, 20);
            buf.setSample(i, y, 1, 28);
            buf.setSample(i, y, 2, 21);
            buf.setSample(i, y, 3, 167);
        }
        for (int i = 1, x = sz.x - 1; i < sz.y - 1; i++) {
            buf.setSample(0, i, 0, 20);
            buf.setSample(0, i, 1, 28);
            buf.setSample(0, i, 2, 21);
            buf.setSample(0, i, 3, 167);
            buf.setSample(x, i, 0, 20);
            buf.setSample(x, i, 1, 28);
            buf.setSample(x, i, 2, 21);
            buf.setSample(x, i, 3, 167);
        }
        for (int y = 1; y < sz.y - 1; y++) {
            for (int x = 1; x < sz.x - 1; x++) {
                buf.setSample(x, y, 0, 36);
                buf.setSample(x, y, 1, 52);
                buf.setSample(x, y, 2, 38);
                buf.setSample(x, y, 3, 125);
            }
        }
        invsq = new TexI(PUtils.rasterimg(buf));
    }

    @RName("inv")
    public static class $_ implements Factory {
        public Widget create(UI ui, Object[] args) {
            return (new Inventory((Coord) args[0]));
        }
    }

    public final Map<String, Tex> cached = new HashMap<>();

    public void draw(GOut g) {
        Coord c = new Coord();
        int mo = 0;
        int cell = 0;
        for (c.y = 0; c.y < isz.y; c.y++) {
            for (c.x = 0; c.x < isz.x; c.x++) {
                if ((sqmask != null) && sqmask[mo++])
                    continue;
                g.image(invsq, c.mul(sqsz));
                if (configuration.showinvnumber)
                    g.aimage(cached.computeIfAbsent(Integer.toString(++cell), s -> Text.render(s, new Color(255, 255, 255, 100)).tex()), c.mul(sqsz).add(invsq.sz().div(2)), 0.5, 0.5);
            }
        }
        super.draw(g);
    }

    public Inventory(Coord sz) {
//        super(invsq.sz().add(new Coord(-1, -1)).mul(sz).add(new Coord(1, 1)));
        super(sqsz.mul(sz).add(1, 1));
        isz = sz;
    }

    public boolean mousewheel(Coord c, int amount) {
        if (ui.modshift) {
            Inventory minv = getparent(GameUI.class).maininv;
            if (minv != this) {
                if (amount < 0)
                    wdgmsg("invxf", minv.wdgid(), 1);
                else if (amount > 0)
                    minv.wdgmsg("invxf", this.wdgid(), 1);
            }
        }
        return (true);
    }

    @Override
    public boolean mousedown(Coord c, int button) {
        return !locked && super.mousedown(c, button);
    }

    public void addchild(Widget child, Object... args) {
        add(child);
        Coord c = (Coord) args[0];
        if (child instanceof GItem) {
            GItem i = (GItem) child;
            wmap.put(i, add(new WItem(i), c.mul(sqsz).add(1, 1)));
        }
    }

    public void cdestroy(Widget w) {
        super.cdestroy(w);
        if (w instanceof GItem) {
            GItem i = (GItem) w;
            ui.destroy(wmap.remove(i));
        }
    }

    public boolean drop(Coord cc, Coord ul) {
        Coord dc = dropul ? ul.add(sqsz.div(2)).div(sqsz) : cc.div(sqsz);
        wdgmsg("drop", dc);
        return (true);
    }

    public boolean iteminteract(Coord cc, Coord ul) {
        return (false);
    }

    public void uimsg(String msg, Object... args) {
        if (msg == "sz") {
            isz = (Coord) args[0];
            resize(invsq.sz().add(UI.scale(new Coord(-1, -1))).mul(isz).add(UI.scale(new Coord(1, 1))));
            sqmask = null;
        } else if (msg == "mask") {
            boolean[] nmask;
            if (args[0] == null) {
                nmask = null;
            } else {
                nmask = new boolean[isz.x * isz.y];
                byte[] raw = (byte[]) args[0];
                for (int i = 0; i < isz.x * isz.y; i++)
                    nmask[i] = (raw[i >> 3] & (1 << (i & 7))) != 0;
            }
            this.sqmask = nmask;
        } else if (msg == "mode") {
            dropul = (((Integer) args[0]) == 0);
        } else {
            super.uimsg(msg, args);
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        final boolean check = args.length > 0 && args[0] instanceof GItem;
        if (check && msg.equals("drop-identical")) {
            GItem gitem = (GItem) args[0];
            Color colorIdentical = null;
            for (WItem item : getIdenticalItems(gitem, false)) {
                try {
                    if (Config.dropcolor) {
                        if (gitem.quality() != null) {
                            if (gitem.quality().color != null) {
                                if (colorIdentical == null) {
                                    colorIdentical = gitem.quality().color;
                                }
                                if (!item.qq.color.equals(colorIdentical)) {
                                    continue;
                                }
                            }
                        }
                    }
                    item.item.wdgmsg("drop", Coord.z);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        } else if (check && msg.startsWith("transfer-identical")) {
//            Window stockpile = ui.gui.getwnd("Stockpile");
//            Window smelter = ui.gui.getwnd("Ore Smelter");
//            Window kiln = ui.gui.getwnd("Kiln");
//            if (stockpile == null || smelter != null || kiln != null) {
            GItem gitem = (GItem) args[0];
            boolean eq = msg.endsWith("eq");
            List<WItem> items = getIdenticalItems(gitem, eq);
            if (!eq) {
                int asc = msg.endsWith("asc") ? 1 : -1;
                try {
                    items.sort((a, b) -> {
                        QBuff aq = a.item.quality();
                        QBuff bq = b.item.quality();
                        if (aq == null || bq == null) {
                            if (aq == null && bq == null) return (0);
                            else if (bq == null) return (asc);
                            else return (-asc);
                        } else {
                            if (aq.q == bq.q) return (0);
                            else if (aq.q > bq.q) return (asc);
                            else return (-asc);
                        }
                    });
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }

            Color colorIdentical = null;
            for (WItem item : items) {
                try {
                    if (Config.transfercolor) {
                        if (gitem.quality() != null) {
                            if (gitem.quality().color != null) {
                                if (colorIdentical == null) {
                                    colorIdentical = gitem.quality().color;
                                }
                                if (item.qq == null || !item.qq.color.equals(colorIdentical)) {
                                    continue;
                                }
                            }
                        } else {
                            if (item.qq != null) {
                                continue;
                            }
                        }
                    }
                    item.item.wdgmsg("transfer", Coord.z);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
//            } else {
//                for (Widget w = stockpile.lchild; w != null; w = w.prev) {
//                    if (w instanceof ISBox) {
//                        ISBox isb = (ISBox) w;
//                        int freespace = isb.getfreespace();
//                        for (WItem item : getIdenticalItems((GItem) args[0])) {
//                            if (freespace-- <= 0)
//                                break;
//                            item.item.wdgmsg("take", new Coord(item.sz.x / 2, item.sz.y / 2));
//                            isb.drop(null, null);
//                        }
//                        break;
//                    }
//                }
//            }
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    public List<WItem> getIdenticalItems(GItem item) {
        List<WItem> items = new ArrayList<WItem>();
        GSprite sprite = item.spr();
        if (sprite != null) {
            String name = sprite.getname();
            String resname = item.resource().name;
            for (Widget wdg = child; wdg != null; wdg = wdg.next) {
                if (wdg instanceof WItem) {
                    if (!((WItem) wdg).locked()) {
                        sprite = ((WItem) wdg).item.spr();
                        if (sprite != null) {
                            Resource res = ((WItem) wdg).item.resource();
                            if (res != null && res.name.equals(resname) && (name == null || name.equals(sprite.getname())))
                                items.add((WItem) wdg);
                        }
                    }
                }
            }
        }
        return items;
    }

    public List<WItem> getIdenticalItems(GItem item, boolean quality) {
        List<WItem> items = new ArrayList<WItem>();
        double q0 = 0;
        if (quality) {
            QBuff aq = item.quality();
            if (aq != null)
                q0 = aq.q;
        }
        GSprite sprite = item.spr();
        if (sprite != null) {
            String name = sprite.getname();
            String resname = item.resource().name;
            for (Widget wdg = child; wdg != null; wdg = wdg.next) {
                if (wdg instanceof WItem) {
                    if (!((WItem) wdg).locked()) {
                        GItem it = ((WItem) wdg).item;
                        sprite = it.spr();
                        if (sprite != null) {
                            Resource res = it.resource();
                            if (res != null && res.name.equals(resname) && (name == null || name.equals(sprite.getname()))) {
                                if (quality) {
                                    QBuff bq = it.quality();
                                    if (bq != null) {
                                        double q1 = bq.q - q0;
                                        if (q1 < 0.1 && q1 > -0.1)
                                            items.add((WItem) wdg);
                                    }
                                } else {
                                    items.add((WItem) wdg);
                                }
                            }
                        }
                    }
                }
            }
        }
        return items;
    }

    /* Following getItem* methods do partial matching of the name *on purpose*.
       Because when localization is turned on, original English name will be in the brackets
       next to the translation
    */
    public List<WItem> getItemsPartial(String... names) {
        List<WItem> items = new ArrayList<WItem>();
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                String wdgname = ((WItem) wdg).item.getname();
                for (String name : names) {
                    if (wdgname.contains(name)) {
                        items.add((WItem) wdg);
                        break;
                    }
                }
            }
        }
        return items;
    }

    public WItem getItemPartial(String name) {
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                String wdgname = ((WItem) wdg).item.getname();
                if (wdgname.contains(name))
                    return (WItem) wdg;
            }
        }
        return null;
    }

    public WItem getItemPartialTrays(String name) {
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                String wdgname = ((WItem) wdg).item.getname();
                if (wdgname.contains(name))
                    return (WItem) wdg;
            }
        }
        return null;
    }

    public WItem getItemPartialDrink(String name) {
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                String wdgname = ((WItem) wdg).item.getname();
                if (wdgname.contains(name))
                    if (!PBotUtils.canDrinkFrom((WItem) wdg))
                        return null;
                if (PBotUtils.canDrinkFrom((WItem) wdg)) {
                    return (WItem) wdg;
                }
            }
        }
        return null;
    }

    public int getItemPartialCount(String name) {
        int count = 0;
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                String wdgname = ((WItem) wdg).item.getname();
                if (wdgname.contains(name))
                    count++;
            }
        }
        return count;
    }

    public int getFreeSpace() {
        int freespace = getMaxSlots();
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem)
                freespace -= (wdg.sz.x * wdg.sz.y) / (sqsz.x * sqsz.y);
        }
        return freespace;
    }

    // Null if no free slots found
    public Coord getFreeSlot() {
        int[][] invTable = new int[isz.x][isz.y];
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                WItem item = (WItem) wdg;
                for (int i = 0; i < item.sz.div(sqsz).y; i++)
                    for (int j = 0; j < item.sz.div(sqsz).x; j++)
                        invTable[item.c.div(sqsz).x + j][item.c.div(sqsz).y + i] = 1;
            }
        }
        int mo = 0;
        for (int i = 0; i < isz.y; i++) {
            for (int j = 0; j < isz.x; j++) {
                if ((sqmask != null) && sqmask[mo++]) continue;
                if (invTable[j][i] == 0)
                    return (new Coord(j, i));
            }
        }
        return (null);
    }

    public List<Coord> getFreeSlots() {
        List<Coord> cordlist = new ArrayList<>();
        int[][] invTable = new int[isz.x][isz.y];
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                WItem item = (WItem) wdg;
                for (int i = 0; i < item.sz.div(sqsz).y; i++)
                    for (int j = 0; j < item.sz.div(sqsz).x; j++)
                        invTable[item.c.div(sqsz).x + j][item.c.div(sqsz).y + i] = 1;
            }
        }
        int mo = 0;
        for (int i = 0; i < isz.y; i++) {
            for (int j = 0; j < isz.x; j++) {
                if ((sqmask != null) && sqmask[mo++]) continue;
                if (invTable[j][i] == 0) cordlist.add(new Coord(j, i));
            }
        }
        return (cordlist);
    }

    public int getMaxSlots() {
        Coord c = new Coord();
        int mo = 0;
        int max = 0;
        for (c.y = 0; c.y < isz.y; c.y++) {
            for (c.x = 0; c.x < isz.x; c.x++) {
                if (sqmask == null || !sqmask[mo++]) max++;
            }
        }
        return (max);
    }

    public boolean drink(int threshold) {
        IMeter.Meter stam = ui.gui.getmeter("stam", 0);
        if (stam == null || stam.a > threshold)
            return (false);

        List<WItem> containers = getItemsPartial("Waterskin", "Waterflask", "Kuksa");
        for (WItem wi : containers) {
            ItemInfo.Contents cont = wi.item.getcontents();
            if (cont != null) {
                FlowerMenu.setNextSelection("Drink");
                ui.lcc = wi.rootpos();
                wi.item.wdgmsg("iact", wi.c, 0);
                return (true);
            }
        }

        return (false);
    }

    public static Coord invsz(Coord sz) {
        return invsq.sz().add(new Coord(-1, -1)).mul(sz).add(new Coord(1, 1));
    }

    public static Coord sqroff(Coord c) {
        return c.div(invsq.sz());
    }

    public static Coord sqoff(Coord c) {
        return c.mul(invsq.sz());
    }

    public void stack() {
        Set<String> ignore = new HashSet<>();
        PBotInventory inv = new PBotInventory(this);
        while (true) {
            try {
                List<PBotItem> items = inv.getInventoryContents().stream().filter(p -> !p.isStack()).filter(p -> !ignore.contains(p.getResname())).collect(Collectors.toList());
                if (!items.isEmpty()) {
                    PBotItem item = items.get(0);
                    String name = item.getResname();
                    List<PBotItem> resItems = inv.getInventoryItemsByResnames(name).stream().filter(p -> !p.isStack()).filter(p -> !p.coord().equals(item.coord())).collect(Collectors.toList());
                    if (!resItems.isEmpty()) {
                        if (item.takeItem(10)) {
                            PBotUtils.sleep(250);
                            resItems.get(0).itemact(3);
                            if (!waitHandOut(250)) {
                                if (!inv.dropItemToInventory(item.coord(), 250)) {
                                    PBotUtils.sysMsg(ui, "Stack broken");
                                }
                            }
                        }
                    }
                    ignore.add(name);
                } else break;
            } catch (Loading l) {}
            PBotUtils.sleep(50);
        }
        PBotUtils.sysMsg(ui, "Stack resolved");
    }

    public void unstack() {
        PBotInventory inv = new PBotInventory(this);
        while (true) {
            try {
                List<PBotItem> stacks = inv.getInventoryContents().stream().filter(p -> p.isStack()).collect(Collectors.toList());
                if (!stacks.isEmpty()) {
                    PBotItem stack = stacks.get(0);
                    if (inv.freeSpaceForItem(stack) != null) {
                        stack.activateItem();
                    } else break;
                } else break;
            } catch (Loading l) {}
            PBotUtils.sleep(50);
        }
    }

    private boolean waitHandOut(int time) {
        for (int i = 0; i < time && PBotUtils.getItemAtHand(ui) != null; i += 25) PBotUtils.sleep(25);
        return (PBotUtils.getItemAtHand(ui) == null);
    }

    /**
     * Sorting system
     */
    public Window sortingWindow() {
        Window window = new Window(Coord.z, "Sorting");
        final String[] t = {"reverse", "none", "normal"};
        Label ql = new Label("Quality ");
        Label qt = new Label("normal    ");
        HSlider quality = new HSlider(50, 0, 2, 2) {
            @Override
            public void changed() {
                qt.settext(t[val]);
            }
        };
        Label rl = new Label("ResName ");
        Label rt = new Label("normal    ");
        HSlider resname = new HSlider(50, 0, 2, 2) {
            @Override
            public void changed() {
                rt.settext(t[val]);
            }
        };
        Label nl = new Label("Name ");
        Label nt = new Label("normal    ");
        HSlider name = new HSlider(50, 0, 2, 2) {
            @Override
            public void changed() {
                nt.settext(t[val]);
            }
        };
        Button sort = new Button("Sorting") {
            @Override
            public void click() {
                Defer.later(() -> {
                    try {
                        String s = "";
                        if (quality.val == 2)
                            s = s.concat("q");
                        else if (quality.val == 0)
                            s = s.concat("!q");
                        if (resname.val == 2)
                            s = s.concat("r");
                        else if (resname.val == 0)
                            s = s.concat("!r");
                        if (name.val == 2)
                            s = s.concat("n");
                        else if (name.val == 0)
                            s = s.concat("!n");
                        sort(s);
                    } catch (Exception e) {
                        e.printStackTrace();
                        PBotUtils.sysMsg(ui, "Sorting Error!");
                    }
                    return (null);
                });
                window.reqdestroy();
            }
        };
        WidgetVerticalAppender wva = new WidgetVerticalAppender(window);
        wva.addRow(ql, qt, quality);
        wva.addRow(rl, rt, resname);
        wva.addRow(nl, nt, name);
        wva.add(sort);
        window.pack();
        return (window);
    }

    public void sort(String s) { //string with q n r
        PBotUtils.sysMsg(ui, "Sorting! Please don't move!");
        List<InvItem> items = new ArrayList<>();
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                InvItem item = new InvItem((WItem) wdg);
                if (!item.getSize().equals(new Coord(1, 1))) {
                    PBotUtils.sysMsg(ui, "Not support large items! " + item.getName());
                    return;
                }
                items.add(item);
            }
        }

        items.sort(Comparator.comparing(InvItem::getSloti));
        if (s.contains("!n"))
            items.sort((o1, o2) -> o2.getName().compareTo(o1.getName()));
        else if (s.contains("n"))
            items.sort(Comparator.comparing(InvItem::getName));
        if (s.contains("!q"))
            items.sort((o1, o2) -> {
                if (o1.getQuality() == null && o2.getQuality() == null) return (0);
                if (o1.getQuality() == null && o2.getQuality() != null) return (-1);
                if (o1.getQuality() != null && o2.getQuality() == null) return (1);
                return Objects.requireNonNull(o2.getQuality()).compareTo(o1.getQuality());
            });
        else if (s.contains("q"))
            items.sort((o1, o2) -> {
                if (o1.getQuality() == null && o2.getQuality() == null) return (0);
                if (o1.getQuality() == null && o2.getQuality() != null) return (1);
                if (o1.getQuality() != null && o2.getQuality() == null) return (-1);
                return Objects.requireNonNull(o1.getQuality()).compareTo(o2.getQuality());
            });

        if (s.contains("!r"))
            items.sort((o1, o2) -> o2.getResname().compareTo(o1.getResname()));
        else if (s.contains("r"))
            items.sort(Comparator.comparing(InvItem::getResname));

        sort:
        for (int i = 0; i < items.size(); i++) {
            InvItem invItem = items.get(i);
            InvItem iItem = getItem(invItem.getSlot());
            if (invItem.equals(iItem) && invItem.getSloti() != i) {
                if (invItem.take() == null)
                    break;
                InvItem item = getItem(slotiToCoord(i));
                if (!drop(slotiToCoord(i)))
                    break;
                while (item != null && ui.gui.vhand != null) {
                    Integer in = getInt(items, item);
                    if (in == null)
                        break;
                    item = getItem(slotiToCoord(in));
                    if (!drop(slotiToCoord(in)))
                        break sort;
                }
            }
        }
        PBotUtils.sysMsg(ui, "Sorting finished!");
    }

    public Integer getInt(List<InvItem> items, InvItem item) {
        for (int i = 0; i < items.size(); i++)
            if (items.get(i).equals(item))
                return (i);
        return (null);
    }

    public InvItem getItem(Coord slot) {
        InvItem item = null;
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                WItem w = (WItem) wdg;
                if (w.c.sub(1, 1).div(sqsz.x, sqsz.y).equals(slot)) {
                    item = new InvItem(w);
                    break;
                }
            }
        }
        return (item);
    }

    public boolean drop(Coord slot) {
        InvItem item = getItem(slot);
        wdgmsg("drop", slot);
        InvItem nitem = getItem(slot);
        for (int sleep = 10; nitem == null || nitem == item; ) {
            nitem = getItem(slot);
            PBotUtils.sleep(sleep);
        }
        return (true);
    }

    public Coord slotiToCoord(int slot) {
        Coord c = new Coord();
        int mo = 0;
        int max = 0;
        for (c.y = 0; c.y < isz.y; c.y++) {
            for (c.x = 0; c.x < isz.x; c.x++) {
                if (sqmask == null || !sqmask[mo++]) {
                    if (slot == max) return (c);
                    else max++;
                }
            }
        }
        return (null);
    }

    public class InvItem {
        private final WItem wItem;
        private GItem gItem;
        private String resname;
        private String name;
        private boolean qinit = false;
        private Double quality;
        private Coord slot;
        private Integer sloti;
        private Coord size;

        public InvItem(WItem wItem) {
            this.wItem = wItem;
        }

        public WItem getWItem() {
            return (this.wItem);
        }

        public GItem getGItem() {
            if (this.gItem == null)
                this.gItem = getWItem().item;
            return (this.gItem);
        }

        public String getResname() {
            if (this.resname == null) {
                Resource res = null;
                for (int sleep = 10; res == null; ) {
                    res = getGItem().resource();
                    PBotUtils.sleep(sleep);
                }
                this.resname = res.name;
            }
            return (this.resname);
        }

        public String getName() {
            if (this.name == null) {
                Optional<String> n = getGItem().name();
                for (int sleep = 10; !n.isPresent(); ) {
                    n = getGItem().name();
                    PBotUtils.sleep(sleep);
                }
                this.name = n.get();
            }
            return (this.name);
        }

        public Double getQuality() {
            if (!this.qinit) {
                QBuff qBuff = getGItem().quality();
                this.quality = qBuff == null ? null : qBuff.q;
                this.qinit = true;
            }
            return (this.quality);
        }

        public Coord getSlot() {
            if (this.slot == null) {
                this.slot = getWItem().c.sub(1, 1).div(sqsz.x, sqsz.y);
            }
            return (this.slot);
        }

        public Integer getSloti() {
            if (this.sloti == null) {
                this.sloti = slotToInt(getSlot());
            }
            return (this.sloti);
        }

        public Coord getSize() {
            if (this.size == null) {
                GSprite spr = null;
                for (int sleep = 10; spr == null; ) {
                    spr = getGItem().spr();
                    PBotUtils.sleep(sleep);
                }
                this.size = spr.sz().div(30);
            }
            return (this.size);
        }

        public WItem take() {
            for (int i = 0; i < 5; i++) {
                getGItem().wdgmsg("take", Coord.z);

                for (int t = 0, sleep = 10; ui.gui.vhand == null; t += sleep) {
                    if (t >= 1000)
                        break;
                    else
                        PBotUtils.sleep(sleep);
                }
                if (ui.gui.vhand != null)
                    break;
            }
            return (ui.gui.vhand);
        }

        public Integer slotToInt(Coord slot) {
            Coord c = new Coord();
            int mo = 0;
            int max = 0;
            for (c.y = 0; c.y < isz.y; c.y++) {
                for (c.x = 0; c.x < isz.x; c.x++) {
                    if (sqmask == null || !sqmask[mo++]) {
                        if (slot.x == c.x && slot.y == c.y) return (max);
                        else max++;
                    }
                }
            }
            return (null);
        }

        @Override
        public String toString() {
            return "InvItem{" +
                    "wItem=" + wItem +
                    ", resname='" + resname + '\'' +
                    ", name='" + name + '\'' +
                    ", quality=" + quality +
                    ", slot=" + slot +
                    ", sloti=" + sloti +
                    ", slotrollback=" + slotiToCoord(sloti) +
                    ", size=" + size +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof InvItem)) return false;
            InvItem invItem = (InvItem) o;
            return getWItem().equals(invItem.getWItem());
        }

        @Override
        public int hashCode() {
            return Objects.hash(wItem);
        }
    }

    public void openStacks() {
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                WItem w = (WItem) wdg;
                if (w.item.contents != null) w.item.showcontwnd(true);
            }
        }
    }
}
