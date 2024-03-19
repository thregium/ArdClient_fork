package haven.res.gfx.terobjs.peacebreaker;

import haven.Coord3f;
import haven.Gob;
import haven.Indir;
import haven.Location;
import haven.Message;
import haven.RenderList;
import haven.Resource;
import haven.Sprite;

public class Peacebreaker extends Sprite implements Gob.Overlay.CUpd {
    public static final Coord3f fstart = Coord3f.o;
    public static final Coord3f fend = new Coord3f(0.00f, 0.00f, 18.00f);
    public static final Indir<Resource> pres = Resource.remote().load("gfx/terobjs/peacebreaker", 1);
    public static final Indir<Resource> sres = Resource.remote().load("gfx/terobjs/peacebreaker-skull", 1);
    private Sprite pole;
    private Sprite flag;
    private float a = 0;

    public Peacebreaker(Owner owner, Resource res) {
        super(owner, res);
        pole = Sprite.create(owner, pres.get(), Message.nil);
        flag = Sprite.create(owner, sres.get(), Message.nil);
    }

    public static Peacebreaker mksprite(Owner owner, Resource res, Message sdt) {
        Peacebreaker ret = new Peacebreaker(owner, res);
        ret.update(sdt);
        return (ret);
    }

    @Override
    public void update(Message sdt) {
        this.a = sdt.eom() ? 0 : sdt.unorm8();
    }

    @Override
    public boolean setup(final RenderList d) {
        d.add(pole, null);
        Coord3f off = fstart.add(fend.sub(fstart).mul(a));
        d.add(flag, Location.xlate(off));
        return (false);
    }

    @Override
    public boolean tick(int dt) {
        pole.tick(dt);
        flag.tick(dt);
        return (super.tick(dt));
    }
}
