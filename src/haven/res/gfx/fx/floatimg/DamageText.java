package haven.res.gfx.fx.floatimg;

import haven.Coord;
import haven.GOut;
import haven.Gob;
import haven.Resource;
import haven.TexI;
import haven.Text;
import haven.UI;
import haven.Utils;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class DamageText extends FloatSprite {
    public static final int id = -14115;
    public static final Text.Foundry fnd = new Text.Foundry(Text.sans, UI.scale(13));
    private static final Color armorcol = new Color(136, 255, 136);
    private static final Color hhpcol = new Color(255, 204, 0);
    private static final Color shpcol = new Color(255, 0, 0);

    public static class Numbers {
        private int shp;
        private int hhp;
        private int armor;

        public Numbers(int shp, int hhp, int armor) {
            this.shp = shp;
            this.hhp = hhp;
            this.armor = armor;
        }

        public void update(int shp, int hhp, int armor) {
            this.shp = shp;
            this.hhp = hhp;
            this.armor = armor;
        }

        public void update(Numbers numbers) {
            this.shp = numbers.shp;
            this.hhp = numbers.hhp;
            this.armor = numbers.armor;
        }
    }

    private Numbers numbers;

    DamageText(Owner owner, Resource res) {
        super(owner, res);
        numbers = new Numbers(0, 0, 0);
    }

    public DamageText(Owner owner, Resource res, Numbers numbers) {
        super(owner, res);
        this.numbers = numbers;
    }

    public void remake() {
        if (owner instanceof Gob) {
            final Gob gob = (Gob) owner;
            owner.glob().gobmap.computeIfAbsent(gob.id, id -> numbers).update(numbers);
        }
        int[] ind = new int[]{numbers.shp, numbers.hhp, numbers.armor};
        Color[] cind = new Color[]{shpcol, hhpcol, armorcol};

        BufferedImage img = null;
        for (int i = 0; i < ind.length; i++) {
            if (ind[i] != 0) {
                if (img != null) {
                    img = Utils.hconcat(img, fnd.render(" " + ind[i], cind[i]).img);
                } else {
                    img = Utils.hconcat(fnd.render(ind[i] + "", cind[i]).img);
                }
            }
        }

        if (img != null) {
            updateTex(new TexI(Utils.outline2(img, Color.BLACK)));
        }
    }

    void incshp(final int shp) {
        numbers.shp += shp;
        remake();
    }

    void inchhp(final int hhp) {
        numbers.hhp += hhp;
        remake();
    }

    void incarmor(final int armor) {
        numbers.armor += armor;
        remake();
    }

    public boolean tick(int dt) {
        //Never delete us
        return false;
    }

    public void draw2d(GOut g) {
        if (tex != null) {
            final Gob gob = (Gob) owner;
            if (gob.sc == null) {
                return;
            }
            Coord sc = gob.sc.add(new Coord(gob.sczu.mul(15))).sub(0, offset);

            g.chcolor(35, 35, 35, 192);
            g.frect(sc.sub(tex.sz().x / 2, 0), tex.sz());
            g.chcolor();
        }
        super.draw2d(g);
    }
}
