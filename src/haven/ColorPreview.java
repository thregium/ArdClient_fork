package haven;

import java.awt.Color;
import java.util.function.Consumer;

/**
 * Color preview square, only really for options
 */
public class ColorPreview extends Widget {
    private Color col;
    private ColorPicker cp = null;
    private final Consumer<Color> callback;
    public String name;

    public ColorPreview(Coord sz, Color cl, Consumer<Color> callback, String name) {
        super(sz);
        this.callback = callback;
        this.name = name;
        col = cl;
        tooltip = RichText.Parser.quote(String.format((this.name.equals("") ? "" : this.name +"\n") + "Red: %d\nGreen: %d\nBlue: %d\nAlpha: %d",
                col.getRed(), col.getGreen(), col.getBlue(), col.getAlpha()));
        tooltip = RichText.render((String) tooltip, 200);
    }

    public ColorPreview(Coord sz, Color cl, Consumer<Color> callback) {
        this(sz, cl, callback, "");
    }

    public ColorPreview(Coord sz, Color cl) {
        this(sz, cl, null);
    }

    public void draw(GOut g) {
        g.chcolor(Color.WHITE);
        g.rect(Coord.z, sz);
        g.chcolor();

        g.chcolor(col);
        g.frect(Coord.z, sz);
        g.chcolor();
    }

    public void setColor(final Color ncol) {
        this.col = ncol;
    }

    public Color getColor() {
        return (col);
    }

    public boolean mousedown(Coord c, int btn) {
        return true;
    }

    public boolean mouseup(Coord c, int btn) {
        if (btn == 1 && cp == null) {
            cp = new ColorPicker(col, (color -> {
                cp = null;
                col = color;
                tooltip = RichText.Parser.quote(String.format((this.name.equals("") ? "" : this.name +"\n") + "Red: %d\nGreen: %d\nBlue: %d\nAlpha: %d", col.getRed(), col.getGreen(), col.getBlue(), col.getAlpha()));
                tooltip = RichText.render((String) tooltip, 200);
                if (callback != null)
                    callback.accept(col);
            }));

            if (ui.gui != null) {
                ui.gui.add(cp, new Coord(50, 50));
            } else {
                ui.root.add(cp, new Coord(50, 50));
            }
        }
        return true;
    }
}
