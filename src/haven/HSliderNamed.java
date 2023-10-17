package haven;

import java.awt.Color;

public class HSliderNamed extends Widget implements Comparable<HSliderNamed> {
    public String name;
    public Text text;
    public HSlider slider;
    public HSliderListboxItem item;

    public HSliderNamed(HSliderListboxItem item, int w, int min, int max, Runnable changed) {
        this.item = item;
        this.name = item.name;
        this.text = Text.renderstroked(name, Color.WHITE, Color.BLACK);
        this.slider = new HSlider(w, min, max, item.val) {
            @Override
            public void changed() {
                super.changed();
                item.val = val;
                save();
            }
        };
        this.save = changed;
        resize(slider.sz);
    }

    Runnable save;

    public void save() {
        if (save != null)
            save.run();
    }

    @Override
    public void tick(double dt) {
        super.tick(dt);
        if (slider != null && item.val != slider.val) {
            slider.val = item.val;
            slider.changed();
        }
    }

    @Override
    public void draw(GOut g) {
        super.draw(g);
        slider.draw(g);
        g.aimage(text.tex(), sz.div(2), 0.5, 0.5);
    }

    public boolean mousedown(Coord c, int button) {
        return (slider.mousedown(c, button));
    }

    public void mousemove(Coord c) {
        slider.mousemove(c);
    }

    public boolean mouseup(Coord c, int button) {
        return (slider.mouseup(c, button));
    }

    public boolean mousewheel(Coord c, int amount) {
        return (slider.mousewheel(c, amount));
    }

    @Override
    public int compareTo(HSliderNamed o) {
        return name.compareTo(o.name);
    }
}
