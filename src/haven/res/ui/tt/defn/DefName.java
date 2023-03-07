package haven.res.ui.tt.defn;

import haven.GSprite;
import haven.ItemInfo;
import haven.ItemInfo.Name;
import haven.ItemInfo.Owner;
import haven.ItemInfo.ResOwner;
import haven.ItemInfo.SpriteOwner;
import haven.Resource;
import haven.Resource.Tooltip;

public class DefName implements ItemInfo.InfoFactory {
    public static String getname(ItemInfo.Owner owner) {
        if (owner instanceof ItemInfo.SpriteOwner) {
            GSprite spr = ((ItemInfo.SpriteOwner) owner).sprite();
            if (spr instanceof DynName)
                return (((DynName) spr).name());
        }
        if (!(owner instanceof ItemInfo.ResOwner))
            return (null);
        Resource res = ((ItemInfo.ResOwner) owner).resource();
        Resource.Tooltip tt = res.layer(Resource.tooltip);
        if (tt == null) {
            return ("*Broken Name");
//            throw (new RuntimeException("Item resource " + res + " is missing default tooltip"));
        }
        return (tt.t);
    }

    public static String getoname(ItemInfo.Owner owner) {
        if (owner instanceof ItemInfo.SpriteOwner) {
            GSprite spr = ((ItemInfo.SpriteOwner) owner).sprite();
            if (spr instanceof DynName)
                return (((DynName) spr).oname());
        }
        if (!(owner instanceof ItemInfo.ResOwner))
            return (null);
        Resource res = ((ItemInfo.ResOwner) owner).resource();
        Resource.Tooltip tt = res.layer(Resource.tooltip);
        if (tt == null) {
            return ("*Broken Name");
//            throw (new RuntimeException("Item resource " + res + " is missing default tooltip"));
        }
        return (tt.origt);
    }

    public ItemInfo build(ItemInfo.Owner owner, ItemInfo.Raw raw, Object... args) {
        String nm = getname(owner);
        String onm = getoname(owner);
        if (onm == null)
            return (null);
        return (new ItemInfo.Name(owner, onm, nm));
    }

    /*public ItemInfo build(Owner owner, Object... args) {
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
    }*/
}
