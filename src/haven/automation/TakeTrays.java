package haven.automation;


import haven.Coord;
import haven.GItem;
import haven.GameUI;
import haven.Inventory;
import haven.WItem;
import haven.Widget;
import haven.purus.pbot.PBotInventory;
import haven.purus.pbot.PBotUtils;

import java.util.List;


public class TakeTrays implements Runnable {
    private GameUI gui;

    public TakeTrays(GameUI gui) {
        this.gui = gui;
    }

    @Override
    public void run() {
        new OpenRacks(gui).run();
        List<WItem> trays;

        for (PBotInventory q : PBotUtils.getAllInventories(gui.ui)) {
            try {
                if (q.inv != gui.maininv) {
                    trays = (q.inv.getItemsPartial("Tray"));
                    System.out.println("trays2 size : " + trays.size());
                    if (!trays.isEmpty()) {
                        for (WItem item : trays)
                            item.item.wdgmsg("transfer", new Coord(item.item.sz.x / 2, item.item.sz.y / 2), -1);
                        trays.clear();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
