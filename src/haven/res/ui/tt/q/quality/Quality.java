package haven.res.ui.tt.q.quality;

import haven.Coord;
import haven.GItem;
import haven.GOut;
import haven.ItemInfo;
import haven.Resource;
import haven.Tex;
import haven.TexI;
import haven.res.ui.tt.q.qbuff.QBuff;

import java.awt.Color;

public class Quality extends QBuff implements GItem.OverlayInfo<Tex> {
    public static boolean show = false;

    public Quality(Owner owner, double q) {
        super(owner, Resource.remote().loadwait("ui/tt/q/quality").layer(Resource.imgc, 0).img, "Quality", q);
    }

    public static ItemInfo mkinfo(Owner owner, Object... args) {
        return (new Quality(owner, ((Number) args[1]).doubleValue()));
    }

    public Tex overlay() {
        return (new TexI(GItem.NumberInfo.numrender((int) Math.round(q), new Color(192, 192, 255, 255))));
    }

    public void drawoverlay(GOut g, Tex ol) {
        if (show)
            g.aimage(ol, new Coord(g.sz.x, 0), 1, 0);
    }
}
