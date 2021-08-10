package haven.automation;


import haven.GameUI;
import haven.purus.pbot.PBotInventory;
import haven.purus.pbot.PBotItem;
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

        for (PBotInventory q : PBotUtils.getAllInventories(gui.ui)) {
            try {
                if (q.inv != gui.maininv) {
                    List<PBotItem> trays = q.getInventoryItemsByNames("Tray");
                    System.out.println("trays2 size : " + trays.size());
                    if (!trays.isEmpty()) {
                        for (PBotItem item : trays)
                            item.transferItem();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
