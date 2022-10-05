package haven.res.lib.layspr;

import haven.Indir;
import haven.Message;
import haven.Resource;
import haven.Utils;
import java.util.List;

public class BaseLayered extends Layered implements haven.res.ui.tt.defn.DynName {
    public final String name;
    public final String oname;

    public BaseLayered(Owner owner, List<Indir<Resource>> lay) {
        super(owner, lay);
        Resource nres = Utils.el(lay).get();
        Resource.Tooltip tt = nres.layer(Resource.tooltip);
        if (tt == null)
            throw (new RuntimeException("Item resource " + nres + " is missing default tooltip"));
        name = tt.t;
        oname = tt.origt;
    }

    public BaseLayered(Owner owner, Resource res, Message sdt) {
        this(owner, decode(owner.context(Resource.Resolver.class), sdt));
    }

    public String name() {
        return (name);
    }

    public String oname() {
        return (oname);
    }
}
