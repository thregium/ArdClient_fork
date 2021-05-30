package modification;

import haven.FastMesh;
import haven.Material;
import haven.Message;
import haven.Rendered;
import haven.Resource;
import haven.Sprite;
import haven.TexGL;
import haven.TexR;
import haven.res.lib.vmat.VarSprite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class Corostone implements Sprite.Factory {
    public Sprite create(Sprite.Owner owner, Resource res, Message sdt) {
        Resource resource = res;
        VarSprite ret = new VarSprite(owner, res, Message.nil) {
            private long rid, nrid;
            private Collection<Rendered> banners = Collections.emptyList();

            public Collection<Rendered> iparts(int mask) {
                Collection<Rendered> rl = super.iparts(mask);
                if (nrid != rid) {
                    if (nrid == 0) {
                        banners = Collections.emptyList();
                    } else {
                        ArrayList<Rendered> banners = new ArrayList<>();
                        Material base = resource.layer(Material.Res.class, 16).get();
                        TexGL tex = resource.pool.dynres(nrid).get().layer(TexR.class).tex();
                        Material sym = new Material(base, tex.draw(), tex.clip());
                        for (FastMesh.MeshRes mr : resource.layers(FastMesh.MeshRes.class)) {
                            if (mr.id == 2)
                                banners.add(sym.apply(mr.m));
                        }
                        banners.trimToSize();
                        this.banners = banners;
                    }
                    rid = nrid;
                }
                if (banners != null) {
                    for (Rendered banner : banners)
                        rl.add(animwrap(banner));
                }
                return (rl);
            }

            public void update(Message sdt) {
                int fl = sdt.eom() ? 0 : sdt.uint8();
                fl &= ~4;
                this.nrid = sdt.eom() ? 0 : sdt.int64();
                update(fl);
            }
        };
        ret.update(sdt);
        return(ret);
    }
}
