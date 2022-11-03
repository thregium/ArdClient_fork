/* Preprocessed source code */
package haven.res.gfx.terobjs.road.routeindicator;

import haven.BGL;
import haven.Coord3f;
import haven.GOut;
import haven.Glob;
import haven.Gob;
import haven.Loading;
import haven.Location;
import haven.Message;
import haven.RenderList;
import haven.Rendered;
import haven.Resource;
import haven.Sprite;
import haven.States;
import javax.media.opengl.GL2;

public class Route extends Sprite implements Rendered {
    public final long src;
    public final Coord3f srcoff, dstoff;
    public final Glob glob;
    private final float[] raw;
    private final float[] clr = {0.75f, 0.75f, 1.0f, 1.0f};

    public Route(Owner owner, Resource res, Message sdt) {
        super(owner, res);
        this.glob = owner.context(Glob.class);
        this.src = sdt.uint32();
        this.srcoff = new Coord3f(sdt.float16(), sdt.float16(), sdt.float16()).mul(11);
        this.dstoff = new Coord3f(sdt.float16(), sdt.float16(), sdt.float16()).mul(11);
        this.raw = new float[6];
    }

    public void draw(GOut g) {
        try {
            Gob dst = (Gob) owner;
            Coord3f ownerc = dst.getc();
            float sin = (float) Math.sin(dst.a), cos = (float) Math.cos(dst.a);
            Coord3f dc = Coord3f.of((dstoff.x * cos) - (dstoff.y * sin), (dstoff.y * cos) + (dstoff.x * sin), dstoff.z);
            Coord3f sc = dc;
            Gob gob = glob.oc.getgob(src);
            if (gob != null) sc = gob.getc().add(srcoff).sub(ownerc);
            raw[0] = sc.x;
            raw[1] = -sc.y;
            raw[2] = sc.z;
            raw[3] = dc.x;
            raw[4] = -dc.y;
            raw[5] = dc.z;

            g.apply();
            BGL gl = g.gl;
            gl.glPushAttrib(GL2.GL_ENABLE_BIT);

            gl.glEnable(GL2.GL_LINES);
            gl.glLineWidth(2.0F);
            gl.glEnable(GL2.GL_LINE_SMOOTH);
            gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);

            gl.glBegin(GL2.GL_LINES);
            gl.glColor4fv(clr, 0);
            gl.glVertex3f(raw[0], raw[1], raw[2]);
            gl.glVertex3f(raw[3], raw[4], raw[5]);
            gl.glEnd();

            gl.glPopAttrib();
        } catch (Loading l) {}
    }

    @Override
    public boolean tick(int dt) {
        try {
            Gob dst = (Gob) owner;
            float sin = (float) Math.sin(dst.a), cos = (float) Math.cos(dst.a);
            Coord3f dc = dst.getc().add((dstoff.x * cos) - (dstoff.y * sin), (dstoff.y * cos) + (dstoff.x * sin), dstoff.z);
            Coord3f sc = dc;
            Gob gob = glob.oc.getgob(src);
            if (gob != null) {
                sc = gob.getc().add(srcoff);
                raw[0] = sc.x; raw[1] = -sc.y; raw[2] = sc.z;
            }
            raw[3] = dc.x; raw[4] = -dc.y; raw[5] = dc.z;
        } catch (Loading l) {
        }
        return (false);
    }

    @Override
    public boolean setup(RenderList rl) {
        rl.prepo(Rendered.postpfx);
        Location.goback(rl.state(), "gobx");
        rl.state().put(States.color, null);
        return (true);
    }
}
