package haven.res.gfx.terobjs.dng.beedungeon;

import haven.*;

@haven.FromResource(name = "gfx/terobjs/dng/beedungeon", version = 4, override = true)
public class Growing extends Sprite implements Gob.Overlay.SetupMod {
    public float a;

    public Growing(Owner owner, Resource res) {
        super(owner, res);
    }

    @Override
    public boolean setup(RenderList d) {
        return false;
    }

    public boolean tick(double ddt) {
        float dt = (float)ddt;
        if(a < 1) {
            a = Math.min(a + (dt * (1.0f / 0.5f)), 1.0f);
            return(false);
        } else {
            return(true);
        }
    }

    public GLState placestate() {
        return(new Location(new Matrix4f(a, 0, 0, 0,
                0, a, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1)));
    }

    @Override
    public void setupgob(GLState.Buffer buf) {}

    @Override
    public void setupmain(RenderList rl) {
        rl.prepc(placestate());
    }
}
