package haven.automation;

import haven.UI;
import haven.purus.pbot.PBotCharacterAPI;
import haven.purus.pbot.PBotGob;
import haven.purus.pbot.PBotGobAPI;
import haven.sloth.gob.Type;

public class AggroAll implements Runnable {
    private final UI ui;

    public AggroAll(final UI ui) {
        this.ui = ui;
    }

    @Override
    public void run() {
        try {
            PBotCharacterAPI.doAct(ui, "atk");
            for (PBotGob gob : PBotGobAPI.getAllGobs(ui))
                if (gob.gob.type == Type.PLAYER && !gob.gob.isplayer() && !gob.gob.isFriend())
                    gob.doClick(1, 0);
            PBotCharacterAPI.cancelAct(ui);
        } catch (Exception e) {
            System.out.println("exception when running aggroall - " + e.getClass().getName());
            e.printStackTrace();
        }
    }
}
