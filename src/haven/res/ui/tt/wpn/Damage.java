package haven.res.ui.tt.wpn;

import haven.ItemInfo;
import haven.res.ui.tt.wpn.info.WeaponInfo;

public class Damage extends WeaponInfo {
    public static final Damage NODAMAGE = new Damage(null, 0);

    public static class Fac implements InfoFactory {
        @Override
        public ItemInfo build(Owner owner, Raw raw, Object... args) {
            return new Damage(owner, ((Number) args[1]).intValue());
        }
    }

    public final int dmg;

    public Damage(Owner owner, int dmg) {
        super(owner);
        this.dmg = dmg;
    }

    public static Damage mkinfo(Owner owner, Object... args) {
        return (new Damage(owner, ((Number) args[1]).intValue()));
    }

    public String wpntips() {
        return ("Damage: " + dmg);
    }

    public int order() {
        return (50);
    }
}