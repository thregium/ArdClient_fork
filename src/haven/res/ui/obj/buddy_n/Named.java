package haven.res.ui.obj.buddy_n;

import haven.BuddyWnd;
import haven.CompImage;
import haven.Coord;
import haven.FromResource;
import haven.GAttrib;
import haven.Gob;
import haven.Message;
import haven.PView;
import haven.Text;
import haven.Utils;
import haven.res.ui.obj.buddy.Info;
import haven.res.ui.obj.buddy.InfoPart;

import java.awt.Color;

@FromResource(name = "ui/obj/buddy-n", version = 3, override = true)
public class Named extends GAttrib implements InfoPart {
    public final Info info;
    public final String nm;
    public final Color col;
    public final boolean auto;

    public Named(Gob gob, String nm, Color col, boolean auto) {
        super(gob);
        this.nm = nm;
        this.col = col;
        this.auto = auto;
        info = Info.add(gob, this);
    }

    public static void parse(Gob gob, Message dat) {
        String nm = dat.string();
        if (nm.length() > 0) {
            Color col = BuddyWnd.gc[dat.uint8()];
            int fl = dat.uint8();
            gob.setattr(new Named(gob, nm, col, (fl & 1) != 0));
        } else {
            gob.delattr(Named.class);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        info.remove(this);
    }

    @Override
    public void draw(CompImage cmp, PView.RenderContext ctx) {
        cmp.add(Utils.outline2(Text.std.render(nm, col).img, Utils.contrast(col)), Coord.z);
    }

    @Override
    public boolean auto() {return (auto);}
}
