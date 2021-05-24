import haven.Coord;
import haven.GOut;
import haven.res.ui.croster.Entry;
import modification.dev;

public class Goat extends Entry {
    static {
        dev.checkFileVersion("gfx/hud/rosters/goat", 58);
    }

    public int meat, milk, wool;
    public int meatq, milkq, woolq, hideq;
    public double tmeatq, tmilkq, twoolq, thideq;
    public int seedq;
    public boolean billy, kid, dead, pregnant;

    public Goat(long id, String name) {
        super(SIZE, id, name);
    }

    public void draw(GOut g) {
        drawbg(g);
        int i = 0;
        drawcol(g, GoatRoster.cols.get(i), 0, this, namerend, i++);
        drawcol(g, GoatRoster.cols.get(i), 0.5, billy, sex, i++);
        drawcol(g, GoatRoster.cols.get(i), 0.5, kid, growth, i++);
        drawcol(g, GoatRoster.cols.get(i), 0.5, dead, deadrend, i++);
        drawcol(g, GoatRoster.cols.get(i), 0.5, pregnant, pregrend, i++);
        drawcol(g, GoatRoster.cols.get(i), 1, q, quality, i++);
        drawcol(g, GoatRoster.cols.get(i), 1, meat, null, i++);
        drawcol(g, GoatRoster.cols.get(i), 1, milk, null, i++);
        drawcol(g, GoatRoster.cols.get(i), 1, wool, null, i++);
        drawcol(g, GoatRoster.cols.get(i), 1, meatq, percent, i++);
        drawcol(g, GoatRoster.cols.get(i), 1, tmeatq, quality, i++);
        drawcol(g, GoatRoster.cols.get(i), 1, milkq, percent, i++);
        drawcol(g, GoatRoster.cols.get(i), 1, tmilkq, quality, i++);
        drawcol(g, GoatRoster.cols.get(i), 1, woolq, percent, i++);
        drawcol(g, GoatRoster.cols.get(i), 1, twoolq, quality, i++);
        drawcol(g, GoatRoster.cols.get(i), 1, hideq, percent, i++);
        drawcol(g, GoatRoster.cols.get(i), 1, thideq, quality, i++);
        drawcol(g, GoatRoster.cols.get(i), 1, seedq, null, i++);
        super.draw(g);
    }

    public boolean mousedown(Coord c, int button) {
        if (GoatRoster.cols.get(1).hasx(c.x)) {
            markall(Goat.class, o -> (o.billy == this.billy));
            return (true);
        }
        if (GoatRoster.cols.get(2).hasx(c.x)) {
            markall(Goat.class, o -> (o.kid == this.kid));
            return (true);
        }
        if (GoatRoster.cols.get(3).hasx(c.x)) {
            markall(Goat.class, o -> (o.dead == this.dead));
            return (true);
        }
        if (GoatRoster.cols.get(4).hasx(c.x)) {
            markall(Goat.class, o -> (o.pregnant == this.pregnant));
            return (true);
        }
        return (super.mousedown(c, button));
    }
}
