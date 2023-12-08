package haven;

import haven.sloth.gui.MovableWidget;

import static haven.Inventory.invsq;

public class CraftHistoryBelt extends MovableWidget {
    private static final int SIZE = 8;
    private MenuGrid.Pagina[] belt = new MenuGrid.Pagina[SIZE];
    private static final Coord vsz = new Coord(invsq.sz().x, invsq.sz().x * SIZE + (2 * (SIZE - 1)));
    private static final Coord hsz = new Coord(invsq.sz().y * SIZE + (2 * (SIZE - 1)), invsq.sz().y);
    private boolean vertical;

    public CraftHistoryBelt(boolean vertical) {
        super(vertical ? vsz : hsz, "CraftHistoryBelt");
        this.vertical = vertical;
    }

    private Coord beltc(int i) {
        if (vertical)
            return new Coord(0, (invsq.sz().x + 2) * i);
        return new Coord((invsq.sz().x + 2) * i, 0);
    }

    private int beltslot(Coord c) {
        for (int i = 0; i < SIZE; i++) {
            if (c.isect(beltc(i), invsq.sz()))
                return i;
        }
        return -1;
    }

    @Override
    public void draw(GOut g) {
        for (int i = 0; i < SIZE; i++) {
            int slot = i;
            Coord c = beltc(i);
            g.image(invsq, c);
            if (belt[slot] != null)
                g.image(belt[slot].img, c.add(1, 1));
        }
        super.draw(g);
    }

    @Override
    public boolean mousedown(Coord c, int button) {
        if (super.mousedown(c, button)) {
            return true;
        }
        int slot = beltslot(c);
        if (slot != -1) {
            if (button == 1) {
                if (ui.modshift) {
                    if (vertical) {
                        sz = hsz;
                        vertical = false;
                    } else {
                        sz = vsz;
                        vertical = true;
                    }
                    Utils.setprefb("histbelt_vertical", vertical);
                    return (true);
                } else if (belt[slot] != null) {
                    String[] ad = belt[slot].act().ad;
                    if (ad.length > 0 && (ad[0].equals("craft") || ad[0].equals("bp"))) {
                        MenuGrid g = ui.gui.menu;
                        g.lastCraft = g.getPagina(ad[1]);
                    }
//                ui.gui.act(ad);
                    act(belt[slot]);
                    return (true);
                }
            }
        }
        if (altMoveHit(c, button)) {
            if (!isLock()) {
                movableBg = true;
                dm = ui.grabmouse(this);
                doff = c;
                parent.setfocus(this);
                raise();
            }
            return (true);
        } else {
            return (false);
        }
    }

    public void act(MenuGrid.Pagina act) {
        if (ui.gui != null) {
            ui.gui.menu.use(act.button(), false);
        }
    }

//    @Override
//    public boolean mouseup(Coord c, int button) {
//        if (dragging != null) {
//            dragging.remove();
//            dragging = null;
//            Utils.setprefc("histbelt_c", this.c);
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

    public void push(MenuGrid.Pagina pagina) {
        int idx = SIZE - 1;
        for (int i = 0; i < belt.length; i++) {
            if (belt[i] == pagina) {
                idx = i;
                break;
            }
        }
        if (idx != 0) {
            for (int i = idx - 1; i >= 0; i--)
                belt[i + 1] = belt[i];
            belt[0] = pagina;
        }
    }

    @Override
    public Object tooltip(Coord c, Widget prev) {
        Object tt = super.tooltip(c, prev);
        if (tt != null) {
            return tt;
        } else {
            int sl = -1;
            if (vertical)
                sl = c.y / vsz.x;
            else
                sl = c.x / hsz.y;

            if (sl < SIZE && sl >= 0)
                if (belt[sl] != null) {
                    return belt[sl].button().rendertt(true);
                } else {
                    return super.tooltip(c, prev);
                }
            else return null;
        }
    }

}