package haven.res.gfx.terobjs.dng.powersplit;

import haven.GOut;
import haven.RenderList;
import haven.Rendered;
import haven.States;
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

    DrawState state = new DrawState(a);

    public boolean setup(RenderList rl) {
        rl.prepo(state);
        rl.add(r, null);
        return (false);
    }

    public void update(float a) {
        this.a = a;
        state = new DrawState(a);
//        for (Rendered slot : slots)
//            slot.ostate(st);
    }

    static final AutoVarying degval = new AutoVarying(FLOAT, "splitval") {
        protected Expression root(VertexContext vctx) {
            return (pick(Tex2D.tex2d.ref(), "y"));
        }
    };
    static final Uniform deg = new Uniform(FLOAT);
//    p ->((DrawState)p.get(States.adhoc)).a,States.adhoc
    static final ShaderMacro topsh = prog -> {
        prog.fctx.mainmod(blk -> blk.add(new If(gt(degval.ref(), deg.ref()),
                        new Discard())),
                -100);
    };
    static final ShaderMacro botsh = prog -> {
        prog.fctx.mainmod(blk -> blk.add(new If(le(degval.ref(), deg.ref()),
                        new Discard())),
                -100);
    };

    @Override
    public void draw(GOut g) {
        r.draw(g);
    }

    class DrawState extends States.AdHoc {
        final float a;

        DrawState(float a) {
            super(top ? topsh : botsh);
            this.a = a;
        }
    }
}
