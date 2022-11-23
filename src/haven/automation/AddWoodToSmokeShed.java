package haven.automation;


import haven.Coord;
import haven.GItem;
import haven.GameUI;
import haven.Gob;
import static haven.OCache.posres;
import haven.Resource;
import haven.WItem;

public class AddWoodToSmokeShed implements Runnable {
    private GameUI gui;
    private Gob shed;
    private int count;
    private static final int TIMEOUT = 2000;
    private static final int HAND_DELAY = 8;
    private Thread Runner;

    public AddWoodToSmokeShed(GameUI gui, int count) {
        this.gui = gui;
        this.count = count;
    }


    @Override
    public void run() {
        for (Gob gob : gui.map.glob.oc.getallgobs()) {
            Resource res = gob.getres();
            if (res != null && res.name.contains("smokeshed")) {
                if (shed == null)
                    shed = gob;
                else if (gob.rc.dist(gui.map.player().rc) < shed.rc.dist(gui.map.player().rc))
                    shed = gob;
            }
        }

        if (shed == null) {
            gui.error("No sheds found");
            return;
        }

        WItem woodw = gui.maininv.getItemPartial("Block of");
        if (woodw == null) {
            gui.error("No wood blocks found in the inventory");
            return;
        }
        GItem wood = woodw.item;

        wood.wdgmsg("take", new Coord(wood.sz.x / 2, wood.sz.y / 2));
        int timeout = 0;
        while (gui.hand.isEmpty() || gui.vhand == null) {
            timeout += HAND_DELAY;
            if (timeout >= TIMEOUT) {
                gui.error("No wood found in the inventory");
                return;
            }
            try {
                Thread.sleep(HAND_DELAY);
            } catch (InterruptedException e) {
                return;
            }
        }
        wood = gui.vhand.item;

        for (; count > 0; count--) {
            gui.map.wdgmsg("itemact", Coord.z, shed.rc.floor(posres), count == 1 ? 0 : 1, 0, (int) shed.id, shed.rc.floor(posres), 0, -1);
            timeout = 0;
            while (true) {
                WItem newwood = gui.vhand;
                if (newwood != null && newwood.item != wood) {
                    wood = newwood.item;
                    break;
                } else if (newwood == null && count == 1) {
                    return;
                }

                timeout += HAND_DELAY;
                if (timeout >= TIMEOUT) {
                    gui.error("Not enough wood blocks. Need to add " + (count - 1) + " more.");
                    return;
                }
                try {
                    Thread.sleep(HAND_DELAY);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }
}

