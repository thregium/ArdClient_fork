package haven.automation;

import haven.Fightview;
import haven.GameUI;
import haven.Gob;
import haven.Resource;
import haven.UI;
import haven.purus.pbot.PBotCharacterAPI;
import haven.purus.pbot.PBotGob;
import haven.purus.pbot.PBotGobAPI;
import haven.purus.pbot.PBotUtils;
import haven.sloth.gob.AggroMark;
import haven.sloth.gob.Type;
import modification.configuration;

public class AggroAll implements Runnable {
    private final UI ui;

    public AggroAll(final UI ui) {
        this.ui = ui;
    }

    @Override
    public void run() {
        try {
            PBotCharacterAPI.doAct(ui, "atk");
            PBotCharacterAPI.doAct(ui, "aggro");
            if (configuration.waitfor(() -> {
                Resource ccurs = ui.getcurs(ui.mc);
                return (ccurs != null && ccurs.name.equals("gfx/hud/curs/atk"));
            }, 1000)) {
                for (PBotGob gob : PBotGobAPI.getGobsInRadius(ui, 190))
                    if (gob.gob.type == Type.HUMAN && !gob.gob.isplayer() && !gob.gob.isFriend() && !isInFight(gob.getGobId())) {
                        gob.doClick(1, 0);
                        PBotUtils.sleep(100);
                    }
                PBotCharacterAPI.cancelAct(ui);
            }
        } catch (Exception e) {
            System.out.println("exception when running aggroall - " + e.getClass().getName());
            e.printStackTrace();
        }
    }

    public boolean isInFight(long id) {
        GameUI gui = ui.gui;
        if (gui != null) {
            if (gui.fv != null && gui.fv.current != null) {
                Fightview fv = gui.fv;
                synchronized (fv.lsrel) {
                    for (Fightview.Relation rel : fv.lsrel) {
                        if (rel.gobid == id)
                            return (true);
                    }
                }
            }
        }
        return (false);
    }
}
