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
                    final List<PBotItem> racktrays = new ArrayList<>();
                    invs.forEach(i -> racktrays.addAll(i.getInventoryItemsByResnames(fullTrayResName)));
                    int count = Math.min(racktrays.size(), freeSpaceForTrays(pinv));
                    int initCount = pinv.getInventoryItemsByResnames(fullTrayResName).size();
                    for (int i = 0; i < count; i++)
                        racktrays.get(i).transferItem();

                    if (!waitFor(() -> {
                        debug(pinv, otherinvs, invs);
                        return (pinv.getInventoryItemsByResnames(fullTrayResName).size() != count + initCount);
                    }, 1000)) {
                        break;
                    }
                }

                {
                    debug(pinv, otherinvs, invs);
                    for (PBotInventory inv : otherinvs) {
                        final List<PBotItem> trays = new ArrayList<>(pinv.getInventoryItemsByResnames(fullTrayResName));
                        if (trays.isEmpty())
                            break;
                        int count = Math.min(trays.size(), freeSpaceForTrays(inv));
                        int initCount = inv.getInventoryItemsByResnames(fullTrayResName).size();
                        for (int i = 0; i < count; i++)
                            inv.transferLastItemToInventoryFromPlayerInventory();

                        if (!waitFor(() -> {
                            debug(pinv, otherinvs, invs);
                            return (inv.getInventoryItemsByResnames(fullTrayResName).size() != count + initCount);
                        }, 1000)) {
                            break;
                        }
                    }
                }

                if (getInventoryItemsByResnames(invs, fullTrayResName).isEmpty() || freeSpaceForTrays(pinv) == 0)
                    break;
            }

            PBotUtils.sysMsg(ui, "Trays transferred!");
        } catch (Exception e) {
            e.printStackTrace();
            PBotUtils.sysMsg(ui, "Failed " + e);
        }
    }
}
