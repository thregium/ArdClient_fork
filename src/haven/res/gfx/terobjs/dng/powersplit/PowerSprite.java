package haven.res.gfx.terobjs.dng.powersplit;

import haven.Coord;
import haven.FastMesh;
import haven.GLState;
import haven.GOut;
import haven.Location;
import haven.Material;
import haven.Matrix4f;
import haven.Message;
import haven.PView;
import haven.RenderList;
import haven.Rendered;
import haven.Resource;
import haven.SkelSprite;
import haven.States;

@haven.FromResource(name = "gfx/terobjs/dng/powersplit", version = 18, override = true)
public class PowerSprite extends SkelSprite {
    public final PowerSplit top, bot;

    public PowerSprite(Owner owner, Resource res) {
        super(owner, res);
        FastMesh r = res.layer(FastMesh.MeshRes.class, 0).m;
        Material tm = res.layer(Material.Res.class, 0).get();
        Material bm = res.layer(Material.Res.class, 1).get();
        top = new PowerSplit(tm.apply(r), true);
        bot = new PowerSplit(bm.apply(r), false);
        update();
    }

    public static PowerSprite mksprite(Owner owner, Resource res, Message sdt) {
        PowerSprite ret = new PowerSprite(owner, res);
        ret.update(sdt);
        return (ret);
    }

    public void update(Message sdt) {
        float a = sdt.eom() ? 0.5f : sdt.unorm8();
        top.update(a);
        bot.update(a);
    }

    @Override
    public boolean setup(RenderList rl) {
        super.setup(rl);
//        rl.add(top, null);
        rl.add(bot, bot.scale);
        return (false);
    }
}
