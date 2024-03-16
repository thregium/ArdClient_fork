package haven.res.ui.obj.buddy_v;

import haven.CompImage;
import haven.Coord;
import haven.FromResource;
import haven.GAttrib;
import haven.Gob;
import haven.Message;
import haven.PView;
import haven.Resource;
import haven.UI;
import haven.res.ui.obj.buddy.Info;
import haven.res.ui.obj.buddy.InfoPart;

import java.awt.image.BufferedImage;

@FromResource(name = "ui/obj/buddy-v", version = 2, override = true)
public class Vilmate extends GAttrib implements InfoPart {
    public static final BufferedImage icon = Resource.remote().loadwait("ui/obj/buddy-v").layer(Resource.imgc, 0).scaled();
    public final Info info;

    public Vilmate(Gob gob) {
        super(gob);
        info = Info.add(gob, this);
    }

    public static void parse(Gob gob, Message dat) {
        int fl = dat.uint8();
        if ((fl & 1) != 0)
            gob.setattr(new Vilmate(gob));
        else
            gob.delattr(Vilmate.class);
    }

    public void dispose() {
        super.dispose();
        info.remove(this);
    }

    public void draw(CompImage cmp, PView.RenderContext ctx) {
        int x = cmp.sz.x;
        if (x > 0)
            x += UI.scale(1);
        cmp.add(icon, Coord.of(x, 0));
    }
}
