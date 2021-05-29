package haven.automation;

import haven.Button;
import haven.Coord;
import haven.Coord2d;
import haven.FlowerMenu;
import haven.GItem;
import haven.GameUI;
import haven.Gob;
import haven.Inventory;
import haven.Label;
import haven.VMeter;
import haven.WItem;
import haven.Widget;
import haven.Window;
import haven.purus.pbot.PBotCharacterAPI;
import haven.purus.pbot.PBotGobAPI;
import haven.purus.pbot.PBotInventory;
import haven.purus.pbot.PBotItem;
import haven.purus.pbot.PBotUtils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;

import static haven.OCache.posres;

public class PepperBotUpRun extends Window implements Runnable {
    private Coord rc1, rc2;
    private ArrayList<Gob> crops = new ArrayList<>();
    private ArrayList<Gob> storages = new ArrayList<>();
    private ArrayList<Gob> tables = new ArrayList<>();
    private ArrayList<Gob> tablesblacklist = new ArrayList<Gob>();
    private boolean stopThread = false;
    private Label lblProg, lblProg2;
    private ArrayList<String> cropName = new ArrayList<String>();
    private boolean harvest = false;
    private Gob htable;
    private Window cwnd;
    public int x, y;
    private Button stopBtn;
    private Gob water, cauldron, barrel, hfire;
    private final int rowgap = 4200;
    private final int travel = 20000;
    private Coord2d retain;
    private Boolean boilmode = false;
    private Coord finalloc;

    private boolean onlyStorages = false;


    public PepperBotUpRun(ArrayList crops, ArrayList storages, ArrayList tables, boolean onlyStorages, boolean harvest, Gob barrel, Gob water, Gob cauldron, Gob hfire) {
        super(new Coord(140, 55), "Trellis Farmer");
        this.harvest = harvest;
        this.water = water;
        this.hfire = hfire;
        this.cauldron = cauldron;
        this.barrel = barrel;
        this.crops = crops;
        this.storages = storages;
        this.tables = tables;
        this.onlyStorages = onlyStorages;

        // Initialise arraylists
        cropName.add("gfx/terobjs/plants/pepper");

        Label lblstxt = new Label("Progress:");
        add(lblstxt, new Coord(15, 35));
        lblProg = new Label("Initialising...");
        add(lblProg, new Coord(70, 35));

        Label lblstxt2 = new Label("Status: ");
        add(lblstxt2, new Coord(15, 45));
        lblProg2 = new Label("Initialising...");
        add(lblProg2, new Coord(70, 45));

        stopBtn = new Button(120, "Stop") {
            @Override
            public void click() {
                stop();
            }
        };
        add(stopBtn, new Coord(10, 0));
    }

    public void run() {
        try {
            PBotUtils.sysMsg(ui, "Pepper Bot started!", Color.white);

            ui.gui.wdgmsg("act", "craft", "boiledpepper");
            PBotUtils.waitForWindow(ui, "Crafting");

            while (!onlyStorages && !this.crops.isEmpty()) {
                if (harvest) {
                    retain = barrel.rc;
                    // Initialise crop list
                    if (tables.isEmpty()) {
                        PBotUtils.sysMsg(ui, "No tables selected, stopping.", Color.white);
                        stopThread = true;
                        stop();
                        return;
                    }
                    List<Gob> crops = new ArrayList<>(this.crops);
                    // Initialize progression label on window
                    int totalCrops = crops.size();
                    int cropsHarvested = 0;
                    lblProg.settext(cropsHarvested + "/" + totalCrops);
                    for (Gob g : crops) {
                        if (stopThread) // Checks if aborted
                            break;
                        try {
                            if (PBotUtils.getEnergy(ui) < 50) {
                                List<PBotItem> porridge = PBotUtils.getInventoryItemsByName(ui.gui.maininv, "gfx/invobjs/porridge");
                                if (!porridge.isEmpty()) {
                                    porridge.get(0).witem.wdgmsg("iact", Coord.z, -1);
                                    FlowerMenu.setNextSelection("Eat");
                                    PBotUtils.sleep(2000);
                                } else {
                                    if (PBotUtils.getEnergy(ui) < 21) {
                                        PBotUtils.sysMsg(ui, "Starving and no porridge detected, stopping bot and logging out.", Color.white);
                                        stopThread = true;
                                        stop();
                                        ui.gui.logoutChar();
                                        PBotUtils.sleep(1000);
                                        return;
                                    }
                                }
                            }
                        } catch (NullPointerException qqq) {
                            //probably null pointer for a reason, stop bot.
                            PBotUtils.sysMsg(ui, "Null pointer exception trying to find food to eat, stopping bot for safety. Please tell Ardennes about this.", Color.white);
                            stopThread = true;
                            stop();
                            return;
                        }

                        // Check if stamina is under 30%, drink if needed
                        if (PBotCharacterAPI.getStamina(ui) < 60) {
                            if (stopThread)
                                break;
                            lblProg2.settext("Drinking");
                            PBotUtils.drink(ui, true);
                        }

                        if (stopThread)
                            break;

                        int stageBefore = g.getStage();

                        // Right click the crop
                        try {
                            if (stopThread)
                                break;
                            lblProg2.settext("Harvesting");
                            if (!pathTo(g)) {
                                // Update progression
                                cropsHarvested++;
                                lblProg.settext(cropsHarvested + "/" + totalCrops);
                                this.crops.remove(g);
                                continue;
                            }
                            PBotUtils.doClick(ui, g, 3, 0);
                        } catch (NullPointerException qq) {
                            PBotUtils.sysMsg(ui, "Null pointer when harvesting, ping related?", Color.white);
                        }

                        int retryharvest = 0;

                        // Wait for harvest menu to appear
                        while (!PBotUtils.petalExists(ui)) {
                            if (stopThread)
                                break;
                            retryharvest++;
                            PBotUtils.sleep(10);
                            if (retryharvest >= 500) {
                                PBotUtils.sysLogAppend(ui, "Retrying harvest", "white");
                                lblProg2.settext("Retry Harvest");
                                PBotUtils.doClick(ui, g, 3, 0);
                                retryharvest = 0;
                            }
                        }

                        // Select the harvest option
                        if (PBotUtils.petalExists(ui)) {
                            PBotUtils.choosePetal(ui, "Harvest");
                        }

                        this.crops.remove(g);

                        // Wait until stage has changed = harvested
                        while (true) {
                            if (stopThread)
                                break;
                            retryharvest++;
                            if (retryharvest >= 500) {
                                PBotUtils.sysLogAppend(ui, "Retrying harvest", "white");
                                lblProg2.settext("Retry Harvest");
                                PBotUtils.doClick(ui, g, 3, 0);
                                retryharvest = 0;
                            }
                            if (PBotUtils.findObjectById(ui, g.id) == null || PBotUtils.findObjectById(ui, g.id).getStage() != stageBefore)
                                break;
                            else
                                PBotUtils.sleep(20);
                        }

                        //boiling
                        boil(4);

                        if (boilmode) {
                            if (stopThread) // Checks if aborted
                                break;
                            lblProg2.settext("Moving to harvest");
                            boilmode = false;
                            PBotUtils.sleep(2000);
                        }
                        // Update progression
                        cropsHarvested++;
                        lblProg.settext(cropsHarvested + "/" + totalCrops);
                    }
                }
                if (stopThread)
                    break;
                ui.gui.act("travel", "hearth");
                PBotUtils.sleep(6000);
                lblProg2.settext("Moving to harvest");
            }
            while (!this.storages.isEmpty()) {
                if (harvest) {
                    retain = barrel.rc;
                    // Initialise crop list
                    if (tables.isEmpty()) {
                        PBotUtils.sysMsg(ui, "No tables selected, stopping.", Color.white);
                        stopThread = true;
                        stop();
                        return;
                    }
                    List<Gob> storages = new ArrayList<>(this.storages);
                    // Initialize progression label on window
                    int totalCrops = storages.size();
                    int cropsHarvested = 0;
                    lblProg.settext(cropsHarvested + "/" + totalCrops);
                    for (Gob g : storages) {
                        if (stopThread) // Checks if aborted
                            break;
                        try {
                            if (PBotUtils.getEnergy(ui) < 50) {
                                List<PBotItem> porridge = PBotUtils.getInventoryItemsByName(ui.gui.maininv, "gfx/invobjs/porridge");
                                if (!porridge.isEmpty()) {
                                    porridge.get(0).witem.wdgmsg("iact", Coord.z, -1);
                                    FlowerMenu.setNextSelection("Eat");
                                    PBotUtils.sleep(2000);
                                } else {
                                    if (PBotUtils.getEnergy(ui) < 21) {
                                        PBotUtils.sysMsg(ui, "Starving and no porridge detected, stopping bot and logging out.", Color.white);
                                        stopThread = true;
                                        stop();
                                        ui.gui.logoutChar();
                                        PBotUtils.sleep(1000);
                                        return;
                                    }
                                }
                            }
                        } catch (NullPointerException qqq) {
                            //probably null pointer for a reason, stop bot.
                            PBotUtils.sysMsg(ui, "Null pointer exception trying to find food to eat, stopping bot for safety. Please tell Ardennes about this.", Color.white);
                            stopThread = true;
                            stop();
                            return;
                        }

                        // Check if stamina is under 30%, drink if needed
                        if (PBotCharacterAPI.getStamina(ui) < 60) {
                            if (stopThread)
                                break;
                            lblProg2.settext("Drinking");
                            PBotUtils.drink(ui, true);
                        }

                        if (stopThread)
                            break;

                        // Right click the storage
                        try {
                            if (stopThread)
                                break;
                            lblProg2.settext("Collecting");
                            if (!PBotUtils.pfRightClick(ui, g, 0)) {
                                PBotUtils.sysMsg(ui, "Not found the path");
                                // Update progression
                                cropsHarvested++;
                                lblProg.settext(cropsHarvested + "/" + totalCrops);
                                this.storages.remove(g);
                                continue;
                            }
                        } catch (NullPointerException qq) {
                            PBotUtils.sysMsg(ui, "Null pointer when harvesting, ping related?", Color.white);
                        }

                        String windowName = PBotGobAPI.gobWindowMap.get(g.getres().name);
                        PBotUtils.waitForWindow(ui, windowName);
                        if (ui.gui.getwnd(windowName) != null) {
                            Window wnd = ui.gui.getwnd(windowName);
                            for (Widget w = wnd.lchild; w != null; w = w.prev) {
                                if (w instanceof Inventory) {
                                    waitres((Inventory) w);
                                    int freeslots = PBotUtils.playerInventory(ui).freeSlotsInv();
                                    List<PBotItem> peppers = PBotUtils.getInventoryItemsByName((Inventory) w, "gfx/invobjs/peppercorn");
                                    int min = Math.min(freeslots, peppers.size());
                                    for (int i = 0; i < min; i++) {
                                        peppers.get(i).transferItem();
                                    }
                                    PBotUtils.sleep(1000);
                                    peppers = PBotUtils.getInventoryItemsByName((Inventory) w, "gfx/invobjs/peppercorn");
                                    if (peppers.isEmpty()) {
                                        // Update progression
                                        cropsHarvested++;
                                        lblProg.settext(cropsHarvested + "/" + totalCrops);
                                        this.storages.remove(g);
                                        break;
                                    }
                                }
                            }
                            PBotUtils.closeWindow(wnd);
                        }

                        //boiling
                        boil(1);

                        if (boilmode) {
                            if (stopThread) // Checks if aborted
                                break;
                            lblProg2.settext("Moving to storages");
                            boilmode = false;
                            PBotUtils.sleep(2000);
                        }
                    }
                }
                if (stopThread)
                    break;
                ui.gui.act("travel", "hearth");
                PBotUtils.sleep(6000);
                lblProg2.settext("Moving to storages");
            }
            boil(PBotUtils.invFreeSlots(ui) + 1);
            stopThread = true;
            stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void boil(int freeslots) {
        if (PBotUtils.invFreeSlots(ui) < freeslots && !stopThread) {
            List<Gob> goblist = PBotUtils.getGobs(ui); //needed to ensure that the table overlay is correct for referencing later
            boilmode = true;
            ui.gui.act("travel", "hearth");
            PBotUtils.sleep(6000);
            while (PBotUtils.invFreeSlots(ui) < freeslots && !stopThread) {
                if (stopThread) // Checks if aborted
                    break;
                List<WItem> pepperlist = ui.gui.maininv.getItemsPartial("Peppercorn");
                if (pepperlist.isEmpty()) {
                    lblProg2.settext("Tables");
                    PBotUtils.sleep(1000);

                    while (ui.gui.maininv.getItemPartialCount("Drupe") > 0) {
                        if (stopThread) // Checks if aborted
                            break;
                        lblProg2.settext("Tables");
                        while (htable == null) {
                            if (tables.isEmpty()) {
                                PBotUtils.sysMsg(ui, "Tables is now empty for some reason, all tables full?", Color.white);
                                stopBtn.click();
                                break;
                            }

                            for (Gob tablelol : tables) {
                                for (Gob idklol : goblist)
                                    if (idklol.id == tablelol.id)
                                        tablelol = idklol;
                                if (tablelol.ols.size() != 2) {
                                    htable = tablelol;
                                    break;
                                } else {
                                    tablesblacklist.add(tablelol);
                                }
                            }
                            tables.removeAll(tablesblacklist);
                            tablesblacklist.clear();
                        }

                        if (!pathTo(htable, 14)) {
                            tables.remove(htable);
                            htable = null;
                            cwnd = null;
                            continue;
                        }
                        PBotUtils.doClick(ui, htable, 3, 0);
                        int retry = 0;
                        while (ui.gui.getwnd("Herbalist Table") == null) {
                            retry++;
                            if (retry > 500) {
                                retry = 0;
                                PBotUtils.doClick(ui, htable, 3, 0);
                            }
                            PBotUtils.sleep(10);
                        }
                        PBotUtils.sleep(100);
                        cwnd = ui.gui.getwnd("Herbalist Table");
                        PBotUtils.sleep(2000);
                        for (Widget w = ui.gui.maininv.child; w != null; w = w.next) {
                            if (w instanceof GItem && ((GItem) w).getname().contains("Pepper")) {
                                GItem item = (GItem) w;
                                try {
                                    item.wdgmsg("transfer", Coord.z);
                                } catch (NullPointerException qip) {
                                }
                            }
                        }
                        PBotUtils.sleep(1500);
                        if (!pathTo(htable, 14)) {
                            tables.remove(htable);
                            htable = null;
                            cwnd = null;
                            continue;
                        }
                        PBotUtils.doClick(ui, htable, 3, 0);
                        PBotUtils.waitForWindow(ui, "Herbalist Table");
                        if (ui.gui.getwnd("Herbalist Table") != null) {
                            cwnd = ui.gui.getwnd("Herbalist Table");
                            for (Widget w = cwnd.lchild; w != null; w = w.prev) {
                                if (w instanceof Inventory) {
                                    int drupes = PBotUtils.getInventoryContents((Inventory) w).size();
                                    if (drupes == 16) {
                                        tables.remove(htable);
                                        break;
                                    }
                                }
                            }
                        }
                        htable = null;
                        cwnd = null;
                    }
                }

                if (PBotCharacterAPI.getStamina(ui) < 60) {
                    if (stopThread)
                        break;
                    lblProg2.settext("Drinking");
                    PBotUtils.drink(ui, true);
                }

                if (PBotUtils.invFreeSlots(ui) > freeslots)
                    return;
                waitres(PBotUtils.playerInventory(ui).inv);
                List<PBotItem> peppers = PBotUtils.playerInventory(ui).getInventoryItemsByResnames("gfx/invobjs/peppercorn");
                if (peppers.isEmpty())
                    return;
                pepperlist.clear();
                lblProg2.settext("Boiling");
                PBotUtils.pfRightClick(ui, cauldron, 0);
                FlowerMenu.setNextSelection("Open");
                int tryagaintimer = 0;
                while (ui.gui.getwnd("Cauldron") == null) {
                    if (stopThread) // Checks if aborted
                        break;
                    PBotUtils.sleep(10);
                    try {
                        Thread.sleep(10);
                        tryagaintimer++;
                        if (tryagaintimer >= 500) {
                            tryagaintimer = 0;
                            PBotUtils.sysLogAppend(ui, "Retrying cauldron open", "white");
                            PBotUtils.pfRightClick(ui, cauldron, 0);
                            FlowerMenu.setNextSelection("Open");
                        }
                    } catch (InterruptedException idk) {
                    }
                }
                PBotUtils.sleep(500);
                cwnd = ui.gui.getwnd("Cauldron");
                PBotUtils.waitForWindow(ui, "Cauldron", 2000);
                PBotUtils.sleep(1000);
                VMeter vm = cwnd.getchild(VMeter.class);
                PBotUtils.craftItem(ui, "boiledpepper", 1);
                PBotUtils.sleep(2000);

                if (vm.amount < 30)
                    RefillCauldron(ui.gui);

                while (ui.gui.prog >= 0) {
                    if (stopThread) // Checks if aborted
                        break;
                    lblProg2.settext("Boiling");
                    PBotUtils.sleep(10);
                }
                if (PBotCharacterAPI.getStamina(ui) < 50) {
                    PBotUtils.craftItem(ui, "boiledpepper", 1);
                }

            }
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender == cbtn) {
            stop();
            reqdestroy();
        } else
            super.wdgmsg(sender, msg, args);
    }

    private void RefillCauldron(GameUI gui) {
        try {
            List<Gob> allgobs = PBotUtils.getGobs(ui);
            for (Gob gobz : allgobs) {
                if (gobz.id == barrel.id) {
                    barrel = gobz;
                    break;
                }
            }
        } catch (ConcurrentModificationException idklolok) {
        }

        PBotUtils.sleep(600);
        if (barrel.ols.isEmpty() && water != null) {
            lblProg2.settext("Refill Barrel");

            PBotUtils.pfRightClick(ui, barrel, 0);
            PBotUtils.sleep(1000);
            Coord2d playerCoord = PBotGobAPI.player(ui).getRcCoords();
            PBotUtils.liftGob(ui, barrel);
            PBotUtils.sleep(1000);
            PBotUtils.pfRightClick(ui, water, 0);
            PBotUtils.sleep(1000);
            pathTo(cauldron);
            PBotUtils.doClick(ui, cauldron, 3, 0);
            PBotUtils.sleep(1000);

            pathTo(playerCoord, 0);
            PBotUtils.mapClick(ui, retain.x, retain.y, 3, 0);
            PBotUtils.sleep(1000);

            FlowerMenu.setNextSelection("Open");
            PBotUtils.pfRightClick(ui, cauldron, 0);
            PBotUtils.sleep(2000);
            PBotUtils.craftItem(ui, "boiledpepper", 1);
            PBotUtils.sleep(2000);
        } else {
            lblProg2.settext("Refill Cauldron");

            PBotUtils.pfRightClick(ui, barrel, 0);
            PBotUtils.sleep(1000);
            Coord2d playerCoord = PBotGobAPI.player(ui).getRcCoords();
            PBotUtils.liftGob(ui, barrel);
            PBotUtils.sleep(1000);
            pathTo(cauldron);
            PBotUtils.doClick(ui, cauldron, 3, 0);
            PBotUtils.sleep(1000);

            pathTo(playerCoord, 0);
            PBotUtils.mapClick(ui, retain.x, retain.y, 3, 0);
            PBotUtils.sleep(1000);

            FlowerMenu.setNextSelection("Open");
            PBotUtils.pfRightClick(ui, cauldron, 0);
            PBotUtils.sleep(2000);
            PBotUtils.craftItem(ui, "boiledpepper", 1);
            PBotUtils.sleep(2000);
        }
    }

    public boolean pathTo(Gob g) {
        Coord2d gCoord = g.rc;

        lblProg2.settext("Find path");
        for (Coord2d c2d : near(gCoord)) {
            if (PBotUtils.pfLeftClick(ui, c2d.x, c2d.y)) {
                PBotUtils.mapClick(ui, c2d, 1, 0);
                return (waitmove(2000, c2d));
            }
        }

        return false;
    }

    public boolean pathTo(Gob g, double offset) {
        Coord2d gCoord = g.rc;

        lblProg2.settext("Find path");
        for (Coord2d c2d : near(gCoord, offset)) {
            if (PBotUtils.pfLeftClick(ui, c2d.x, c2d.y)) {
                PBotUtils.mapClick(ui, c2d, 1, 0);
                return (waitmove(2000, c2d));
            }
        }

        return false;
    }

    public boolean pathTo(Coord2d gCoord, double offset) {

        lblProg2.settext("Find path");
        for (Coord2d c2d : near(gCoord, offset)) {
            if (PBotUtils.pfLeftClick(ui, c2d.x, c2d.y)) {
                PBotUtils.mapClick(ui, c2d, 1, 0);
                return (waitmove(2000, c2d));
            }
        }

        return false;
    }

    public List<Coord2d> near(Coord2d coord2d) {
        return near(coord2d, 11);
    }

    public List<Coord2d> near(Coord2d coord2d, double distance) {
        List<Coord2d> coord2ds = new ArrayList<>();
        coord2ds.add(new Coord2d(coord2d.x + distance, coord2d.y));
        coord2ds.add(new Coord2d(coord2d.x - distance, coord2d.y));
        coord2ds.add(new Coord2d(coord2d.x, coord2d.y + distance));
        coord2ds.add(new Coord2d(coord2d.x, coord2d.y - distance));

        coord2ds.sort(Comparator.comparingDouble(a -> PBotGobAPI.player(ui).getRcCoords().dist(a)));

        return coord2ds;
    }

    public boolean waitmove(int time, Coord2d c2d) {
        for (int i = 0, sleep = 10; i < time; i += sleep) {
            Coord2d playerrc = PBotGobAPI.player(ui).getRcCoords();
            if (c2d.x - 1 < playerrc.x && c2d.x + 1 > playerrc.x && c2d.y - 1 < playerrc.y && c2d.y + 1 > playerrc.y)
                return true;
            PBotUtils.sleep(sleep);
        }
        return false;
    }

    public void stop() {
        // Stops thread
        PBotUtils.sysMsg(ui, "Trellis Farmer stopped!", Color.white);
        ui.gui.map.wdgmsg("click", Coord.z, ui.gui.map.player().rc.floor(posres), 1, 0);
        if (ui.gui.map.pfthread != null) {
            ui.gui.map.pfthread.interrupt();
        }
        stopThread = true;
        harvest = false;
        this.destroy();
    }

    public void waitres(Inventory inv) {
        List<PBotItem> a = new PBotInventory(inv).getInventoryContents();
        for (PBotItem i : a) {
            while (i.getResname() == null) {
                PBotUtils.sleep(100);
            }
        }
    }
}
