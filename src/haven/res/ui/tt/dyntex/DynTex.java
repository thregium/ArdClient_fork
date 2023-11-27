package haven.res.ui.tt.dyntex;

import haven.Coord;
import haven.ItemInfo;
import haven.PUtils;
import haven.Resource;
import haven.TexI;
import haven.TexL;
import haven.TexR;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class DynTex extends ItemInfo.Tip {
    public static final PUtils.Convolution filter = new PUtils.Lanczos(2);
    public final TexL img;

    public DynTex(Owner owner, long id) {
        super(owner);
        this.img = (TexL) Resource.classres(DynTex.class).pool.dynres(id).get().layer(TexR.class).tex();
    }

    public static ItemInfo mkinfo(Owner owner, Object... args) {
        return (new DynTex(owner, ((Number) args[1]).longValue()));
    }

    public BufferedImage tipimg() {
        BufferedImage img = this.img.fill();
        Coord sz = PUtils.imgsz(img);
        if (sz.y > 64) {
            sz = new Coord((64 * sz.x) / sz.y, 64);
            img = PUtils.convolvedown(img, sz, filter);
        }
        BufferedImage res = TexI.mkbuf(sz.add(10, 20));
        Graphics g = res.getGraphics();
        g.drawImage(img, 10, 10, null);
        g.dispose();
        return (res);
    }
}
