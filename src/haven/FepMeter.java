package haven;

import java.awt.Color;

public class FepMeter extends IMeter {
    private final CharWnd.FoodMeter food;

    public FepMeter(CharWnd.FoodMeter food, String name) {
        super(Resource.local().load("hud/meter/fepmeter"), name);
        this.food = food;
    }

    @Override
    protected void drawMeters(GOut g) {
        double x = 0;
        int w = IMeter.msz.x;
        for (CharWnd.FoodMeter.El el : food.els) {
            int l = (int) Math.floor((x / food.cap) * w);
            int r = (int) Math.floor(((x += el.a) / food.cap) * w);
            try {
                Color col = el.ev().col;
                g.chcolor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 255));
                g.frect(off.add(l, 0).mul(this.scale), Coord.of(r - l, msz.y).mul(this.scale));
            } catch (Loading e) {}
        }
    }

    @Override
    public void tick(double dt) {
        super.tick(dt);
        double sum = food.els.stream().mapToDouble(el -> el.a).sum();
        meterinfo = String.format("%s/%s", Utils.odformat2(sum, 2), Utils.odformat(food.cap, 2));
    }

    @Override
    public Object tooltip(Coord c, Widget prev) {
        return (food.tooltip(c, prev));
    }
}