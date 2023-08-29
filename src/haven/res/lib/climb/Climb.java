package haven.res.lib.climb;

import haven.Composite;
import haven.Composited;
import haven.GLState;
import haven.Gob;
import haven.Loading;
import haven.Location;
import haven.RenderList;
import haven.Resource;
import haven.Sprite;

public class Climb extends Sprite implements Gob.Overlay.SetupMod, Sprite.CDel {
    public final Composited comp;
    public final Composited.Poses op;
    public Composited.Poses mp;
    public boolean del = false;
    public Location loc = null;

    public Climb(Owner owner, Resource res) {
        super(owner, res);
        try {
            comp = ((Gob) owner).getattr(Composite.class).comp;
            op = comp.poses;
        } catch (NullPointerException e) {
            throw (new Loading("Applying climbing effect to non-complete gob", e));
        }
    }

    @Override
    public boolean setup(final RenderList d) {
        return false;
    }

    @Override
    public void setupmain(final RenderList rl) {}

    @Override
    public void setupgob(final GLState.Buffer buf) {
        if (loc != null) {
            loc.prep(buf);
        }
    }

    public GLState placestate() {
        return (loc);
    }

    public boolean tick(double dt) {
        if (del)
            return (true);
        return (false);
    }

    public void delete() {
        if (comp.poses == mp)
            op.set(Composite.ipollen);
        del = true;
    }


}