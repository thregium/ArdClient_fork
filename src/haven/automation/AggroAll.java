package haven.automation;

import haven.Resource;
import haven.UI;
import haven.purus.pbot.PBotCharacterAPI;
import haven.purus.pbot.PBotGob;
import haven.purus.pbot.PBotGobAPI;
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
            if (configuration.waitfor(() -> {
                Resource ccurs = ui.getcurs(ui.mc);
                return (ccurs != null && ccurs.name.equals("gfx/hud/curs/atk"));
            }, 1000)) {
                for (PBotGob gob : PBotGobAPI.getAllGobs(ui))
                    if (gob.gob.type == Type.PLAYER && !gob.gob.isplayer() && !gob.gob.isFriend())
                        gob.doClick(1, 0);
                PBotCharacterAPI.cancelAct(ui);
            }
        } catch (Exception e) {
            System.out.println("exception when running aggroall - " + e.getClass().getName());
            e.printStackTrace();
        }
    }
}
