package haven.res.gfx.terobjs.dng.beedungeon;

import haven.Gob;
import haven.Message;
import haven.Resource;
import haven.res.lib.vmat.VarSprite;

@haven.FromResource(name = "gfx/terobjs/dng/beedungeon", version = 4, override = true)
public class Beehive extends VarSprite {
    public Beehive(Owner owner, Resource res, boolean newp) {
        super(owner, res, Message.nil);
        Gob gob = (owner instanceof Gob) ? (Gob) owner : owner.context(Gob.class);
        if (newp)
            gob.addol(new Growing(owner, res));
    }

    public static Beehive mksprite(Owner owner, Resource res, Message sdt) {
        return (new Beehive(owner, res, sdt.eom() ? false : (sdt.uint8() != 0)));
    }
}
