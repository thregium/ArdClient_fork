package haven.res.gfx.hud.buffs.travelwar;

import haven.Buff;
import haven.Coord;
import haven.ItemInfo;
import haven.Text;
import haven.Utils;

/* >tt: Weariness */
public class Weariness extends ItemInfo.Tip implements Buff.AMeterInfo {
    public final double cur, cap, a;

    public Weariness(Owner owner, double cur, double cap) {
        super(owner);
        this.cur = cur;
        this.cap = cap;
        this.a = Utils.clip(cur / cap, 0, 1);
    }

    public void layout(Layout l) {
        Text text = Text.render(String.format(": %.1f/%.1f (%d%%)", cur, cap, (int) Math.ceil(100 * a)));
        l.cmp.add(text.img, new Coord(l.cmp.sz.x, 0));
    }

    public int order() {return (5);}

    public Tip shortvar() {return (this);}

    public double ameter() {return (a);}

    public static Weariness mkinfo(Owner owner, Object... args) {
        return (new Weariness(owner, ((Number) args[1]).doubleValue(), ((Number) args[2]).doubleValue()));
    }
}
