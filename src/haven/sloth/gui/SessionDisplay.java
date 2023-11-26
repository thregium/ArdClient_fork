package haven.sloth.gui;

import haven.Button;
import haven.Coord;
import haven.MainFrame;
import haven.UI;
import haven.Widget;
import haven.sloth.util.ObservableListener;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SessionDisplay extends MovableWidget implements ObservableListener<UI> {
    private static class UIDisplay extends Widget {
        private final UI ui;
        private final Button btn;
        private String nm;

        public UIDisplay(final UI ui) {
            super(Coord.z);
            this.ui = ui;
            this.nm = "Login";
            add(this.btn = new Button(UI.scale(100), nm, this::click));
            add(new Button(0, "X", this::close), new Coord(btn.sz.x, 0));
            pack();
        }

        @Override
        public void tick(double dt) {
            if (nm.equals("Login") && ui.sess != null && ui.sess.alive() && ui.sess.username != null) {
                nm = ui.sess.username;
                btn.change(nm);
            } else if (!nm.equals("Login") && (ui.sess == null || !ui.sess.alive())) {
                nm = "Login";
                btn.change(nm);
            }
        }

        private void click() {
            MainFrame.instance.p.setActiveUI(ui);
        }

        private void close() {
            if (ui.sess != null && ui.sess.alive()) {
                ui.sess.close();
            } else {
                if (!MainFrame.instance.p.isMasterUI(ui)) {
                    MainFrame.instance.p.removeUI(ui);
                }
            }
        }
    }

    private final Map<UI, UIDisplay> uimap = new HashMap<>();
    private final Grouping grp = new GridGrouping("Sessions", UI.scale(5), UI.scale(500));
    private final Button add = new Button(UI.scale(100), "New Session", this::newSession);

    public SessionDisplay() {
        super(Coord.z, "Session Display");
        add(grp);
        grp.add(add);
        MainFrame.instance.p.sessions.addListener(this);
    }

    private void newSession() {
        MainFrame.instance.makeNewSession();
    }

    @Override
    protected void removed() {
        MainFrame.instance.p.sessions.removeListener(this);
    }

    public void init(Collection<UI> base) {
        synchronized (base) {
            for (final UI lui : base) {
                final UIDisplay display = new UIDisplay(lui);
                uimap.put(lui, display);
                grp.add(display);
            }
        }
        add.destroy();
        grp.add(add);
        grp.pack();
        pack();
    }

    public void added(UI item) {
        final UIDisplay display = new UIDisplay(item);
        uimap.put(item, display);
        grp.add(display);
        add.destroy();
        grp.add(add);
        grp.pack();
        pack();
    }

    @Override
    public void edited(UI olditem, UI newitem) {
    }

    public void remove(UI item) {
        final UIDisplay display = uimap.remove(item);
        if (display != null) {
            display.destroy();
            grp.pack();
            pack();
        }
    }

    @Override
    public boolean mousedown(final Coord mc, final int button) {
        if (super.mousedown(mc, button)) {
            //Give preference to the Widget using this
            return (true);
        } else if (altMoveHit(mc, button)) {
            if (!isLock()) {
                movableBg = true;
                dm = ui.grabmouse(this);
                doff = mc;
                parent.setfocus(this);
                raise();
            }
            return (true);
        } else {
            return (false);
        }
    }
}