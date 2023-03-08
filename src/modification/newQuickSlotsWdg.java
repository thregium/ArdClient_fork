package modification;

import haven.Config;
import haven.Coord;
import haven.DTarget;
import haven.Equipory;
import haven.GItem;
import haven.GOut;
import haven.GSprite;
import haven.Resource;
import haven.WItem;
import haven.Widget;
import haven.sloth.gui.MovableWidget;

import static haven.Inventory.invsq;

public class newQuickSlotsWdg extends MovableWidget implements DTarget {
    public static final int slots = 4;
    private static final Coord ssz = new Coord(33, 33);
    private static final Coord spz = new Coord(4, 33);

    public static class Item {
        public Coord coord;
        int slot;
        int eqslot;

        public Item(int slot, int eqslot) {
            this.slot = slot;
            this.eqslot = eqslot;
            this.coord = new Coord((ssz.x + spz.x) * (slot - 1) + 1, 1);
        }
    }

    public static final Item[] items = new Item[]{new Item(1, 6), new Item(2, 7), new Item(3, 5), new Item(4, 14)};

    public static final Coord leftCoord = new Coord((ssz.x + spz.x) * 0, 1);
    public static final Coord rightCoord = new Coord((ssz.x + spz.x) * 1, 1);
    public static final Coord beltCoord = new Coord((ssz.x + spz.x) * 2, 1);
    public static final Coord cloakCoord = new Coord((ssz.x + spz.x) * 3, 1);

    public newQuickSlotsWdg() {
        super(new Coord(ssz.x * 4 + spz.x * 3, ssz.y), "NewQuickSlotsWdg");
    }

    public void draw(GOut g) {
        Equipory e = null;
        if (ui.gui != null) e = ui.gui.getequipory();
        if (e != null) {

            for (int i = 0; i < items.length; ++i) {
                Item item = items[i];
                GOut gi = g.reclip(item.coord.add(1, 1), ssz.sub(1, 1));

                gi.chcolor(255, 255, 0, 64);
                gi.frect(Coord.z, ssz);
                gi.chcolor();

                gi.aimage(invsq, ssz.div(2), 0.5, 0.5);
                if (Equipory.ebgs[item.eqslot] != null)
                    gi.aimage(Equipory.ebgs[item.eqslot], ssz.div(2), 0.5, 0.5);
                if (e.quickslots[item.eqslot] != null) {
                    e.quickslots[item.eqslot].draw(gi);
                }
            }

        }
        super.draw(g);
    }

    public boolean drop(Coord cc, Coord ul) {
        Equipory e = ui.gui.getequipory();
        if (e != null) {
            int sl = -1;

            for (int i = 0; i < 4; ++i) {
                if (cc.x <= (ssz.x + spz.x / 2) * items[i].slot) {
                    sl = i;
                    break;
                }
            }

            if (sl >= 0) {
                e.wdgmsg("drop", items[sl].eqslot);
                return true;
            }
        }

        return false;
    }

    public boolean iteminteract(Coord cc, Coord ul) {
        Equipory e = ui.gui.getequipory();
        if (e != null) {
            WItem w = null;
            for (int i = 0; i < 4; ++i) {
                if (cc.x <= (ssz.x + spz.x / 2) * items[i].slot) {
                    w = e.quickslots[items[i].eqslot];
                    break;
                }
            }
            if (w != null) {
                return w.iteminteract(cc, ul);
            }
        }

        return false;
    }

    public boolean mousedown(Coord c, int button) {
        if (super.mousedown(c, button))
            return true;
        if (ui.modmeta) {
            return true;
        } else if (ui.modctrl && button == 1 && Config.disablequickslotdrop) {
            return true;
        } else {
            Equipory e = ui.gui.getequipory();
            if (e != null) {
                WItem w = null;
                for (int i = 0; i < 4; ++i) {
                    if (c.x <= (ssz.x + spz.x / 2) * items[i].slot) {
                        w = e.quickslots[items[i].eqslot];
                        break;
                    }
                }
                if (w != null) {
                    w.mousedown(new Coord(w.sz.x / 2, w.sz.y / 2), button);
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public boolean mousehover(Coord c) {
        Equipory e = ui.gui.getequipory();
        if (e != null) {
            WItem w = null;
            for (int i = 0; i < 4; ++i) {
                if (c.x <= (ssz.x + spz.x / 2) * items[i].slot) {
                    w = e.quickslots[items[i].eqslot];
                    break;
                }
            }
            if (w != null) {
                w.mousehover(new Coord(w.sz.x / 2, w.sz.y / 2));
                return (true);
            }
        }
        return (super.mousehover(c));
    }

    public void simulateclick(Coord c) {
        Equipory e = ui.gui.getequipory();
        if (e != null) {
            WItem w = null;
            for (int i = 0; i < 4; ++i) {
                if (c.x <= (ssz.x + spz.x / 2) * items[i].slot) {
                    w = e.quickslots[items[i].eqslot];
                    break;
                }
            }
            if (w != null) {
                w.item.wdgmsg("take", new Coord(w.sz.x / 2, w.sz.y / 2));
            }
        }
    }

    public Object tooltip(Coord c, Widget prev) {
        Object tt = super.tooltip(c, prev);
        if (tt != null) {
            return tt;
        } else {
            int sl = -1;

            for (int i = 0; i < 4; ++i) {
                if (c.x <= (ssz.x + spz.x / 2) * items[i].slot) {
                    sl = i;
                    break;
                }
            }

            Equipory e = ui.gui.getequipory();
            if (e != null && sl >= 0) {
                WItem it = e.quickslots[items[sl].eqslot];
                if (it != null) {
                    return (it.tooltip(c, prev));
                } else {
                    return ((Equipory.etts[items[sl].eqslot]));
                }
            } else {
                return (null);
            }
        }
    }
}
