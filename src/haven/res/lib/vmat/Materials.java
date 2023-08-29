package haven.res.lib.vmat;

import haven.GLState;
import haven.Gob;
import haven.Indir;
import haven.IntMap;
import haven.Material;
import haven.Message;
import haven.Pair;
import haven.Resource;
import modification.dev;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

public class Materials extends Mapping {
    public static final Map<Integer, Material> empty = Collections.emptyMap();
    public final Map<Integer, Material> mats;

    public static Map<Integer, Material> decode(Resource.Resolver rr, Message sdt) {
        Map<Integer, Material> ret = new IntMap<>();
        int idx = 0;
        while (!sdt.eom()) {
            Indir<Resource> mres = rr.getres(sdt.uint16());
            int mid = sdt.int8();
            Material.Res mat;
            if (mid >= 0)
                mat = mres.get().layer(Material.Res.class, mid);
            else
                mat = mres.get().layer(Material.Res.class);
            ret.put(idx++, mat.get());
        }
        return (ret);
    }

    public static Material stdmerge(Material orig, Material var) {
        haven.resutil.OverTex otex = Arrays.stream(orig.states).filter(haven.resutil.OverTex.class::isInstance).map(haven.resutil.OverTex.class::cast).findFirst().orElse(null);
        if (otex == null)
            return (var);
        return (new Material(var, otex));
    }

    public Material mergemat(Material orig, int mid) {
        if (!mats.containsKey(mid))
            return (orig);
        Material var = mats.get(mid);
        return (stdmerge(orig, var));
    }

    public Materials(Gob gob, Map<Integer, Material> mats) {
        super(gob);
        this.mats = mats;
    }

    public Materials(Gob gob, Message dat) {
        super(gob);
        this.mats = decode(gob.context(Resource.Resolver.class), dat);
    }

    public static void parse(Gob gob, Message dat) {
        gob.setattr(new Materials(gob, decode(gob.context(Resource.Resolver.class), dat)));
    }
}

