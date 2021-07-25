package haven;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HSliderListbox extends Listbox<HSliderNamed> {
    public final List<HSliderNamed> items = new ArrayList<>();

    public HSliderListbox(int w, int h) {
        super(w, h, 18);
    }

    public HSliderListbox(int w, int h, int itemh) {
        super(w, h, itemh);
    }

    public void addItem(HSliderNamed item) {
        items.add(item);
        items.sort(Comparator.comparing(o -> o.name));
    }

    public void addItems(List<HSliderNamed> list) {
        items.addAll(list);
        items.sort(Comparator.comparing(o -> o.name));
    }

    public void clear() {
        new ArrayList<>(items).forEach(i -> {
            items.remove(i);
            i.reqdestroy();
        });
    }

    private HSliderNamed holded = null;

    @Override
    public boolean mousedown(Coord c, int button) {
        Coord cc = xlate(sb.c, true);
        if (c.isect(cc, sb.sz)) {
            if (sb.mousedown(c.add(cc.inv()), button)) {
                return (true);
            }
        }
        int idx = idxat(c);
        HSliderNamed item = (idx >= listitems() || idx < 0) ? null : listitem(idx);
        if ((item == null) && (button == 1))
            change(null);
        else if (item != null) {
            holded = item;
            return (item.mousedown(c.sub(idxc(idx)), button));
        }
        return (super.mousedown(c, button));
    }


    @Override
    public void mousemove(Coord c) {
        int idx = idxat(c);
        HSliderNamed item = (idx >= listitems() || idx < 0) ? null : listitem(idx);
        if (item == null)
            change(null);
        else if (item != null && item.equals(holded))
            item.mousemove(c.sub(idxc(idx)));
        super.mousemove(c);
    }

    @Override
    public boolean mouseup(Coord c, int button) {
        int idx = idxat(c);
        HSliderNamed item = (idx >= listitems() || idx < 0) ? null : listitem(idx);
        if ((item == null) && (button == 1))
            change(null);
        else if (item != null && item.equals(holded)) {
            holded = null;
            return (item.mouseup(c.sub(idxc(idx)), button));
        }
        return (super.mouseup(c, button));
    }

    protected HSliderNamed listitem(int idx) {
        return (items.get(idx));
    }

    protected int listitems() {
        return items.size();
    }

    @Override
    protected void drawitem(GOut g, HSliderNamed itm, int i) {
        GOut ig = g.reclip(Coord.z.add(0, itemh / 2).sub(0, itm.sz.y / 2), itm.sz);
        itm.draw(ig);
    }

    public void drawbg(GOut g) {
        g.chcolor(0, 0, 0, 128);
        g.frect(Coord.z, sz);
        g.chcolor();
    }

    @Override
    public void tick(double dt) {
        super.tick(dt);
        items.forEach(h -> {
            if (h.slider.ui == null)
                h.slider.attach(ui);
        });
    }

    protected Object itemtooltip(Coord c, HSliderNamed itm) {
        return (itm.tooltip(c, null));
    }
}