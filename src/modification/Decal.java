package modification;

import haven.Config;
import haven.Coord3f;
import haven.FastMesh;
import haven.GLState;
import haven.Gob;
import haven.Location;
import haven.MCache;
import haven.Material;
import haven.Message;
import haven.RenderList;
import haven.Rendered;
import haven.Resource;
import haven.Skeleton;
import haven.Sprite;
import haven.States;
import haven.StaticSprite;
import haven.TexL;
import haven.Utils;

import java.awt.image.BufferedImage;

public class Decal implements Sprite.Factory {
    public Sprite create(Sprite.Owner owner, Resource res, Message sdt) {
        GLState eq = null;
        if (owner instanceof Gob) {
            Gob gob = (Gob) owner;
            Resource ores = gob.getres();
            if (ores != null) {
                Skeleton.BoneOffset bo = ores.layer(Skeleton.BoneOffset.class, "decal");
                if (bo != null)
                    eq = bo.forpose(Skeleton.getpose(gob));
            }
        }
        Material base = res.layer(Material.Res.class, 16).get();
        FastMesh proj = res.layer(FastMesh.MeshRes.class, 0).m;
        Coord3f pc;
        if (sdt.eom()) {
            pc = Coord3f.o;
        } else {
            pc = new Coord3f((float) (sdt.float16() * MCache.tilesz.x), -(float) (sdt.float16() * MCache.tilesz.y), 0);
        }
        if (owner.getres() != null && owner.getres().toString().contains("gfx/terobjs/cupboard") && Config.flatcupboards)
            pc = Coord3f.of(0, 0, 1);
        Location offset = null;
        if (eq == null)
            offset = Location.xlate(pc);
        Material sym = null;
        if (!sdt.eom()) {
            BufferedImage img = ItemTex.create(owner, sdt);
            if (img != null) {
                TexL tex = ItemTex.fixup(img);
                sym = new Material(base, tex.draw, tex.clip);
            }
        }
        Rendered[] parts = StaticSprite.lsparts(res, Message.nil);
        if (sym != null)
            parts = Utils.extend(parts, sym.apply(proj));
        Location cpoffset = offset;
        GLState cpeq = eq;
        return (new StaticSprite(owner, res, parts) {
            GLState normal = cpeq != null ? cpeq : cpoffset;
            GLState xray = GLState.compose(normal, States.xray);

            @Override
            public boolean setup(RenderList rl) {
                for (int i = 0; i < parts.length; i++) {
                    rl.add(parts[i], i == parts.length - 1 ? xray : normal);
                }
                return (false);
            }
        });
    }
}
