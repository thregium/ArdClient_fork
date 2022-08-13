package haven.sloth.gui.Timer;


import haven.Audio;
import haven.Button;
import haven.Coord;
import haven.DefSettings;
import haven.FastText;
import haven.GOut;
import haven.Glob;
import haven.Label;
import haven.Resource;
import haven.Widget;
import haven.Window;
import haven.sloth.io.TimerData;
import haven.sloth.util.ObservableListener;

import java.util.Collection;

public class TimerWdg extends Widget implements ObservableListener<TimerData.TimerInstance> {
    private static final Resource timersfx = Resource.local().loadwait("custom/sfx/timer");
    private final static int width = 420;
    private final static int height = 31;
    private final static int txty = 8;
    private final static Coord timec = new Coord(210, txty);

    private class TimerInstWdg extends Widget {
        final TimerData.TimerInstance inst;
        private long elapsed = (long) (time.duration * getGlob().getTimeFac());

        private TimerInstWdg(final TimerData.TimerInstance inst) {
            this.inst = inst;
            adda(new Button(50, "Cancel", this::cancel), new Coord(width - 5, 3), 1, 0);
            pack();
        }

        @Override
        public void draw(GOut g) {
            FastText.aprint(g, timec, 0.5, 0, timeFormat((long) (elapsed / ui.sess.glob.getTimeFac())) + " (" + timeFormat(elapsed) + ")");
            super.draw(g);
        }

        @Override
        public void tick(double dt) {
            elapsed = (long) (time.duration / 3 * ui.sess.glob.getTimeFac()) - (long) ((ui.sess.glob.currenttime() - inst.start));
            if (elapsed <= 0) {
                ui.gui.add(new TimerDoneWindow(time.name), new Coord(50, 50));
                Audio.play(timersfx, DefSettings.TIMERVOLUME.get() / 1000f);
                time.finish(inst);
            }
            super.tick(dt);
        }

        private void cancel() {
            time.finish(inst);
        }
    }

    public TimerData.Timer time;
    private Glob glob;
    private int base_height;

    public Button X, editbtn, startbtn;
    public Label namelbl, timelbl;

    TimerWdg(TimerData.Timer t, Glob glob) {
        time = t;
        this.glob = glob;
        sz = new Coord(width, height);
        namelbl = new Label(time.name);
        adda(namelbl, new Coord(3, sz.y / 2), 0, 0.5);
        X = new Button(20, "X", this::delete);
        adda(X, new Coord(sz.x - 5, sz.y / 2), 1, 0.5);
        editbtn = new Button(50, "Edit", this::edit);
        adda(editbtn, new Coord(X.c.x - 5, sz.y / 2), 1, 0.5);
        startbtn = new Button(50, "Start", this::start);
        adda(startbtn, new Coord(editbtn.c.x - 5, sz.y / 2), 1, 0.5);
        timelbl = new Label(timeFormat(time.duration / 3) + " (" + timeFormat((long) (time.duration / 3 * getGlob().getTimeFac())) + ")");
        adda(timelbl, new Coord(startbtn.c.x - 15, sz.y / 2), 1, 0.5);

        pack();
        base_height = sz.y;
    }

    private Glob getGlob() {
        return (glob);
    }

    @Override
    protected void added() {
        time.listen(this);
    }

    private static String timeFormat(long ts) {
        return String.format("%02d:%02d.%02d", (int) (ts / 3600), (int) ((ts % 3600) / 60), (int) (ts % 60));
    }

    public void start() {
        time.makeInstance((long) ui.sess.glob.currenttime());
    }

    public void edit() {
        ui.gui.add(new TimerEditWnd("Edit Timer", time), new Coord(ui.gui.sz.x / 2 - 200, ui.gui.sz.y / 2 - 200));
    }

    public void update() {
        namelbl.settext(time.name);
        timelbl.settext(timeFormat(time.duration / 3) + " (" + timeFormat((long) (time.duration / 3 * ui.sess.glob.getTimeFac())) + ")");
        timelbl.move(new Coord(startbtn.c.x - 15, timelbl.c.y), 1, 0);
    }

    public void delete() {
        TimerData.remTimer(time);
    }

    @Override
    public void init(Collection<TimerData.TimerInstance> base) {
        for (final TimerData.TimerInstance inst : base) {
            add(new TimerInstWdg(inst));
        }
        pack();
        getparent(TimersWnd.class).pack();
    }

    @Override
    public void added(TimerData.TimerInstance item) {
        add(new TimerInstWdg(item));
        pack();
        getparent(TimersWnd.class).pack();
    }

    @Override
    public void edited(TimerData.TimerInstance olditem, TimerData.TimerInstance newitem) {
    }

    @Override
    public void remove(TimerData.TimerInstance item) {
        ui.destroy(find(item));
        pack();
        getparent(TimersWnd.class).pack();
    }

    public void pack() {
        int y = base_height;

        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof TimerInstWdg) {
                wdg.c = new Coord(wdg.c.x, y);
                y += wdg.sz.y;
            }
        }
        super.pack();
    }

    public TimerInstWdg find(TimerData.TimerInstance t) {
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof TimerInstWdg) {
                TimerInstWdg tw = (TimerInstWdg) wdg;
                if (tw.inst == t)
                    return tw;
            }
        }

        return null;
    }

    @Override
    protected void removed() {
        time.unlisten(this);
    }

    private class TimerDoneWindow extends Window {
        private TimerDoneWindow(String timername) {
            super(new Coord(300, 130), "Hooray!", "Hooray!");

            Label lbltimer = new Label(timername);
            add(lbltimer, new Coord(300 / 2 - lbltimer.sz.x / 2, 20));

            Label lblinf = new Label("has finished running");
            add(lblinf, new Coord(300 / 2 - lblinf.sz.x / 2, 50));

            adda(new Button(60, "Close") {
                @Override
                public void click() {
                    parent.reqdestroy();
                }
            }, new Coord(sz.x / 4, 90), 0.5, 0);
            adda(new Button(60, "Close & Restart") {
                @Override
                public void click() {
                    TimerWdg.this.start();
                    parent.reqdestroy();
                }
            }, new Coord(sz.x / 4 * 3, 90), 0.5, 0);
        }

        public void close() {
            ui.destroy(this);
        }
    }
}
