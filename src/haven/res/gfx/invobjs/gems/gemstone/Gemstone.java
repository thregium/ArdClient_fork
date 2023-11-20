package haven.res.gfx.invobjs.gems.gemstone;

import haven.Coord;
import haven.GOut;
import haven.GSprite;
import haven.Message;
import static haven.PUtils.Lanczos;
import static haven.PUtils.alphablit;
import static haven.PUtils.blit;
import static haven.PUtils.convolvedown;
import static haven.PUtils.imgraster;
import static haven.PUtils.imgsz;
import static haven.PUtils.rasterimg;
import static haven.PUtils.tilemod;
import haven.Resource;
import haven.Tex;
import haven.TexI;
import haven.TexL;
import haven.TexR;
import haven.UI;
import modification.configuration;
import org.json.JSONObject;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.Map;

public class Gemstone extends GSprite implements GSprite.ImageSprite, haven.res.ui.tt.defn.DynName {
    public final BufferedImage img;
    public final Tex tex;
    public final String name;
    public static Map<String, BufferedImage> cachedImg = new HashMap<>();
    public static Map<String, Tex> cachedTex = new HashMap<>();
    public static Map<String, String> cachedName = new HashMap<>();
    public static JSONObject object;

    static {
        try {
            object = configuration.loadjson("Gemstones.json");
            if (object.length() > 0) {
                for (String sdt : object.keySet()) {
                    try {
                        JSONObject jo = object.getJSONObject(sdt);
                        BufferedImage bi = configuration.bytesToImage(configuration.JSONArrayToBytes(jo.getJSONArray("BufferedImage")));
                        cachedImg.put(sdt, bi);
                        cachedTex.put(sdt, new TexI(bi));
                        cachedName.put(sdt, (String) jo.get("Name"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            object = new JSONObject();
            e.printStackTrace();
        }
    }

    public Gemstone(Owner owner, Resource res, Message sdt) {
        super(owner);
        String cmsg = sdt.toString();
        Resource.Resolver rr = owner.context(Resource.Resolver.class);
        if (!sdt.eom()) {
            Resource cut = rr.getres(sdt.uint16()).get();
            int texid = sdt.uint16();
            if (texid != 65535) {
                if (configuration.cachedGem) {
                    BufferedImage cimg = cachedImg.get(cmsg);
                    if (cimg == null) {
                        Resource tex = rr.getres(texid).get();
                        this.tex = new TexI(this.img = construct(cut, tex));
                        this.name = cut.layer(Resource.tooltip).t + " " + tex.layer(Resource.tooltip).t;
                        cachedImg.put(cmsg, this.img);
                        cachedTex.put(cmsg, this.tex);
                        cachedName.put(cmsg, this.name);
                        try {
                            JSONObject cache = new JSONObject();
                            cache.put("BufferedImage", configuration.imageToBytes(this.img));
                            cache.put("Name", this.name);
                            object.put(cmsg, cache);
                            configuration.savejson("Gemstones.json", object);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        this.img = cimg;
                        this.tex = cachedTex.get(cmsg);
                        this.name = cachedName.get(cmsg);
                    }
                } else {
                    Resource tex = rr.getres(texid).get();
                    this.tex = new TexI(this.img = construct(cut, tex));
                    this.name = cut.layer(Resource.tooltip).t + " " + tex.layer(Resource.tooltip).t;
                }
            } else {
                this.tex = new TexI(this.img = construct(cut, null));
                this.name = cut.layer(Resource.tooltip).t + " Gemstone";
            }
        } else {
            this.tex = new TexI(this.img = TexI.mkbuf(new Coord(32, 32)));
            this.name = "Broken gem";
        }
    }

    public static BufferedImage convert(BufferedImage img) {
        WritableRaster buf = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, img.getWidth(), img.getHeight(), 4, null);
        BufferedImage tgt = new BufferedImage(TexI.glcm, buf, false, null);
        Graphics g = tgt.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return (tgt);
    }

    public static final WritableRaster alphamod(WritableRaster dst) {
        int w = dst.getWidth(), h = dst.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                dst.setSample(x, y, 3, (dst.getSample(x, y, 3) * 3) / 4);
            }
        }
        return (dst);
    }

    public static final WritableRaster alphasq(WritableRaster dst) {
        int w = dst.getWidth(), h = dst.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = dst.getSample(x, y, 3);
                dst.setSample(x, y, 3, (a * a) / 255);
            }
        }
        return (dst);
    }

    public static BufferedImage construct(Resource cut, Resource tex) {
        Resource.Image outl, body, hili;
        BufferedImage outli, bodyi, hilii;
        try {
            outl = cut.layer(Resource.imgc, 0);
            body = cut.layer(Resource.imgc, 1);
            hili = cut.layer(Resource.imgc, 2);
            outli = convert(outl.scaled());
            bodyi = convert(body.scaled());
            hilii = convert(hili.scaled());
        } catch (RuntimeException e) {
            throw (new RuntimeException("invalid gemstone in " + cut.name, e));
        }
        Coord sz = UI.scale(32, 32);
        WritableRaster buf = imgraster(sz);
        blit(buf, outli.getRaster(), outl.o);
        WritableRaster buf2 = imgraster(sz);
        blit(buf2, bodyi.getRaster(), body.o);
        alphablit(buf2, hilii.getRaster(), hili.o);
        if (tex != null) {
            BufferedImage texi = ((TexL) tex.layer(TexR.class).tex()).fill();
            texi = convolvedown(texi, sz.mul(2), new Lanczos(3));
            tilemod(buf2, texi.getRaster(), Coord.z);
        }
        // alphamod(buf2);
        alphablit(buf2, alphasq(blit(imgraster(imgsz(hilii)), hilii.getRaster(), Coord.z)), hili.o);
        alphablit(buf, buf2, Coord.z);
        return (rasterimg(buf));
    }

    public Coord sz() {
        return (imgsz(img));
    }

    public void draw(GOut g) {
        g.image(tex, Coord.z);
    }

    public void draw(GOut g, Coord sz) {
        g.image(tex, Coord.z, sz);
    }

    public String name() {
        return (name);
    }

    public BufferedImage image() {
        return (img);
    }
}