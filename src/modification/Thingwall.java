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
import java.util.Collection;

public class Thingwall implements Sprite.Factory {
    public Sprite create(Sprite.Owner owner, Resource res, Message sdt) {
        Resource resource = res;
        VarSprite ret = new VarSprite(owner, res, Message.nil) {
            private long rid, nrid;
            private Rendered banner;

            public Collection<Rendered> iparts(int mask) {
                Collection<Rendered> rl = super.iparts(mask);
                if (nrid != rid) {
                    if (nrid == 0) {
                        banner = null;
                    } else {
                        Material base = resource.layer(Material.Res.class, 16).get();
                        FastMesh proj = resource.layer(FastMesh.MeshRes.class, 1).m;
                        TexGL tex = resource.pool.dynres(nrid).get().layer(TexR.class).tex();
                        Material sym = new Material(base, tex.draw(), tex.clip());
                        banner = sym.apply(proj);
                    }
                    rid = nrid;
                }
                if (banner != null)
                    rl.add(animwrap(banner));
                return (rl);
            }

            public void update(Message sdt) {
                int fl = sdt.eom() ? 0 : sdt.uint8();
                fl &= ~2;
                this.nrid = sdt.eom() ? 0 : sdt.int64();
                update(fl);
            }
        };
        ret.update(sdt);
        return(ret);
    }
}
