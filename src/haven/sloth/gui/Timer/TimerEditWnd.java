package haven.sloth.gui.Timer;


import haven.Button;
import haven.CheckBox;
import haven.Coord;
import haven.Label;
import haven.TextEntry;
import haven.UI;
import haven.Window;
import haven.sloth.io.TimerData;

import java.awt.event.KeyEvent;

public class TimerEditWnd extends Window {
    TimerEditWnd(String cap) {
        super(UI.scale(355, 100), cap, cap);

        add(new Label("Name"), UI.scale(15, 10));
        final TextEntry txtname = new TextEntry(UI.scale(200), "");
        add(txtname, UI.scale(15, 30));

        add(new Label("HH"), UI.scale(225, 10));
        final TextEntry txthours = new TextEntry(UI.scale(35), "") {
            @Override
            public boolean keydown(KeyEvent ev) {
                final char c = ev.getKeyChar();
                if (c == 0x8 || c == 0x7f || c == 0x09 || (c >= 0x30 && c <= 0x39 && text().length() <= 2))
                    return super.keydown(ev);
                return true;
            }
        };
        add(txthours, UI.scale(225, 30));

        add(new Label("MM"), UI.scale(265, 10));
        final TextEntry txtminutes = new TextEntry(UI.scale(35), "") {
            @Override
            public boolean keydown(KeyEvent ev) {
                final char c = ev.getKeyChar();
                if (c == 0x8 || c == 0x7f || c == 0x09 || (c >= 0x30 && c <= 0x39 && text().length() <= 1))
                    return super.keydown(ev);
                return true;
            }
        };
        add(txtminutes, UI.scale(265, 30));

        add(new Label("SS"), UI.scale(305, 10));
        final TextEntry txtseconds = new TextEntry(UI.scale(35), "") {
            @Override
            public boolean keydown(KeyEvent ev) {
                final char c = ev.getKeyChar();
                if (c == 0x8 || c == 0x7f || c == 0x09 || (c >= 0x30 && c <= 0x39 && text().length() <= 1))
                    return super.keydown(ev);
                return true;
            }
        };
        add(txtseconds, UI.scale(305, 30));

        CheckBox realtime = new CheckBox("Real time") {
            public void set(boolean val) {
                long hours = Long.parseLong(txthours.text().equals("") ? "0" : txthours.text());
                long minutes = Long.parseLong(txtminutes.text().equals("") ? "0" : txtminutes.text());
                long seconds = Long.parseLong(txtseconds.text().equals("") ? "0" : txtseconds.text());
                long duration = ((60 * hours + minutes) * 60 + seconds);
                duration = val ? Math.round(duration / ui.sess.glob.getTimeFac()) : Math.round(duration * ui.sess.glob.getTimeFac());
                int h = (int) (duration / 3600);
                int m = (int) ((duration % 3600) / 60);
                int s = (int) (duration % 60);
                txthours.settext(h == 0 ? "" : h + "");
                txtminutes.settext(m == 0 ? "" : m + "");
                txtseconds.settext(s == 0 ? "" : s + "");

                a = val;
            }
        };
        adda(realtime, new Coord(sz.x / 2, UI.scale(70)), 0.5, 0);

        Button add = new Button(UI.scale(60), "Add", () -> {
            try {
                long hours = Long.parseLong(txthours.text().equals("") ? "0" : txthours.text());
                long minutes = Long.parseLong(txtminutes.text().equals("") ? "0" : txtminutes.text());
                long seconds = Long.parseLong(txtseconds.text().equals("") ? "0" : txtseconds.text());
                long duration = ((60 * hours + minutes) * 60 + seconds) * 3;
                if (realtime.a) duration = Math.round(duration * ui.sess.glob.getTimeFac());
                TimerData.addTimer(txtname.text(), duration);
                ui.destroy(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        add(add, UI.scale(15, 70));

        Button cancel = new Button(UI.scale(60), "Cancel") {
            @Override
            public void click() {
                parent.reqdestroy();
            }
        };
        add(cancel, UI.scale(275, 70));
    }

    TimerEditWnd(String cap, TimerData.Timer timer) {
        super(UI.scale(355, 100), cap, cap);

        add(new Label("Name"), UI.scale(15, 10));
        final TextEntry txtname = new TextEntry(UI.scale(200), timer.name);
        add(txtname, UI.scale(15, 30));

        long ts = timer.duration / 3;

        add(new Label("HH"), UI.scale(225, 10));
        final TextEntry txthours = new TextEntry(UI.scale(35), (int) (ts / 3600) == 0 ? "" : (int) (ts / 3600) + "") {
            @Override
            public boolean keydown(KeyEvent ev) {
                final char c = ev.getKeyChar();
                if (c == 0x8 || c == 0x7f || c == 0x09 || (c >= 0x30 && c <= 0x39 && text().length() <= 2))
                    return super.keydown(ev);
                return true;
            }
        };
        add(txthours, UI.scale(225, 30));

        add(new Label("MM"), UI.scale(265, 10));
        final TextEntry txtminutes = new TextEntry(UI.scale(35), (int) ((ts % 3600) / 60) == 0 ? "" : (int) ((ts % 3600) / 60) + "") {
            @Override
            public boolean keydown(KeyEvent ev) {
                final char c = ev.getKeyChar();
                if (c == 0x8 || c == 0x7f || c == 0x09 || (c >= 0x30 && c <= 0x39 && text().length() <= 1))
                    return super.keydown(ev);
                return true;
            }
        };
        add(txtminutes, UI.scale(265, 30));

        add(new Label("SS"), UI.scale(305, 10));
        final TextEntry txtseconds = new TextEntry(UI.scale(35), (int) (ts % 60) == 0 ? "" : (int) (ts % 60) + "") {
            @Override
            public boolean keydown(KeyEvent ev) {
                final char c = ev.getKeyChar();
                if (c == 0x8 || c == 0x7f || c == 0x09 || (c >= 0x30 && c <= 0x39 && text().length() <= 1))
                    return super.keydown(ev);
                return true;
            }
        };
        add(txtseconds, UI.scale(305, 30));

        CheckBox realtime = new CheckBox("Real time") {
            public void set(boolean val) {
                long hours = Long.parseLong(txthours.text().equals("") ? "0" : txthours.text());
                long minutes = Long.parseLong(txtminutes.text().equals("") ? "0" : txtminutes.text());
                long seconds = Long.parseLong(txtseconds.text().equals("") ? "0" : txtseconds.text());
                long duration = (60 * hours + minutes) * 60 + seconds;
                duration = val ? Math.round(duration / ui.sess.glob.getTimeFac()) : Math.round(duration * ui.sess.glob.getTimeFac());
                int h = (int) (duration / 3600);
                int m = (int) ((duration % 3600) / 60);
                int s = (int) (duration % 60);
                txthours.settext(h == 0 ? "" : h + "");
                txtminutes.settext(m == 0 ? "" : m + "");
                txtseconds.settext(s == 0 ? "" : s + "");

                a = val;
            }
        };
        adda(realtime, new Coord(sz.x / 2, UI.scale(70)), 0.5, 0);

        Button edit = new Button(UI.scale(60), "Edit", () -> {
            try {
                long hours = Long.parseLong(txthours.text().equals("") ? "0" : txthours.text());
                long minutes = Long.parseLong(txtminutes.text().equals("") ? "0" : txtminutes.text());
                long seconds = Long.parseLong(txtseconds.text().equals("") ? "0" : txtseconds.text());
                long duration = ((60 * hours + minutes) * 60 + seconds) * 3;
                if (realtime.a) duration = Math.round(duration * ui.sess.glob.getTimeFac());
                TimerData.editTimer(timer, txtname.text(), duration);
                ui.destroy(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        add(edit, UI.scale(15, 70));

        Button cancel = new Button(UI.scale(60), "Cancel") {
            @Override
            public void click() {
                parent.reqdestroy();
            }
        };
        add(cancel, UI.scale(275, 70));
    }

    public void close() {
        ui.destroy(this);
    }
}