import haven.Indir;
import haven.ItemInfo.Layout.ID;
import haven.ItemInfo.Tip;
import haven.Message;
import haven.MessageBuf;
import haven.ResData;
import haven.Resource;
import haven.RichText;
import haven.UI;
import haven.res.lib.tspec.Spec;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class Ingredient extends Tip {
    public final String name;
    public final String oname;
    public final Double val;

    public Ingredient(Owner owner, String name, Double val) {
        super(owner);
        this.oname = name;
        this.name = name;
        this.val = val;
    }

    public Ingredient(Owner owner, String oname, String name, Double val) {
        super(owner);
        this.oname = oname;
        this.name = name;
        this.val = val;
    }

    public Ingredient(Owner owner, String name) {
        this(owner, name, name, null);
    }

    public Ingredient(Owner owner, String oname, String name) {
        this(owner, oname, name, null);
    }

    public static Ingredient mkinfo(Owner owner, Object... args) {
        int a = 1;
        String name;
        String oname;
        if (args[a] instanceof String) {
            name = (String) args[a++];
            oname = name;
        } else if (args[1] instanceof Integer) {
            Indir<Resource> res = owner.context(Resource.Resolver.class).getres((Integer) args[a++]);
            Message sdt = Message.nil;
            if ((args.length > a) && (args[a] instanceof byte[]))
                sdt = new MessageBuf((byte[]) args[a++]);
            Spec spec = new Spec(new ResData(res, sdt), owner, null);
            oname = res.get().layer(Resource.tooltip).origt;
            name = spec.name();
        } else {
            throw (new IllegalArgumentException());
        }
        Double val = null;
        if (args.length > a)
            val = (args[a] == null) ? null : ((Number) args[a]).doubleValue();
        return (new Ingredient(owner, oname, name, val));
    }

    public static class Line extends Tip {
        final List<Ingredient> all = new ArrayList<>();

        Line() {
            super(null);
        }

        public BufferedImage tipimg() {
            StringBuilder buf = new StringBuilder();
            all.sort(Comparator.comparing(a -> a.name));
            buf.append(Resource.getLocString(Resource.BUNDLE_LABEL, "Made from")).append(" ");
            buf.append(all.get(0).descr());
            if (all.size() > 2) {
                for (int i = 1; i < all.size() - 1; i++) {
                    buf.append(", ");
                    buf.append(all.get(i).descr());
                }
            }

            if (all.size() > 1) {
                buf.append(" ").append(Resource.getLocString(Resource.BUNDLE_LABEL, "and")).append(" ");
                buf.append(all.get(all.size() - 1).descr());
            }

            return (RichText.render(buf.toString(), UI.scale(250)).img);
        }
    }

    public static final ID<Line> id = Line::new;

    public void prepare(Layout l) {
        l.intern(id).all.add(this);
    }

    public String descr() {
        if (val == null)
            return (name);
        return (String.format("%s (%d%%)", name, (int) Math.floor(val * 100.0)));
    }
}
