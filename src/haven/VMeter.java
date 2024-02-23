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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VMeter extends Widget implements ItemInfo.Owner {
    static Tex bg = Theme.tex("vm", 0);
    static Tex fg = Theme.tex("vm", 1);
    //    Color cl;
//    public int amount;
    private ItemInfo.Raw rawinfo = null;
    private List<ItemInfo> info = Collections.emptyList();
    private static final Map<String, Integer> levels = new HashMap<String, Integer>(3) {{
        put("Oven", 3 * 4);   // amount per unit * number of units
        put("Finery Forge", 6 * 2);
        put("Ore Smelter", (int) (3.3 * 12));
        put("Smith's Smelter", (int) (3.3 * 12));
    }};
    public static final List<Kit> kits = new ArrayList<Kit>() {{
        add(new Kit("Cauldron", new ArrayList<TypeLimit>() {{
            add(new TypeLimit(new Color(71, 101, 153), 30f, "L"));
            add(new TypeLimit(new Color(255, 128, 0), 10f, "ticks", TypeLimit.Tooltip.fuel));
        }}));
        add(new Kit("Tub", new ArrayList<TypeLimit>() {{
            add(new TypeLimit(new Color(165, 117, 62), 40f, "L"));
        }}));
        add(new Kit("Fireplace", new ArrayList<TypeLimit>() {{
            add(new TypeLimit(new Color(255, 128, 0), 10f, "ticks", TypeLimit.Tooltip.fireplace));
        }}));
        add(new Kit("Kiln", new ArrayList<TypeLimit>() {{
            add(new TypeLimit(new Color(255, 128, 0), 30f, "ticks", TypeLimit.Tooltip.fuel));
        }}));
        add(new Kit("Oven", new ArrayList<TypeLimit>() {{
            add(new TypeLimit(new Color(255, 128, 0), 30f, "ticks", TypeLimit.Tooltip.fuel, "$b{$col[255,128,0]{\n4 ticks to cook}}"));
        }}));
        add(new Kit("Ore Smelter", new ArrayList<TypeLimit>() {{
            add(new TypeLimit(new Color(255, 128, 0), 30f, "ticks", TypeLimit.Tooltip.smeltery, "$b{$col[255,128,0]{\n12 ticks to smelt\n9 ticks to smelt well mined}}"));
        }}));
        add(new Kit("Smith's Smelter", new ArrayList<TypeLimit>() {{
            add(new TypeLimit(new Color(255, 128, 0), 30f, "ticks", TypeLimit.Tooltip.smeltery, "$b{$col[255,128,0]{\n12 ticks to smelt.\n9 ticks to smelt well mined}}"));
        }}));
        add(new Kit("Steelbox", new ArrayList<TypeLimit>() {{
            add(new TypeLimit(new Color(255, 128, 0), 18f, "ticks", TypeLimit.Tooltip.crucible, "$b{$col[255,128,0]{\n84 ticks to steel}}"));
        }}));
    }};
    public List<IMeter.Meter> meters;

    public static class Kit {
        public final String windowName;
        public final List<TypeLimit> typeLimit;

        public Kit(String windowName, ArrayList<TypeLimit> typeLimit) {
            this.windowName = windowName;
            this.typeLimit = typeLimit;
        }

        public static Kit getKit(String windowName) {
            for (Kit kit : kits)
                if (kit.windowName.equals(windowName))
                    return (kit);
            return (null);
        }
    }

    public static class TypeLimit {
        public final Color color;
        public final double limit;
        public final String subText;
        public final String tooltip;
        public final String addTooltip;

        public TypeLimit(Color color, double limit, String subText, Tooltip tooltip, String addTooltip) {
            this.color = color;
            this.limit = limit;
            this.subText = subText;
            this.tooltip = getTooltip(tooltip);
            this.addTooltip = addTooltip;
        }

        public TypeLimit(Color color, double limit, String subText, Tooltip tooltip) {
            this(color, limit, subText, tooltip, "");
        }

        public TypeLimit(Color color, double limit, String subText) {
            this(color, limit, subText, null);
        }

        public static TypeLimit getTypeLimit(Kit kit, Color color) {
            for (TypeLimit typeLimit : kit.typeLimit)
                if (typeLimit.color.equals(color))
                    return (typeLimit);
            return (null);
        }

        enum Tooltip {
            fuel, fireplace, smeltery, crucible
        }

        public String getTooltip(Tooltip type) {
            if (type == null) return "";
            switch (type) {
                case fuel:
                    return "\n1 tick = 15 minutes\n1 branch = 1 tick\nCoal, Black coal = 2 ticks\nBlock of Wood = 5 ticks\nTarsticks = 20 ticks";
                case fireplace:
                    return "\n1 tick = 20 minutes\n1 branch = 1 tick\nCoal, Black coal = 2 ticks\nBlock of Wood = 5 ticks\nTarsticks = 20 ticks";
                case smeltery:
                    return "\n1 tick = 15 minutes\nCoal, Black coal = 1 ticks";
                case crucible:
                    return "\n1 tick = 2 hour\n1 branch = 1 tick\nCoal, Black coal = 2 ticks\nBlock of Wood = 5 ticks\nTarsticks = 20 ticks";
                default:
                    return "";
            }
        }
    }

    @RName("vm")
    public static class $_ implements Factory {
        public Widget create(UI ui, Object[] args) {
            return (new VMeter(decmeters(args, 0)));
        }
    }

    /*public VMeter(int amount, Color cl) {
        super(bg.sz());
        this.amount = amount;
        this.cl = cl;
    }*/

    public VMeter(List<IMeter.Meter> meters) {
        super(bg.sz());
        set(meters);
    }

    public void draw(GOut g) {
        int hm = (sz.y - 6);
        g.image(bg, Coord.z);
        for (IMeter.Meter m : meters) {
            g.chcolor(m.c);
            int h = (hm * m.a) / 100;
            g.image(fg, new Coord(0, 0), new Coord(0, sz.y - 3 - h), sz.add(0, h));
            g.chcolor();
        }

        Widget p = this.parent;
        if (p instanceof Window) {
            Integer lvl = levels.get(((Window) p).origcap);
            if (lvl != null) {
                g.chcolor(Color.WHITE);
                int y = sz.y - 3 - (hm * lvl) / 100;
                g.line(new Coord(3, y), new Coord(sz.x - 3, y), 1);
                g.chcolor();
            }
        }
    }

    private static final OwnerContext.ClassResolver<VMeter> ctxr = new OwnerContext.ClassResolver<VMeter>()
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

    private static final String INGAME_TIME = "\nIngame time";
    private double hoverstart;
    private Tex shorttip, longtip;

    public Object tooltip(Coord c, Widget prev) {
        if (ui.modctrl) {
            Widget p = this.parent;
            if (p instanceof Window) {
                for (IMeter.Meter m : meters) {
                    for (Kit kit : kits) {
                        if (((Window) p).origcap.equals(kit.windowName)) {
                            for (TypeLimit tl : kit.typeLimit) {
                                if (m.c.equals(tl.color)) {
                                    String ca = (tl.limit * m.a / 100 % 1 == 0 ? String.format("%.0f", tl.limit * m.a / 100) : tl.limit * m.a / 100) + "";
                                    String cl = (tl.limit % 1 == 0 ? String.format("%.0f", tl.limit) : tl.limit) + "";
                                    String stt = "$b{$col[255,223,5]{" + ca + " / " + cl + " " + tl.subText + " (" + m.a + "%)}}";
                                    return (RichText.render(stt + tl.tooltip + INGAME_TIME + tl.addTooltip, -1).tex());
                                }
                            }
                        }
                    }
                }
                return (RichText.render("$b{$col[255,223,5]{" + meters.stream().map(meter -> meter.a).collect(Collectors.toList()) + "%}}", -1).tex());
            }
        }
        if (rawinfo == null) {
            return (super.tooltip(c, prev));
        }
        double now = Utils.rtime();
        if (prev != this)
            hoverstart = now;
        try {
            if (now - hoverstart < 1.0 && !Config.longtooltips) {
                if (shorttip == null)
                    shorttip = new TexI(ItemInfo.shorttip(info()));
                return (shorttip);
            } else {
                if (longtip == null)
                    longtip = new TexI(ItemInfo.longtip(info()));
                return (longtip);
            }
        } catch (Loading e) {
            return ("...");
        }
    }

    public void set(List<IMeter.Meter> meters) {
        this.meters = meters;
    }

    public void set(double a, Color c) {
        set(Collections.singletonList(new IMeter.Meter(a, c)));
    }

    private static double av(Object arg) {
        if (arg instanceof Integer)
            return (((Integer) arg).doubleValue() * 0.01);
        else
            return (((Number) arg).doubleValue());
    }

    public static List<IMeter.Meter> decmeters(Object[] args, int s) {
        if (args.length == s)
            return (Collections.emptyList());
        ArrayList<IMeter.Meter> buf = new ArrayList<>();
        if (args[s] instanceof Number) {
            for (int a = s; a < args.length; a += 2)
                buf.add(new IMeter.Meter(av(args[a]), (Color) args[a + 1]));
        } else {
            /* XXX: To be considered deprecated, but is was the
             * traditional argument layout of IMeter, so let clients
             * with the newer convention spread before converting the
             * server. */
            for (int a = s; a < args.length; a += 2)
                buf.add(new IMeter.Meter(av(args[a + 1]), (Color) args[a]));
        }
        buf.trimToSize();
        return (buf);
    }

    public void uimsg(String msg, Object... args) {
        if (msg == "set") {
            if (args.length == 1) {
                set(av(args[0]), meters.isEmpty() ? Color.WHITE : meters.get(0).c);
            } else {
                set(decmeters(args, 0));
//                for (int i = 0; i < args.length; i += 2)
//                    meters.add(new Meter((Color) args[i], (Integer) args[i + 1]));
//                this.meters = meters;
            }
//            amount = (Integer) args[0];
//            if (args.length > 1)
//                cl = (Color) args[1];
//        } else if (msg == "col") {
//            cl = (Color) args[0];
        } else if (msg == "tip") {
            if (args[0] instanceof Object[]) {
                rawinfo = new ItemInfo.Raw((Object[]) args[0]);
                info = null;
                shorttip = longtip = null;
            } else {
                super.uimsg(msg, args);
            }
        } else {
            super.uimsg(msg, args);
        }
    }
}
