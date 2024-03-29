package haven.res.gfx.fx.floatimg;

import haven.Coord;
import haven.GOut;
import haven.Gob;
import haven.PView;
import haven.RenderList;
import haven.Resource;
import haven.Sprite;
import haven.Tex;
import haven.UI;
import haven.Utils;
import haven.sloth.gfx.GobCombatSprite;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FloatSprite extends Sprite implements PView.Render2D {
    public static int OY = UI.scale(Utils.getprefi("combatdamageheight", 30));
    public static final int DY = UI.scale(10);
    public final int ms; //How long we last for
    public Tex tex; //Our texture
    int sy = 0; //Our location above the player
    int offset = 92;
    int sx;
    double a = 0.0; //How long its been.
    private static final Map<Owner, FloatSprite> OFFSET = Collections.synchronizedMap(new HashMap<>());

    public int cury() {
        return (sy + (int) ((OY - sy) * a));
    }

    public FloatSprite(Owner owner, Resource res, Tex tex, int time, int sx) {
        super(owner, res);
        this.tex = tex;
        this.ms = time;
        this.sx = sx;
        this.sy = Math.min(init(owner), UI.scale(30));
    }

    public FloatSprite(Owner owner, Resource res, Tex tex, int time) {
        this(owner, res, tex, time, 0);
    }

    FloatSprite(Owner owner, Resource res) {
        super(owner, res);
        this.ms = -1;
        this.sy = 0;
    }

    public void updateTex(final Tex tex) {
        this.tex = tex;
    }

    public void draw2d(GOut g) {
        if (tex != null) {
            final Gob gob = (Gob) owner;
            if (gob.sc == null) {
                return;
            }
            Coord sc = gob.sc.add(new Coord(gob.sczu.mul(15))).sub(0, DY);
//            Coord c = new Coord(sc.add(sczu.mul(16))).sub(0, DY);
            int i;
            if (this.a < 0.75) {
                i = 255;
            } else {
                i = (int) Utils.clip(255.0 * ((1.0 - this.a) / 0.25), 0.0, 255.0);
            }
            g.chcolor(255, 255, 255, i);
            g.aimage(this.tex, sc.add(sx, -cury()), 0.5, 1.0);
            g.chcolor();
        }
    }

    public boolean setup(RenderList rl) {
        rl.prepo(last);
        return true;
    }

    @Override
    public boolean tick(int dt) {
        if (owner instanceof Gob) {
            final Gob gob = (Gob) owner;
            if (gob.findol(GobCombatSprite.id) != null) {
                offset = 92;
            } else {
                offset = 52;
            }
        }

        if (ms > 0) {
            a += dt / (double) this.ms;
            return a >= 1.0; //Once we're over 1.0 delete us
        } else {
            return false;
        }
    }

    private int init(Owner owner) {
        int ret = DY;
        synchronized (OFFSET) {
            FloatSprite value = OFFSET.get(owner);
            if (value != null) {
                ret = value.cury() - tex.sz().y;
            }
            OFFSET.put(owner, this);
        }
        return (ret);
    }

    @Override
    public void dispose() {
        super.dispose();
        synchronized (OFFSET) {
            FloatSprite value = OFFSET.get(owner);
            if (value == this) {
                OFFSET.remove(owner);
            }
        }
    }
}
