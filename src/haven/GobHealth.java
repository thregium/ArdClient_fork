/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import java.awt.Color;

import static haven.Gob.SEMISTATIC;

/**
 * TODO: Think of a way to represent the stage in 3D to avoid static/semistatic mess.
 */
public class GobHealth extends GAttrib implements PView.Render2D {
    public float hp;

    public GobHealth(Gob g, float hp) {
        super(g);
        this.hp = hp;
        update(hp);
    }

    public double asfloat() {
        return (((double) hp) / 4.0);
    }

    public float val;
    private Tex tex;
    private static Matrix4f mv = new Matrix4f();
    private Projection proj;
    private Coord wndsz;
    private Location.Chain loc;
    private Camera camp;

    @Override
    public boolean setup(final RenderList r) {
        r.prepo(last);
        GLState.Buffer buf = r.state();
        proj = buf.get(PView.proj);
        wndsz = buf.get(PView.wnd).sz();
        loc = buf.get(PView.loc);
        camp = buf.get(PView.cam);
        return (true);
    }

    public Object staticp() {
        if (!DefSettings.SHOWGOBHP.get() || hp >= 4)
            return super.staticp();
        else
            return SEMISTATIC;
    }

    @Override
    public void draw(final GOut g) {}

    @Override
    public void draw2d(final GOut g) {
        if (tex != null) {
            float[] c = mv.load(camp.fin(Matrix4f.id)).mul1(loc.fin(Matrix4f.id)).homoc();
            Coord sc = proj.get2dCoord(c, wndsz);
//        sc.x -= 15;
//        sc.y -= 20;
            g.aimage(tex, sc, 0.5, 0.5);
        }
    }

    private static final Tex hlt0 = PUtils.strokeTex(Text.num12Fnd.render("1/4", new Color(255, 0, 0)));
    private static final Tex hlt1 = PUtils.strokeTex(Text.num12Fnd.render("2/4", new Color(255, 128, 0)));
    private static final Tex hlt2 = PUtils.strokeTex(Text.num12Fnd.render("3/4", new Color(255, 255, 0)));

    public void update(float val) {
        this.val = val;
        int v = (int) (val * 4) - 1;
        switch (v) {
            case 0:
                tex = hlt0;
                break;
            case 1:
                tex = hlt1;
                break;
            case 2:
                tex = hlt2;
                break;
        }
    }

    @OCache.DeltaType(OCache.OD_HEALTH)
    public static class $health implements OCache.Delta {
        @Override
        public void apply(Gob g, OCache.AttrDelta msg) {
            int hp = msg.uint8();
            g.setattr(new GobHealth(g, hp / 4.0f));
        }
    }
}
