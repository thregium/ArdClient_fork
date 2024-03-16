package haven.res.gfx.hud.mmap.plo;

import haven.BuddyWnd;
import haven.Coord;
import haven.FromResource;
import haven.GOut;
import haven.Gob;
import haven.GobIcon;
import haven.OwnerContext;
import haven.PUtils;
import haven.Resource;
import haven.Utils;
import haven.res.ui.obj.buddy.Buddy;

import java.awt.Color;
import java.awt.image.BufferedImage;

@FromResource(name = "gfx/hud/mmap/plo", version = 9, override = true)
public class Player extends GobIcon.Icon {
    public static final Resource.Image img = Resource.remote().loadwait("gfx/hud/mmap/plo").layer(Resource.imgc);
    public final Gob gob = owner.fcontext(Gob.class, false);
    public final int group;
    public final GobIcon.Image image;

    public Player(OwnerContext owner, Resource res, int group) {
        super(owner, res);
        this.image = new GobIcon.Image(res);
        this.group = group;
        if ((group < 0) && (gob != null) && (group() < 0)) {
            /* XXX? I'm not sure how nice this is, but it's better
             * than mis-classifying players as the wrong group just
             * because the buddy-info hasn't been resolved yet. */
            throw (new Gob.DataLoading(gob, "Waiting for group-info..."));
        }
    }

    public Player(OwnerContext owner, Resource res) {
        this(owner, res, -1);
    }

    public int group() {
        Buddy buddy = gob.getattr(Buddy.class);
        if ((buddy != null) && (buddy.buddy() != null))
            return (buddy.buddy().group);
        return (-1);
    }

    public Object[] id() {
        int grp = (this.group >= 0) ? this.group : group();
        if (grp <= 0)
            return (nilid);
        return (new Object[grp]);
    }

    public Color color() {
        int grp = group();
        if ((grp >= 0) && (grp < BuddyWnd.gc.length))
            return (BuddyWnd.gc[grp]);
        return (Color.WHITE);
    }

    public String name() {return ("Player");}

    public BufferedImage image() {
        if (group < 0)
            return (img.img);
        BufferedImage buf = PUtils.copy(img.img);
        PUtils.colmul(buf.getRaster(), BuddyWnd.gc[group]);
        return (buf);
    }

    @Override
    public GobIcon.Image img() {
        return (image);
    }

    public void draw(GOut g, Coord cc) {
        Color col = Utils.colmul(g.getcolor(), color());
        g.chcolor(col);
        g.rotimage(img.tex(), cc, img.ssz.div(2), -gob.a - (Math.PI * 0.5));
        g.chcolor();
    }

    public boolean checkhit(Coord c) {
        return (c.isect(img.ssz.div(2).inv(), img.ssz));
    }

    public int z() {return (img.z);}
}

/* >mapicon: Factory */
