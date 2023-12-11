package haven.res.lib.icave;

import haven.Coord;
import haven.Coord3f;
import haven.GLState;
import haven.MapMesh;
import haven.MapMesh.Scan;
import haven.Material;
import haven.MeshBuf;
import haven.SNoise3;
import haven.Surface;
import haven.Surface.Vertex;
import haven.Tiler;
import haven.Tileset;
import haven.resutil.Ridges;
import haven.resutil.TerrainTile;

import java.util.Random;

public class InnerCave extends TerrainTile.RidgeTile {
    public static final float h = 16;
    public final Material wtex;

    public static class Walls {
        public final MapMesh m;
        public final Scan cs;
        public final Vertex[][] wv;
        private MapMesh.MapSurface ms;

        public Walls(MapMesh m) {
            this.m = m;
            this.ms = m.data(MapMesh.gnd);
            cs = new Scan(Coord.z, m.sz.add(1, 1));
            wv = new Vertex[cs.l][];
        }

        public Vertex[] fortile(Coord tc) {
            if (wv[cs.o(tc)] == null) {
                Random rnd = m.grnd(tc.add(m.ul));
                Vertex[] buf = wv[cs.o(tc)] = new Vertex[4];
                buf[0] = ms.new Vertex(ms.fortile(tc));
                for (int i = 1; i < buf.length; i++) {
                    buf[i] = ms.new Vertex(buf[0].x, buf[0].y, buf[0].z + (i * h / (buf.length - 1)));
                    buf[i].x += (rnd.nextFloat() - 0.5f) * 3.0f;
                    buf[i].y += (rnd.nextFloat() - 0.5f) * 3.0f;
                    buf[i].z += (rnd.nextFloat() - 0.5f) * 3.5f;
                }
            }
            return (wv[cs.o(tc)]);
        }
    }

    public static final MapMesh.DataID<Walls> walls = MapMesh.makeid(Walls.class);

    public static class Factory implements Tiler.Factory {
        @Override
        public Tiler create(int id, Tileset set) {
            TerrainTile.RidgeTile base = (TerrainTile.RidgeTile) new TerrainTile.RidgeTile.RFactory().create(id, set);
            Material wtex = null;
            for (Object rdesc : set.ta) {
                Object[] desc = (Object[]) rdesc;
                String p = (String) desc[0];
                if (p.equals("wmat")) {
                    wtex = set.getres().layer(Material.Res.class, (Integer) desc[1]).get();
                }
            }
            Ridges.TexCons rtex = (Ridges.TexCons) base.rcons;
            return (new InnerCave(id, base.noise, base.base, base.var, base.transset, base.rth, rtex.mat, rtex.texh, wtex));
        }
    }

    public InnerCave(int id, SNoise3 noise, GLState base, Var[] var, Tileset transset, int rth, GLState rmat, float texh, Material wtex) {
        super(id, noise, base, var, transset, rth, rmat, texh);
        this.wtex = wtex;
    }

    private static final Coord[] tces = {new Coord(0, -1), new Coord(1, 0), new Coord(0, 1), new Coord(-1, 0)};
    private static final Coord[] tccs = {new Coord(0, 0), new Coord(1, 0), new Coord(1, 1), new Coord(0, 1)};

    @Override
    public void trans(MapMesh m, Random rnd, Tiler gt, Coord lc, Coord gc, int z, int bmask, int cmask) {
        /* XXX: This hack is horrible. */
        if (m.map.tilesetr(m.map.gettile(gc)).name.equals("gfx/tiles/nil"))
            return;
        super.trans(m, rnd, gt, lc, gc, z, bmask, cmask);
    }

    private void modelwall(Walls w, Coord ltc, Coord rtc) {
        Vertex[] lw = w.fortile(ltc), rw = w.fortile(rtc);
        for (int i = 0; i < lw.length - 1; i++) {
            w.ms.new Face(lw[i + 1], lw[i], rw[i + 1]);
            w.ms.new Face(lw[i], rw[i], rw[i + 1]);
        }
    }

    @Override
    public void model(MapMesh m, Random rnd, Coord lc, Coord gc) {
        super.model(m, rnd, lc, gc);
        Walls w = null;
        for (int i = 0; i < 4; i++) {
            int cid = m.map.gettile(gc.add(tces[i]));
            if (cid <= id || (m.map.tiler(cid) instanceof InnerCave) || !m.map.tilesetr(cid).name.equals("gfx/tiles/nil"))
                continue;
            if (w == null) w = m.data(walls);
            modelwall(w, lc.add(tccs[i]), lc.add(tccs[(i + 1) % 4]));
        }
    }

    private void mkwall(MapMesh m, Walls w, Coord ltc, Coord rtc) {
        Vertex[] lw = w.fortile(ltc), rw = w.fortile(rtc);
        MapMesh.Model mod = MapMesh.Model.get(m, wtex);
        MeshBuf.Vertex[] lv = new MeshBuf.Vertex[lw.length], rv = new MeshBuf.Vertex[rw.length];
        MeshBuf.Tex tex = mod.layer(mod.tex);
        for (int i = 0; i < lv.length; i++) {
            float ty = (float) i / (float) (lv.length - 1);
            lv[i] = new Surface.MeshVertex(mod, lw[i]);
            tex.set(lv[i], new Coord3f(0, ty, 0));
            rv[i] = new Surface.MeshVertex(mod, rw[i]);
            tex.set(rv[i], new Coord3f(1, ty, 0));
        }
        for (int i = 0; i < lv.length - 1; i++) {
            mod.new Face(lv[i + 1], lv[i], rv[i + 1]);
            mod.new Face(lv[i], rv[i], rv[i + 1]);
        }
    }

    @Override
    public void lay(MapMesh m, Random rnd, Coord lc, Coord gc) {
        super.lay(m, rnd, lc, gc);
        Walls w = null;
        for (int i = 0; i < 4; i++) {
            int cid = m.map.gettile(gc.add(tces[i]));
            if (cid <= id || (m.map.tiler(cid) instanceof InnerCave) || !m.map.tilesetr(cid).name.equals("gfx/tiles/nil"))
                continue;
            if (w == null) w = m.data(walls);
            mkwall(m, w, lc.add(tccs[i]), lc.add(tccs[(i + 1) % 4]));
        }
    }
}
