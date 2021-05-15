package haven.sloth.gob;

import haven.GLState;
import haven.Gob;
import haven.Material;
import haven.Message;
import haven.RenderList;
import haven.Resource;
import haven.SkelSprite;

import java.awt.Color;

public class AggroMark extends SkelSprite implements Gob.Overlay.SetupMod {
    final GLState col = new Material.Colors(Color.RED);
    private static final Resource tgtfx = Resource.local().loadwait("custom/fx/partytgt");
    public static final int id = -4214129;

    private boolean alive;
    private boolean current = false;
    private int life;
    private long time;

    public AggroMark(final int life) {
        super(null, tgtfx, Message.nil);
        this.life = life;
        alive = life != -1;
    }

    public void setLife(final int life) {
        if (alive)
            this.life = life;
        if (life == -1)
            alive = false;
    }

    public void revoke() {
        alive = true;
        life = 0;
    }


    public void rem() {
        alive = false;
    }

    public boolean tick(int dt) {
        super.tick(dt);
        time += dt;
        if (alive) {
            life -= dt;
            return life <= 0;
        } else {
            return false;
        }
    }

    @Override
    public void setupgob(GLState.Buffer buf) {
    }

    @Override
    public void setupmain(RenderList rl) {
        rl.prepc(col);
    }
}
