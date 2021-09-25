package haven.sloth.gui.Timer;


import haven.Button;
import haven.CheckBox;
import haven.Coord;
import haven.Glob;
import haven.Label;
import haven.TextEntry;
import haven.Window;
import haven.sloth.io.TimerData;

import java.awt.event.KeyEvent;

public class TimerEditWnd extends Window {
    TimerEditWnd(String cap) {
        super(new Coord(355, 100), cap, cap);

        add(new Label("Name"), new Coord(15, 10));
        final TextEntry txtname = new TextEntry(200, "");
        add(txtname, new Coord(15, 30));

        add(new Label("HH"), new Coord(225, 10));
        final TextEntry txthours = new TextEntry(35, "") {
            @Override
            public boolean keydown(KeyEvent ev) {
                final char c = ev.getKeyChar();
                if (c == 0x8 || c == 0x7f || c == 0x09 || (c >= 0x30 && c <= 0x39 && text().length() <= 2))
                    return super.keydown(ev);
                return true;
            }
        };
        add(txthours, new Coord(225, 30));

        add(new Label("MM"), new Coord(265, 10));
        final TextEntry txtminutes = new TextEntry(35, "") {
            @Override
            public boolean keydown(KeyEvent ev) {
                final char c = ev.getKeyChar();
                if (c == 0x8 || c == 0x7f || c == 0x09 || (c >= 0x30 && c <= 0x39 && text().length() <= 1))
                    return super.keydown(ev);
                return true;
            }
        };
        add(txtminutes, new Coord(265, 30));

        add(new Label("SS"), new Coord(305, 10));
        final TextEntry txtseconds = new TextEntry(35, "") {
            @Override
            public boolean keydown(KeyEvent ev) {
                final char c = ev.getKeyChar();
                if (c == 0x8 || c == 0x7f || c == 0x09 || (c >= 0x30 && c <= 0x39 && text().length() <= 1))
                    return super.keydown(ev);
                return true;
            }
        };
        add(txtseconds, new Coord(305, 30));

        CheckBox gametime = new CheckBox("Ingame time") {
            public void set(boolean val) {
                long hours = Long.parseLong(txthours.text().equals("") ? "0" : txthours.text());
                long minutes = Long.parseLong(txtminutes.text().equals("") ? "0" : txtminutes.text());
                long seconds = Long.parseLong(txtseconds.text().equals("") ? "0" : txtseconds.text());
                long duration = ((60 * hours + minutes) * 60 + seconds);
                if (val) {
                    duration = Math.round(duration * Glob.SERVER_TIME_RATIO);
                } else {
                    duration = Math.round(duration / Glob.SERVER_TIME_RATIO);
                }
                int h = (int) (duration / 3600);
                int m = (int) ((duration % 3600) / 60);
                int s = (int) (duration % 60);
                txthours.settext(h == 0 ? "" : h + "");
                txtminutes.settext(m == 0 ? "" : m + "");
                txtseconds.settext(s == 0 ? "" : s + "");

                a = val;
            }
        };
        adda(gametime, new Coord(sz.x / 2, 70), 0.5, 0);

        Button add = new Button(60, "Add", () -> {
            try {
                long hours = Long.parseLong(txthours.text().equals("") ? "0" : txthours.text());
                long minutes = Long.parseLong(txtminutes.text().equals("") ? "0" : txtminutes.text());
                long seconds = Long.parseLong(txtseconds.text().equals("") ? "0" : txtseconds.text());
                long duration = ((60 * hours + minutes) * 60 + seconds) * 3;
                if (gametime.a) duration = Math.round(duration / Glob.SERVER_TIME_RATIO);
                TimerData.addTimer(txtname.text(), duration);
                ui.destroy(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        add(add, new Coord(15, 70));

        Button cancel = new Button(60, "Cancel") {
            @Override
            public void click() {
                parent.reqdestroy();
            }
        };
        add(cancel, new Coord(275, 70));
    }

    TimerEditWnd(String cap, TimerData.Timer timer) {
        super(new Coord(355, 100), cap, cap);

        add(new Label("Name"), new Coord(15, 10));
        final TextEntry txtname = new TextEntry(200, timer.name);
        add(txtname, new Coord(15, 30));

        long ts = timer.duration / 3;

        add(new Label("HH"), new Coord(225, 10));
        final TextEntry txthours = new TextEntry(35, (int) (ts / 3600) == 0 ? "" : (int) (ts / 3600) + "") {
            @Override
            public boolean keydown(KeyEvent ev) {
                final char c = ev.getKeyChar();
                if (c == 0x8 || c == 0x7f || c == 0x09 || (c >= 0x30 && c <= 0x39 && text().length() <= 2))
                    return super.keydown(ev);
                return true;
            }
        };
        add(txthours, new Coord(225, 30));

        add(new Label("MM"), new Coord(265, 10));
        final TextEntry txtminutes = new TextEntry(35, (int) ((ts % 3600) / 60) == 0 ? "" : (int) ((ts % 3600) / 60) + "") {
            @Override
            public boolean keydown(KeyEvent ev) {
                final char c = ev.getKeyChar();
                if (c == 0x8 || c == 0x7f || c == 0x09 || (c >= 0x30 && c <= 0x39 && text().length() <= 1))
                    return super.keydown(ev);
                return true;
            }
        };
        add(txtminutes, new Coord(265, 30));

        add(new Label("SS"), new Coord(305, 10));
        final TextEntry txtseconds = new TextEntry(35, (int) (ts % 60) == 0 ? "" : (int) (ts % 60) + "") {
            @Override
            public boolean keydown(KeyEvent ev) {
                final char c = ev.getKeyChar();
                if (c == 0x8 || c == 0x7f || c == 0x09 || (c >= 0x30 && c <= 0x39 && text().length() <= 1))
                    return super.keydown(ev);
                return true;
            }
        };
        add(txtseconds, new Coord(305, 30));

        CheckBox gametime = new CheckBox("Ingame time") {
            public void set(boolean val) {
                long hours = Long.parseLong(txthours.text().equals("") ? "0" : txthours.text());
                long minutes = Long.parseLong(txtminutes.text().equals("") ? "0" : txtminutes.text());
                long seconds = Long.parseLong(txtseconds.text().equals("") ? "0" : txtseconds.text());
                long duration = ((60 * hours + minutes) * 60 + seconds);
                if (val) {
                    duration = Math.round(duration * Glob.SERVER_TIME_RATIO);
                } else {
                    duration = Math.round(duration / Glob.SERVER_TIME_RATIO);
                }
                int h = (int) (duration / 3600);
                int m = (int) ((duration % 3600) / 60);
                int s = (int) (duration % 60);
                txthours.settext(h == 0 ? "" : h + "");
                txtminutes.settext(m == 0 ? "" : m + "");
                txtseconds.settext(s == 0 ? "" : s + "");

                a = val;
            }
        };
        adda(gametime, new Coord(sz.x / 2, 70), 0.5, 0);

        Button edit = new Button(60, "Edit", () -> {
            try {
                long hours = Long.parseLong(txthours.text().equals("") ? "0" : txthours.text());
                long minutes = Long.parseLong(txtminutes.text().equals("") ? "0" : txtminutes.text());
                long seconds = Long.parseLong(txtseconds.text().equals("") ? "0" : txtseconds.text());
                long duration = ((60 * hours + minutes) * 60 + seconds) * 3;
                if (gametime.a) duration = Math.round(duration / Glob.SERVER_TIME_RATIO);
                TimerData.editTimer(timer, txtname.text(), duration);
                ui.destroy(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        add(edit, new Coord(15, 70));

        Button cancel = new Button(60, "Cancel") {
            @Override
            public void click() {
                parent.reqdestroy();
            }
        };
        add(cancel, new Coord(275, 70));
    }

    public void close() {
        ui.destroy(this);
    }
}