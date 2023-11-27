package haven.res.ui.tt.carrytag;

import haven.ItemInfo;

@haven.FromResource(name = "ui/tt/carrytag", version = 2, override = true)
public class Tagged extends ItemInfo {
    public final long id;

    public Tagged(Owner owner, long id) {
        super(owner);
        this.id = id;
    }

    public static ItemInfo mkinfo(Owner owner, Object... args) {
        long id = ((Number) args[1]).longValue();
        return (new Tagged(owner, id));
    }
}
