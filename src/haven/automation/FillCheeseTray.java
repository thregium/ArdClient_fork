package haven.automation;

import haven.Coord;
import haven.GameUI;
import haven.ItemInfo;
import haven.UI;
import haven.Window;
import haven.purus.pbot.PBotInventory;
import haven.purus.pbot.PBotItem;
import haven.purus.pbot.PBotUtils;
import haven.purus.pbot.PBotWindowAPI;

import java.util.ArrayList;
import java.util.List;

import static haven.automation.CheeseAPI.beltWndName;
import static haven.automation.CheeseAPI.curdInfo;
import static haven.automation.CheeseAPI.curdResName;
import static haven.automation.CheeseAPI.debug;
import static haven.automation.CheeseAPI.emptyTrayResName;
import static haven.automation.CheeseAPI.itemClick;
import static haven.automation.CheeseAPI.timeout;
import static haven.automation.CheeseAPI.waitFor;

public class FillCheeseTray implements Runnable {
    private final GameUI gui;

    public FillCheeseTray(GameUI gui) {
        this.gui = gui;
    }

    @Override
    public void run() {
        UI ui = gui.ui;
        try {
            final List<PBotInventory> otherinvs = PBotUtils.getAllInventories(ui);
            final PBotInventory pinv = PBotUtils.playerInventory(ui);

            otherinvs.removeIf(pinv::equals);
            Window beltWnd = PBotWindowAPI.getWindow(ui, beltWndName);
            if (beltWnd != null) {
                PBotInventory beltInv = PBotWindowAPI.getInventory(beltWnd);
                if (beltInv != null)
                    otherinvs.removeIf(beltInv::equals);
            }

            long time = System.currentTimeMillis();
            while (System.currentTimeMillis() - time < 5000) {
                debug(pinv, otherinvs);
                final List<PBotItem> trays = new ArrayList<>(pinv.getInventoryItemsByResnames(emptyTrayResName));
                otherinvs.forEach(i -> trays.addAll(i.getInventoryItemsByResnames(emptyTrayResName)));

                if (trays.isEmpty())
                    break;

                final List<PBotItem> othercurds = new ArrayList<>();
                otherinvs.forEach(i -> othercurds.addAll(i.getInventoryItemsByResnames(curdResName)));

                int count = Math.min(pinv.freeSlotsInv(), othercurds.size());
                int initCount = pinv.getInventoryItemsByResnames(curdResName).size();
                for (int i = 0; i < count; i++)
                    othercurds.get(i).transferItem();

                if (!waitFor(() -> {
                    debug(pinv, otherinvs);
                    return (pinv.getInventoryItemsByResnames(curdResName).size() != count + initCount);
                }, 1000)) {
                    break;
                }

                debug(pinv, otherinvs);
                final List<PBotItem> curds = new ArrayList<>(pinv.getInventoryItemsByResnames(curdResName));
                if (!curds.isEmpty()) {
                    curds.get(curds.size() - 1).takeItem(timeout);
                    int ic = curds.size();
                    for (PBotItem tray : trays) {
                        int clicks = Math.min(checkInfo(tray), ic);
                        for (int c = 0; c < clicks; c++)
                            tray.itemact(1);
                        ic -= clicks;
                        if (ic == 0)
                            break;
                    }
                    int finalIc = ic;
                    if (!waitFor(() -> {
                        debug(pinv, otherinvs);
                        return (pinv.getInventoryItemsByResnames(curdResName).size() != finalIc);
                    }, 1000)) {
                        break;
                    }
                    if (PBotUtils.getItemAtHand(ui) != null) {
                        Coord slot = pinv.getFreeSlot();
                        if (slot != null)
                            pinv.dropItemToInventory(slot, timeout);
                    }

                    PBotUtils.sleep(10);
                } else {
                    break;
                }
            }
            PBotUtils.debugMsg(ui, "Curd is filled!");
        } catch (Exception e) {
            e.printStackTrace();
            PBotUtils.debugMsg(ui, "Failed " + e);
        }
    }

    public int checkInfo(PBotItem item) {
        if (item.getResname().contains(emptyTrayResName)) {
            for (ItemInfo info : item.gitem.info())
                if (info instanceof ItemInfo.Contents) {
                    ItemInfo.Contents contents = (ItemInfo.Contents) info;
                    for (ItemInfo sinfo : contents.sub)
                        if (sinfo instanceof ItemInfo.Name) {
                            ItemInfo.Name nameInfo = (ItemInfo.Name) sinfo;
                            String text = nameInfo.str.text;
                            if (text.contains(curdInfo))
                                try {
                                    return (itemClick - Integer.parseInt(text.substring(0, 1)));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                        }
                }
            return (itemClick);
        }
        return (0);
    }
}
