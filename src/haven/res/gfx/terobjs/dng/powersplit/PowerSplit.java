package haven.res.gfx.terobjs.dng.powersplit;

import haven.Coord;
import haven.GOut;
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
    }

    DrawState state = new DrawState();

    public boolean setup(RenderList rl) {
        rl.prepo(state);
        rl.add(r, null);
        return (false);
    }

    public void update(float a) {
        this.a = a;
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

        @Override
        public void apply(GOut g) {
            super.apply(g);
            reapply(g);
        }

        @Override
        public void reapply(GOut g) {
            super.reapply(g);
            if (a < 1)
                g.gl.glUniform1f(g.st.prog.uniform(deg), a);
        }
    }
}
