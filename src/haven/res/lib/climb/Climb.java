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

public class Climb extends Sprite implements Gob.Overlay.SetupMod, Gob.Overlay.CDel {
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
    public boolean setup(RenderList rl) {
        return (false);
    }

    @Override
    public void setupmain(RenderList rl) {}

    @Override
    public void setupgob(final GLState.Buffer buf) {
        if (loc != null)
            loc.prep(buf);
    }

    @Override
    public boolean tick(int dt) {
        if (del)
            return (true);
        if (comp.poses != mp)
            mp.set(Composite.ipollen);
        return (false);
    }

    @Override
    public void delete() {
        if (comp.poses == mp)
            op.set(Composite.ipollen);
        del = true;
    }


}