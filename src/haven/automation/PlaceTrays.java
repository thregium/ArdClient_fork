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

import static haven.automation.CheeseAPI.beltWndName;
import static haven.automation.CheeseAPI.fullTrayResName;
import static haven.automation.CheeseAPI.rackWndName;

public class PlaceTrays implements Runnable {
    private final GameUI gui;

    public PlaceTrays(GameUI gui) {
        this.gui = gui;
    }

    @Override
    public void run() {
        UI ui = gui.ui;
        try {
            new OpenRacks(gui).run();
            PBotUtils.sleep(500);

            final List<PBotInventory> otherinvs = PBotUtils.getAllInventories(ui);
            final PBotInventory pinv = PBotUtils.playerInventory(ui);

            final List<PBotInventory> invs = new ArrayList<>();
            final List<Window> wnds = PBotWindowAPI.getWindows(ui, rackWndName);
            wnds.forEach(w -> invs.addAll(PBotWindowAPI.getInventories(w)));

            otherinvs.removeIf(invs::contains);
            otherinvs.removeIf(pinv::equals);
            Window beltWnd = PBotWindowAPI.getWindow(ui, beltWndName);
            if (beltWnd != null) {
                PBotInventory beltInv = PBotWindowAPI.getInventory(beltWnd);
                if (beltInv != null)
                    otherinvs.removeIf(beltInv::equals);
            }

            final List<PBotItem> trays = new ArrayList<>(pinv.getInventoryItemsByResnames(fullTrayResName));
            otherinvs.forEach(i -> trays.addAll(i.getInventoryItemsByResnames(fullTrayResName)));

            while (checkFreeSpace(invs) && !trays.isEmpty()) {
                try {
                    trays.forEach(PBotItem::transferItem);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                trays.clear();

                trays.addAll(pinv.getInventoryItemsByResnames(fullTrayResName));
                otherinvs.forEach(i -> trays.addAll(i.getInventoryItemsByResnames(fullTrayResName)));

                PBotUtils.sleep(10);
            }

            PBotUtils.sysMsg(ui, "Trays is placed!");
        } catch (Exception e) {
            e.printStackTrace();
            PBotUtils.sysMsg(ui, "Failed " + e);
        }
    }

    public boolean checkFreeSpace(List<PBotInventory> invs) {
        for (PBotInventory inv : invs)
            if (inv.freeSlotsInv() > 0)
                return (true);
        return (false);
    }
}
