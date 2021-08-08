import haven.Coord;
import haven.GOut;
import haven.res.ui.croster.Entry;

public class Horse extends Entry {
    public int meat, milk;
    public int meatq, milkq, hideq;
    public double tmeatq, tmilkq, thideq;
    public int seedq;
    public int end, stam, mb;
    public boolean stallion, foal, dead, pregnant, lactate;

    public Horse(long id, String name) {
        super(SIZE, id, name);
    }

    public void draw(GOut g) {
        drawbg(g);
        int i = 0;
        drawcol(g, HorseRoster.cols.get(i), 0, this, namerend, i++);
        drawcol(g, HorseRoster.cols.get(i), 0.5, stallion, sex, i++);
        drawcol(g, HorseRoster.cols.get(i), 0.5, foal, growth, i++);
        drawcol(g, HorseRoster.cols.get(i), 0.5, dead, deadrend, i++);
        drawcol(g, HorseRoster.cols.get(i), 0.5, pregnant, pregrend, i++);
        drawcol(g, HorseRoster.cols.get(i), 0.5, lactate, lactrend, i++);
        drawcol(g, HorseRoster.cols.get(i), 1, q, quality, i++);
        drawcol(g, HorseRoster.cols.get(i), 1, end, null, i++);
        drawcol(g, HorseRoster.cols.get(i), 1, stam, null, i++);
        drawcol(g, HorseRoster.cols.get(i), 1, mb, null, i++);
        drawcol(g, HorseRoster.cols.get(i), 1, meat, null, i++);
        drawcol(g, HorseRoster.cols.get(i), 1, milk, null, i++);
        drawcol(g, HorseRoster.cols.get(i), 1, meatq, percent, i++);
        drawcol(g, HorseRoster.cols.get(i), 1, tmeatq, quality, i++);
        drawcol(g, HorseRoster.cols.get(i), 1, milkq, percent, i++);
        drawcol(g, HorseRoster.cols.get(i), 1, tmilkq, quality, i++);
        drawcol(g, HorseRoster.cols.get(i), 1, hideq, percent, i++);
        drawcol(g, HorseRoster.cols.get(i), 1, thideq, quality, i++);
        drawcol(g, HorseRoster.cols.get(i), 1, seedq, null, i++);
        super.draw(g);
    }

    public boolean mousedown(Coord c, int button) {
        if (HorseRoster.cols.get(1).hasx(c.x)) {
            markall(Horse.class, o -> (o.stallion == this.stallion));
            return (true);
        }
        if (HorseRoster.cols.get(2).hasx(c.x)) {
            markall(Horse.class, o -> (o.foal == this.foal));
            return (true);
        }
        if (HorseRoster.cols.get(3).hasx(c.x)) {
            markall(Horse.class, o -> (o.dead == this.dead));
            return (true);
        }
        if (HorseRoster.cols.get(4).hasx(c.x)) {
            markall(Horse.class, o -> (o.pregnant == this.pregnant));
            return (true);
        }
        if (HorseRoster.cols.get(5).hasx(c.x)) {
            markall(Horse.class, o -> (o.lactate == this.lactate));
            return (true);
        }
        return (super.mousedown(c, button));
    }
}
