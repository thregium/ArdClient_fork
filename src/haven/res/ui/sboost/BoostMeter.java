package haven.res.ui.sboost;

import haven.*;
import modification.dev;
import java.awt.Color;

public abstract class BoostMeter extends Widget {
    static {
        dev.checkFileVersion("ui/sboost", 1);
    }

    public static final int N = 8;
    public static final double st = 0.5;
    private int rlvl;
    private Text rmeter;

    public BoostMeter(Coord sz) {
        super(sz);
    }

    public abstract int level();

    public void draw(GOut g) {
        int lvl = level();
        if((rmeter == null) || (lvl != rlvl))
            rmeter = Text.render(String.format("%.1f\u00d7", ((rlvl = lvl) * st) + 1));
        g.chcolor(0, 0, 0, 255);
        g.frect(Coord.z, sz);

        g.chcolor(24, 48, 24, 255);
        g.frect2(new Coord(1, 1), new Coord(sz.x / 2, sz.y - 1));
        g.chcolor(48, 24, 24, 255);
        g.frect2(new Coord(sz.x / 2, 1), new Coord(sz.x - 1, sz.y - 1));

        if(lvl > 0) {
            g.chcolor(128, 0, 0, 255);
            g.frect(new Coord(1, 1), new Coord(((sz.x - 2) * lvl) / N, sz.y - 2));
        }

        g.chcolor(192, 192, 192, 255);
        for(int i = 0; i <= N; i++) {
            int h = UI.scale((((i * st) - (int)(i * st)) == 0) ? 3 : 2);
            int x = 1 + ((sz.x - 3) * i) / N;
            g.line(new Coord(x, 1), new Coord(x, 1 + h), 1);
            g.line(new Coord(x, sz.y - 2), new Coord(x, sz.y - 2 - h), 1);
        }

        g.chcolor();
        g.aimage(rmeter.tex(), sz.div(2), 0.5, 0.5);
    }
}
