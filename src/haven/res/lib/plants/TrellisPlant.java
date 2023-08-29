package haven.res.lib.plants;

import haven.Config;
import haven.FastMesh.MeshRes;
import haven.Gob;
import haven.Message;
import haven.Resource;
import haven.Sprite;
import haven.Sprite.Factory;
import haven.Sprite.Owner;
import haven.Sprite.ResourceException;
import haven.resutil.CSprite;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class TrellisPlant implements Factory {
    public final int num;

    public TrellisPlant(int num) {
        this.num = num;
    }

    public TrellisPlant() {
        this(2);
    }

    public TrellisPlant(Object[] args) {
        this(((Number) args[0]).intValue());
    }

    public Sprite create(Owner owner, Resource res, Message std) {

        int st = std.uint8();
        List<MeshRes> meshes = res.layers(MeshRes.class).stream().filter(m -> m.id / 10 == st).collect(Collectors.toList());

        if (meshes.isEmpty())
            throw new ResourceException("No variants for grow stage " + st, res);
        CSprite spr = new CSprite(owner, res);
        if (Config.simplecrops) {
            MeshRes mesh = meshes.get(0);
            spr.addpart(0, 0, mesh.mat.get(), mesh.m);
        } else {
            Random rnd = owner.mkrandoom();
            double a = ((Gob) owner).a;
            float ac = (float) Math.cos(a), as = -(float) Math.sin(a);
            float d = 11f / (float) this.num;
            float c = -5.5f + d / 2f;

            for (int i = 0; i < this.num; ++i) {
                MeshRes mesh = meshes.get(rnd.nextInt(meshes.size()));
                spr.addpart(c * as, c * ac, mesh.mat.get(), mesh.m);
                c += d;
            }
        }

        return spr;
    }
}
