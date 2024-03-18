package haven.res.ui.obj.buddy;

import haven.CompImage;
import haven.Coord;
import haven.FromResource;
import haven.GAttrib;
import haven.GOut;
import haven.Gob;
import haven.Loading;
import haven.PView;
import haven.RenderList;
import haven.Rendered;
import haven.Tex;
import haven.TexI;
import haven.Utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@FromResource(name = "ui/obj/buddy", version = 2, override = true)
public class Info extends GAttrib implements Rendered, PView.Render2D {
    public final List<InfoPart> parts = Collections.synchronizedList(new ArrayList<>());
    private Tex rend = null;
    private boolean dirty;
    private double seen = 0;
    private boolean auto;

    public Info(Gob gob) {
        super(gob);
    }

    @Override
    public void draw(final GOut g) {}

    @Override
    public void draw2d(final GOut g) {
        Coord sc = gob.sc.add(new Coord(gob.sczu.mul(15)));
//        Coord sc = Homo3D.obj2view(new Coord3f(0, 0, 15), state, Area.sized(g.sz())).round2();
        if (dirty && ctx != null) {
//            PView.RenderContext ctx = g.st.get(PView.ctx);
//            RenderContext ctx = state.get(RenderContext.slot);
            CompImage cmp = new CompImage();
            dirty = false;
            auto = false;
            for (InfoPart part : parts) {
                try {
                    part.draw(cmp, ctx);
                    auto |= part.auto();
                } catch (Loading l) {
                    dirty = true;
                }
            }
            rend = cmp.sz.equals(Coord.z) ? null : new TexI(cmp.compose());
        }
        if ((rend != null) && sc.isect(Coord.z, g.sz())) {
            double now = Utils.rtime();
            if (seen == 0)
                seen = now;
            double tm = now - seen;
            Color show = null;
            if (false) {
                /* XXX: QQ, RIP in peace until constant
                 * mouse-over checks can be had. */
                if (auto && (tm < 7.5)) {
                    show = Utils.clipcol(255, 255, 255, (int) (255 - ((255 * tm) / 7.5)));
                }
            } else {
                show = Color.WHITE;
            }
            if (show != null) {
                g.chcolor(show);
                g.aimage(rend, sc, 0.5, 1.0);
                g.chcolor();
            }
        } else {
            seen = 0;
        }
    }

    public void dirty() {
        if (rend != null)
            rend.dispose();
        rend = null;
        dirty = true;
    }

    public Tex rendered() {
        return (rend);
    }

    public static Info add(Gob gob, InfoPart part) {
        Info info = gob.getattr(Info.class);
        if (info == null)
            gob.setattr(info = new Info(gob));
        info.parts.add(part);
        info.parts.sort(Comparator.comparing(InfoPart::order));
        info.dirty();
        return (info);
    }

    public void remove(InfoPart part) {
        parts.remove(part);
        dirty();
    }

    PView.RenderContext ctx;

    @Override
    public boolean setup(final RenderList r) {
        if (dirty || ctx == null)
            ctx = r.cstate().get(PView.ctx);
        return (true);
    }

    @Override
    public Object staticp() {return (null);}
}

