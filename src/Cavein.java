import haven.*;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import java.awt.Color;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Cavein extends Sprite implements Gob.Overlay.CDel, PView.Render2D {
    static final GLState mat;
    List<Boll> bollar = new LinkedList<>();
    Random rnd = new Random();
    boolean spawn = true;
    FloatBuffer posb = null, nrmb = null;
    float de = 0;
    float str;
    float life;
    Coord3f off;
    Coord3f sz;

    public Cavein(Owner owner, Resource res, Message sdt) {
        super(owner, res);
        str = sdt.uint8();
        sz = new Coord3f(sdt.float8() * 11f, sdt.float8() * 11f, 0f);
        off = new Coord3f(-sz.x / 2f, -sz.y / 2f, sdt.float8() * 11f);
        sdt.uint8();
        life = 60 * Config.caveinduration;//make dust last however many minutes selected in display settings
    }

    static {
        if (!Config.colorfulcaveins)
            mat = new Material.Colors(new Color(255, 255, 255), new Color(255, 255, 255), new Color(128, 128, 128), new Color(0, 0, 0), 1);
        else {
            Random rnd = new Random();
            mat = new haven.Material.Colors(new Color(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)),
                    new Color(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)),
                    new Color(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)),
                    new Color(0, 0, 0), 1);
        }
    }

    @Override
    public void draw2d(GOut g) {
        if (owner instanceof Gob) {
            Gob gob = (Gob) owner;
            g.text(Math.round(str / 30.0) + " ", gob.sc);
        }
    }

    class Boll {
        Coord3f p, v, n;
        float sz;
        float t;

        Boll(Coord3f pos, float sz) {
            this.p = new Coord3f(pos.x, pos.y, pos.z);
            this.v = new Coord3f(0, 0, 0);
            this.n = new Coord3f(rnd.nextFloat() - 0.5f, rnd.nextFloat() - 0.5f, rnd.nextFloat() - 0.5f).norm();
            this.sz = sz;
            this.t = -1;
        }

        boolean tick(float dt) {
            v.z -= dt;
            v.z = Math.min(0, v.z + (dt * 5f * v.z * v.z / sz));
            v.x += dt * (float)rnd.nextGaussian() * 0.1f;
            v.y += dt * (float)rnd.nextGaussian() * 0.1f;
            p.x += v.x;
            p.y += v.y;
            p.z += v.z;
            if(p.z < 0) {
                p.z = 0;
                v.z *= -0.7f;
                v.x = v.z * (rnd.nextFloat() - 0.5f);
                v.y = v.z * (rnd.nextFloat() - 0.5f);
                if(t < 0)
                    t = 0;
            }
            if(t >= 0) {
                t += dt;
            }
            return(t > 1.5f);
        }
    }

    public void draw(GOut g) {
        updpos(g);
        if(posb == null)
            return;
        g.apply();
        BGL gl = g.gl;
        posb.rewind();
        nrmb.rewind();
        gl.glPointSize(1.1f);
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glVertexPointer(3, GL.GL_FLOAT, 0, posb);
        gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
        gl.glNormalPointer(GL.GL_FLOAT, 0, nrmb);
        gl.glDrawArrays(GL.GL_POINTS, 0, bollar.size());
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
    }

    void updpos(GOut g) {
        if(bollar.size() < 1) {
            posb = null; nrmb = null;
            return;
        }
        if((posb == null) || (posb.capacity() < bollar.size() * 3)) {
            int n = (posb == null)?512:(posb.capacity() / 3);
            posb = Utils.mkfbuf(n * 2 * 3);
            nrmb = Utils.mkfbuf(n * 2 * 3);
        }
        FloatBuffer tpos = Utils.wfbuf(3 * bollar.size()), tnrm = Utils.wfbuf(3 * bollar.size());
        for(Boll boll : bollar) {
            tpos.put(boll.p.x).put(boll.p.y).put(boll.p.z);
            tnrm.put(boll.n.x).put(boll.n.y).put(boll.n.z);
        }
        g.gl.bglCopyBufferf(posb, 0, tpos, 0, tpos.capacity());
        g.gl.bglCopyBufferf(nrmb, 0, tnrm, 0, tpos.capacity());
    }

    public boolean tick(int idt) {
        float dt = idt / 1000.0f;
        de += dt * str;
        if(spawn && (de > 1)) {
            de -= 1;
            bollar.add(new Boll(off.add(rnd.nextFloat() * sz.x, rnd.nextFloat() * sz.y, rnd.nextFloat() * sz.x), 0.5f + (rnd.nextFloat() * 1.5f)));
        }
        bollar.removeIf(boll -> boll.tick(dt));
        if(life > 0 && ((life -= dt) <= 0))
            spawn = false;
        return(!spawn && bollar.isEmpty());
    }

    public boolean setup(RenderList rl) {
        rl.prepo(Light.deflight);
        rl.prepo(mat);
        return(true);
    }

    public void delete() {
        spawn = false;
    }
}
