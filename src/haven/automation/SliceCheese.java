package haven.automation;

import haven.GameUI;
import haven.UI;
import haven.Window;
import haven.purus.pbot.PBotInventory;
import haven.purus.pbot.PBotItem;
import haven.purus.pbot.PBotUtils;
import haven.purus.pbot.PBotWindowAPI;

import java.util.ArrayList;
import java.util.List;

import static haven.automation.CheeseAPI.flowerPetal;
import static haven.automation.CheeseAPI.fullTrayResName;
import static haven.automation.CheeseAPI.rackWndName;
import static haven.automation.CheeseAPI.timeout;

public class SliceCheese implements Runnable {
    private final GameUI gui;

    public SliceCheese(GameUI gui) {
        this.gui = gui;
    }

    @Override
    public void run() {
        UI ui = gui.ui;
        try {
            final List<PBotItem> trays = new ArrayList<>();
            final List<PBotInventory> invs = new ArrayList<>(PBotUtils.getAllInventories(ui));

            invs.forEach(i -> trays.addAll(i.getInventoryItemsByResnames(fullTrayResName)));

            trays.forEach(t -> {
                t.activateItem();
                if (PBotUtils.waitForFlowerMenu(ui, timeout)) {
                    if (PBotUtils.choosePetal(ui, flowerPetal)) {
                        if (PBotUtils.waitFlowermenuClose(ui, timeout)) {
                            PBotUtils.debugMsg(ui, "FlowerMenu close failed!");
                            return;
                        }
                    } else {
                        if (PBotUtils.closeFlowermenu(ui, timeout)) {
                            PBotUtils.debugMsg(ui, "FlowerMenu close failed!");
                            return;
                        }
                    }
                }
            });

            PBotUtils.debugMsg(ui, trays.size() + " cheeses is sliced!");
        } catch (Exception e) {
            e.printStackTrace();
            PBotUtils.debugMsg(ui, "Failed " + e);
        }
    }
}
