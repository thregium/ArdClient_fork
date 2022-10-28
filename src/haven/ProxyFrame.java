package haven;

import java.awt.Color;

public class ProxyFrame<T extends Widget> extends Frame {
    public final T ch;
    public Color color = Color.WHITE;

    public ProxyFrame(T child, boolean resize) {
        super(child.sz, !resize);
        this.ch = child;
        if (resize)
            ch.resize(inner());
        add(child, Coord.z);
    }

    public void drawframe(GOut g) {
        if (color != null) {
            g.chcolor(color);
            box.draw(g, Coord.z, sz);
        }
    }

    public void uimsg(String msg, Object... args) {
        if ("col".equals(msg)) {
            color = (Color) args[0];
        } else {
            ch.uimsg(msg, args);
        }
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender == ch)
            wdgmsg(msg, args);
        else
            super.wdgmsg(sender, msg, args);
    }
}
