package haven.res.ui.surv;

import haven.BGL;
import haven.Button;
import haven.Coord;
import haven.Coord3f;
import haven.FastMesh;
import haven.GLState;
import haven.GOut;
import haven.GameUI;
import haven.HSlider;
import haven.IButton;
import haven.Label;
import haven.Loading;
import haven.Location;
import haven.MCache;
import static haven.MCache.tilesz;
import haven.MapView;
import haven.RenderList;
import haven.Rendered;
import haven.States;
import haven.States.ColState;
import haven.States.DepthOffset;
import haven.States.PointSize;
import haven.Text;
import haven.Theme;
import haven.UI;
import haven.Utils;
import haven.VertexBuf;
import haven.VertexBuf.VertexArray;
import haven.Widget;
import haven.Window;
import static javax.media.opengl.GL.GL_FLOAT;
import static javax.media.opengl.GL.GL_POINTS;
import static javax.media.opengl.fixedfunc.GLPointerFunc.GL_COLOR_ARRAY;
import static javax.media.opengl.fixedfunc.GLPointerFunc.GL_VERTEX_ARRAY;
import modification.dev;
import java.awt.Color;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class LandSurvey extends Window {
    static {
        dev.checkFileVersion("ui/surv", 42);
    }

    final Coord ul;
    final Coord br;
    MapView mv;
    Display dsp;
    final FastMesh ol;
    final Location dloc;
    final Label albl;
    final Label zdlbl;
    final Label wlbl;
    final Label dlbl;
    final HSlider zset;
    final float gran;
    int tz;
    int defz;

    public IButton plus;
    public IButton minus;
    public Label zvalue;
    public Label value;

    public LandSurvey(Coord ul, Coord br, float gran, int tz) {
        super(Coord.z, "Land survey", "Land survey", true);
        this.ul = ul;
        this.br = br;
        this.gran = gran;
        this.tz = tz;
        this.dloc = Location.xlate(new Coord3f(this.ul.x * (float) tilesz.x, -this.ul.y * (float) tilesz.y, 0));
        VertexArray olv = new VertexArray(FloatBuffer.wrap(new float[]{
                0, 0, 0,
                (br.x - ul.x) * (float) tilesz.x, 0, 0,
                (br.x - ul.x) * (float) tilesz.x, -(br.y - ul.y) * (float)
                MCache.tilesz.y, 0, 0, -(br.y - ul.y) * (float) tilesz.y, 0,
        }));
        ol = new FastMesh(new VertexBuf(olv), ShortBuffer.wrap(new short[]{
                0, 3, 1,
                1, 3, 2,
        }));
        albl = add(new Label(String.format("Area: %d m\u00b2", (br.x - ul.x) * (br.y - ul.y))), 0, 0);
        zdlbl = add(new Label("..."), UI.scale(0, 15));
        wlbl = add(new Label("..."), UI.scale(0, 30));
        dlbl = add(new Label("..."), UI.scale(0, 45));
        zset = add(new HSlider(UI.scale(225), -1, 1, tz) {
            public void changed() {
                LandSurvey.this.tz = val; changeVal(String.format("%s%d", (val > defz) ? "+" : "", val - defz));
                upd = true;
                sendtz = Utils.rtime() + 0.5;
            }

            public Object tooltip(Coord c, Widget prev) {
                return Text.render(String.format("Z: %d, %s%d", val, (val > defz) ? "+" : "", val - defz)).tex();
            }
        }, UI.scale(0, 60));
        zvalue = add(new Label("Z"), UI.scale(0, 70));
        value = add(new Label("...") {
            public boolean mousewheel(Coord c, int amount) {
                final int v; if (ui.modshift) v = amount * 10;
                else if (ui.modctrl) v = amount * 5;
                else v = amount; wheel(-v); return (true);
            }
        }, UI.scale(0, 70));
        plus = add(new IButton(Theme.fullres("buttons/circular/small/add"), this::plus), UI.scale(0, 70));
        minus = add(new IButton(Theme.fullres("buttons/circular/small/sub"), this::minus), UI.scale(0, 70));
        add(new Button(UI.scale(100), "Make level") {
            public void click() {
                LandSurvey.this.wdgmsg("lvl", LandSurvey.this.tz / (gran * 11));
            }
        }, UI.scale(0, 90));
        add(new Button(UI.scale(100), "Remove") {
            public void click() {
                LandSurvey.this.wdgmsg("rm");
            }
        }, UI.scale(125, 90));
        pack();
    }

    public void changeVal(String text) {
        zvalue.settext("z: " + tz);
        value.settext(text);
        value.move(UI.scale(new Coord(asz.x / 2, value.c.y)), 0.5, 0);
        plus.move(UI.scale(new Coord(value.c.x + value.sz.x + 5, value.c.y)));
        minus.move(UI.scale(new Coord(value.c.x - 5, value.c.y)), 1, 0);
    }

    public void plus() {
        zset.val++;
        zset.changed();
    }

    public void minus() {
        zset.val--;
        zset.changed();
    }

    public void wheel(int a) {
        zset.val += a;
        zset.changed();
    }

    public static Widget mkwidget(UI ui, Object... args) {
        Coord ul = (Coord) args[0];
        Coord br = (Coord) args[1];
        float gran = ((Number) args[3]).floatValue() / 11;
        int tz = (args[2] == null) ? Integer.MIN_VALUE : Math.round(((Number) args[2]).floatValue() * gran * 11);
        return (new LandSurvey(ul, br, gran, tz));
    }

    protected void attached() {
        super.attached();
        this.mv = getparent(GameUI.class).map;
        this.defz = autoz();
        changeVal(zset.val - defz + "");
        this.dsp = new Display();
    }

    class Display implements Rendered {
        final GLState ptsz = new PointSize(3);
        final MCache map;
        final FloatBuffer cposb;
        final FloatBuffer ccolb;
        final int area;

        Display() {
            map = mv.ui.sess.glob.map;
            area = (br.x - ul.x + 1) * (br.y - ul.y + 1);
            cposb = Utils.mkfbuf(area * 3);
            ccolb = Utils.mkfbuf(area * 4);
            update();
        }

        public void draw(GOut g) {
            g.apply();
            BGL bg = g.gl;
            this.cposb.rewind();
            this.ccolb.rewind();
            bg.glEnableClientState(GL_VERTEX_ARRAY);
            bg.glVertexPointer(3, GL_FLOAT, 0, cposb);
            bg.glEnableClientState(GL_COLOR_ARRAY);
            bg.glColorPointer(4, GL_FLOAT, 0, ccolb);
            bg.glDrawArrays(GL_POINTS, 0, area);
            bg.glDisableClientState(GL_VERTEX_ARRAY);
            bg.glDisableClientState(GL_COLOR_ARRAY);
        }

        void update() {
            float E = 0.001f;
            cposb.rewind();
            ccolb.rewind();
            Coord c = new Coord();
            float tz = LandSurvey.this.tz / gran;
            for (c.y = ul.y; c.y <= br.y; c.y++) {
                for (c.x = ul.x; c.x <= br.x; c.x++) {
                    float z = (float) map.getfz(c);
                    cposb.put((c.x - ul.x) * (float) tilesz.x).put(-(c.y - ul.y) * (float) tilesz.y).put(tz);
                    if (Math.abs(tz - z) < E) {
                        ccolb.put(0).put(1).put(0).put(1);
                    } else if (tz < z) {
                        ccolb.put(1).put(0).put(1).put(1);
                    } else {
                        ccolb.put(0).put(0.5f).put(1).put(1);
                    }
                }
            }

        }

        public boolean setup(RenderList rls) {
            rls.prepo(dloc);
            rls.prepo(ptsz);
            rls.prepo(States.ndepthtest);
            rls.prepo(last);
            rls.prepo(States.vertexcolor);
            return (true);
        }
    }

    private int autoz() {
        MCache map = mv.ui.sess.glob.map;
        double zs = 0;
        int nv = 0;
        Coord c = new Coord();
        for (c.y = ul.y; c.y <= br.y; c.y++) {
            for (c.x = ul.x; c.x <= br.x; c.x++) {
                zs += map.getfz(c);
                nv++;
            }
        }
        return ((int) Math.round(zs / nv));
    }

    private boolean upd = true;

    private void updmap() {
        MCache map = mv.ui.sess.glob.map;
        Coord c = new Coord();
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        int sd = 0, hn = 0; for (c.y = ul.y; c.y <= br.y; c.y++) {
            for (c.x = ul.x; c.x <= br.x; c.x++) {
                int z = (int) Math.round(map.getfz(c) * gran);
                min = Math.min(min, z);
                max = Math.max(max, z);
                sd += tz - z;
                if (z > tz) hn += z - tz;
            }
        } zset.min = min - Math.round(11 * gran);
        zset.max = max + Math.round(11 * gran);
        zdlbl.settext(String.format("Peak to trough: %.1f m", (max - min) / 10.0));
        if (sd >= 0) wlbl.settext(String.format("Units of soil required: %d", sd));
        else wlbl.settext(String.format("Units of soil left over: %d", -sd));
        dlbl.settext(String.format("Units of soil to dig: %d", hn)); dsp.update();
    }

    private double sendtz = 0;
    private static final GLState olmat = GLState.compose(new ColState(new Color(255, 0, 0, 64)), Rendered.eyesort, new DepthOffset(-2, -2));
    private int olseq = -1;

    public void tick(double dt) {
        if (tz == Integer.MIN_VALUE) {
            try {
                zset.val = defz = tz = autoz();
                changeVal(0 + "");
                olseq = mv.ui.sess.glob.map.olseq;
                upd = true;
            } catch (Loading l) {
            }
        } else {
            if (upd || (olseq != mv.ui.sess.glob.map.olseq)) {
                try {
                    updmap();
                    olseq = mv.ui.sess.glob.map.olseq;
                    upd = false;
                } catch (Loading l) {
                }
            }

            if (olseq != -1) {
                mv.drawadd(dsp);
                mv.drawadd(GLState.compose(olmat, Location.xlate(new Coord3f(ul.x * (float) tilesz.x, -ul.y * (float) tilesz.y, tz))).apply(ol));
            }
        }
        if ((sendtz != 0) && (Utils.rtime() > sendtz)) {
            wdgmsg("tz", tz / (gran * 11));
            sendtz = 0;
        }
        super.tick(dt);
    }

    public void uimsg(String name, Object... args) {
        if (name == "tz") {
            tz = Math.round(((Number) args[0]).floatValue() * gran); zset.val = tz; upd = true;
        } else {
            super.uimsg(name, args);
        }
    }
}
