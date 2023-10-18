package integrations.mapv4;

import haven.Coord;
import haven.FColor;
import haven.GLState;
import haven.Loading;
import haven.MCache;
import haven.MapFile;
import haven.Material;
import haven.PUtils;
import haven.Resource;
import haven.States;
import haven.TexI;
import haven.Tiler;
import haven.Utils;
import haven.resutil.Ridges;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

import static haven.MCache.cmaps;

/**
 * @author APXEOLOG (Artyom Melnikov), at 28.01.2019
 */
public class MinimapImageGenerator {

    private static BufferedImage tileimg(int t, BufferedImage[] texes, MCache map) {
        BufferedImage img = texes[t];
        if (img == null) {
            Resource r = map.tilesetr(t);
            if (r == null)
                return (null);
            Resource.Image ir = r.layer(Resource.imgc);
            if (ir == null)
                return (null);
            img = ir.img;
            texes[t] = img;
        }
        return (img);
    }

    private static Color olcol(MCache.OverlayInfo olid) {
        /* XXX? */
        Material mat = olid.mat();
        FColor bc = null;
        for (GLState state : mat.states) {
            if (state instanceof States.ColState) {
                States.ColState col = (States.ColState) state;
                bc = new FColor(col.c);
                break;
            } else if (state instanceof Material.Colors) {
                Material.Colors col = (Material.Colors) state;
                bc = new FColor(col.emi[0], col.emi[1], col.emi[2]);
                break;
            }
        }
        return (bc != null ? new Color(Math.round(bc.r * 255), Math.round(bc.g * 255), Math.round(bc.b * 255), 255) : (null));
    }

    public static BufferedImage drawoverlay(MCache map, MCache.Grid grid) {
        WritableRaster buf = PUtils.imgraster(cmaps);
        MapFile.Grid g = MapFile.Grid.from(map, grid);
        for (MapFile.Overlay ol : g.ols) {
            MCache.ResOverlay olid = ol.olid.loadsaved().flayer(MCache.ResOverlay.class);
            if (!olid.tags().contains("realm"))
                continue;
            Color col = olcol(olid);
            if (col == null)
                continue;
            Coord c = new Coord();
            for (c.y = 0; c.y < cmaps.y; c.y++) {
                for (c.x = 0; c.x < cmaps.x; c.x++) {
                    if (ol.get(c)) {
                        buf.setSample(c.x, c.y, 0, ((col.getRed() * col.getAlpha()) + (buf.getSample(c.x, c.y, 1) * (255 - col.getAlpha()))) / 255);
                        buf.setSample(c.x, c.y, 1, ((col.getGreen() * col.getAlpha()) + (buf.getSample(c.x, c.y, 1) * (255 - col.getAlpha()))) / 255);
                        buf.setSample(c.x, c.y, 2, ((col.getBlue() * col.getAlpha()) + (buf.getSample(c.x, c.y, 2) * (255 - col.getAlpha()))) / 255);
                        buf.setSample(c.x, c.y, 3, Math.max(buf.getSample(c.x, c.y, 3), col.getAlpha()));
                    }
                }
            }
        }
        return (PUtils.rasterimg(buf));
    }

    public static Loading checkForLoading(MCache map, MCache.Grid grid) {
        Loading error = null;
        Coord c = new Coord();
        for (c.y = 0; c.y < MCache.cmaps.y; c.y++) {
            for (c.x = 0; c.x < MCache.cmaps.x; c.x++) {
                try {
                    grid.gettile(c);
                } catch (Loading l) {
                    error = l;
                }
            }
        }
        return (error);
    }

    public static BufferedImage drawmap(MCache map, MCache.Grid grid) {
        BufferedImage[] texes = new BufferedImage[map.tiles.length];
        BufferedImage buf = TexI.mkbuf(MCache.cmaps);
        Coord c = new Coord();
        for (c.y = 0; c.y < MCache.cmaps.y; c.y++) {
            for (c.x = 0; c.x < MCache.cmaps.x; c.x++) {
                BufferedImage tex = tileimg(grid.gettile(c), texes, map);
                int rgb = 0;
                if (tex != null)
                    rgb = tex.getRGB(Utils.floormod(c.x, tex.getWidth()),
                            Utils.floormod(c.y, tex.getHeight()));
                buf.setRGB(c.x, c.y, rgb);
            }
        }
        for (c.y = 0; c.y < MCache.cmaps.y; c.y++) {
            for (c.x = 0; c.x < MCache.cmaps.x; c.x++) {
                int t = grid.gettile(c);
                Tiler tl = map.tiler(t);
                if (tl instanceof Ridges.RidgeTile) {
                    if (Ridges.brokenp(map, c, grid)) {
                        buf.setRGB(c.x, c.y, Color.BLACK.getRGB());
                        for (int y = Math.max(c.y - 1, 0); y <= Math.min(c.y + 1, cmaps.y - 1); y++) {
                            for (int x = Math.max(c.x - 1, 0); x <= Math.min(c.x + 1, cmaps.x - 1); x++) {
                                if (x == c.x && y == c.y)
                                    continue;
                                Color cc = new Color(buf.getRGB(x, y));
                                buf.setRGB(x, y, Utils.blendcol(cc, Color.BLACK, ((x == c.x) && (y == c.y)) ? 1 : 0.1).getRGB());
                            }
                        }
                    }
                }
            }
        }
        for (c.y = 0; c.y < MCache.cmaps.y; c.y++) {
            for (c.x = 0; c.x < MCache.cmaps.x; c.x++) {
                int t = grid.gettile(c);
                for (Coord ec : new Coord[]{new Coord(-1, 0), new Coord(1, 0), new Coord(0, -1), new Coord(0, 1)}) {
                    Coord coord = c.add(ec);
                    if (coord.x < 0 || coord.x > MCache.cmaps.x - 1 || coord.y < 0 || coord.y > MCache.cmaps.y - 1)
                        continue;
                    if (grid.gettile(coord) > t) {
                        buf.setRGB(c.x, c.y, Color.BLACK.getRGB());
                        break;
                    }
                }
            }
        }
        return buf;
    }
}
