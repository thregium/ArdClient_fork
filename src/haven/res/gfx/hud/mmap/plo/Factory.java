package haven.res.gfx.hud.mmap.plo;

import haven.BuddyWnd;
import haven.GobIcon;
import haven.Message;
import haven.OwnerContext;
import haven.Resource;

import java.util.ArrayList;
import java.util.Collection;

public class Factory implements GobIcon.Icon.Factory {
    public Player create(OwnerContext owner, Resource res, Message sdt) {
        return (new Player(owner, res));
    }

    public Collection<Player> enumerate(OwnerContext owner, Resource res, Message sdt) {
        Collection<Player> ret = new ArrayList<>();
        for (int i = 0; i < BuddyWnd.gc.length; i++)
            ret.add(new Player(owner, res, i));
        return (ret);
    }
}
