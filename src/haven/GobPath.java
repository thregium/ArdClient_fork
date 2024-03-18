package haven;

import haven.res.ui.obj.buddy.Buddy;
import haven.sloth.gob.Movable;
import haven.sloth.gob.Type;
import modification.configuration;

import javax.media.opengl.GL;
import java.util.Iterator;

public class GobPath extends Sprite {
    public final States.ColState clrst;

    public GobPath(Gob gob) {
        super(gob, null);

        if (gob.isplayer()) {
            clrst = Movable.playercol;
        } else if (gob.type == Type.VEHICLE || gob.type == Type.WATERVEHICLE) {
            clrst = Movable.vehiclepathcol;
        } else if (gob.type == Type.ANIMAL || gob.type == Type.SMALLANIMAL || gob.type == Type.TAMEDANIMAL || gob.type == Type.DANGANIMAL) {
            clrst = Movable.animalpathcol;
        } else {
            //Humans, based off kin
            final Buddy buddy = gob.getattr(Buddy.class);
            if (buddy != null) {
                clrst = Movable.buddycol[buddy.group()];
            } else {
                clrst = Movable.unknowngobcol;
            }
        }
    }

    public boolean setup(RenderList rl) {
        Location.goback(rl.state(), "gobx");
        rl.prepo(States.xray);
        rl.prepo(clrst);
        return (true);
    }

    public void draw(GOut g) {
        Gob gob = (Gob) owner;
        UI ui = gob.glob.ui.get();
        if (ui != null) {
            MapView mv = ui.gui.map;
            g.apply();
            BGL gl = g.gl;
            gl.glLineWidth(2F);
            gl.glEnable(GL.GL_BLEND);
            gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
            gl.glEnable(GL.GL_LINE_SMOOTH);
            gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
            gl.glBegin(GL.GL_LINES);

            if (drawLinMove(gl, mv, gob) && gob.isplayer())
                drawLastMC(gl, mv, gob);
            if (gob.isplayer())
                drawMoveQueue(gl, mv, gob);

            gl.glEnd();
            gl.glDisable(GL.GL_LINE_SMOOTH);
        }
    }

    void drawMoveQueue(BGL gl, MapView mv, Gob gob) {
        final Coord2d movingto = mv.movingto();
        if (movingto != null) {
            final Coord3f pc = gob.getc();
            final Iterator<Coord2d> queue = mv.movequeue();
            double x = movingto.x - pc.x;
            double y = -(movingto.y - pc.y);
            double z = Config.disableelev || Math.sqrt(x * x + y * y) >= 44 * 11 ? 0 : gob.glob.map.getcz(movingto) - pc.z;

            gl.glVertex3f(0, 0, 0);
            gl.glVertex3f((float) x, (float) y, (float) z);
            Coord3f last = Coord3f.of((float) x, (float) y, (float) z);

            while (queue.hasNext()) {
                Coord2d next = queue.next();
                x = next.x - pc.x;
                y = -(next.y - pc.y);
                z = Config.disableelev || Math.sqrt(x * x + y * y) >= 44 * 11 ? 0 : gob.glob.map.getcz(next) - pc.z;

                gl.glVertex3f(last.x, last.y, last.z);
                gl.glVertex3f((float) x, (float) y, (float) z);

                last = Coord3f.of((float) x, (float) y, (float) z);
            }
        }
    }

    void drawLastMC(BGL gl, MapView mv, Gob gob) {
        Coord2d mc = mv.pllastcc;
        if (mc != null) {
            final Coord3f pc = gob.getc();
            double x = mc.x - pc.x;
            double y = -(mc.y - pc.y);
            double z = Config.disableelev || Math.sqrt(x * x + y * y) >= 44 * 11 ? 0 : gob.glob.map.getcz(mc.x, mc.y) - pc.z;

            gl.glVertex3f(0, 0, 0);
            gl.glVertex3f((float) x, (float) y, (float) z);
        }
    }

    boolean drawLinMove(BGL gl, MapView mv, Gob gob) {
        Moving lm = gob.getattr(Moving.class);
        if (lm != null && lm.getDest().isPresent()) {
            if (!(lm instanceof LinMove && !configuration.showlinmove)) {
                final Coord3f pc = gob.getc();
                final Coord2d coord = lm.getDest().orElse(Coord2d.of(pc));
                double x = coord.x - pc.x;
                double y = -(coord.y - pc.y);
                double z = Config.disableelev || Math.sqrt(x * x + y * y) >= 44 * 11 ? 0 : gob.glob.map.getcz(coord.x, coord.y) - pc.z;

                gl.glVertex3f(0, 0, 0);
                gl.glVertex3f((float) x, (float) y, (float) z);
            }
        }
        return (lm == null || lm instanceof LinMove);
    }
}
