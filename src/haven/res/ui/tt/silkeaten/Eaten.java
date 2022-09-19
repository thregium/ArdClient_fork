package haven.res.ui.tt.silkeaten;

import haven.*;
import java.awt.Color;
import java.awt.image.BufferedImage;

public class Eaten extends ItemInfo.Tip implements GItem.NumberInfo {
    public static final Color ocolor = new Color(32, 96, 32);
    public static final Color color = new Color(192, 255, 64);
    public final int max, cur;

    public Eaten(Owner owner, int max, int cur) {
        super(owner);
        this.max = max;
        this.cur = cur;
    }

    public BufferedImage tipimg() {
//        throw(new RuntimeException("This client is deprecated"));
         return(RichText.render(String.format("Silkworm nibbles: $col[192,255,64]{%s}/$col[192,255,64]{%s}", cur, max), 0).img);
    }

    public int itemnum() {
        return(max - cur);
    }

    public static ItemInfo mkinfo(Owner owner, Object... args) {
        int max = ((Number)args[1]).intValue();
        int cur = ((Number)args[2]).intValue();
        return(new Eaten(owner, max, cur));
    }
}
