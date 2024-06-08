package haven.sloth.gob;

import haven.GLState;
import haven.Gob;
import haven.Material;
import haven.Message;
import haven.RenderList;
import haven.Resource;
import haven.SkelSprite;
import haven.Utils;

import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Mark extends SkelSprite implements Gob.Overlay.SetupMod {
    public static final String CHAT_FMT = "$Mark{%d,%d}";
    public static final String CHAT_TILE_FMT = "$MarkTile{%d,%s,%s}";
    public static final Pattern CHAT_FMT_PAT = Pattern.compile("\\$Mark\\{([0-9]+),([0-9]+)}");
    public static final Pattern CHAT_TILE_FMT_PAT = Pattern.compile("\\$MarkTile\\{(-?[0-9]+),([0-9]+\\.[0-9]+),([0-9]+\\.[0-9]+)}");
    private static final Resource tgtfx = Resource.local().loadwait("custom/fx/partytgt");
    public static final int id = -24441;
    private float[] emi = {1.0f, 0.0f, 1.0f, 0.0f};
    private float[] clr = Utils.c2fa(new Color(255, 0, 255, 0));
    private int life;
    private boolean haslife;
    private long time;

    public Mark(final int life) {
        super(null, tgtfx, Message.nil);
        this.life = life;
        haslife = life != -1;
    }

    private static final Map<Double, GLState> stateMap = Collections.synchronizedMap(new HashMap<>());

    private GLState getfx() {
        return (stateMap.computeIfAbsent(Math.sin(2 * Math.PI * time / 3000f), sin -> {
            float f = (float) (1 + sin);
            float[] emi = {this.emi[0], this.emi[1], this.emi[2], f};
            float[] clr = {this.clr[0], this.clr[1], this.clr[2], f};
            return (new Material.Colors(clr, clr, clr, emi, 128));
        }));
    }

    public void setLife(final int life) {
        if (haslife)
            this.life = life;
        if (life == -1)
            haslife = false;
    }

    public void revoke() {
        haslife = true;
        life = 0;
    }

    public boolean tick(int dt) {
        super.tick(dt);
        time += dt;
        if (haslife) {
            life -= dt;
            return life <= 0;
        } else {
            return false;
        }
    }

    @Override
    public void setupgob(GLState.Buffer buf) {}

    @Override
    public void setupmain(RenderList rl) {
        rl.prepc(getfx());
    }
}
