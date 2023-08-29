package haven.res.lib.tree;

import haven.GAttrib;
import haven.GLState;
import haven.Gob;
import haven.Location;
import haven.RenderList;

public class TreeRotation extends GAttrib implements Gob.Overlay.SetupMod {
    public final Location rot;

    public TreeRotation(Gob gob, Location rot) {
        super(gob);
        this.rot = rot;
    }

    public GLState placestate() {
        return (rot);
    }

    @Override
    public void setupgob(final GLState.Buffer buf) {
        if (rot != null) {
            rot.prep(buf);
        }
    }

    @Override
    public void setupmain(final RenderList rl) {
        if (rot != null)
            rl.prepc(rot);
    }
}