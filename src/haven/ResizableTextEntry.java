package haven;

import java.awt.Color;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;

public class ResizableTextEntry extends TextEntry {
    public int addWidth = 30;
    public final int defwidth;

    public ResizableTextEntry(int defwidth,  String deftext) {
        super(defwidth, deftext, null, null);
        this.defwidth = defwidth;
    }
    public ResizableTextEntry(String deftext) {
        super(0, deftext, null, null);
        this.defwidth = 0;
    }

    public void draw(GOut g) {
        Coord size = Text.render(text(), Color.WHITE, fnd).sz();
        int w = text().equals("") ? Math.max(defwidth, addWidth) : Math.max(Math.max(size.x + 12, addWidth), defwidth);
        if (sz.x != w)
            sz = new Coord(w, mext.sz().y);
        super.draw(g);
    }

    public double getTextWidth(String text) {
        double textWidth;
        if (text.length() > 0)
            textWidth = Math.ceil(new TextLayout(text, fnd.font, new FontRenderContext(null, true, true)).getBounds().getWidth());
        else textWidth = 0;
        return (int) textWidth + addWidth;
    }
}
