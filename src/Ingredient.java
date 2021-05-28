import haven.Indir;
import haven.ItemInfo;
import haven.ItemInfo.Layout.ID;
import haven.ItemInfo.Tip;
import haven.Message;
import haven.MessageBuf;
import haven.ResData;
import haven.Resource;
import haven.RichText;
import haven.res.lib.tspec.Spec;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Ingredient extends Tip {
    public final String name;
    public final String oname;
    public final Double val;
    public static final ID<Line> id = Line::new;

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

    public void prepare(Layout var1) {
        var1.intern(id).all.add(this);
    }

    public String descr() {
        return this.val == null ?
                Resource.getLocString(Resource.BUNDLE_LABEL, name) :
                String.format("%s (%d%%)", Resource.getLocString(Resource.BUNDLE_INGREDIENT, name), Integer.valueOf((int) Math.floor(this.val.doubleValue() * 100.0D)));
    }

    public static class Fac implements InfoFactory {
        public ItemInfo build(Owner owner, Object... args) {
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
    }

    public static class Line extends Tip {
        final List<Ingredient> all = new ArrayList<>();

        Line() {
            super(null);
        }

        public BufferedImage tipimg() {
            StringBuilder ings = new StringBuilder();
            Collections.sort(this.all, (a, b) -> a.name.compareTo(b.name));

            switch (all.size()) {
                case 1:
                    ings.append(String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "Made from %s"), all.get(0).descr()));
                    break;
                case 2:
                    ings.append(String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "Made from %s and %s"), all.get(0).descr(), all.get(1).descr()));
                    break;
                case 3:
                    ings.append(String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "Made from %s, %s and %s"), all.get(0).descr(), all.get(1).descr(), all.get(2).descr()));
                    break;
                case 4:
                    ings.append(String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "Made from %s, %s, %s and %s"), all.get(0).descr(), all.get(1).descr(), all.get(2).descr(), all.get(3).descr()));
                    break;
            }

            if (ings.length() == 0) {
                ings.append(Resource.getLocString(Resource.BUNDLE_LABEL, "Made from "));
                ings.append(this.all.get(0).descr());
                if (this.all.size() > 2) {
                    for (int i = 1; i < this.all.size() - 1; ++i) {
                        ings.append(", ");
                        ings.append(this.all.get(i).descr());
                    }
                }

                if (this.all.size() > 1) {
                    ings.append(" and ");
                    ings.append(this.all.get(this.all.size() - 1).descr());
                }
            }

            return RichText.render(ings.toString(), 250, new Object[0]).img;
        }
    }
}
