package haven.res.lib.plants;

import haven.Config;
import haven.FastMesh.MeshRes;
import haven.Message;
import haven.Resource;
import haven.Sprite;
import haven.Sprite.Factory;
import haven.Sprite.Owner;
import haven.resutil.CSprite;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GaussianPlant implements Factory {
    public final int numl;
    public final int numh;
    public final float r;

    public GaussianPlant(int numl, int numh, float r) {
        this.numl = numl;
        this.numh = numh;
        this.r = r;
    }

    public Sprite create(Owner owner, Resource res, Message sdt) {
        List<MeshRes> meshes = new ArrayList<>(res.layers(MeshRes.class));
        CSprite cs = new CSprite(owner, res);
        if (Config.simpleforage) {
            MeshRes mesh = meshes.get(0);
            cs.addpart(0, 0, mesh.mat.get(), mesh.m);
        } else {
            Random rnd = owner.mkrandoom();
            int scount = rnd.nextInt(this.numh - this.numl + 1) + this.numl;
            for (int i = 0; i < scount; ++i) {
                MeshRes mesh = meshes.get(rnd.nextInt(meshes.size()));
                cs.addpart((float) rnd.nextGaussian() * this.r, (float) rnd.nextGaussian() * this.r, mesh.mat.get(), mesh.m);
            }
        }

        return (cs);
    }
}
