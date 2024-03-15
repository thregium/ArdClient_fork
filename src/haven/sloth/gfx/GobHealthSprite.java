package haven.sloth.gfx;

import haven.Camera;
import haven.Coord;
import haven.DefSettings;
import haven.GLState;
import haven.GOut;
import haven.Gob;
import haven.GobHealth;
import haven.Location;
import haven.Matrix4f;
import haven.PView;
import haven.Projection;
import haven.RenderList;
import haven.Sprite;
import haven.Tex;
import haven.Text;

import java.awt.Color;

public class GobHealthSprite extends Sprite {
    public static final int id = -24446;
    private static final Tex[] gobhp = new Tex[]{
            Text.renderstroked("25%", Color.WHITE, Color.BLACK, Gob.gobhpf).tex(),
            Text.renderstroked("50%", Color.WHITE, Color.BLACK, Gob.gobhpf).tex(),
            Text.renderstroked("75%", Color.WHITE, Color.BLACK, Gob.gobhpf).tex()
    };

    private Tex tex;
    private final Matrix4f mv = new Matrix4f();
    private Projection proj;
    private Coord wndsz;
    private Location.Chain loc;
    private Camera camp;

    public GobHealthSprite(final Gob g) {
        super(g, null);
    }

    public void draw(GOut g) {
        if (DefSettings.SHOWGOBHP.get() && tex != null) {
            float[] c = mv.load(camp.fin(Matrix4f.id)).mul1(loc.fin(Matrix4f.id)).homoc();
            Coord sc = proj.get2dCoord(c, wndsz);
            sc.x -= tex.sz().x / 2;
            sc.y -= 10;
            g.image(tex, sc);
        }
    }

    public boolean setup(RenderList rl) {
        rl.prepo(last);
        GLState.Buffer buf = rl.state();
        proj = buf.get(PView.proj);
        wndsz = buf.get(PView.wnd).sz();
        loc = buf.get(PView.loc);
        camp = buf.get(PView.cam);
        return true;
    }

    @Override
    public boolean tick(int dt) {
        final Gob g = (Gob) owner;
        final GobHealth hp = g.getattr(GobHealth.class);
        if (hp != null) {
            if (hp.hp < 1) {
                tex = gobhp[(int)(hp.hp * 4) - 1];
            } else {
                tex = null;
            }
        } else {
            tex = null;
        }
        return super.tick(dt);
    }

    public Object staticp() {
        return Gob.STATIC;
    }
}
