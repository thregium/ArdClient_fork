package haven.automation;

import haven.Button;
import haven.CheckBox;
import haven.Coord;
import haven.Coord2d;
import haven.FastMesh;
import haven.Gob;
import haven.Inventory;
import haven.Label;
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
import java.util.List;
import java.util.Random;

import static haven.OCache.posres;

public class PepperGrinderUpRun extends Window implements Runnable {
    private Coord rc1, rc2;
    private ArrayList<Gob> crops = new ArrayList<Gob>();
    private ArrayList<Gob> tables = new ArrayList<Gob>();
    private ArrayList<Gob> storages = new ArrayList<>();
    public ArrayList<Gob> blacklist = new ArrayList<Gob>();
    private boolean stopThread = false;
    private Label lblProg;
    private ArrayList<String> cropName = new ArrayList<String>();
    private ArrayList<String> seedName = new ArrayList<String>();
    private String trellis = "gfx/terobjs/plants/trellis";
    private boolean harvest = false;
    private boolean destroy = false;
    public Gob htable;
    private boolean replant = false;
    private static final int TIMEOUT = 1000;
    public Button stopBtn;
    public int x, y;
    private Gob chest, water, rowmarker, cauldron, barrel, hfire, grinder;
    private final int rowgap = 4200;
    private final int travel = 20000;
    public boolean onlyStorages = false;
    private CheckBox pftype = new CheckBox("Path purus/sloth");

    private static final List<String> storagesTypes = new ArrayList<String>() {{
        add("gfx/terobjs/woodbox");
        add("gfx/terobjs/create");
        add("gfx/terobjs/wbasket");
        add("gfx/terobjs/cupboard");
        add("gfx/terobjs/chest");
        add("gfx/terobjs/largechest");
        add("gfx/terobjs/matalcabinet");
    }};

    public Widget craftall;
    private Boolean boilmode = false;
    private Coord finalloc;
    private Thread t;

    public PepperGrinderUpRun(Coord rc1, Coord rc2, Gob grinder, boolean onlyStorages) {
        super(new Coord(120, 75), "Pepper Grinder");
        this.grinder = grinder;
        this.rc1 = rc1;
        this.rc2 = rc2;
        this.onlyStorages = onlyStorages;

        // Initialise arraylists

        lblProg = new Label("Initialising...");
        add(lblProg, new Coord(15, 35));

        stopBtn = new Button(120, "Stop") {
            @Override
            public void click() {
                stopThread = true;
                stop();
            }
        };
        add(stopBtn, new Coord(0, 0));
        add(pftype, new Coord(0, 55));
    }

    public void run() {
        try {
            tables = Tables();
            storages = Storages();
            PBotUtils.sysMsg(ui, "Pepper Grinder Bot started! Tables selected : " + tables.size(), Color.white);

            ui.gui.wdgmsg("act", "craft", "blackpepper");
            PBotUtils.waitForWindow(ui, "Crafting");

            if (stopThread) // Checks if aborted
                return;

            while (!onlyStorages && !tables.isEmpty() && !stopThread) {
                if (PBotCharacterAPI.getStamina(ui) <= 60) {
                    lblProg.settext("Drinking");
                    PBotUtils.drink(ui, true);
                    PBotUtils.sleep(5000);
                }

                if (stopThread)
                    return;

                int finishtimeout = 0;
                while (PBotUtils.invFreeSlots(ui) > 2 && !stopThread) {
                    if (stopThread)
                        return;
                    finishtimeout++;
                    if (finishtimeout > 10000) {
                        stopThread = true;
                        return;
                    }
                    lblProg.settext("Status - Collecting");
                    while (!tables.isEmpty() && htable == null && !stopThread) {
                        finishtimeout++;
                        if (finishtimeout > 10000) {
                            stopThread = true;
                            return;
                        }
                        for (Gob tablelol : tables) {
                            if (!tablelol.ols.isEmpty())
                                htable = tablelol;
                            else
                                blacklist.add(tablelol);
                        }
                    }
                    if (tables.isEmpty())
                        break;
                    if (stopThread)
                        return;
                    tables.removeAll(blacklist);
                    blacklist.clear();

                    if (!pathTo(htable, 14)) {
                        tables.remove(htable);
                        continue;
                    }
                    PBotUtils.doClick(ui, htable, 3, 0);
                    PBotUtils.sleep(1000);
                    int retrytimer = 0;
                    int retrycount = 0;
                    while (ui.gui.getwnd("Herbalist Table") == null && !stopThread) {
                        retrytimer++;
                        if (retrytimer > 1000) {
                            retrytimer = 0;
                            retrycount++;
                            if (retrycount > 1) {
                                lblProg.settext("Unstucking");
                                Gob player = ui.gui.map.player();
                                Coord location = player.rc.floor(posres);
                                int x = location.x + +getrandom();
                                int y = location.y + +getrandom();
                                Coord finalloc = new Coord(x, y);
                                ui.gui.map.wdgmsg("click", Coord.z, finalloc, 1, 0);
                                retrycount = 0;
                                PBotUtils.sleep(1000);
                            }
                            pfRight(htable, 0);
                        }
                        PBotUtils.sleep(10);
                    }
                    PBotUtils.waitForWindow(ui, "Herbalist Table");
                    Window herbtable = ui.gui.getwnd("Herbalist Table");
                    if (herbtable == null)
                        continue;
                    int freeslots;
                    for (Widget w = herbtable.lchild; w != null; w = w.prev) {
                        if (w instanceof Inventory) {
                            Inventory inv = (Inventory) w;
                            List<PBotItem> items = new PBotInventory(inv).getInventoryItemsByResnames(".*pepperdrupedried");
                            for (PBotItem item : items) {
                                freeslots = PBotUtils.invFreeSlots(ui);
                                if (freeslots > 16) {
                                    System.out.println("Transferring pepper freeslots : " + freeslots);
                                    item.gitem.wdgmsg("transfer", Coord.z);
                                } else if (freeslots > 2) {
                                    System.out.println("Transferring pepper freeslots : " + freeslots);
                                    item.gitem.wdgmsg("transfer", Coord.z);
                                    PBotUtils.sleep(300);
                                } else
                                    break;
                            }
                            items = new PBotInventory(inv).getInventoryItemsByResnames(".*pepperdrupedried");
                            if (items.isEmpty())
                                tables.remove(htable);
                        }
                    }
                    herbtable.close();
                    htable = null;
                }
                if (PBotUtils.invFreeSlots(ui) <= 2) {
                    waitres(PBotUtils.playerInventory(ui));
                    List<PBotItem> peppers = PBotUtils.playerInventory(ui).getInventoryItemsByResnames(".*pepperdrupedried");
                    if (!peppers.isEmpty()) {
                        lblProg.settext("Status - Going to Grind");
                        pathTo(grinder);
                        PBotUtils.doClick(ui, grinder, 3, 0);
                        PBotUtils.sleep(1000); //sleep 6 seconds to walk to grinder
                        int timeout = 0;
                        int retrycount = 0;
                        peppers = PBotUtils.playerInventory(ui).getInventoryItemsByResnames(".*pepperdrupedried");
                        while (peppers.size() > 5 && !stopThread) {
                            timeout++;
                            if (timeout > 5000) {
                                ui.gui.maininv.getItemPartial("Dried").item.wdgmsg("drop", Coord.z);
                                timeout = 0;
                            }
                            while (ui.gui.prog >= 0 && !stopThread) {
                                PBotUtils.sleep(100);
                                lblProg.settext("Status - Grinding");
                            }
                            if (PBotUtils.getStamina(ui) > 50) {
                                PBotUtils.craftItem(ui, "blackpepper", 1);
                                PBotUtils.sleep(2000);
                                retrycount++;
                                if (retrycount > 1) {
                                    lblProg.settext("Unstucking");
                                    Gob player = ui.gui.map.player();
                                    Coord location = player.rc.floor(posres);
                                    int x = location.x + +getrandom();
                                    int y = location.y + +getrandom();
                                    Coord finalloc = new Coord(x, y);
                                    ui.gui.map.wdgmsg("click", Coord.z, finalloc, 1, 0);
                                    retrycount = 0;
                                    PBotUtils.sleep(1000);
                                    pathTo(grinder);
                                    PBotUtils.doClick(ui, grinder, 3, 0);
                                }
                            } else {
                                lblProg.settext("Status - Drinking");
                                PBotUtils.drink(ui, true);
                                PBotUtils.sleep(5000);
                            }
                            peppers = PBotUtils.playerInventory(ui).getInventoryItemsByResnames(".*pepperdrupedried");
                        }
                    }
                }
            }

            while (!this.storages.isEmpty() && !stopThread) {
                List<Gob> storages = new ArrayList<>(this.storages);
                // Initialize progression label on window
                lblProg.settext("Status - Collecting");
                for (Gob g : storages) {
                    if (stopThread) // Checks if aborted
                        break;

                    // Check if stamina is under 30%, drink if needed
                    if (PBotCharacterAPI.getStamina(ui) < 60) {
                        if (stopThread)
                            break;
                        lblProg.settext("Drinking");
                        PBotUtils.drink(ui, true);
                    }

                    if (stopThread)
                        stop();

                    // Right click the storage
                    try {
                        if (stopThread)
                            break;
                        lblProg.settext("Collecting");
                        if (!pfRight(g, 0)) {
                            PBotUtils.sysMsg(ui, "Not found the path");
                            // Update progression
                            lblProg.settext("Status - Collecting");
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
                                List<PBotItem> peppers = PBotUtils.getInventoryItemsByName((Inventory) w, "gfx/invobjs/pepperdrupedried");
                                int min = Math.min(freeslots, peppers.size());
                                for (int i = 0; i < min; i++) {
                                    peppers.get(i).transferItem();
                                }
                                PBotUtils.sleep(1000);
                                peppers = PBotUtils.getInventoryItemsByName((Inventory) w, "gfx/invobjs/pepperdrupedried");
                                if (peppers.isEmpty()) {
                                    // Update progression
                                    lblProg.settext("Status - Collecting");
                                    this.storages.remove(g);
                                    break;
                                }
                            }
                        }
                        PBotUtils.closeWindow(wnd);
                    }
                    if (PBotUtils.invFreeSlots(ui) <= 2) {
                        waitres(PBotUtils.playerInventory(ui));
                        List<PBotItem> peppers = PBotUtils.playerInventory(ui).getInventoryItemsByResnames(".*pepperdrupedried");
                        if (!peppers.isEmpty()) {
                            lblProg.settext("Status - Going to Grind");
                            pathTo(grinder);
                            PBotUtils.doClick(ui, grinder, 3, 0);
                            PBotUtils.sleep(1000); //sleep 6 seconds to walk to grinder
                            int timeout = 0;
                            int retrycount = 0;
                            peppers = PBotUtils.playerInventory(ui).getInventoryItemsByResnames(".*pepperdrupedried");
                            while (peppers.size() > 5 && !stopThread) {
                                timeout++;
                                if (timeout > 5000 && !stopThread) {
                                    ui.gui.maininv.getItemPartial("Dried").item.wdgmsg("drop", Coord.z);
                                    timeout = 0;
                                }
                                while (ui.gui.prog >= 0 && !stopThread) {
                                    PBotUtils.sleep(100);
                                    lblProg.settext("Status - Grinding");
                                }
                                if (PBotUtils.getStamina(ui) > 50) {
                                    PBotUtils.craftItem(ui, "blackpepper", 1);
                                    PBotUtils.sleep(2000);
                                    retrycount++;
                                    if (retrycount > 1) {
                                        lblProg.settext("Unstucking");
                                        Gob player = ui.gui.map.player();
                                        Coord location = player.rc.floor(posres);
                                        int x = location.x + +getrandom();
                                        int y = location.y + +getrandom();
                                        Coord finalloc = new Coord(x, y);
                                        ui.gui.map.wdgmsg("click", Coord.z, finalloc, 1, 0);
                                        retrycount = 0;
                                        PBotUtils.sleep(1000);
                                        pathTo(grinder);
                                        PBotUtils.doClick(ui, grinder, 3, 0);
                                    }
                                } else {
                                    lblProg.settext("Status - Drinking");
                                    PBotUtils.drink(ui, true);
                                    PBotUtils.sleep(5000);
                                }
                                peppers = PBotUtils.playerInventory(ui).getInventoryItemsByResnames(".*pepperdrupedried");
                            }
                        }
                    }
                }
                if (stopThread)
                    break;
            }
            if (stopThread)
                return;
        } catch (Exception e) {
            e.printStackTrace();
        }
        PBotUtils.sysMsg(ui, "Done", Color.white);
        stopThread = true;
        stop();
    }

    public int getrandom() {
        Random r = new Random();
        int randomNumber = r.ints(1, -6000, 6000).findFirst().getAsInt();
        return randomNumber;
    }


    public ArrayList<Gob> Crops(boolean checkStage) {
        // Initialises list of crops to harvest between selected coordinates
        ArrayList<Gob> gobs = new ArrayList<Gob>();
        double bigX = Math.max(rc1.x, rc2.x);
        double smallX = Math.min(rc1.x, rc2.x);
        double bigY = Math.max(rc1.y, rc2.y);
        double smallY = Math.min(rc1.y, rc2.y);
        synchronized (ui.sess.glob.oc) {
            for (Gob gob : ui.sess.glob.oc) {
                if (gob.rc.x <= bigX && gob.rc.x >= smallX && gob.getres() != null && gob.rc.y <= bigY
                        && gob.rc.y >= smallY && cropName.contains(gob.getres().name)) {
                    // Add to list if its max stage
                    if (checkStage) {
                        int cropstgmaxval = 0;
                        for (FastMesh.MeshRes layer : gob.getres().layers(FastMesh.MeshRes.class)) {
                            int stg = layer.id / 10;
                            if (stg > cropstgmaxval)
                                cropstgmaxval = stg;
                        }
                        if (gob.getStage() == cropstgmaxval) {
                            gobs.add(gob);
                        }
                    } else
                        gobs.add(gob);
                }
            }
        }
        gobs.sort(new CoordSortEW());
        return gobs;
    }

    public ArrayList<Gob> Tables() {
        // Initialises list of crops to harvest between selected coordinates
        ArrayList<Gob> gobs = new ArrayList<Gob>();
        double bigX = Math.max(rc1.x, rc2.x);
        double smallX = Math.min(rc1.x, rc2.x);
        double bigY = Math.max(rc1.y, rc2.y);
        double smallY = Math.min(rc1.y, rc2.y);
        synchronized (ui.sess.glob.oc) {
            for (Gob gob : ui.sess.glob.oc) {
                if (gob.rc.x <= bigX && gob.rc.x >= smallX && gob.getres() != null && gob.rc.y <= bigY
                        && gob.rc.y >= smallY && gob.getres().basename().contains("htable")) {
                    gobs.add(gob);
                }
            }
        }
        gobs.sort(new CoordSortNS());
        return gobs;
    }

    public ArrayList<Gob> Storages() {
        // Initialises list of crops to harvest between selected coordinates
        ArrayList<Gob> gobs = new ArrayList<Gob>();
        double bigX = Math.max(rc1.x, rc2.x);
        double smallX = Math.min(rc1.x, rc2.x);
        double bigY = Math.max(rc1.y, rc2.y);
        double smallY = Math.min(rc1.y, rc2.y);
        synchronized (ui.sess.glob.oc) {
            for (Gob gob : ui.sess.glob.oc) {
                if (gob.rc.x <= bigX && gob.rc.x >= smallX && gob.getres() != null && gob.rc.y <= bigY
                        && gob.rc.y >= smallY && storagesTypes.contains(gob.getres().name)) {
                    // Add to list if its max stage
                    gobs.add(gob);
                }
            }
        }
        gobs.sort(new CoordSortEW());

        return gobs;
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender == cbtn) {
            stop();
            reqdestroy();
        } else
            super.wdgmsg(sender, msg, args);
    }

    // Sorts coordinate array to efficient sequence
    class CoordSortEW implements Comparator<Gob> { // sorts high Y to low Y along same X Axis
        public int compare(Gob a, Gob b) {
            if (a.rc.x == b.rc.x) {
                if (a.rc.x % 2 == 0)
                    return Double.compare(b.rc.y, a.rc.y);
                else
                    return Double.compare(a.rc.y, b.rc.y);
            } else
                return Double.compare(a.rc.x, b.rc.x);
        }
    }

    class CoordSortNS implements Comparator<Gob> { // sorts high X to low X along the same Y Axis
        public int compare(Gob a, Gob b) {
            if (a.rc.y == b.rc.y) {
                if (a.rc.y % 2 == 0)
                    return Double.compare(b.rc.x, a.rc.x);
                else
                    return Double.compare(a.rc.x, b.rc.x);
            } else
                return Double.compare(a.rc.y, b.rc.y);
        }
    }

    public boolean pathTo(Gob g) {
        Coord2d gCoord = g.rc;

        lblProg.settext("Find path");
        for (Coord2d c2d : near(gCoord)) {
            if (pfLeft(c2d)) {
                PBotUtils.mapClick(ui, c2d, 1, 0);
                return (waitmove(2000, c2d));
            }
        }

        return false;
    }

    public boolean pathTo(Gob g, double offset) {
        Coord2d gCoord = g.rc;

        lblProg.settext("Find path");
        for (Coord2d c2d : near(gCoord, offset)) {
            if (pfLeft(c2d)) {
                PBotUtils.mapClick(ui, c2d, 1, 0);
                return (waitmove(2000, c2d));
            }
        }

        return false;
    }

    public boolean pathTo(Coord2d gCoord, double offset) {

        lblProg.settext("Find path");
        for (Coord2d c2d : near(gCoord, offset)) {
            if (pfLeft(c2d)) {
                PBotUtils.mapClick(ui, c2d, 1, 0);
                return (waitmove(2000, c2d));
            }
        }

        return false;
    }

    public boolean pfRight(Gob g, int mod) {
        return (pftype.a ? PBotUtils.PathfinderRightClick(ui, g, mod) : PBotUtils.pfRightClick(ui, g, mod));
    }

    public boolean pfLeft(Coord2d c2d) {
        return (pftype.a ? PBotUtils.pfmove(ui, c2d.x, c2d.y) : PBotUtils.pfLeftClick(ui, c2d.x, c2d.y));
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
        for (int i = 0, sleep = 10; i < time && !stopThread; i += sleep) {
            Coord2d playerrc = PBotGobAPI.player(ui).getRcCoords();
            if (c2d.x - 1 < playerrc.x && c2d.x + 1 > playerrc.x && c2d.y - 1 < playerrc.y && c2d.y + 1 > playerrc.y)
                return true;
            PBotUtils.sleep(sleep);
        }
        return false;
    }

    public void waitres(Inventory inv) {
        lblProg.settext("Resource debug");
        List<PBotItem> a = new PBotInventory(inv).getInventoryContents();
        for (PBotItem i : a) {
            while (i.getResname() == null && !stopThread) {
                PBotUtils.sleep(100);
            }
        }
    }

    public void waitres(PBotInventory inv) {
        lblProg.settext("Resource debug");
        List<PBotItem> a = inv.getInventoryContents();
        for (PBotItem i : a) {
            while (i.getResname() == null && !stopThread) {
                PBotUtils.sleep(100);
            }
        }
    }


    public void stop() {
        // Stops thread
        PBotUtils.sysMsg(ui, "Pepper Grinder Stopped!", Color.white);
        //ui.gui.map.wdgmsg("click", Coord.z, ui.gui.map.player().rc.floor(posres), 1, 0);
        if (ui.gui.map.pfthread != null) {
            ui.gui.map.pfthread.interrupt();
        }
        if (ui.gui.map.pastaPathfinder != null) {
            ui.gui.map.pastaPathfinder.interrupt();
        }
        stopThread = true;
        this.destroy();
    }

    @Override
    public void close() {
        stopThread = true;
        stop();
    }
}
