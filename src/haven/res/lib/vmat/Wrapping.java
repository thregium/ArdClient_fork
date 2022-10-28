package haven.res.lib.vmat;

import haven.GLState;
import haven.GOut;
import haven.RenderList;
import haven.Rendered;
import modification.dev;

public class Wrapping implements Rendered {
    static {
        dev.checkFileVersion("lib/vmat", 36);
    }

    public final Rendered r;
    public final GLState st;
    public final int mid;

    public Wrapping(Rendered r, GLState st, int mid) {
        this.r = r;
        this.st = st;
        this.mid = mid;
    }

    public void draw(GOut g) {
    }

    public boolean setup(RenderList rl) {
        rl.add(r, st);
        return (false);
    }

    public String toString() {
        return (String.format("#<vmat %s %s>", mid, st));
    }
}

