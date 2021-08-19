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

import static haven.automation.CheeseAPI.fullTrayResName;
import static haven.automation.CheeseAPI.rackWndName;

public class TakeTrays implements Runnable {
    private final GameUI gui;

    public TakeTrays(GameUI gui) {
        this.gui = gui;
    }

    @Override
    public void run() {
        UI ui = gui.ui;
        try {
            new OpenRacks(gui).run();
            PBotUtils.sleep(500);

            final List<PBotItem> trays = new ArrayList<>();
            final List<PBotInventory> invs = new ArrayList<>();
            final List<Window> wnds = PBotWindowAPI.getWindows(ui, rackWndName);

            wnds.forEach(w -> invs.addAll(PBotWindowAPI.getInventories(w)));
            invs.forEach(i -> trays.addAll(i.getInventoryItemsByResnames(fullTrayResName)));
            trays.forEach(PBotItem::transferItem);

            PBotUtils.sysMsg(ui, trays.size() + " trays transferred!");
        } catch (Exception e) {
            e.printStackTrace();
            PBotUtils.sysMsg(ui, "Failed " + e);
        }
    }
}
