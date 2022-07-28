package haven.res.gfx.fx.bprad;

import haven.Coord2d;
import haven.GLState;
import haven.GOut;
import haven.Glob;
import haven.Loading;
import haven.Location;
import haven.Material;
import haven.Message;
import haven.RenderList;
import haven.Rendered;
import haven.Resource;
import haven.Sprite;
import haven.States;
import haven.Utils;
import haven.VertexBuf;
import java.awt.Color;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import javax.media.opengl.GL;

public class BPRad extends Sprite {
    static final GLState smat = new States.ColState(new Color(192, 0, 0, 128));
    static final GLState emat = new States.ColState(new Color(255, 224, 96));
    final VertexBuf.VertexArray posa;
    final VertexBuf.NormalArray nrma;
    final ShortBuffer sidx;
    final ShortBuffer eidx;
    float[] barda;
    private Coord2d lc;

    public BPRad(Owner owner, Resource res, Message sdt) {
        this(owner, res, Utils.hfdec((short) sdt.int16()) * 11);
    }

    public BPRad(Owner owner, Resource res, float r) {
        super(owner, res);
        int n = Math.max(24, (int) (2 * Math.PI * r / 11.0));
        FloatBuffer posb = Utils.wfbuf(n * 3 * 2);
        FloatBuffer nrmb = Utils.wfbuf(n * 3 * 2);
        ShortBuffer sidx = Utils.mksbuf(n * 6);
        ShortBuffer eidx = Utils.mksbuf(n);

        for (int i = 0; i < n; i++) {
            float s = (float) Math.sin(2 * Math.PI * i / n);
            float c = (float) Math.cos(2 * Math.PI * i / n);
            posb.put(i * 3 + 0, c * r).put(i * 3 + 1, s * r).put(i * 3 + 2, 10);
            posb.put((n + i) * 3 + 0, c * r).put((n + i) * 3 + 1, s * r).put((n + i) * 3 + 2, -10);
            nrmb.put(i * 3 + 0, c).put(i * 3 + 1, s).put(i * 3 + 2, 0);
            nrmb.put((n + i) * 3 + 0, c).put((n + i) * 3 + 1, s).put((n + i) * 3 + 2, 0);
            int g = i * 6;
            sidx.put(g + 0, (short) i).put(g + 1, (short) (i + n)).put(g + 2, (short) ((i + 1) % n));
            sidx.put(g + 3, (short) (i + n)).put(g + 4, (short) ((i + 1) % n + n)).put(g + 5, (short) ((i + 1) % n));
            eidx.put(i, (short) i);
        }

        VertexBuf.VertexArray posa = new VertexBuf.VertexArray(posb);
        VertexBuf.NormalArray nrma = new VertexBuf.NormalArray(nrmb);
        this.posa = posa;
        this.nrma = nrma;
        this.sidx = sidx;
        this.eidx = eidx;
    }

    private void setz(Glob glob, Coord2d c) {
        FloatBuffer posb = posa.data;
        int n = posa.size() / 2;
        try {
            float bz = (float) glob.map.getcz(c.x, c.y);
            for (int i = 0; i < n; i++) {
                float z = (float) glob.map.getcz(c.x + posb.get(i * 3), c.y - posb.get(i * 3 + 1)) - bz;
                posb.put(i * 3 + 2, z + 10);
                posb.put((n + i) * 3 + 2, z - 10);
            }
        } catch (Loading e) {
        }
    }

    public void draw(GOut g) {
        g.state(smat());
        g.apply();
        this.posa.bind(g, false);
        this.nrma.bind(g, false);
        this.sidx.rewind();
        g.gl.glDrawElements(4, this.sidx.capacity(), GL.GL_UNSIGNED_SHORT, this.sidx);
        g.state(emat());
        g.apply();
        this.eidx.rewind();
        g.gl.glLineWidth(3.0F);
        g.gl.glDrawElements(2, this.eidx.capacity(), GL.GL_UNSIGNED_SHORT, this.eidx);
        this.posa.unbind(g);
        this.nrma.unbind(g);
    }

    public boolean setup(RenderList rl) {
        rl.prepo(Rendered.eyesort);
        rl.prepo(Material.nofacecull);
        Location.goback(rl.state(), "gobx");
        rl.state().put(States.color, null);
        return (true);
    }

    public GLState smat() {
        return (smat);
    }

    public GLState emat() {
        return (emat);
    }
}
