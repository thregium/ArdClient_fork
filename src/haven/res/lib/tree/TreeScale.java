package haven.res.lib.tree;

import haven.GAttrib;
import haven.GLState;
import haven.Gob;
import haven.Location;
import haven.RenderList;
import haven.States;

public class TreeScale extends GAttrib implements Gob.Overlay.SetupMod {
    public final float scale;
    public final Location mod;

    public TreeScale(Gob gob, float scale) {
        super(gob);
        this.scale = scale;
        this.mod = Tree.mkscale(scale);
    }

    public GLState placestate() {
        return (mod);
    }

    @Override
    public void setupgob(final GLState.Buffer buf) {
        if (mod != null) {
            mod.prep(buf);
        }
    }

    @Override
    public void setupmain(final RenderList rl) {
        if (mod != null) {
            rl.prepc(mod);
            rl.prepc(States.normalize);
        }
    }
}