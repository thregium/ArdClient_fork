package haven.res.gfx.fx.msrad;

import haven.GLState;
import haven.Message;
import haven.RenderList;
import haven.Resource;
import haven.States;
import haven.Utils;
import haven.res.gfx.fx.bprad.BPRad;
import java.awt.Color;

public class MSRad extends BPRad {
    public static Color getColor1() {
        return (new Color(Utils.getprefi("minesupportcolor1", new Color(192, 0, 0, 128).getRGB()), true));
    }

    public static Color getColor2() {
        return (new Color(Utils.getprefi("minesupportcolor2", new Color(255, 224, 96).getRGB()), true));
    }

    static GLState smat = new States.ColState(getColor1());
    static GLState emat = new States.ColState(getColor2());

    public static boolean show = false;

    public MSRad(Owner owner, Resource res, Message sdt) {
        super(owner, res, Utils.hfdec((short)sdt.int16()) * 11);
    }

    public boolean setup(RenderList rl) {
        if(!show)
            return(false);
        return(super.setup(rl));
    }

    @Override
    public GLState emat() {
        return (emat);
    }

    @Override
    public GLState smat() {
        return (smat);
    }

    public static void changeColor1(Color color) {
        Utils.setprefi("minesupportcolor1", color.getRGB());
        smat = new States.ColState(color);
    }
    public static void changeColor2(Color color) {
        Utils.setprefi("minesupportcolor2", color.getRGB());
        emat = new States.ColState(color);
    }
}
