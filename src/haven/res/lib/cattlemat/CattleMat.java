package haven.res.lib.cattlemat;

import haven.GLState;
import haven.GOut;
import haven.Material;
import haven.Message;
import haven.Resource;
import haven.States;
import haven.TexGL;
import haven.TexR;
import haven.glsl.AutoVarying;
import haven.glsl.Cons;
import haven.glsl.Expression;
import haven.glsl.Function;
import haven.glsl.Return;
import haven.glsl.ShaderMacro;
import haven.glsl.Tex2D;
import haven.glsl.Type;
import haven.glsl.Uniform;
import haven.glsl.ValBlock;
import haven.glsl.Variable;
import haven.glsl.VertexContext;
import haven.resutil.OverTex;

import javax.media.opengl.GL;
import java.util.Random;

public class CattleMat implements Material.Factory {
    public static class TexMix extends GLState {
        public static final Uniform[] vtexu = new Uniform[4];
        public static final Uniform ptexu = new Uniform(Type.SAMPLER2D);
        public final TexGL ctl;
        public final TexGL[] vars;

        static {
            for (int i = 0; i < vtexu.length; i++) {
                int u = i;
                vtexu[u] = new Uniform(Type.SAMPLER2D);
            }
        }

        public TexMix(TexGL ctl, TexGL[] vars) {
            this.ctl = ctl;
            this.vars = vars;
        }

        private static final AutoVarying otexcv = new AutoVarying(Type.VEC2, "otexc") {
            protected Expression root(VertexContext vctx) {
                return OverTex.otexc.ref();
            }
        };

        private static final Function pvfun = new Function.Def(Type.VEC4) {
            {
                Variable.Ref permv = this.param(PDir.IN, Type.VEC4).ref();
                Expression[] vals = new Expression[4];
                for (int i = 0; i < 4; ++i)
                    vals[i] = this.param(PDir.IN, Type.VEC4).ref();
                this.code.add(new Return(Cons.add(Cons.mul(vals[0], Cons.pick(permv, "r")), Cons.mul(vals[1], Cons.pick(permv, "g")), Cons.mul(vals[2], Cons.pick(permv, "b")), Cons.mul(vals[3], Cons.pick(permv, "a")))));
            }
        };

        private static final ShaderMacro shader = (prog) -> {
            ValBlock.Value[] vvals = new ValBlock.Value[4];
            Tex2D.texcoord(prog.fctx);
            for (int I = 0; I < 4; I++) {
                int i = I;
                vvals[i] = prog.fctx.uniform.new Value(Type.VEC4) {
                    public Expression root() {
                        return Cons.texture2D(CattleMat.TexMix.vtexu[i].ref(), Tex2D.texcoord(prog.fctx).depref());
                    }
                };
                vvals[i].force();
            }

            ValBlock.Value permv = prog.fctx.uniform.new Value(Type.VEC4) {
                public Expression root() {
                    return Cons.texture2D(CattleMat.TexMix.ptexu.ref(), CattleMat.TexMix.otexcv.ref());
                }
            };
            permv.force();

            prog.fctx.fragcol.mod((in) -> pvfun.call(permv.ref(), vvals[0].ref(), vvals[1].ref(), vvals[2].ref(), vvals[3].ref()), 0);
        };

        public ShaderMacro shader() {
            return shader;
        }

        public void reapply(GOut out) {
            out.gl.glUniform1i(out.st.prog.uniform(ptexu), this.psamp.id);

            for (int i = 0; i < 4; i++) {
                out.gl.glUniform1i(out.st.prog.uniform(vtexu[i]), this.vsamp[i].id);
            }

        }

        private GLState.TexUnit psamp;
        private GLState.TexUnit[] vsamp = new GLState.TexUnit[4];

        public void apply(GOut out) {
            this.ctl.glid(out);

            int i;
            for (i = 0; i < 4; i++) {
                this.vars[i].glid(out);
            }

            this.psamp = TexGL.lbind(out, this.ctl);

            for (i = 0; i < 4; ++i) {
                this.vsamp[i] = TexGL.lbind(out, this.vars[i]);
            }

            this.reapply(out);
        }

        public void unapply(GOut out) {
            this.psamp.ufree(out);
            this.psamp = null;

            for (int var2 = 0; var2 < 4; ++var2) {
                this.vsamp[var2].ufree(out);
                this.vsamp[var2] = null;
            }

        }

        public void prep(GLState.Buffer buf) {
            buf.put(States.adhoc, this);
        }
    }

    public Material create(Material.Owner owner, Resource res, Message sdt) {
        int[] rnd = new int[8];
        if (sdt.eom()) {
            Random rs = new Random(System.identityHashCode(owner));
            for (int i = 0; i < rnd.length; i++)
                rnd[i] = rs.nextInt(256);
        } else {
            for (int i = 0; i < rnd.length; i++)
                rnd[i] = sdt.uint8();
        }
        Material base = res.layer(Material.Res.class).get();
        PermTex ptex = new PermTex(res, rnd);
        ptex.magfilter(GL.GL_LINEAR);
        TexGL[] vars = new TexGL[4];
        for (int i = 0; i < 4; i++)
            vars[i] = res.layer(TexR.class, i).tex();
        return new Material(base, new TexMix(ptex, vars));
    }
}
