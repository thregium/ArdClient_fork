package haven.automation;

import haven.Coord2d;
import haven.GameUI;
import haven.UI;
import haven.purus.pbot.PBotGob;
import haven.purus.pbot.PBotGobAPI;
import haven.purus.pbot.PBotUtils;
import haven.purus.pbot.PBotWindowAPI;

import java.util.Comparator;
import java.util.List;

import static haven.automation.CheeseAPI.rackResName;
import static haven.automation.CheeseAPI.rackWndName;
import static haven.automation.CheeseAPI.radius;
import static haven.automation.CheeseAPI.timeout;

public class OpenRacks implements Runnable {
    private final GameUI gui;

    public OpenRacks(GameUI gui) {
        this.gui = gui;
    }

    @Override
    public void run() {
        UI ui = gui.ui;
        PBotGob closestRack = PBotGobAPI.findGobByNames(ui, radius, rackResName);
        if (closestRack != null) {
            closestRack.doClick(3, 0);
            if (PBotWindowAPI.waitForWindow(ui, rackWndName, timeout) != null) {
                final List<PBotGob> racks = PBotGobAPI.findObjectsByNames(ui, radius, rackResName);
                racks.removeIf(closestRack::equals);
                Coord2d playerRc = PBotGobAPI.player(ui).getRcCoords();
                racks.sort(Comparator.comparingDouble(o -> playerRc.dist(o.getRcCoords())));
                racks.forEach(r -> {
                    r.doClick(3, 0);
                    if (PBotWindowAPI.getWindow(ui, rackWndName) == null)
                        PBotWindowAPI.waitForWindow(ui, rackWndName, timeout);
                });
                PBotUtils.debugMsg(ui, racks.size() + " racks opened!");
            }
        }
    }
}










