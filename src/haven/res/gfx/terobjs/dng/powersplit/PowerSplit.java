package haven.res.gfx.terobjs.dng.powersplit;

import haven.Coord;
import haven.Coord3f;
import haven.GLState;
import haven.GOut;
import haven.Location;
import haven.Matrix4f;
import haven.PView;
import haven.RenderList;
import haven.Rendered;
import haven.States;
import haven.Text;
import haven.glsl.AutoVarying;
import haven.glsl.Discard;
import haven.glsl.Expression;
import haven.glsl.If;
import haven.glsl.ShaderMacro;
import haven.glsl.Tex2D;
import haven.glsl.Uniform;
import haven.glsl.VertexContext;

import static haven.glsl.Cons.gt;
import static haven.glsl.Cons.le;
import static haven.glsl.Cons.pick;
import static haven.glsl.Type.FLOAT;

@haven.FromResource(name = "gfx/terobjs/dng/powersplit", version = 18, override = true)
public class PowerSplit implements Rendered {
    public final Rendered r;
    public final boolean top;
    public float a = 0;

    public PowerSplit(Rendered r, boolean top) {
        this.r = r;
        this.top = top;
        scale = Location.scale(1, 1, 1);
    }

    DrawState state = new DrawState();
    Location scale;

    public static volatile float XLATEZ = 6f;
    public static volatile float SCALEZ = 0.9f;
    public boolean setup(RenderList rl) {
//        rl.prepo(state);
        rl.add(r, null);
        return (false);
    }

    public void update(float a) {
        this.a = a;
//        scale = Location.scale(1, 1, (1 - a) + (a * 0.32f));//(1 - a) * 1.5f
        scale = new Location(new Matrix4f(
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, a, XLATEZ * (1 - a),
                0, 0, 0, 1
        ));
//        state = new DrawState(a);
//        for (Rendered slot : slots)
//            slot.ostate(st);
    }

    static final AutoVarying degval = new AutoVarying(FLOAT, "splitval") {
        protected Expression root(VertexContext vctx) {
            return (pick(VertexContext.gl_MultiTexCoord[0].ref(), "y"));
        }
    };
    static final Uniform deg = new Uniform(FLOAT);
//    p ->((DrawState)p.get(States.adhoc)).a,States.adhoc
    static final ShaderMacro topsh = prog -> prog.fctx.mainmod(blk -> blk.add(new If(gt(degval.ref(), deg.ref()), new Discard())), -100);
    static final ShaderMacro botsh = prog -> prog.fctx.mainmod(blk -> blk.add(new If(le(degval.ref(), deg.ref()), new Discard())), -100);

    @Override
    public void draw(GOut g) {}

    class DrawState extends States.AdHoc {
        DrawState() {
            super(top ? topsh : botsh);
        }
    }
}
