package haven.automation;


import haven.Coord;
import haven.GameUI;
import haven.WItem;
import haven.purus.pbot.PBotUtils;
import haven.purus.pbot.PBotItem;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class OysterOpener implements Runnable {
    private GameUI gui;
    int startsize = 0;

    public OysterOpener(GameUI gui) {
        this.gui = gui;
    }

    @Override
    public void run() {
        List<WItem> oysters = new ArrayList<>();
        try {
            oysters.addAll(PBotUtils.getPlayerInvContentsExact(gui.ui, "Oyster"));
            for(PBotItem item : PBotUtils.playerInventory(gui.ui).getInventoryContents()) {
                if(item.isStack()) {
                    for(PBotItem ItemInStack : item.getStackContents()) {
                        if(ItemInStack.getName().equals("Oyster")) oysters.add(ItemInStack.witem);
                    }
                }
            }
            startsize = oysters.size();
        } catch (Exception q) {
        }
        if (oysters.size() == 0) {
            PBotUtils.debugMsg(gui.ui, "No Oysters found.", Color.white);
            return;
        }
        int startid = gui.ui.next_predicted_id;
        int iterations = 0;
        for (WItem item : oysters) {
            //     FlowerMenu.setNextSelection("Crack open");
            item.item.wdgmsg("iact", Coord.z, -1);
            gui.ui.wdgmsg(startid + iterations, "cl", 0, 0);
            iterations = iterations + 2;
            //   int timeout = 0;
         /*   while(PBotUtils.getPlayerInvContentsExact("Oyster").size() == startsize) {
                timeout++;
                if(timeout > 200)
                    break;
                PBotUtils.sleep(100);
            }
            startsize--;*/
        }
        PBotUtils.debugMsg(gui.ui, "Exited OysterOpener", Color.white);
    }
}

