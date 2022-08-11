package haven;

import java.awt.Color;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public abstract class SListBox<I, W extends Widget> extends SListWidget<I, W> implements Scrollable {
    public static final Color every = new Color(255, 255, 255, 16);
    public static final Color other = new Color(255, 255, 255, 32);
    public final int itemh;
    public final int marg;
    public final Scrollbar sb;
    private Map<I, W> curw = new IdentityHashMap<>();
    private I[] curi;
    private int n = -1;
    private int h;
    private int curo = 0;
    private int itemw = 0;
    private int maxy = 0, cury = 0;

    public SListBox(Coord sz, int itemh, int marg) {
        super(sz);
        this.itemh = itemh;
        this.marg = marg;
        if (autoscroll())
            this.sb = add(new Scrollbar(0, this));
        else
            this.sb = null;
        resize(sz);
    }

    public SListBox(Coord sz, int itemh) {this(sz, itemh, 0);}

    protected boolean autoscroll() {return (true);}

    public int scrollmin() {return (0);}

    public int scrollmax() {return (maxy);}

    public int scrollval() {return (cury);}

    public void scrollval(int val) {cury = val;}

    @Override
    @SuppressWarnings("unchecked")
    public void tick(double dt) {
        boolean reset = false;
        List<? extends I> items = items();
        if (items.size() != n) {
            n = items.size();
            int th = (n == 0) ? 0 : (itemh + ((n - 1) * (itemh + marg)));
            maxy = th - sz.y;
            cury = Math.min(cury, Math.max(maxy, 0));
        }
        int itemw = sz.x - (((sb != null) && sb.vis()) ? sb.sz.x : 0);
        if (itemw != this.itemw) {
            reset = true;
            this.itemw = itemw;
        }
        int sy = cury, off = sy / (itemh + marg);
        if (reset) {
            for (W cw : curw.values())
                cw.destroy();
            curi = null;
            curw.clear();
        }
        boolean update = false;
        if ((curi == null) || (curi.length != h) || (curo != off))
            update = true;
        if (!update) {
            for (int i = 0; i < h; i++) {
                I item = (i + curo < items.size()) ? items.get(i + curo) : null;
                if ((curi[i] != item)) {
                    update = true;
                    break;
                }
            }
        }
        if (update) {
            I[] newi = (I[]) new Object[h];
            Map<I, W> neww = new IdentityHashMap<>();
            Coord itemsz = Coord.of(itemw, itemh);
            for (int i = 0; i < h; i++) {
                int np = i + off;
                newi[i] = (np < items.size()) ? items.get(np) : null;
                if (newi[i] != null) {
                    W pw = curw.remove(newi[i]);
                    if (pw == null)
                        neww.put(newi[i], add(makeitem(newi[i], np, itemsz)));
                    else
                        neww.put(newi[i], pw);
                }
            }
            for (W pw : curw.values())
                pw.destroy();
            curi = newi;
            curw = neww;
            curo = off;
        }
        boolean updpos = update;
        if (!updpos) {
            for (int i = 0; i < curi.length; i++) {
                if (curi[i] != null) {
                    W w = curw.get(curi[i]);
                    if (w.c.y != ((i * (itemh + marg)) - sy)) {
                        updpos = true;
                        break;
                    }
                }
            }
        }
        if (updpos) {
            for (int i = 0; i < curi.length; i++) {
                if (curi[i] != null)
                    curw.get(curi[i]).move(Coord.of(0, ((i + curo) * (itemh + marg)) - sy));
            }
        }
        super.tick(dt);
    }

    protected void drawbg(GOut g) {
    }

    protected void drawbg(GOut g, I item, int idx, Area area) {
        g.chcolor(((idx % 2) == 0) ? every : other);
        g.frect2(area.ul, area.br);
        g.chcolor();
    }

    protected void drawsel(GOut g, I item, int idx, Area area) {
        g.chcolor(255, 255, 0, 128);
        g.frect2(area.ul, area.br);
        g.chcolor();
    }

    protected void drawslot(GOut g, I item, int idx, Area area) {
        drawbg(g, item, idx, area);
        if ((sel != null) && (sel == item))
            drawsel(g, item, idx, area);
    }

    @Override
    public void draw(GOut g) {
        drawbg(g);
        if (curi != null) {
            List<? extends I> items = items();
            int sy = cury;
            for (int i = 0; (i < curi.length) && (i + curo < items.size()); i++) {
                drawslot(g, curi[i], i + curo, Area.sized(Coord.of(0, ((i + curo) * (itemh + marg)) - sy), Coord.of(itemw, itemh)));
            }
        }
        super.draw(g);
    }

    @Override
    public boolean mousewheel(Coord c, int amount) {
        if (super.mousewheel(c, amount))
            return (true);
        int step = sz.y / 8;
        if (maxy > 0)
            step = Math.min(step, maxy / 8);
        step = Math.max(step, itemh);
        cury = Math.max(Math.min(cury + (step * amount), maxy), 0);
        return (true);
    }

    protected boolean unselect(int button) {
        if (button == 1)
            change(null);
        return (true);
    }

    @Override
    public boolean mousedown(Coord c, int button) {
        if (super.mousedown(c, button))
            return (true);
        return (unselect(button));
    }

    @Override
    public void resize(Coord sz) {
        super.resize(sz);
        if (sb != null) {
            sb.resize(sz.y);
            sb.c = Coord.of(sz.x - sb.sz.x, 0);
        }
        h = Math.max(((sz.y + itemh + marg - 2) / (itemh + marg)), 0) + 1;
    }

    public void display(int idx) {
        int y = idx * (itemh + marg);
        if (y < cury)
            cury = y;
        else if (y + itemh >= cury + sz.y)
            cury = Math.max((y + itemh) - sz.y, 0);
    }

    public void display(I item) {
        int p = items().indexOf(item);
        if (p >= 0)
            display(p);
    }

    public void display() {
        display(sel);
    }
}
