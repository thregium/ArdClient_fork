package modification;

import haven.Coord;
import haven.GOut;
import haven.GSprite;
import haven.GobIcon;
import haven.ItemInfo;
import haven.Loading;
import haven.Message;
import haven.PUtils;
import haven.Resource;
import haven.Tex;
import haven.TexI;
import haven.UI;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

public class DecalItem extends GSprite implements haven.res.ui.tt.defn.DynName {
    public static final Coord layc = UI.scale(8, 5);
    public static final int laysz = UI.scale(18);
    private static final Object[] definfo = {
            new Object[]{Loading.waitfor(Resource.remote().load("ui/tt/defn", 5))},
    };
    public final Tex bg = Resource.remote().loadwait("gfx/invobjs/parchment-decal").layer(Resource.imgc).tex();
    public final Tex lay;
    public String nm = "Decal";
    public Coord layoff = Coord.z;

    public DecalItem(Owner owner, Resource res, Message sdt) {
        super(owner);
        BufferedImage img = null;
        if (!sdt.eom()) {
            GSprite spr = ItemTex.mkspr(owner, sdt);
            img = ItemTex.sprimg(spr);
            if (img != null) {
                Coord isz = PUtils.imgsz(img);
                if (isz.x > isz.y) {
                    isz = new Coord(laysz, (isz.y * laysz) / isz.x);
                    layoff = new Coord(0, (laysz - isz.y) / 2);
                } else {
                    isz = new Coord((isz.x * laysz) / isz.y, laysz);
                    layoff = new Coord((laysz - isz.x) / 2, 0);
                }
                img = PUtils.convolve(img, isz, GobIcon.filter);
            }
            List<ItemInfo> info = ItemInfo.buildinfo(new ItemInfo.SpriteOwner() {
                public <T> T context(Class<T> cl) {
                    return (spr.owner.context(cl));
                }

                public List<ItemInfo> info() {
                    return (Collections.emptyList());
                }

                public Resource resource() {
                    return (spr.owner.getres());
                }

                public GSprite sprite() {
                    return (spr);
                }
            }, definfo);
            ItemInfo.Name laynm = ItemInfo.find(ItemInfo.Name.class, info);
            if (laynm != null)
                nm = "Decal of " + laynm.str.text;
        }
        lay = (img != null) ? new TexI(img) : null;
    }

    public void draw(GOut g) {
        g.image(bg, Coord.z);
        if (lay != null)
            g.image(lay, layc.add(layoff));
    }

    public Coord sz() {
        return (bg.sz());
    }

    public String name() {
        return (nm);
    }
}
