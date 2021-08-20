package haven.automation;

import haven.Coord;
import haven.purus.pbot.PBotInventory;
import haven.purus.pbot.PBotItem;
import haven.purus.pbot.PBotUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

public class CheeseAPI {
    public static final String curdResName = "gfx/invobjs/curd-.*";
    public static final String curdInfo = "pieces of curd";
    public static final String rackResName = "gfx/terobjs/cheeserack";
    public static final String emptyTrayResName = "gfx/invobjs/cheesetray";
    public static final String fullTrayResName = "gfx/invobjs/cheesetray-.*";
    public static final String beltWndName = "Belt";
    public static final String rackWndName = "Rack";
    public static final String flowerPetal = "Slice up";
    public static final Coord traySize = Coord.of(2, 1);
    public static final int itemClick = 4;
    public static final int radius = 14;
    public static final int timeout = 1000;

    @SafeVarargs
    public static void debug(PBotInventory pinv, List<PBotInventory>... invslist) {
        Arrays.asList(invslist).forEach(invs -> invs.forEach(CheeseAPI::debug));
        debug(pinv);
    }

    public static void debug(PBotInventory inv) {
        inv.getInventoryContents().forEach(item -> {
            while (item.getResname() == null)
                PBotUtils.sleep(10);
        });
    }

    public static boolean waitFor(Callable<Boolean> task, int limit) {
        try {
            for (int i = 0, sleep = 10; task.call(); i += sleep) {
                if (i >= limit) {
                    return (false);
                } else {
                    PBotUtils.sleep(sleep);
                }
            }
            return (true);
        } catch (Exception e) {
            return (false);
        }
    }

    public static int freeSpaceForTrays(List<PBotInventory> invs) {
        int count = 0;
        for (PBotInventory inv : invs)
            count += freeSpaceForTrays(inv);
        return (count);
    }

    public static int freeSpaceForTrays(PBotInventory inv) {
        return (inv.itemCountAtFreeSpace(traySize));
    }

    public static List<PBotItem> getInventoryItemsByResnames(List<PBotInventory> invs, String... pattern) {
        final List<PBotItem> items = new ArrayList<>();
        for (PBotInventory inv : invs)
            items.addAll(inv.getInventoryItemsByResnames(pattern));
        return (items);
    }
}
