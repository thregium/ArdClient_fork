package haven.res.lib.plants;

import haven.Config;
import haven.FastMesh.MeshRes;
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

public class GrowingPlant implements Factory {
    public final int num;

    public GrowingPlant(int num) {
        this.num = num;
    }

    public GrowingPlant(Object[] args) {
        this(((Number) args[0]).intValue());
    }

    public Sprite create(Owner owner, Resource res, Message sdt) {
        int st = sdt.uint8();
        List<MeshRes> meshes = res.layers(MeshRes.class).stream().filter(m -> m.id / 10 == st).collect(Collectors.toList());

        if (meshes.isEmpty())
            throw (new ResourceException("No variants for grow stage " + st, res));
        CSprite spr = new CSprite(owner, res);
        if (Config.simplecrops) {
            MeshRes mesh = meshes.get(0);
            spr.addpart(0, 0, mesh.mat.get(), mesh.m);
        } else {
            Random rnd = owner.mkrandoom();
            for (int i = 0; i < this.num; ++i) {
                MeshRes mesh = meshes.get(rnd.nextInt(meshes.size()));
                spr.addpart(0, 0, mesh.mat.get(), mesh.m);
                if (this.num > 1) {
                    spr.addpart(rnd.nextFloat() * 11f - 5.5f, rnd.nextFloat() * 11f - 5.5f, mesh.mat.get(), mesh.m);
                } else {
                    spr.addpart(rnd.nextFloat() * 4.4f - 2.2f, rnd.nextFloat() * 4.4f - 2.2f, mesh.mat.get(), mesh.m);
                }
            }
        }
        return (spr);
    }
}
