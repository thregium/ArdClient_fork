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

public class Bed implements Sprite.Factory {
    Material base = Resource.remote().loadwait("gfx/terobjs/furn/bed-sturdy").layer(Material.Res.class, 16).get();

    public Sprite create(Sprite.Owner owner, Resource res, Message sdt) {
        VarSprite ret = new VarSprite(owner, res, Message.nil) {
            Material sym = null;

            public Collection<Rendered> iparts(int mask) {
                Collection<Rendered> rl = super.iparts(mask);
                FastMesh proj = null;
                if ((mask & 1) != 0)
                    proj = res.layer(FastMesh.MeshRes.class, 2).m;
                else if ((mask & 2) != 0)
                    proj = res.layer(FastMesh.MeshRes.class, 3).m;
                if ((sym != null) && (proj != null))
                    rl.add(animwrap(sym.apply(proj)));
                return (rl);
            }

            public void update(Message sdt) {
                if (!sdt.eom() && (sdt.uint8() == 1)) {
                    long id = sdt.int64();
                    TexGL tex = Resource.remote().dynres(id).get().layer(TexR.class).tex();
                    sym = new Material(base, tex.draw(), tex.clip());
                } else {
                    sym = null;
                }
                super.update(sdt);
            }
        };
        ret.update(sdt);
        return (ret);
    }
}
