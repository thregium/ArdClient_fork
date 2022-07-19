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
import static haven.automation.CheeseAPI.debug;
import static haven.automation.CheeseAPI.freeSpaceForTrays;
import static haven.automation.CheeseAPI.fullTrayResName;
import static haven.automation.CheeseAPI.getInventoryItemsByResnames;
import static haven.automation.CheeseAPI.rackWndName;
import static haven.automation.CheeseAPI.waitFor;

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

            long time = System.currentTimeMillis();
            while (System.currentTimeMillis() - time < 5000) {
                {
                    debug(pinv, otherinvs, invs);
                    final List<PBotItem> trays = new ArrayList<>(pinv.getInventoryItemsByResnames(fullTrayResName));
                    int count = Math.min(trays.size(), freeSpaceForTrays(invs));
                    int initCount = getInventoryItemsByResnames(invs, fullTrayResName).size();
                    for (int i = 0; i < count; i++)
                        trays.get(i).transferItem();

                    if (!waitFor(() -> {
                        debug(pinv, otherinvs, invs);
                        return (getInventoryItemsByResnames(invs, fullTrayResName).size() != count + initCount);
                    }, 1000)) {
                        break;
                    }
                }

                {
                    debug(pinv, otherinvs, invs);
                    final List<PBotItem> othertrays = new ArrayList<>();
                    otherinvs.forEach(i -> othertrays.addAll(i.getInventoryItemsByResnames(fullTrayResName)));
                    int count = Math.min(othertrays.size(), freeSpaceForTrays(pinv));
                    int initCount = pinv.getInventoryItemsByResnames(fullTrayResName).size();
                    for (int i = 0; i < count; i++)
                        othertrays.get(i).transferItem();

                    if (!waitFor(() -> {
                        debug(pinv, otherinvs, invs);
                        return (pinv.getInventoryItemsByResnames(fullTrayResName).size() != count + initCount);
                    }, 1000)) {
                        break;
                    }
                }

                if (pinv.getInventoryItemsByResnames(fullTrayResName).isEmpty() || freeSpaceForTrays(invs) == 0)
                    break;
            }

            PBotUtils.debugMsg(ui, "Trays is placed!");
        } catch (Exception e) {
            e.printStackTrace();
            PBotUtils.debugMsg(ui, "Failed " + e);
        }
    }
}
