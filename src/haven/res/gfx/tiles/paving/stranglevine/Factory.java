package haven.res.gfx.tiles.paving.stranglevine;

import haven.FastMesh;
import haven.Message;
import haven.Resource;
import haven.Sprite;
import haven.resutil.CSprite;

import java.util.ArrayList;
import java.util.Random;

@haven.FromResource(name = "gfx/tiles/paving/stranglevine", version = 13, override = true)
public class Factory implements Sprite.Factory {
    public static final int[] dnum = {3, 6, 12};
    public static final float r = 3;

    public Sprite create(Sprite.Owner owner, Resource res, Message sdt) {
        ArrayList<FastMesh.MeshRes> var = new ArrayList<FastMesh.MeshRes>(res.layers(FastMesh.MeshRes.class));
        Random rnd = owner.mkrandoom();
        CSprite spr = new CSprite(owner, res);
        int st = sdt.eom() ? 2 : sdt.uint8();
        int num = rnd.nextInt(dnum[st]) + dnum[st];
        for (int i = 0; i < num; i++) {
            FastMesh.MeshRes v = var.get(rnd.nextInt(var.size()));
            spr.addpart((float) rnd.nextGaussian() * r, (float) rnd.nextGaussian() * r, v.mat.get(), v.m);
        }
        return (spr);
    }
}
