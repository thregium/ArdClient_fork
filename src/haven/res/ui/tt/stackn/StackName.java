package haven.res.ui.tt.stackn;

import haven.ItemInfo;
import haven.res.ui.tt.defn.DefName;

public class StackName implements ItemInfo.InfoFactory {
    public ItemInfo build(ItemInfo.Owner owner, ItemInfo.Raw raw, Object... args) {
        String nm = DefName.getname(owner);
        String onm = DefName.getoname(owner);
        if (onm == null)
            return (null);
        return (new ItemInfo.Name(owner, onm + ", stack of", nm + ", stack of"));
    }
}

