package haven.res.ui.tt.defn;

import haven.GSprite;
import haven.ItemInfo;
import haven.ItemInfo.InfoFactory;
import haven.ItemInfo.Name;
import haven.ItemInfo.Owner;
import haven.ItemInfo.ResOwner;
import haven.ItemInfo.SpriteOwner;
import haven.Resource;
import haven.Resource.Tooltip;

public class DefName implements InfoFactory {
    public DefName() {}

    public ItemInfo build(Owner owner, Object... args) {
        if (owner instanceof SpriteOwner) {
            GSprite spr = ((SpriteOwner) owner).sprite();
            if (spr instanceof DynName) {
                return (new Name(owner, ((DynName) spr).oname(), ((DynName) spr).name()));
            }
        }

        if (!(owner instanceof ResOwner)) {
            return null;
        } else {
            Resource res = ((ResOwner) owner).resource();
            Tooltip tt = res.layer(Resource.tooltip);
            if (tt == null) {
                return (new Name(owner, "*Broken Name", "*Broken Name"));
//                throw new RuntimeException("Item resource " + res + " is missing default tooltip");
            } else {
                return (new Name(owner, tt.origt, tt.t));
            }
        }
    }
}
