package haven.res.ui.apower;

import haven.Coord;
import haven.GOut;
import haven.Tex;
import haven.TexI;
import haven.Text;
import haven.Utils;
import haven.Widget;
import modification.dev;
import java.awt.Color;

public class PowerMeter extends Widget {
    static {
        dev.checkFileVersion("ui/apower", 11);
    }

    public final int N;
    public double apow;
    private final Tex[] lbls;

    public PowerMeter(Coord sz, int N, int k, int p, int q) {
        super(sz);
        this.N = N;
        this.lbls = new Tex[N];
        for (int i = 0; i < N; i++) {
            int n = i + k;
            if (((n * p) % q) == 0)
                lbls[i] = new TexI(Utils.outline2(Text.render(Integer.toString((n * p) / q), Color.WHITE).img, Utils.contrast(Color.WHITE)));
        }
    }

    public PowerMeter(Coord sz, int N) {
        this(sz, N, 1, 1, 1);
    }

    public void draw(GOut g) {
        g.chcolor(0, 0, 0, 255);
        g.frect(Coord.z, sz);
        if (apow > 0) {
            g.chcolor(64, 0, 0, 255);
            g.frect(new Coord(1, 1), new Coord((sz.x * (int) Math.ceil(apow)) / N, sz.y - 2));
        }
        g.chcolor(128, 0, 0, 255);
        int x = (int) Math.round((apow / N) * sz.x);
        if (x >= 1)
            g.frect(new Coord(1, 1), new Coord(x, sz.y - 2));
        g.chcolor(192, 192, 192, 255);
        for (int i = 1; i < N; i++)
            g.line(new Coord((sz.x * i) / N, 2), new Coord((sz.x * i) / N, sz.y - 2), 1);
        g.chcolor();
        for (int i = 0; i < N; i++) {
            if (lbls[i] != null) {
                double ic = (((sz.x * (i + 1)) / N) - ((sz.x * i) / N) - 1) * 0.5;
                g.image(lbls[i], new Coord((int) Math.round(((sz.x * i) / N) + 1 + ic - ((double) lbls[i].sz().x * 0.5)), (sz.y - lbls[i].sz().y) / 2));
            }
        }
    }

    public void set(double apow) {
        this.apow = Utils.clip(apow, 0, N);
    }
}
