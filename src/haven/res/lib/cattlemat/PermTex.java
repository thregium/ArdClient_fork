package haven.res.lib.cattlemat;

import haven.Coord;
import haven.GOut;
import haven.Loading;
import haven.PUtils;
import haven.RenderList;
import haven.Resource;
import haven.SNoise3;
import haven.TexI;
import haven.TexL;
import haven.Utils;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

public class PermTex extends TexL {
    public static final SNoise3 noise = new SNoise3(5326222472434911090L);
    public final BufferedImage vmod;
    public final int[] rnd;

    public PermTex(BufferedImage vmod, int[] rnd) {
        super(PUtils.imgsz(vmod));
        this.vmod = vmod;
        this.rnd = rnd;
    }

    public PermTex(Resource res, int[] rnd) {
        this(res.layer(Resource.imgc, 100).rawimage, rnd);
    }

    public static double mtri(int rv) {
        rv &= 255;
        return (double) (rv < 128 ? rv : 256 - rv) / 128.0;
    }

    public static BufferedImage convert(BufferedImage img) {
        WritableRaster buf = Raster.createInterleavedRaster(0, img.getWidth(), img.getHeight(), 4, null);
        BufferedImage tgt = new BufferedImage(TexI.glcm, buf, false, null);
        Graphics2D g = tgt.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return tgt;
    }

    public static BufferedImage alphainv(BufferedImage img) {
        WritableRaster src = img.getRaster();
        WritableRaster buf = PUtils.imgraster(PUtils.imgsz(img));
        for (int y = 0; y < buf.getHeight(); y++) {
            for (int x = 0; x < buf.getWidth(); x++) {
                buf.setSample(x, y, 0, src.getSample(x, y, 0));
                buf.setSample(x, y, 1, src.getSample(x, y, 1));
                buf.setSample(x, y, 2, src.getSample(x, y, 2));
                buf.setSample(x, y, 3, 255 - src.getSample(x, y, 3));
            }
        }

        return PUtils.rasterimg(buf);
    }

    public BufferedImage fill() {
        WritableRaster vmod = convert(this.vmod).getRaster();
        Coord vres = PUtils.imgsz(vmod);
        WritableRaster buf = PUtils.imgraster(vres);
        for (int y = 0; y < vres.y; y++) {
            for (int x = 0; x < vres.x; x++) {
                for (int b = 0; b < 4; b++)
                    buf.setSample(x, y, b, 0);
            }
        }

        int pb = (rnd[0] & 0x3) >> 0;
        int sb = (rnd[0] & 0x0c) >> 2;
        if (sb == pb)
            sb = (sb + 1) % 4;
        double conv = mtri(this.rnd[1]);
        double[] fs = new double[]{1.0, 2.5, 10.0};
        double[] zs = new double[]{mtri(this.rnd[2]), mtri(this.rnd[3]), mtri(this.rnd[4])};
        double convz = zs[0] + zs[1] + zs[2];
        double[] ks = new double[]{mtri(this.rnd[5]) * 0.5, mtri(this.rnd[5] * 3) * 0.5, 0.0};
        ks[2] = 1.0 - (ks[0] + ks[1]);
        double[] mcs = new double[]{mtri(this.rnd[6]) * 2.0 - 1.0, mtri(this.rnd[6] * 2) * 2.0 - 1.0, mtri(this.rnd[6] * 3) * 2.0 - 1.0};
        double tl = mtri(this.rnd[7]) * 0.5 + 0.25;

        double summ = 1.0;
        double sumk = 1.0 / (2.0 + (mcs[0] + mcs[1] + mcs[2]) * 0.5);

        for (int Y = 0; Y < vres.y; Y++) {
            for (int X = 0; X < vres.x; X++) {
                double x = (double) X / (double) (vres.x - 1);
                double y = (double) Y / (double) (vres.y - 1);
                double mx = x + noise.get(1.0, x * 5.0, y * 5.0, convz * 5.0 + 582.0) * conv;
                double my = y + noise.get(1.0, x * 5.0, y * 5.0, convz * 5.0 - 238.0) * conv;
                double v = 0.0;

                for (int l = 0; l < 3; l++) {
                    v += noise.get(1.0, mx, my, zs[l] + (double) (l * 278)) * ks[l];
                    v += (double) vmod.getSample(X, Y, l) / 255.0 * mcs[l] * 0.5;
                }

                v = (v + summ) * sumk;
                v = Utils.clip((v - tl) * 25.0, 0.0, 1.0);
                buf.setSample(X, Y, pb, (float) Math.round(v * 255.0));
                buf.setSample(X, Y, sb, (float) (255L - Math.round(v * 255.0)));
            }
        }

        BufferedImage ret = PUtils.rasterimg(buf);
        return ret;
    }

    protected void fill(GOut out) {
        try {
            super.fill(out);
        } catch (Loading l) {
            throw new RenderList.RLoad(l);
        }
    }
}
