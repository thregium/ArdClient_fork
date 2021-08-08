package haven.res.ui.croster;

import haven.GAttrib;
import haven.Gob;
import haven.Message;

public class CattleId extends GAttrib {
    public final long id;
    public CattleIdSprite sprite;

    public CattleId(Gob gob, long id) {
        super(gob);
        this.id = id;
    }

    public static void parse(Gob gob, Message dat) {
        long id = dat.int64();
        gob.setattr(new CattleId(gob, id));
    }

    public void tick() {
        if (sprite != null)
            sprite.update();
    }
}
