package haven.res.lib.vmat;

import haven.FastMesh;
import haven.Gob;
import haven.Material;
import haven.Rendered;
import haven.Resource;

import java.util.Collection;
import java.util.LinkedList;

public abstract class Mapping extends Gob.ResAttr {
    public Mapping() {
//        super(null);
    }

    public abstract Material mergemat(Material orig, int mid);

    public Rendered[] apply(Resource res) {
        Collection<Rendered> rl = new LinkedList<>();
        for (FastMesh.MeshRes mr : res.layers(FastMesh.MeshRes.class)) {
            String sid = mr.rdat.get("vm");
            int mid = (sid == null) ? -1 : Integer.parseInt(sid);
            if (mid >= 0) {
                rl.add(mergemat(mr.mat.get(), mid).apply(mr.m));
            } else if (mr.mat != null) {
                rl.add(mr.mat.get().apply(mr.m));
            }
        }
        return (rl.toArray(new Rendered[0]));
    }

    public static final Mapping empty = new Mapping() {
        public Material mergemat(Material orig, int mid) {
            return (orig);
        }
    };
}
