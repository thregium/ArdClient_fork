import haven.Defer;
import haven.GOut;
import haven.Gob;
import haven.Light;
import haven.Location;
import haven.Message;
import haven.RenderList;
import haven.Rendered;
import haven.Resource;
import haven.Sprite;
import haven.States;
import haven.UI;

import javax.media.opengl.GL2;
import java.awt.Color;

/**
 * Spawns a window stating the angles, closing the window will close this.
 * You'll be able to customize the color in the window
 * <p>
 * TODO: optimize with a FastMesh or something
 * TODO: As the owner gob moves around a1, a2 should update
 * On constructor once we have a1, a2 get our coordinate
 * multiple this coordinate out by 10000? tiles in the direction of each angle to get c1, c2
 * Anytime the owner gob moves update a1, a2 by getting the angles from new coordinate to c1, c2.
 * - not worth the time right now due to the coordinate systems being different..
 */
public class DowseFx extends Sprite {
    public static final double ln = 2.0D;
    public static final double r = 100.0D;
    public final double a1; //Arc is a1 to a2, a1 < a2
    public final double a2;

    private Color col = new Color(255, 0, 0, 128);
    private boolean delete = false;
    private boolean hidden = false;
    private long start = System.currentTimeMillis();

    public DowseFx(Owner owner, Resource res, Message msg) {
        super(owner, res);
        if (msg.eom()) {
            this.a1 = -Math.PI / 8;
            this.a2 = Math.PI / 8;
        } else {
            double d1 = -(msg.uint8() / 256.0D) * Math.PI * 2.0D;
            double d2 = -(msg.uint8() / 256.0D) * Math.PI * 2.0D;
            while (d1 < d2) {
                d1 += 2 * Math.PI;
            }
            this.a1 = d2;
            this.a2 = d1;
        }
        if (owner instanceof Gob) {
            final Gob g = (Gob) owner;
            final UI ui = g.glob.ui.get();
            if (ui != null && ui.gui != null) {
                Defer.later(() ->
                        ui.gui.makeDowseWnd(g.rc, a1, a2, col -> {
                            this.col = col;
                            hidden = false;
                            start = System.currentTimeMillis();
                        }, this::delete)
                );
            }
        }
    }

    public void delete() {
        this.delete = true;
    }

    public void draw(GOut g) {
        if (!hidden) {
            float time = (System.currentTimeMillis() - start) / 1000f;
            if (time >= 10) {
                hidden = true;
                return;
            }
            int alpha = (int) (col.getAlpha() * ((10f - time) / 10f));
            g.state(new States.ColState(new Color(col.getRed(), col.getGreen(), col.getBlue(), alpha)));
            g.apply();
            //render just the arrow 100 units out from us in an arc
            //The color
            g.gl.glBegin(GL2.GL_TRIANGLE_FAN);
            //center point, our gob
            g.gl.glVertex3f(0.0F, 0.0F, 0.0F);
            //Arc edges a1 -> a2
            for (double d1 = this.a1; d1 < this.a2; d1 += Math.PI / 64) {
                g.gl.glVertex3f((float) (Math.cos(d1) * 100.0D), (float) (Math.sin(d1) * 100.0D), 15.0F);
            }
            //final end point
            g.gl.glVertex3f((float) (Math.cos(this.a2) * 100.0D), (float) (Math.sin(this.a2) * 100.0D), 15.0F);
            g.gl.glEnd();
        }
    }

    public boolean setup(RenderList rl) {
        //color vertex with our color
        rl.prepo(States.vertexcolor);
        rl.prepo(States.presdepth);
        //Remove our gob's angle from affecting this, just keep it relative to position
        rl.prepo(Location.goback("gobx"));
        rl.prepo(Rendered.eyesort);
        //Don't apply lighting to us
        rl.state().put(Light.lighting, null);
        return true;
    }

    public boolean tick(int dt) {
        return delete; //don't delete until told
    }
}
