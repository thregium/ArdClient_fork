import haven.BuddyWnd;
import static haven.BuddyWnd.width;
import haven.CharWnd;
import haven.Coord;
import haven.Frame;
import haven.GOut;
import haven.Img;
import haven.Label;
import haven.Polity;
import haven.Tex;
import haven.TexI;
import haven.Text;
import haven.UI;
import haven.Utils;
import haven.Widget;
import haven.res.ui.polity.GroupWidget;
import haven.res.ui.sboost.BoostMeter;
import modification.dev;
import java.awt.Color;

public class Village extends Polity {
    static {
        dev.checkFileVersion("ui/vlg", 36);
    }

    final BuddyWnd.GroupSelector gsel;
    private final int my;
    double power;
    int boost;

    public Village(String name) {
        super("Village", name);
        Widget prev = add(new Img(CharWnd.catf.render("Village").tex()));

        prev = add(new Label(name, nmf), prev.pos("bl").adds(0, 5));
        prev = add(new AuthMeter(new Coord(width, UI.scale(20))), prev.pos("bl").adds(0, 2));
        prev = add(new Widget(new Coord((width / 2) - UI.scale(5), UI.scale(20))) {
            int it;
            Tex rt = null;

            public void draw(GOut g) {
                g.chcolor(0, 0, 0, 255);
                g.frect(Coord.z, sz);
                g.chcolor(128, 0, 0, 255);
                g.frect(new Coord(1, 1), new Coord((int) Math.round(power * (sz.x - 2)), sz.y - 2));
                g.chcolor();
                int ct = (int) Math.round(power * 100);
                if ((rt != null) && (it != ct)) {
                    rt.dispose(); rt = null;
                }
                if (rt == null) {
                    rt = new TexI(Utils.outline2(Text.render(String.format("%d%%", it = ct), Color.WHITE).img, Utils.contrast(Color.WHITE)));
                }
                g.aimage(rt, sz.div(2), 0.5, 0.5);
            }
        }, prev.pos("bl").adds(0, 10));
        add(new BoostMeter(prev.sz) {
            public int level() {return (boost);}
        }, prev.pos("ur").adds(10, 0));
        prev = add(new Label("Groups:"), prev.pos("bl").adds(0, 15));
        gsel = add(new BuddyWnd.GroupSelector(-1) {
            public void tick(double dt) {
                if (mw instanceof GroupWidget)
                    update(((GroupWidget) mw).id);
                else
                    update(-1);
            }

            public void select(int group) {
                Village.this.wdgmsg("gsel", group);
            }
        }, prev.pos("bl").adds(0, 2));
        prev = add(new Label("Members:"), gsel.pos("bl").adds(0, 5));
        prev = add(Frame.with(new MemberList(width, 7), true), prev.pos("bl").adds(0, 2));
        pack();
        this.my = prev.pos("bl").adds(0, 5).y;
    }

    public static Widget mkwidget(UI ui, Object[] args) {
        String name = (String) args[0];
        return (new Village(name));
    }

    public void addchild(Widget child, Object... args) {
        if (args[0] instanceof String) {
            String p = (String) args[0];
            if (p.equals("m")) {
                mw = child;
                add(child, 0, my);
                pack();
                return;
            }
        }
        super.addchild(child, args);
    }

    public void uimsg(String msg, Object... args) {
        if (msg == "ppower") {
            power = ((Number) args[0]).doubleValue() * 0.01;
            boost = ((Number) args[1]).intValue();
        } else {
            super.uimsg(msg, args);
        }
    }
}
