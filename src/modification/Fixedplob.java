package modification;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import haven.MapView;
import haven.MapView.Plob;
import haven.MapView.PlobAdjust;
import haven.Message;
import haven.RenderList;
import haven.Resource;
import haven.Sprite;

import static haven.MCache.tilesz;

public class Fixedplob extends Sprite implements PlobAdjust {
    public final double a;

    boolean freerot = false;
    Coord2d gran = (MapView.plobgran == 0) ? null : new Coord2d(1.0 / MapView.plobgran, 1.0 / MapView.plobgran).mul(tilesz);

    public Fixedplob(Owner owner, Resource res, Message sdt) {
        super(owner, res);
        this.a = 2 * Math.PI * sdt.uint8() / 180.0D;
        if (owner instanceof Plob) {
            Plob plob = (Plob) owner;
            plob.adjust = this;
        }
    }

    public boolean setup(RenderList rl) {
        return (false);
    }

    public void adjust(Plob plob, Coord pc, Coord2d mc, int modflags) {
        if ((modflags & 2) == 0)
            plob.rc = mc.floor(tilesz).mul(tilesz).add(tilesz.div(2));
        else if (gran != null)
            plob.rc = mc.add(gran.div(2)).floor(gran).mul(gran);
        else
            plob.rc = mc;
        Gob pl = plob.mv().player();
        if ((pl != null) && !freerot)
            plob.a = Math.round(plob.rc.angle(pl.rc) / (Math.PI / 2)) * (Math.PI / 2);
    }

//    public void adjust(Plob plob, Coord pc, Coord2d mc, int modflags) {
//        if ((modflags & 2) == 0) {
//            plob.rc = mc.floor(MCache.tilesz).mul(MCache.tilesz).add(MCache.tilesz.div(2.0D));
//        } else {
//            plob.rc = mc;
//        }
//
//        plob.a = this.a;
//    }

    /*
     * Not work for this
     */
    public boolean rotate(Plob plob, int amount, int modflags) {
        return (false);
    }
}
