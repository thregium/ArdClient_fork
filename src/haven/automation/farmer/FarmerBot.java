package haven.automation.farmer;


import haven.Button;
import haven.Coord;
import haven.GItem;
import haven.Gob;
import haven.GobHighlight;
import haven.Label;
import haven.Loading;
import haven.Resource;
import haven.Sprite;
import haven.Widget;
import haven.Window;
import haven.purus.pbot.PBotCharacterAPI;
import haven.purus.pbot.PBotInventory;
import haven.purus.pbot.PBotItem;
import haven.purus.pbot.PBotUtils;
import haven.purus.pbot.PBotWindowAPI;
import haven.res.ui.tt.q.qbuff.QBuff;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static haven.OCache.posres;

public class FarmerBot extends Window {
    public Kit current;
    public String windowName;

    public Label lblProg, lblProg2, lblhighestq;
    public int cropsHarvested;
    private double highestquality;
    public Thread runner;
    public Button stopBtn;
    public List<Gob> blacklist = new ArrayList<>();
    public Gob g;
    private boolean stopThread = false;
    private Set<String> plants = new HashSet<>(5);

    private static List<Kit> kits = new ArrayList<>();

    public static class Kit {
        public String fullname;
        public String name;
        public String terobjs;
        public String invobjs;
        public boolean isSeed;
        public int stage;
        public List<String> adds = new ArrayList<>();

        public Kit(String textname) {
            this.name = shortName(textname);
            this.fullname = firstUpper(name);

            this.isSeed = textname.contains("seed");
            this.terobjs = "gfx/terobjs/plants/" + name;
            this.invobjs = "gfx/invobjs/" + (isSeed ? "seed-" : "") + name;

            this.stage = getStage(textname);
//            this.adds; //need items like hemp-fresh
        }

        public int getStage(String textname) {
            switch (textname) {
                case "seed-turnip":
                    return 1;
                case "yellowonion":
                case "redonion":
                case "beetroot":
                case "turnip":
                case "seed-barley":
                case "seed-wheat":
                case "seed-millet":
                case "seed-flax":
                case "seed-carrot":
                case "hemp-fresh":
                    return 3;
                case "carrot":
                case "leek":
                case "seed-poppy":
                case "seed-pipeweed":
                case "seed-lettuce":
                case "seed-hemp":
                case "seed-pumpkin":
                    return 4;
                default:
                    return -1;
            }
        }
    }

    public static String shortName(String text) {
        return text.contains("seed") ? text.replace("seed-", "") : text;
    }

    public static String firstUpper(String text) {
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    public FarmerBot(String name) {
        super(new Coord(140, 95), firstUpper(shortName(name)) + " Farmer Bot", firstUpper(shortName(name)) + " Farmer Bot");
        windowName = firstUpper(shortName(name)) + " FarmerBots";
        current = new Kit(name);
    }

    public void added() {
        plants.add(current.terobjs);
        cropsHarvested = 0;
        Label lblstxt = new Label("Progress:");
        add(lblstxt, new Coord(15, 35));
        lblProg = new Label("Initialising...");
        add(lblProg, new Coord(15, 45));
        Label lblstxt2 = new Label("Status:");
        add(lblstxt2, new Coord(15, 55));
        lblProg2 = new Label("Initialising...");
        add(lblProg2, new Coord(15, 65));
        Label lblhighest = new Label("Top Q:");
        add(lblhighest, new Coord(15, 75));
        lblhighestq = new Label("Initialising...");
        add(lblhighestq, new Coord(15, 85));


        stopBtn = new Button(120, "Stop") {
            @Override
            public void click() {
                stop();
            }
        };
        add(stopBtn, new Coord(0, 0));
        runner = new Thread(new runner(), windowName);
    }

    private class runner implements Runnable {
        @Override
        public void run() {
            highestquality = 0;
            PBotUtils.debugMsg(ui, current.fullname + " Bot Started!", Color.white);
            lblProg.settext(cropsHarvested + " Units Harvested");
            lblProg2.settext(cropsHarvested + "Starting");
            while (!stopThread || PBotWindowAPI.getWindow(ui, windowName) != null) {
                try {
                    Gob player = PBotUtils.player(ui);
                    if (player != null) {
                        lblProg.settext(cropsHarvested + " Units Harvested");
                        if (PBotCharacterAPI.getStamina(ui) <= 30) {
                            lblProg2.settext("Drinking");
                            PBotUtils.drink(ui, true);
                        }

                        if (stopThread || PBotWindowAPI.getWindow(ui, windowName) == null)
                            return;

                        while (PBotUtils.findNearestStageCrop(ui, 5000, current.stage, current.terobjs) == null) {
                            if (stopThread || PBotWindowAPI.getWindow(ui, windowName) == null)
                                return;
                            lblProg2.settext("No " + current.fullname);
                            PBotUtils.sleep(200);
                        }
                        while (g == null) {
                            lblProg2.settext("Found " + current.fullname);
                            if (stopThread || PBotWindowAPI.getWindow(ui, windowName) == null)
                                return;
                            g = PBotUtils.findNearestStageCrop(ui, 5000, current.stage, current.terobjs);
                        }

                        PBotUtils.doClick(ui, g, 1, 0);

                        int retryharvest = 0;
                        int retrycount = 0;
                        PBotUtils.mapClick(ui, g.rc, 1, 0);
                        while (player.rc.x != g.rc.x || player.rc.y != g.rc.y) {
                            if (stopThread || PBotWindowAPI.getWindow(ui, windowName) == null)
                                return;
                            if (!PBotUtils.isMoving(ui))
                                retryharvest++;
                            lblProg2.settext("Moving to Crop");
                            if (retryharvest >= 500) {
                                lblProg2.settext("Retry Movement");
                                PBotUtils.sysLogAppend(ui, "Moving char in move loop", "white");
                                Coord location = player.rc.floor(posres);
                                int x = location.x + getrandom();
                                int y = location.y + getrandom();
                                Coord finalloc = new Coord(x, y);
                                PBotUtils.mapFinalClick(ui, finalloc, 1, 0);
                                retryharvest = 0;
                                PBotUtils.sleep(1000);
                                PBotUtils.mapClick(ui, g.rc, 1, 0);
                            }
                            PBotUtils.sleep(10);
                        }
                        lblProg2.settext("Harvesting");
                        try {
                            PBotUtils.pfRightClick(ui, g, 0);
                        } catch (NullPointerException qq) {
                            PBotUtils.sysLogAppend(ui, current.fullname + " I found is now null, weird. Retrying.", "white");
                            g = null;
                            while (PBotUtils.findNearestStageCrop(ui, 5000, current.stage, current.terobjs) == null)
                                PBotUtils.sleep(10);
                            g = PBotUtils.findNearestStageCrop(ui, 5000, current.stage, current.terobjs);
                            PBotUtils.pfRightClick(ui, g, 0);
                        }

                        // Wait for harvest menu to appear and harvest the crop
                        while (!PBotUtils.petalExists(ui)) {
                            lblProg2.settext("Waiting for Flowermenu");
                            if (stopThread || PBotWindowAPI.getWindow(ui, windowName) == null)
                                return;
                            retryharvest++;
                            PBotUtils.sleep(10);
                            if (PBotUtils.getItemAtHand(ui) != null) {
                                Coord slot = PBotUtils.getFreeInvSlot(ui.gui.maininv);
                                PBotUtils.dropItemToInventory(slot, ui.gui.maininv);
                                while (PBotUtils.getItemAtHand(ui) != null)
                                    PBotUtils.sleep(50);
                            }
                            if (retryharvest >= 500) {
                                lblProg2.settext("Retry Harvest");
                                if (retrycount >= 3) {
                                    lblProg2.settext("Unstucking");
                                    PBotUtils.sysLogAppend(ui, "Moving char", "white");
                                    Coord location = player.rc.floor(posres);
                                    int x = location.x + getrandom();
                                    int y = location.y + getrandom();
                                    Coord finalloc = new Coord(x, y);
                                    PBotUtils.mapFinalClick(ui, finalloc, 1, 0);
                                    retrycount = 0;
                                    PBotUtils.sleep(1000);
                                    PBotUtils.pfRightClick(ui, g, 0);
                                }
                                PBotUtils.sysLogAppend(ui, "Retrying harvest", "white");
                                lblProg2.settext("Retrying Harvest");
                                try {
                                    PBotUtils.doClick(ui, g, 3, 0);
                                } catch (NullPointerException qq) {
                                    PBotUtils.sysLogAppend(ui, current.fullname + " I found is now null, weird. Retrying.", "white");
                                    g = null;
                                    while (PBotUtils.findNearestStageCrop(ui, 5000, current.stage, current.terobjs) == null)
                                        PBotUtils.sleep(10);
                                    g = PBotUtils.findNearestStageCrop(ui, 5000, current.stage, current.terobjs);
                                    PBotUtils.doClick(ui, g, 3, 0);
                                }
                                retryharvest = 0;
                                retrycount++;
                            }

                            if (stopThread || PBotWindowAPI.getWindow(ui, windowName) == null)
                                return;
                        }

                        if (stopThread || PBotWindowAPI.getWindow(ui, windowName) == null)
                            return;

                        if (PBotUtils.petalExists(ui)) {
                            if (stopThread)
                                return;
                            PBotUtils.choosePetal(ui, "Harvest");
                        }
                        while (PBotUtils.findObjectById(ui, g.id) != null) {
                            lblProg2.settext("Waiting for Harvest");
                            if (stopThread || PBotWindowAPI.getWindow(ui, windowName) == null)
                                return;
                            retryharvest++;
                            PBotUtils.sleep(10);
                            if (retryharvest >= 500) {
                                lblProg2.settext("Retry Harvest");
                                if (retrycount >= 3) {
                                    lblProg2.settext("Unstucking");
                                    PBotUtils.sysLogAppend(ui, "Moving char", "white");
                                    Coord location = player.rc.floor(posres);
                                    int x = location.x + getrandom();
                                    int y = location.y + getrandom();
                                    Coord finalloc = new Coord(x, y);
                                    PBotUtils.mapFinalClick(ui, finalloc, 1, 0);
                                    retrycount = 0;
                                    PBotUtils.sleep(1000);
                                }
                                PBotUtils.sysLogAppend(ui, "Retrying harvest", "white");
                                try {
                                    PBotUtils.doClick(ui, g, 3, 0);
                                } catch (NullPointerException qq) {
                                    PBotUtils.sysLogAppend(ui, current.fullname + " I found is now null, weird. Retrying.", "white");
                                    g = null;
                                    while (PBotUtils.findNearestStageCrop(ui, 5000, current.stage, current.terobjs) == null)
                                        PBotUtils.sleep(10);
                                    g = PBotUtils.findNearestStageCrop(ui, 5000, current.stage, current.terobjs);
                                    PBotUtils.doClick(ui, g, 3, 0);
                                }
                                retryharvest = 0;
                                retrycount++;
                            }
                        }

                        if (stopThread || PBotWindowAPI.getWindow(ui, windowName) == null)
                            return;
                        PBotUtils.sleep(200);

                        try {
                            PBotInventory inv = PBotUtils.playerInventory(ui);
                            while (inv.getInventoryItemsByResnames(current.invobjs) == null)
                                PBotUtils.sleep(10);
                            while (inv.getInventoryItemsByResnames(current.invobjs) == null)
                                PBotUtils.sleep(10);
                            List<PBotItem> items = inv.getInventoryItemsByResnames(current.invobjs);
                            sort(items);
                            for (PBotItem seeds : items) {
                                GItem item = seeds.gitem;
                                if (item.quality().q > highestquality) {
                                    highestquality = item.quality().q;
                                    lblhighestq.settext("Quality " + item.quality().q);
                                }
                                if (PBotUtils.getAmount(item) >= 5) {
                                    lblProg2.settext("Picking Up Seeds");
                                    //    PBotUtils.sysLogAppend("" + item.quality().q, "white");
                                    PBotUtils.takeItem(ui, item);
                                    break;
                                }
                            }
                            retryharvest = 0;
                            while (PBotUtils.getItemAtHand(ui) == null) {
                                lblProg2.settext("Waiting to Pickup Seeds");
                                PBotUtils.sleep(10);
                                retryharvest++;
                                if (retryharvest > 500) {
                                    items = inv.getInventoryItemsByResnames(current.invobjs);
                                    sort(items);
                                    for (PBotItem seeds : items) {
                                        GItem item = seeds.gitem;
                                        if (PBotUtils.getAmount(item) >= 5) {
                                            lblProg2.settext("Picking Up Seeds");
                                            PBotUtils.takeItem(ui, item);
                                            break;
                                        }
                                    }
                                }
                            }
                            // Plant the seed from hand
                            int amount;
                            GItem atHand = PBotUtils.getGItemAtHand(ui);
                            if (atHand != null) {
                                amount = PBotUtils.getAmount(atHand);
                                lblProg2.settext("Planting");
//                                PBotUtils.mapInteractClick();
                                ui.gui.map.wdgmsg("itemact", Coord.z, player.rc.floor(posres), 0, 0, (int) player.id, player.rc.floor(posres), 0, -1);
                                retrycount = 0;
                                while (PBotUtils.findNearestStageCrop(ui, 5, 0, current.terobjs) == null || (PBotUtils.getItemAtHand(ui) != null
                                        && amount == PBotUtils.getAmount(PBotUtils.getGItemAtHand(ui)))) {
                                    if (stopThread || PBotWindowAPI.getWindow(ui, windowName) == null)
                                        return;
                                    retryharvest++;
                                    if (retryharvest > 500) {
                                        lblProg2.settext("Retry Planting");
//                                          PBotUtils.mapInteractClick();
                                        ui.gui.map.wdgmsg("itemact", Coord.z, player.rc.floor(posres), 0, 0, (int) player.id, player.rc.floor(posres), 0, -1);
                                        retryharvest = 0;
                                        retrycount++;
                                    }
                                    if (PBotUtils.getItemAtHand(ui) != null & retrycount >= 3) {
                                        PBotUtils.sysLogAppend(ui, "Giving up on this replant, skipping", "white");
                                        break;
                                    }
                                    if (PBotUtils.getItemAtHand(ui) == null && retrycount >= 3) {
                                        PBotUtils.sysLogAppend(ui, "Retry pickup and plant", "white");
                                        lblProg2.settext("Retry Pickup Item and plant");
                                        sort(items);
                                        for (PBotItem seeds : items) {
                                            GItem item = seeds.gitem;
                                            if (item.quality().q > highestquality) {
                                                highestquality = item.quality().q;
                                                lblhighestq.settext("Quality " + item.quality().q);
                                            }
                                            if (PBotUtils.getAmount(item) >= 5) {
                                                lblProg2.settext("Picking Up Seeds");
                                                PBotUtils.sysLogAppend(ui, "Replanting " + current.name + " of quality : " + item.quality().q, "white");
                                                PBotUtils.takeItem(ui, item);
                                                break;
                                            }
                                        }
                                        //  PBotUtils.mapInteractClick();
                                        ui.gui.map.wdgmsg("itemact", Coord.z, player.rc.floor(posres), 0, 0, (int) player.id, player.rc.floor(posres), 0, -1);
                                    }
                                    lblProg2.settext("Waiting for Planting Complete");
                                    PBotUtils.sleep(10);
                                }
                            }
                            retrycount = 0;


                            // Merge seed from hand into inventory or put it in inventory
                            //commented out at request to prevent mixing high and low q seeds
                            if (inv.getInventoryItemsByResnames(current.invobjs) != null && PBotUtils.getItemAtHand(ui) != null && PBotUtils.getAmount(PBotUtils.getGItemAtHand(ui)) != 50) {
                                items = inv.getInventoryItemsByResnames(current.invobjs);
                                for (PBotItem seedslol : items) {
                                    if (PBotUtils.getAmount(seedslol.gitem) < 50)
                                        continue;
                                    if (PBotUtils.getAmount(PBotUtils.getGItemAtHand(ui)) == 50)
                                        break;
                                    if (seedslol.gitem.quality().q == PBotUtils.getGItemAtHand(ui).quality().q) {
                                        System.out.println("Combining quality : " + PBotUtils.getGItemAtHand(ui).quality().q + " with quality : " + seedslol.gitem.quality().q + " seeds.");
                                        int handAmount = PBotUtils.getAmount(PBotUtils.getGItemAtHand(ui));
                                        try {
                                            seedslol.gitem.wdgmsg("itemact", 0);
                                        } catch (Exception e) {
                                        }
                                        while (PBotUtils.getItemAtHand(ui) != null && PBotUtils.getAmount(PBotUtils.getGItemAtHand(ui)) == handAmount) {
                                            if (stopThread || PBotWindowAPI.getWindow(ui, windowName) == null)
                                                return;
                                            PBotUtils.sleep(50);
                                        }
                                        break;
                                    }
                                }
                            }

                            if (PBotUtils.getItemAtHand(ui) != null) {
                                lblProg2.settext("Dropping Seeds to Inv");
                                Coord slot = PBotUtils.getFreeInvSlot(inv);
                                if (slot != null) {
                                    PBotUtils.dropItemToInventory(slot, inv);
                                    while (PBotUtils.getItemAtHand(ui) != null)
                                        PBotUtils.sleep(50);
                                }
                            }
                            if (PBotUtils.invFreeSlots(ui) < 3) {
                                if (stopThread || PBotWindowAPI.getWindow(ui, windowName) == null)
                                    return;
                                lblProg2.settext("Barreling");
                                Gob barrel = PBotUtils.findNearestBarrel(ui, 5000, blacklist);
                                barrel.delattr(GobHighlight.class);
                                barrel.setattr(new GobHighlight(barrel));
                                items = inv.getInventoryItemsByResnames(current.invobjs);
                                sort(items);
                                if (PBotUtils.getItemAtHand(ui) != null)
                                    PBotUtils.dropItem(ui, 0);
                                PBotUtils.pfRightClick(ui, barrel, 0);
                                PBotUtils.waitForWindow(ui, (Resource.getLocString(Resource.BUNDLE_WINDOW, "Barrel")));
                                if (PBotUtils.getItemAtHand(ui) != null) {
                                    ui.gui.map.wdgmsg("itemact", Coord.z, barrel.rc.floor(posres), 0, 0, (int) barrel.id,
                                            barrel.rc.floor(posres), 0, -1);
                                    int i = 0;
                                    while (PBotUtils.getItemAtHand(ui) != null) {
                                        if (i > 250) {
                                            PBotUtils.sysLogAppend(ui, "Blacklisting barrel, appears to be full", "white");
                                            blacklist.add(barrel);
                                            barrel = PBotUtils.findNearestBarrel(ui, 2000, blacklist);
                                            PBotUtils.sleep(500);
                                            if (PBotUtils.getItemAtHand(ui) != null) {
                                                lblProg2.settext("Dropping Seeds to Inv");
                                                Coord slot = PBotUtils.getFreeInvSlot(ui.gui.maininv);
                                                if (slot != null) {
                                                    PBotUtils.dropItemToInventory(slot, ui.gui.maininv);
                                                    while (PBotUtils.getItemAtHand(ui) != null)
                                                        PBotUtils.sleep(50);
                                                }
                                            }
                                            break;
                                        }
                                        PBotUtils.sleep(10);
                                        i++;
                                    }
                                }
                                items.subList(0, 14).clear();
                                for (PBotItem seed : items) {
                                    if (stopThread)
                                        break;
                                    GItem item = seed.gitem;
                                    PBotUtils.takeItem(ui, item);

                                    ui.gui.map.wdgmsg("itemact", Coord.z, barrel.rc.floor(posres), 0, 0, (int) barrel.id, barrel.rc.floor(posres), 0, -1);
                                    int i = 0;
                                    while (PBotUtils.getItemAtHand(ui) != null) {
                                        if (i > 250) {
                                            PBotUtils.sysLogAppend(ui, "Blacklisting barrel, appears to be full", "white");
                                            blacklist.add(barrel);
                                            Coord slot = PBotUtils.getFreeInvSlot(ui.gui.maininv);
                                            PBotUtils.dropItemToInventory(slot, ui.gui.maininv);
                                            PBotUtils.sleep(250);
                                            barrel = PBotUtils.findNearestBarrel(ui, 2000, blacklist);
                                            PBotUtils.pfRightClick(ui, barrel, 0);
                                            int retryclick = 0;
                                            while (PBotWindowAPI.getWindow(ui, (Resource.getLocString(Resource.BUNDLE_WINDOW, "Barrel"))) == null) {
                                                if (retryclick > 200) {
                                                    retryclick = 0;
                                                    PBotUtils.pfRightClick(ui, barrel, 0);
                                                }
                                                retryclick++;
                                                PBotUtils.sleep(10);
                                            }
                                            break;
                                        }
                                        PBotUtils.sleep(10);
                                        i++;
                                    }
                                }
                            }

                        } catch (NullPointerException x) {
                            PBotUtils.sysLogAppend(ui, "Null pointer exception caught, crash prevented.", "white");
                        }
                        g = null;
                        cropsHarvested++;
                        lblProg.settext(cropsHarvested + " Units Harvested");
                    }
                } catch (Loading | Sprite.ResourceException | NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public int getrandom() {
        Random r = new Random();
        int randomNumber = r.ints(1, -6000, 6000).findFirst().getAsInt();
        return randomNumber;
    }

    class CoordSort implements Comparator<Gob> {
        public int compare(Gob a, Gob b) {
            if (a.rc.floor().x == b.rc.floor().x) {
                if (a.rc.floor().x % 2 == 0)
                    return (a.rc.floor().y < b.rc.floor().y) ? 1 : (a.rc.floor().y > b.rc.floor().y) ? -1 : 0;
                else
                    return (a.rc.floor().y < b.rc.floor().y) ? -1 : (a.rc.floor().y > b.rc.floor().y) ? 1 : 0;
            } else
                return (a.rc.floor().x < b.rc.floor().x) ? -1 : (a.rc.floor().x > b.rc.floor().x) ? 1 : 0;
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender == cbtn) {
            stopThread = true;
            stop();
            reqdestroy();
        } else
            super.wdgmsg(sender, msg, args);
    }

    public void stop() {
        // Stops thread
        PBotUtils.debugMsg(ui, current.fullname + " FarmerBots stopped!", Color.white);
        runner.interrupt();
        stopThread = true;
        this.destroy();
    }

    public void sort(List<PBotItem> items) {
        items.sort((a, b) -> {
            QBuff aq = a.witem.item.quality();
            QBuff bq = b.witem.item.quality();
            if (aq == null || bq == null)
                return 0;
            else return Double.compare(bq.q, aq.q);
        });
    }
}


