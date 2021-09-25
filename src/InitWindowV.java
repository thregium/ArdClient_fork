/* Preprocessed source code */

import haven.Button;
import haven.Coord;
import haven.GOut;
import haven.Label;
import haven.Resource;
import haven.TextEntry;
import haven.UI;
import haven.Widget;
import haven.Window;

import java.awt.Color;

/* >wdg: InitWindow */
public class InitWindowV extends Window {
    public static final Resource.Image skull = Resource.remote().loadwait("ui/vinit").layer(Resource.imgc);
    public static final Color wait = new Color(128, 128, 255);
    public static final Color ready = new Color(128, 255, 128);
    private int req;
    private boolean master;
    private TextEntry name;
    private Label[] slots;
    private Button form;

    public InitWindowV(int req, boolean master) {
        super(new Coord(200, 200 + (req * 15)), "Village Founding", true, Coord.z, Coord.z);
        this.req = req;
        this.master = master;
        int y = skull.sz.y + 10;
        if (master) {
            add(new Label("Village name:"), 0, y);
            y += 15;
            name = add(new TextEntry(200, ""), 0, y);
            y += 30;
        }
        add(new Label("Founding fathers:"), 0, y);
        y += 20;
        slots = new Label[req];
        for (int i = 0; i < req; i++) {
            slots[i] = add(new Label("Awaiting..."), 15, y);
            slots[i].setcolor(wait);
            y += 15;
        }
        if (master)
            form = add(new Button(200, "Found village"), 0, y);
        pack();
    }

    public static Widget mkwidget(UI ui, Object[] args) {
        int req = (Integer) args[0];
        boolean master = (Integer) args[1] != 0;
        return (new InitWindowV(req, master));
    }

    public void cdraw(GOut g) {
        g.image(skull.tex(), new Coord((g.sz.x - skull.sz.x) / 2, 0));
    }

    public void uimsg(String msg, Object... args) {
        if (msg == "slot") {
            int s = (Integer) args[0];
            if (args.length > 1) {
                slots[s].settext((String) args[1]);
                slots[s].setcolor(ready);
            } else {
                slots[s].settext("Awaiting...");
                slots[s].setcolor(wait);
            }
        }
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender == form) {
            wdgmsg("found", name.text());
            return;
        }
        super.wdgmsg(sender, msg, args);
    }
}
