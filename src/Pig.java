import haven.Coord;
import haven.GOut;
import haven.res.ui.croster.Entry;
import modification.dev;

public class Pig extends Entry {
    static {
        dev.checkFileVersion("gfx/hud/rosters/pig", 58);
    }

    public int meat, milk;
    public int meatq, milkq, hideq;
    public double tmeatq, tmilkq, thideq;
    public int seedq;
    public int prc;
    public boolean hog, piglet, dead, pregnant;

    public Pig(long id, String name) {
        super(SIZE, id, name);
    }

    public void draw(GOut g) {
        drawbg(g);
        int i = 0;
        drawcol(g, PigRoster.cols.get(i), 0, this, namerend, i++);
        drawcol(g, PigRoster.cols.get(i), 0.5, hog, sex, i++);
        drawcol(g, PigRoster.cols.get(i), 0.5, piglet, growth, i++);
        drawcol(g, PigRoster.cols.get(i), 0.5, dead, deadrend, i++);
        drawcol(g, PigRoster.cols.get(i), 0.5, pregnant, pregrend, i++);
        drawcol(g, PigRoster.cols.get(i), 1, q, quality, i++);
        drawcol(g, PigRoster.cols.get(i), 1, prc, null, i++);
        drawcol(g, PigRoster.cols.get(i), 1, meat, null, i++);
        drawcol(g, PigRoster.cols.get(i), 1, milk, null, i++);
        drawcol(g, PigRoster.cols.get(i), 1, meatq, percent, i++);
        drawcol(g, PigRoster.cols.get(i), 1, tmeatq, quality, i++);
        drawcol(g, PigRoster.cols.get(i), 1, milkq, percent, i++);
        drawcol(g, PigRoster.cols.get(i), 1, tmilkq, quality, i++);
        drawcol(g, PigRoster.cols.get(i), 1, hideq, percent, i++);
        drawcol(g, PigRoster.cols.get(i), 1, thideq, quality, i++);
        drawcol(g, PigRoster.cols.get(i), 1, seedq, null, i++);
        super.draw(g);
    }

    public boolean mousedown(Coord c, int button) {
        if (PigRoster.cols.get(1).hasx(c.x)) {
            markall(Pig.class, o -> (o.hog == this.hog));
            return (true);
        }
        if (PigRoster.cols.get(2).hasx(c.x)) {
            markall(Pig.class, o -> (o.piglet == this.piglet));
            return (true);
        }
        if (PigRoster.cols.get(3).hasx(c.x)) {
            markall(Pig.class, o -> (o.dead == this.dead));
            return (true);
        }
        if (PigRoster.cols.get(4).hasx(c.x)) {
            markall(Pig.class, o -> (o.pregnant == this.pregnant));
            return (true);
        }
        return (super.mousedown(c, button));
    }
}
