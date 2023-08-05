package haven.res.ui.tt.slot;

import haven.CharWnd;
import haven.Coord;
import haven.ItemInfo;
import haven.ItemInfo.Tip;
import haven.Resource;
import haven.RichText;
import haven.Text;
import haven.Text.Line;

import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;

import static haven.PUtils.convolvedown;

public class Slotted extends Tip {
    public static final Line ch = Text.render(Resource.getLocString(Resource.BUNDLE_LABEL, "As gilding:"));
    public final double pmin;
    public final double pmax;
    public final Resource[] attrs;
    public final List<ItemInfo> sub;

    public Slotted(Owner owner, double pmin, double pmax, Resource[] attrs, List<ItemInfo> sub) {
        super(owner);
        this.pmin = pmin;
        this.pmax = pmax;
        this.attrs = attrs;
        this.sub = sub;
    }

    public static ItemInfo mkinfo(Owner owner, Object... args) {
        Resource.Resolver rr = owner.context(Resource.Resolver.class);
        int a = 1;
        double pmin = ((Number) args[a++]).doubleValue();
        double pmax = ((Number) args[a++]).doubleValue();
        List<Resource> attrs = new LinkedList<>();
        while (args[a] instanceof Integer)
            attrs.add(rr.getres((Integer) args[a++]).get());
        Object[] raw = (Object[]) args[a++];
        return (new Slotted(owner, pmin, pmax, attrs.toArray(new Resource[0]), buildinfo(owner, raw)));
    }

    public static final String chc = "192,192,255";

    public void layout(Layout l) {
        l.cmp.add(ch.img, new Coord(0, l.cmp.sz.y));
        BufferedImage head;
        if (this.attrs.length > 0) {
            String chanceStr = Resource.getLocString(Resource.BUNDLE_LABEL, "Chance: $col[%s]{%d%%} to $col[%s]{%d%%}");
            head = RichText.render(String.format(chanceStr, chc, Math.round(100 * pmin), chc, Math.round(100 * pmax)), 0).img;
            int h = head.getHeight();
            int x = 10, y = l.cmp.sz.y;
            l.cmp.add(head, new Coord(x, y));
            x += head.getWidth() + 10;

            for (int i = 0; i < attrs.length; i++) {
                BufferedImage icon = convolvedown(attrs[i].layer(Resource.imgc).img, new Coord(h, h), CharWnd.iconfilter);
                l.cmp.add(icon, new Coord(x, y));
                x += icon.getWidth() + 2;
            }
        } else {
            String chanceStr = Resource.getLocString(Resource.BUNDLE_LABEL, "Chance: $col[%s]{%d%%}");
            head = RichText.render(String.format(chanceStr, chc, (int) Math.round(100 * pmin)), 0).img;
            l.cmp.add(head, new Coord(10, l.cmp.sz.y));
        }

        head = longtip(sub);
        if (head != null) {
            l.cmp.add(head, new Coord(10, l.cmp.sz.y));
        }
    }

    public int order() {
        return 200;
    }
}