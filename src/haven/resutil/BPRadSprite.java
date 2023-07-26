package haven.resutil;

import haven.Config;
import haven.Coord2d;
import haven.DefSettings;
import haven.GLState;
import haven.GOut;
import haven.Glob;
import haven.Gob;
import haven.Loading;
import haven.Location;
import haven.Material;
import haven.RenderList;
import haven.Rendered;
import haven.Sprite;
import haven.States;
import haven.States.ColState;
import haven.Utils;
import haven.VertexBuf.NormalArray;
import haven.VertexBuf.VertexArray;
import modification.configuration;

import javax.media.opengl.GL;
import java.awt.Color;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Objects;

public class BPRadSprite extends Sprite {
    public static int getId(String name) {
        int h = 0;
        for (int i = 0; i < name.length(); i++)
            h = (h * 31) + name.charAt(i);
        return (h);
    }

    public static GLState smatDanger = new ColState(DefSettings.ANIMALDANGERCOLOR.get());
    public static GLState smatSupports = new ColState(DefSettings.SUPPORTDANGERCOLOR.get());
    public static GLState smatBeehive = new ColState((DefSettings.BEEHIVECOLOR.get()));
    public static GLState smatTrough = new ColState(DefSettings.TROUGHCOLOR.get());
    public static GLState smatMoundbed = new ColState(DefSettings.MOUNDBEDCOLOR.get());
    public static GLState smatBarter = new ColState(DefSettings.BARTERCOLOR.get());
    final GLState smat;
    final VertexArray posa;
    final NormalArray nrma;
    final ShortBuffer sidx;
    private Coord2d lc;
    float height;

    public BPRadSprite(Owner owner, float rad, float basez, GLState smat) {
        super(owner, null);

        this.smat = smat;
        this.height = configuration.radiusheight;

        int per = Math.max(24, (int) (2 * Math.PI * rad / 11.0));
        FloatBuffer pa = Utils.mkfbuf(per * 3 * 2);
        FloatBuffer na = Utils.mkfbuf(per * 3 * 2);
        ShortBuffer sa = Utils.mksbuf(per * 6);

        for (int i = 0; i < per; i++) {
            float s = (float) Math.sin(2 * Math.PI * i / per);
            float c = (float) Math.cos(2 * Math.PI * i / per);
            pa.put(i * 3 + 0, c * rad).put(i * 3 + 1, s * rad).put(i * 3 + 2, height);
            pa.put((per + i) * 3 + 0, c * rad).put((per + i) * 3 + 1, s * rad).put((per + i) * 3 + 2, basez);
            na.put(i * 3 + 0, c).put(i * 3 + 1, s).put(i * 3 + 2, 0.0F);
            na.put((per + i) * 3 + 0, c).put((per + i) * 3 + 1, s).put((per + i) * 3 + 2, 0.0F);
            int v = i * 6;
            sa.put(v + 0, (short) i).put(v + 1, (short) (i + per)).put(v + 2, (short) ((i + 1) % per));
            sa.put(v + 3, (short) (i + per)).put(v + 4, (short) ((i + 1) % per + per)).put(v + 5, (short) ((i + 1) % per));
        }

        this.posa = new VertexArray(pa);
        this.nrma = new NormalArray(na);
        this.sidx = sa;
    }

    private void setz(Glob glob, Coord2d rc) {
        FloatBuffer pa = posa.data;
        int p = posa.size() / 2;
        try {
            float rz = (float) glob.map.getcz(rc);

            for (int i = 0; i < p; i++) {
                float z = Config.disableelev ? 0 : (float) glob.map.getcz(rc.x + pa.get(i * 3), rc.y - pa.get(i * 3 + 1)) - rz;
                pa.put(i * 3 + 2, z + height);
                pa.put((p + i) * 3 + 2, z - height);
            }
        } catch (Loading e) {
        }
    }

    public boolean tick(int dt) {
        Coord2d rc = ((Gob) owner).rc;
        if (lc == null || !lc.equals(rc) || height != configuration.radiusheight) {
            setz(this.owner.context(Glob.class), rc);
            if (!Objects.equals(lc, rc)) lc = rc;
            if (height != configuration.radiusheight) height = configuration.radiusheight;
        }

        return false;
    }

    public boolean setup(RenderList rl) {
        rl.prepo(Rendered.eyesort);
        rl.prepo(Material.nofacecull);
        Location.goback(rl.state(), "gobx");
        rl.state().put(States.color, null);
        return true;
    }

    public void updateSmatSupports() {
        smatSupports = new ColState(new Color(Config.smatSupportsred, Config.smatSupportsgreen, Config.smatSupportsblue, 100));
    }

    public void draw(GOut g) {
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
