package haven;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

public class WidgetList<T extends Widget> extends ListWidget<T> {
    public static final IBox BOX = new IBox("gfx/hud/box", "tl", "tr", "bl", "br", "extvl", "extvr", "extht", "exthb");
    public static final Coord BTLOFF = BOX.btloff();
    protected final List<T> list = new LinkedList<>();
    public final Scrollbar sb;
    protected final Coord itemsz, widesz;
    protected final int h;
    private Color bgcolor = new Color(0, 0, 0, 96);
    protected T over;
    public boolean canselect = true;

    public WidgetList(Coord itemsz, int h) {
        super(itemsz, itemsz.y);
        this.itemsz = itemsz;
        this.h = h;
        sz = BOX.bisz().add(itemsz.x, h * itemsz.y);
        sb = add(new Scrollbar(sz.y, 0, 20), sz.x, 0);
        this.widesz = itemsz.add(sb.sz.x, 0);
        pack();
    }

    public T additem(T item) {
        list.add(item);
        add(item, itempos(listitems() - 1));
        sb.resize(contentsz().y);
//        sb.max = Math.max(0, contentsz().y - sz.y);
        return item;
    }

    public boolean removeitem(T item, boolean destroy) {
        boolean removed = list.remove(item);
        if (removed) {
            if (destroy) {
                ui.destroy(item);
            } else {
                item.unlink();
            }
            sb.resize(contentsz().y);
//            sb.max = Math.max(0, contentsz().y - sz.y);
        }
        return removed;
    }

    public void clear(boolean destroy) {
        for (T item : list) {
            if (destroy) {
                ui.destroy(item);
            } else {
                item.unlink();
            }
        }
        sb.resize(contentsz().y);
//        sb.max = Math.max(0, contentsz().y - sz.y);
        list.clear();
    }

    public Coord itempos(int idx) {
        return BTLOFF.add(0, idx * itemsz.y);
    }

    protected void drawbg(GOut g) {
        if (bgcolor != null) {
            g.chcolor(bgcolor);
            g.frect(Coord.z, sz);
            g.chcolor();
        }
        BOX.draw(g, Coord.z, sz);
    }

    protected void drawsel(GOut g, Color color) {
        g.chcolor(color);
        g.frect(Coord.z, g.sz);
        g.chcolor();
    }

    @Override
    public void draw(GOut g) {
        drawbg(g);

        int n = listitems();
        sb.max = n - h;
        Coord isz = sb.vis() ? itemsz : widesz;
        for (int i = 0; i < h; i++) {
            int idx = i + sb.val;
            if (idx >= n)
                break;
            T item = listitem(idx);
            GOut ig = g.reclip(itempos(i), isz);
            if (item == sel) {
                drawsel(ig, Listbox.selc);
            } else if (item == over) {
                drawsel(ig, Listbox.overc);
            }
            drawitem(ig, item, idx);
        }

        sb.draw(g.reclip(xlate(sb.c, true), sb.sz));
    }

    @Override
    protected T listitem(int idx) {
        return list.get(idx);
    }

    @Override
    protected int listitems() {
        return list.size();
    }

    @Override
    protected void drawitem(GOut g, T item, int i) {
        item.draw(g);
    }

    public T itemat(Coord c) {
        int idx = c.x > itemsz.x ? -1 : (c.y / itemsz.y) + sb.val;
        if (idx >= listitems() || idx < 0)
            return (null);
        return (listitem(idx));
    }

    protected void itemclick(T item, int button) {
        if (button == 1) {
            change(item);
            item.mousedown(xlate(item.c, true), button);
        }
    }

    @Override
    public void change(T item) {
        if (canselect) {
            super.change(item);
            if (item != null) {
                selected(item);
            }
        }
    }

    public void selected(T item) {
    }

    @Override
    public Coord xlate(Coord c, boolean in) {
        if (in)
            return (c.add(0, -sb.val * itemh));
        else
            return (c.add(0, sb.val * itemh));
    }

    @Override
    public boolean mousedown(Coord c, int button) {
//        Coord c = (c0.x < sb.c.x) ? c0.add(0, sb.val * itemh) : c0;
        if (super.mousedown(c, button))
            return (true);
        T item = itemat(c);
        if ((item == null) && (button == 1)) {
            change(null);
        } else if (item != null) {
            itemclick(item, button);
        }
        return (true);
    }

    @Override
    public void mousemove(Coord c) {
        super.mousemove(c);
        if (c.isect(Coord.z, sz)) {
            over = itemat(c);
        } else {
            over = null;
        }
    }

    @Override
    public boolean mousewheel(Coord c, int amount) {
        sb.ch(amount);
        return (true);
    }

    protected Object itemtooltip(Coord c, T item) {
        return (item.tooltip(xlate(item.c, true), null));
    }

    @Override
    public Object tooltip(Coord c, Widget prev) {
        final T item = itemat(c);
        if (item != null) {
            return (itemtooltip(c, item));
        } else {
            return (super.tooltip(c, prev));
        }
    }
}
