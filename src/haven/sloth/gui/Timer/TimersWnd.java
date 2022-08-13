package haven.sloth.gui.Timer;


import haven.Button;
import haven.Coord;
import haven.GOut;
import haven.Scrollport;
import haven.Text;
import haven.Widget;
import haven.Window;
import haven.sloth.io.TimerData;
import haven.sloth.util.ObservableListener;

import java.util.Collection;

public class TimersWnd extends Window implements ObservableListener<TimerData.Timer> {
    public static final int width = 460;
    public final Scrollport port;

    public TimersWnd() {
        super(Coord.z, "Timers", "Timers");

        Button btna = new Button(100, "Add", () ->
                ui.gui.add(new TimerEditWnd("Create New Timer"),
                        new Coord(ui.gui.sz.x / 2 - 200, ui.gui.sz.y / 2 - 200)));
        add(btna, new Coord(0, 10));
//        Button btnl = new Button(100, "Load") {
//            public void click() {
//                TimerData.oldload();
//                ui.destroy(this);
//            }
//
//            public Object tooltip(Coord c, Widget prev) {
//                return Text.render("Load old amber timers and delete them").tex();
//            }
//        };
//        add(btnl, new Coord(btna.c.x + btna.sz.x + 10, 10));

        port = new Scrollport(new Coord(width - 20 - 15, 0), 30) {
            @Override
            public void draw(GOut g) {
                g.chcolor(0, 0, 0, 128);
                g.frect(Coord.z, sz);
                g.chcolor();
                super.draw(g);
            }
        };
        add(port, btna.c.add(0, btna.sz.y + 10));

        TimerData.listenTimers(this);
        pack();
    }

    @Override
    public void init(Collection<TimerData.Timer> base) {
        for (final TimerData.Timer timer : base) {
            port.cont.add(new TimerWdg(timer));
        }
        pack();
    }

    @Override
    public void added(TimerData.Timer item) {
        port.cont.add(new TimerWdg(item));
        pack();
    }

    @Override
    public void edited(TimerData.Timer olditem, TimerData.Timer newitem) {
        TimerWdg timerWdg = find(olditem);
        if (timerWdg != null) {
            timerWdg.time = newitem;
            timerWdg.time.instances = olditem.instances;
            timerWdg.update();
        }
        pack();
    }

    @Override
    public void remove(TimerData.Timer item) {
        ui.destroy(find(item));
        pack();
    }

    public TimerWdg find(TimerData.Timer t) {
        for (Widget wdg = port.cont.child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof TimerWdg) {
                TimerWdg tw = (TimerWdg) wdg;
                if (tw.time == t)
                    return tw;
            }
        }

        return null;
    }

    public void pack() {
        int y = 0;
        for (Widget wdg = port.cont.child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof TimerWdg) {
                wdg.c = new Coord(wdg.c.x, y);
                y += wdg.sz.y;
            }
        }
        int portHeight = Math.min(y, 400);
        port.resize(port.sz.x, portHeight);
        port.cont.update();
        port.bar.resize(portHeight);
        port.bar.changed();
        super.pack();
    }

    public void close() {
        hide();
    }

    @Override
    protected void removed() {
        TimerData.removeTimerListener(this);
    }
}
