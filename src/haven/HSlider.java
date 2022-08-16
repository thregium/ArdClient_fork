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

public class HSlider extends Widget {
    private static final Tex schainl = Theme.tex("scroll/horizontal", 0);
    private static final Tex schainm = Theme.tex("scroll/horizontal", 1);
    private static final Tex schainr = Theme.tex("scroll/horizontal", 2);
    private static final Tex sflarp = Theme.tex("scroll/horizontal", 3);
    private final IButton plus = new IButton(Theme.fullres("buttons/circular/small/add"), this::plus);
    private final IButton minus = new IButton(Theme.fullres("buttons/circular/small/sub"), this::minus);

    public int val, min, max;
    private UI.Grab drag = null;


    public HSlider(int w, int min, int max, int val) {
        super(new Coord(w, sflarp.sz().y));
        this.val = val;
        this.min = min;
        this.max = max;
    }

    @Override
    protected void added() {
        super.added();
        adda(plus, Coord.of(sz.x, sz.y / 2), 1, 0.5);
        adda(minus, Coord.of(0, sz.y / 2), 0, 0.5);
    }

    @Override
    protected void attach(UI ui) {
        super.attach(ui);
        plus.attach(ui);
        minus.attach(ui);
    }

    public void draw(GOut g) {
        g.chcolor(DefSettings.SLIDERCOL.get());
        //y offset incase sflarp.sz.y > schain.sz.y
        int cy = (sflarp.sz().y / 2) - (schainl.sz().y / 2);
        //Top
        g.image(schainl, new Coord(0 + minus.sz.x, cy));
        //middle
        for (int x = schainl.sz().x + minus.sz.x; x < sz.x - schainr.sz().x - plus.sz.x; x += schainm.sz().x)
            g.image(schainm, new Coord(x, cy));
        //bottom
        g.image(schainr, new Coord(sz.x - schainr.sz().x - plus.sz.x, cy));
        //slider
        int fx = ((sz.x - sflarp.sz().x - minus.sz.x - plus.sz.x) * (val - min)) / (max - min) + minus.sz.x;
        g.image(sflarp, new Coord(fx, 0));
        g.chcolor();
        plus.draw(g.reclip(Coord.of(sz.x - plus.sz.x, -((plus.sz.y - sz.y) / 2)), Coord.of(plus.sz.x, sz.y + ((plus.sz.y - sz.y) / 2))));
        minus.draw(g.reclip(Coord.of(0, -((minus.sz.y - sz.y) / 2)), Coord.of(minus.sz.x, sz.y + ((minus.sz.y - sz.y) / 2))));
    }

    private int scval(int amount) {
        final int v;
        if (ui.modflags() == UI.MOD_SHIFT)
            v = amount * 10;
        else if (ui.modflags() == UI.MOD_CTRL)
            v = amount * 5;
        else if (ui.modflags() == (UI.MOD_CTRL | UI.MOD_SHIFT))
            v = amount * 100;
        else
            v = amount;
        return (v);
    }

    public void plus() {
        val = Math.min(val + scval(1), max);
        changed();
    }

    public void minus() {
        val = Math.max(val - scval(1), min);
        changed();
    }

    public boolean mousedown(Coord c, int button) {
        if (c.isect(Coord.z, Coord.of(minus.sz.x, sz.y)))
            return (minus.mousedown(c, button));
        else if (c.isect(Coord.of(sz.x - plus.sz.x, 0), Coord.of(plus.sz.x, sz.y)))
            return (plus.mousedown(c.sub(sz.x - plus.sz.x, 0), button));
        else {
            if (button != 1)
                return (false);
            drag = ui.grabmouse(this);
            mousemove(c);
            return (true);
        }
    }

    public void mousemove(Coord c) {
        if (c.isect(Coord.z, Coord.of(minus.sz.x, sz.y)))
            minus.mousemove(c);
        else if (c.isect(Coord.of(sz.x - plus.sz.x, 0), Coord.of(plus.sz.x, sz.y)))
            plus.mousemove(c.sub(sz.x - plus.sz.x, 0));
        else {
            if (drag != null) {
                double a = (double) (c.x - (sflarp.sz().x / 2) - minus.sz.x) / (double) (sz.x - sflarp.sz().x - minus.sz.x - plus.sz.x);
                if (a < 0)
                    a = 0;
                if (a > 1)
                    a = 1;
                val = (int) Math.round(a * (max - min)) + min;
                changed();
            }
        }
    }

    public boolean mouseup(Coord c, int button) {
        if (button != 1)
            return (false);
        if (drag == null) {
            if (c.isect(Coord.z, Coord.of(minus.sz.x, sz.y)))
                return (minus.mouseup(c, button));
            if (c.isect(Coord.of(sz.x - plus.sz.x, 0), Coord.of(plus.sz.x, sz.y)))
                return (plus.mouseup(c.sub(sz.x - plus.sz.x, 0), button));
            return (false);
        } else {
            drag.remove();
            drag = null;
            return (true);
        }
    }

    public boolean mousewheel(Coord c, int amount) {
        val = Math.max(Math.min(val - scval(amount), max), min);
        changed();
        return (true);
    }

    public void changed() {
    }

    public void resize(int w) {
        super.resize(new Coord(w, sflarp.sz().y));
    }
}
