package haven.resutil;

import haven.Config;
import haven.Coord2d;
import haven.Coord3f;
import haven.GLState;
import haven.GOut;
import haven.Glob;
import haven.Gob;
import haven.Loading;
import haven.Location;
import haven.Material;
import haven.PView;
import haven.RenderList;
import haven.Rendered;
import haven.Sprite;
import haven.States;
import haven.Utils;
import haven.VertexBuf.NormalArray;
import haven.VertexBuf.VertexArray;

import javax.media.opengl.GL;
import java.awt.Color;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.function.Supplier;

public class RectSprite extends Sprite {
    final Coord2d fixator;
    final VertexArray posa;
    final NormalArray nrma;
    final ShortBuffer sidx;
    private Coord2d lc;
    final Coord2d size;
    final Supplier<Color> colorSupplier;
    Color color;
    GLState smat;

    public RectSprite(Owner owner, Coord2d size, Supplier<Color> colorSupplier, Coord2d fixator) {
        super(owner, null);
        this.size = size;
        this.colorSupplier = colorSupplier;
        this.smat = new States.ColState(this.color = colorSupplier.get());
        this.fixator = fixator;
        float l = (float) (-this.size.x / 2);
        float u = (float) (-this.size.y / 2);
        float r = -l;
        float b = -u;
        int xn = Math.max(2, (int) ((r - l) / 11.0));
        int yn = Math.max(2, (int) ((b - u) / 11.0));
        int hn = xn + yn, n = hn * 2;
        FloatBuffer pa = Utils.wfbuf(n * 3 * 2);
        FloatBuffer na = Utils.wfbuf(n * 3 * 2);
        ShortBuffer sa = Utils.wsbuf(n * 6);
        int I, ii, v, N = n * 3;
        for (int i = 0; i < xn; i++) {
            float x = l + ((r - l) * i) / (xn - 1);
            I = i;
            v = I * 3;
            pa.put(v + 0, x).put(v + 1, -u).put(v + 2, 10);
            pa.put(v + N + 0, x).put(v + N + 1, -u).put(v + N + 2, -10);
            na.put(v + 0, 0).put(v + 1, 1).put(v + 2, 0);
            na.put(v + N + 0, 0).put(v + N + 1, 1).put(v + N + 2, 0);
            if (i < xn - 1) {
                ii = i * 6;
                sa.put(ii + 0, (short) (I + 1)).put(ii + 1, (short) (I + 1 + n)).put(ii + 2, (short) I);
                sa.put(ii + 3, (short) (I + 1 + n)).put(ii + 4, (short) (I + n)).put(ii + 5, (short) I);
            }
            I = i + hn;
            v = I * 3;
            pa.put(v + 0, x).put(v + 1, -b).put(v + 2, 10);
            pa.put(v + N + 0, x).put(v + N + 1, -b).put(v + N + 2, -10);
            na.put(v + 0, 0).put(v + 1, -1).put(v + 2, 0);
            na.put(v + N + 0, 0).put(v + N + 1, -1).put(v + N + 2, 0);
            if (i < xn - 1) {
                ii = (i + hn) * 6;
                sa.put(ii + 0, (short) I).put(ii + 1, (short) (I + n)).put(ii + 2, (short) (I + 1));
                sa.put(ii + 3, (short) (I + n)).put(ii + 4, (short) (I + 1 + n)).put(ii + 5, (short) (I + 1));
            }
        }
        for (int i = 0; i < yn; i++) {
            float y = u + ((b - u) * i) / (yn - 1);
            I = i + xn;
            v = I * 3;
            pa.put(v + 0, r).put(v + 1, -y).put(v + 2, 10);
            pa.put(v + N + 0, r).put(v + N + 1, -y).put(v + N + 2, -10);
            na.put(v + 0, 1).put(v + 1, 0).put(v + 2, 0);
            na.put(v + N + 0, 1).put(v + N + 1, 0).put(v + N + 2, 0);
            if (i < yn - 1) {
                ii = (i + xn) * 6;
                sa.put(ii + 0, (short) I).put(ii + 1, (short) (I + n)).put(ii + 2, (short) (I + 1));
                sa.put(ii + 3, (short) (I + n)).put(ii + 4, (short) (I + 1 + n)).put(ii + 5, (short) (I + 1));
            }
            I = i + xn + hn;
            v = I * 3;
            pa.put(v + 0, l).put(v + 1, -y).put(v + 2, 10);
            pa.put(v + N + 0, l).put(v + N + 1, -y).put(v + N + 2, -10);
            na.put(v + 0, -1).put(v + 1, 0).put(v + 2, 0);
            na.put(v + N + 0, -1).put(v + N + 1, 0).put(v + N + 2, 0);
            if (i < yn - 1) {
                ii = (i + xn + hn) * 6;
                sa.put(ii + 0, (short) (I + 1)).put(ii + 1, (short) (I + 1 + n)).put(ii + 2, (short) I);
                sa.put(ii + 3, (short) (I + 1 + n)).put(ii + 4, (short) (I + n)).put(ii + 5, (short) I);
            }
        }
        VertexArray posa = new VertexArray(Utils.bufcp(pa));
        NormalArray nrma = new NormalArray(Utils.bufcp(na));
        this.posa = posa;
        this.nrma = nrma;
        this.sidx = sa;
    }

    private void setz(Glob glob, Coord2d rc) {
        FloatBuffer pa = posa.data;
        int p = posa.size() / 2;
        try {
            float rz = (float) glob.map.getcz(rc);
            pos = Location.xlate(new Coord3f((float) rc.x, -(float) rc.y, rz));
            for (int i = 0; i < p; i++) {
                float z = (float) glob.map.getcz(rc.x + pa.get(i * 3), rc.y - pa.get(i * 3 + 1)) - rz;
                pa.put(i * 3 + 2, z + 10);
                pa.put((p + i) * 3 + 2, z - 10);
            }
        } catch (Loading e) {
        }
    }

    Location pos = null;

    public boolean tick(int dt) {
        Coord2d cc = ((Gob) owner).rc;
        if (fixator != null)
            cc = cc.floor(fixator).mul(fixator).add(fixator.div(2));
        if ((lc == null) || !lc.equals(cc)) {
            if (!Config.disableelev)
                setz(owner.context(Glob.class), cc);
            lc = cc;
        }
        return (false);
    }

    public boolean setup(RenderList rl) {
        rl.prepo(Rendered.eyesort);
        rl.prepo(Material.nofacecull);
        rl.state().put(PView.loc, null);
        if (pos != null)
            rl.prepo(pos);
        rl.state().put(States.color, null);
        return (true);
    }

    public void draw(GOut g) {
        if (!colorSupplier.get().equals(color))
            smat = new States.ColState(color = colorSupplier.get());
        g.state(smat);
        g.apply();

        posa.bind(g, false);
        nrma.bind(g, false);
        sidx.rewind();
        g.gl.glDrawElements(GL.GL_TRIANGLES, sidx.capacity(), GL.GL_UNSIGNED_SHORT, sidx);

        posa.unbind(g);
        nrma.unbind(g);
    }
}
