import haven.GSprite;
import haven.Indir;
import haven.Message;
import haven.Resource;
import haven.res.lib.layspr.Layered;
import java.util.List;

public class Meat extends Layered implements haven.res.ui.tt.defn.DynName {
    public final String name;
    public final String oname;

    private static String ncomb(String a, String b) {
        int p = a.indexOf('%');
        if (p < 0) {
            if (b.indexOf('%') >= 0)
                return (ncomb(b, a));
            return (a);
        }
        return (a.substring(0, p) + b + a.substring(p + 1));
    }

    private Meat(GSprite.Owner owner, List<Indir<Resource>> lay) {
        super(owner, lay);
        String cn = null;
        String ocn = null;
        for (Indir<Resource> res : lay) {
            Resource.Tooltip tt = res.get().layer(Resource.tooltip);
            if (tt == null)
                continue;
            if (ocn == null) {
                cn = tt.t;
                ocn = tt.origt;
            } else {
                cn = ncomb(cn, tt.t);
                ocn = ncomb(ocn, tt.origt);
            }
        }
        if (ocn != null) {
            int p1 = cn.indexOf('%');
            if (p1 >= 0) {
                cn = cn.substring(0, p1).trim() + " " + cn.substring(p1 + 1).trim();
            }
            int p2 = ocn.indexOf('%');
            if (p2 >= 0) {
                ocn = ocn.substring(0, p2).trim() + " " + ocn.substring(p2 + 1).trim();
            }
            name = cn;
            oname = ocn;
        } else {
            name = "Meat";
            oname = name;
        }
    }

    public Meat(GSprite.Owner owner, Resource res, Message sdt) {
        this(owner, decode(owner.context(Resource.Resolver.class), sdt));
    }

    public String name() {
        return (name);
    }

    public String oname() {
        return (oname);
    }
}
