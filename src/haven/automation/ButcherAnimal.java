package haven.automation;


import haven.Coord;
import haven.GameUI;
import haven.WItem;
import haven.purus.pbot.PBotUtils;
import haven.purus.pbot.PBotItem;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ButcherAnimal implements Runnable {
    private GameUI gui;
    int startsize = 0;

    public ButcherAnimal(GameUI gui) {
        this.gui = gui;
    }

    @Override
    public void run() {
        List<WItem> animals = new ArrayList<>();
        List<String> livinganimals = new ArrayList<>();
        livinganimals.add("gfx/invobjs/squirrel");
        livinganimals.add("gfx/invobjs/rooster");
        livinganimals.add("gfx/invobjs/hen");
        livinganimals.add("gfx/invobjs/rabbit");
        livinganimals.add("gfx/invobjs/rabbit-doe");
        livinganimals.add("gfx/invobjs/hedgehog");
        livinganimals.add("gfx/invobjs/mole");
        livinganimals.add("gfx/invobjs/bogturtle");
        livinganimals.add("gfx/invobjs/swan");
        livinganimals.add("gfx/invobjs/rockdove");
        livinganimals.add("gfx/invobjs/magpie");
        livinganimals.add("gfx/invobjs/woodgrouse-m");
        livinganimals.add("gfx/invobjs/woodgrouse-f");
        livinganimals.add("gfx/invobjs/bullfinch-m");
        livinganimals.add("gfx/invobjs/bullfinch-f");
        livinganimals.add("gfx/invobjs/quail");
        livinganimals.add("gfx/invobjs/pelican");
        livinganimals.add("gfx/invobjs/seagull");
        livinganimals.add("gfx/invobjs/mallard-m");
        livinganimals.add("gfx/invobjs/mallard-f");
        try {
            for(PBotItem items : PBotUtils.playerInventory(gui.ui).getInventoryItemsByResnames(livinganimals)) {
                animals.add(items.witem);
            }
            for(PBotItem items : PBotUtils.playerInventory(gui.ui).getInventoryItemsByResnames(".*dead")) {
                animals.add(items.witem);
            }
            for(PBotItem items : PBotUtils.playerInventory(gui.ui).getInventoryItemsByResnames(".*carcass")) {
                animals.add(items.witem);
            }
            for(PBotItem items : PBotUtils.playerInventory(gui.ui).getInventoryItemsByResnames(".*clean")) {
                animals.add(items.witem);
            }

            //animals.addAll(PBotUtils.getPlayerInvContentsExact(gui.ui, "Squirrel"));
            startsize = animals.size();
        } catch (Exception q) {
        }
        if (animals.size() == 0) {
            PBotUtils.debugMsg(gui.ui, "No Animal found.", Color.white);
            return;
        }
        int startid = gui.ui.next_predicted_id;
        int iterations = 0;
        for (WItem item : animals) {
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
        PBotUtils.debugMsg(gui.ui, "Exited Butcher Small Animal", Color.white);
    }
}

