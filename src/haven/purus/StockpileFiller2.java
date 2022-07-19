package haven.purus;

import haven.Button;
import haven.Coord;
import haven.GameUI;
import haven.Gob;
import haven.Label;
import haven.MCache;
import haven.WItem;
import haven.Widget;
import haven.WidgetVerticalAppender;
import haven.Window;
import haven.automation.GobSelectCallback;
import haven.purus.pbot.PBotGob;
import haven.purus.pbot.PBotGobAPI;
import haven.purus.pbot.PBotItem;
import haven.purus.pbot.PBotUtils;
import haven.purus.pbot.PBotWindowAPI;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class StockpileFiller2 extends Window implements GobSelectCallback, ItemClickCallback {

    private ArrayList<Gob> stockpiles = new ArrayList<Gob>();
    private String invobj, terobj;

    private boolean stop = false;
    private boolean terobjCallback = false;
    private Thread selectingarea;
    private Coord a, b;
    private Button clearBtn, startBtn;
    private Label infoLbl = new Label("");

    public StockpileFiller2() {
        super(new Coord(270, 200), "Stockpile Filler2");
    }

    public void added() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(this);
        PBotUtils.debugMsg(ui, "Alt + Click to select stockpiles", Color.GREEN);
        Button gobselectBtn = new Button(140, "Choose gob area") {
            @Override
            public void click() {
                PBotUtils.debugMsg(ui, "Click and Drag over 2 wide area for stockpiles", Color.WHITE);
                StockpileFiller2.this.selectingarea = new Thread((Runnable) new selectingarea(), "Farming Bots");
                StockpileFiller2.this.selectingarea.start();
            }
        };

        Button invobjBtn = new Button(140, "Choose item from inventory") {
            @Override
            public void click() {
                PBotUtils.debugMsg(ui, "Click the stockpile item in inventory", Color.GREEN);
                registerItemCallback();
            }
        };
        Button terobjBtn = new Button(140, "Choose item from ground") {
            @Override
            public void click() {
                terobjCallback = true;
                PBotUtils.debugMsg(ui, "Alt + Click to select ground item", Color.GREEN);
            }
        };
        clearBtn = new Button(140, "Clear/Stop") {
            @Override
            public void click() {
                try {
                    startBtn.show();
                    stop = true;
                    stop();
                    stockpiles = new ArrayList<Gob>();
                    PBotUtils.debugMsg(ui, "Cleared the list of selected stockpiles", Color.GREEN);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        startBtn = new Button(140, "Start") {
            @Override
            public void click() {
                if (stockpiles.isEmpty()) {
                    PBotUtils.debugMsg(ui, "No stockpiles chosen!", Color.GREEN);
                } else if (terobj == null) {
                    PBotUtils.debugMsg(ui, "No ground item chosen!", Color.GREEN);
                } else if (invobj == null) {
                    PBotUtils.debugMsg(ui, "No inventory item chosen!", Color.GREEN);
                } else {
                    try {
                        t.start();
                    } catch (Exception e) {
                        t.stop();
                        e.printStackTrace();
                    }
                }
            }
        };

        appender.add(gobselectBtn);
        appender.add(invobjBtn);
        appender.add(terobjBtn);
        appender.add(clearBtn);
        appender.add(startBtn);
        appender.add(infoLbl);
        pack();
    }

    Thread t = new Thread(() -> {
        startBtn.hide();
        stop = false;
        main:
        try {
            for (PBotGob teritem = PBotGobAPI.findGobByNames(ui, 5000, terobj);
                 teritem != null; teritem = PBotGobAPI.findGobByNames(ui, 5000, terobj)) {
                if (PBotUtils.playerInventory(ui).freeSlotsInv() == 0 && PBotUtils.getItemAtHand(ui) != null) {
                    if (!stckpl()) break main;
                }
                if (!takeItem(teritem)) {
                    PBotUtils.debugMsg(ui, "Can't take ground item", Color.GREEN);
                    continue;
                } else {
                    setInfo(teritem + "");
                }
            }
            if (!stckpl()) break main;
        } catch (Exception e) {
            PBotUtils.debugMsg(ui, "Something went wrong. Restart! " + e.getMessage());
            e.printStackTrace();
        }
        PBotUtils.debugMsg(ui, "Finish!", Color.GREEN);
        stop();
    }, "StockpileFiller");

    public boolean toStockpiles() {
        List<PBotItem> invitems = PBotUtils.playerInventory(ui).getInventoryItemsByResnames(invobj);
        if (invitems.size() > 0) {
            for (int j = 0; j < invitems.size(); j++) {
                Gob stock;
                Gob fstock = null;
                if (waitSleep()) return false;
                while (!stockpiles.isEmpty()) {
                    stock = stockpiles.get(0);
                    if (stock == null) {
                        stockpiles.remove(0);
                        continue;
                    }
                    fstock = PBotUtils.findObjectById(ui, stock.id);
                    if (fstock == null) {
                        stockpiles.remove(0);
                        continue;
                    }
                    if (PBotUtils.stockpileIsFull(fstock)) {
                        stockpiles.remove(0);
                        continue;
                    }
                    break;
                }

                if (stockpiles.isEmpty()) {
                    return false;
                }

                if (waitSleep()) return false;
                if (PBotWindowAPI.getWindow(ui, "Stockpile") == null)
                    PBotUtils.pfRightClick(ui, fstock, 0);
                if (waitSleep()) return false;
                if (PBotWindowAPI.waitForWindow(ui, "Stockpile", 5000) == null) return false;
                if (waitSleep()) return false;
                invitems.get(j).itemact(0);
                if (waitSleep()) return false;
            }
        } else {
            PBotUtils.debugMsg(ui, "Inventory is full! Please clear it!", Color.GREEN);
            return false;
        }
        return true;
    }

    public void areaselect(Coord a, Coord b) {
        this.a = a.mul(MCache.tilesz2);
        this.b = b.mul(MCache.tilesz2).add(11, 11);
        PBotUtils.debugMsg(ui, "Area selected!", Color.WHITE);
        ui.gui.map.unregisterAreaSelect();
    }

    private class selectingarea
            implements Runnable {
        private selectingarea() {
        }

        @Override
        public void run() {
            try {
                if (StockpileFiller2.this.stockpiles.isEmpty()) {
                    PBotUtils.debugMsg(ui, "Please select a first stockpile Alt + Click - try again.");
                    return;
                }
                PBotUtils.selectArea(ui);
                Coord aPnt = PBotUtils.getSelectedAreaA();
                Coord bPnt = PBotUtils.getSelectedAreaB();
                if (Math.abs(aPnt.x - bPnt.x) > 22 && Math.abs(aPnt.y - bPnt.y) > 22) {
                    PBotUtils.debugMsg(ui, "Please select an area at least 2 tiles wide - try again.");
                    return;
                }
                List<PBotGob> gobs = PBotUtils.gobsInArea(ui, aPnt, bPnt);
                int i = 0;
                while (i < gobs.size()) {
                    if (gobs.get(i).gob.getres().basename().equals((StockpileFiller2.this.stockpiles.get(0)).getres().basename())) {
                        StockpileFiller2.this.gobselect(gobs.get(i).gob);
                    }
                    ++i;
                }
            } catch (Exception e) {
                PBotUtils.debugMsg(ui, "Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void registerItemCallback() {
        synchronized (GobSelectCallback.class) {
            ui.gui.registerItemCallback(this);
        }
    }

    @Override
    public void gobselect(Gob gob) {
        if (terobjCallback) {
            terobjCallback = false;
            terobj = gob.getres().name;
            PBotUtils.debugMsg(ui, "Ground item chosen!", Color.GREEN);
        } else if (gob.getres().basename().contains("stockpile")) {
            stockpiles.add(gob);
            PBotUtils.debugMsg(ui, "Stockpile added to list! Total stockpiles chosen: " + stockpiles.size(), Color.GREEN);
        }
        synchronized (GobSelectCallback.class) {
            ui.gui.map.registerGobSelect(this);
        }
    }

    @Override
    public void itemClick(WItem item) {
        invobj = item.item.getres().name;
        PBotUtils.debugMsg(ui, "Inventory item to put in the stockpiles chosen!", Color.GREEN);
        synchronized (ItemClickCallback.class) {
            ui.gui.unregisterItemCallback();
        }
    }

    public void setInfo(String text) {
        infoLbl.settext(text);
        System.out.println(this.cap.text + ": " + text);
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (msg == "close") {
            stop();
            reqdestroy();
        } else
            super.wdgmsg(sender, msg, args);
    }

    public void stop() {
        setInfo("");
        if (t != null)
            t.interrupt();
        stop = true;
        GameUI gui = getparent(GameUI.class);
        if (gui != null) {
            synchronized (ItemClickCallback.class) {
                gui.unregisterItemCallback();
            }
            synchronized (ItemClickCallback.class) {
                gui.unregisterItemCallback();
            }
        }
        startBtn.show();
//        reqdestroy();
        //ui.gui.map.wdgmsg("click", Coord.z, new Coord((int)BotUtils.player().rc.x, (int)BotUtils.player().rc.y), 1, 0);
    }

    public boolean stckpl() {
        if (stockpiles.isEmpty()) {
            PBotUtils.debugMsg(ui, "Stockpiles full!", Color.GREEN);
            return (false);
        }

        List<PBotItem> invitems = PBotUtils.playerInventory(ui).getInventoryItemsByResnames(invobj);

        if (invitems == null) {
            PBotUtils.debugMsg(ui, invobj + " not found!", Color.GREEN);
            return (false);
        }

        while (!PBotUtils.playerInventory(ui).getInventoryItemsByResnames(invobj).isEmpty() && !stop) {
            while (!stockpiles.isEmpty() && !stop) {
                Gob stock = null;
                PBotGob fstock = null;
                stock = stockpiles.get(0);
                if (stock == null) {
                    stockpiles.remove(0);
                    PBotUtils.debugMsg(ui, "gob not found", Color.GREEN);
                    continue;
                }
                fstock = PBotGobAPI.findGobById(ui, stock.id);
                if (fstock == null) {
                    stockpiles.remove(0);
                    PBotUtils.debugMsg(ui, "gob didn't find", Color.GREEN);
                    continue;
                }
                if (fstock.stockpileIsFull()) {
                    stockpiles.remove(0);
                    PBotUtils.debugMsg(ui, "Stockpile is full!", Color.GREEN);
                    continue;
                }
                if (!itemactchain(fstock)) {
                    stockpiles.remove(0);
                    continue;
                }
                if (!takeItem(invobj)) {
                    PBotUtils.debugMsg(ui, "Can't take item!", Color.GREEN);
                    return (false);
                }
                itemact(fstock);
                break;
            }
        }
        return (true);
    }

    public boolean itemactchain(PBotGob fstock) {
        int maxretry = 5;
        boolean success = false;

        for (int i = 0; i < maxretry; i++) {
            drophanditem();

            if (!pftostock(fstock)) {
                PBotUtils.debugMsg(ui, "Path not found!", Color.GREEN);
                success = false;
                continue;
            }
            if (!waitwindow()) {
                PBotUtils.debugMsg(ui, "Window not found!", Color.GREEN);
                success = false;
                continue;
            }
            success = true;
            break;
        }
        return (success);
    }

    public boolean pftostock(PBotGob fstock) {
        int maxretry = 5;
        boolean success = false;

        for (int i = 0; i < maxretry; i++) {
            success = fstock.pfClick(3, 0);
            if (success) break;
        }
        return (success);
    }

    public boolean waitwindow() {
        int maxretry = 5;
        boolean success = false;

        for (int i = 0; i < maxretry; i++) {
            success = PBotWindowAPI.waitForWindow(ui, "Stockpile", 1000) != null;
            if (success) break;
        }
        return (success);
    }

    public void drophanditem() {
        if (PBotUtils.getItemAtHand(ui) != null) {
            PBotUtils.dropItem(ui, 0);
        }
    }

    public boolean waitSleep() {
        PBotUtils.sleep(10);
        if (stop) return (true);
        while (PBotGobAPI.player(ui).isMoving()) {
            PBotUtils.sysLogAppend(ui, "Wait moving!", "white");
            PBotUtils.sleep(1000);
        }
        PBotUtils.sleep(10);
        if (stop) return (true);
        while (PBotUtils.getItemAtHand(ui) != null) {
            PBotUtils.sysLogAppend(ui, "Item in hand. Drop it for continue!", "white");
            PBotUtils.sleep(1000);
        }
        PBotUtils.sleep(10);
        if (stop) return (true);
        return (false);
    }

    public boolean takeItem(PBotGob item) {
        int maxretry = 5;
        boolean success = false;

        for (int i = 0; i < maxretry; i++) {
            int stoping = 0;
            int maxstoping = 200;
            int time = 10;
            boolean interrupt = false;

            item.doClick(3, 0);
            while (PBotGobAPI.findGobById(ui, item.getGobId()) != null && !stop) {
                if (stoping >= maxstoping) {
                    interrupt = true;
                    break;
                }
                while (PBotGobAPI.player(ui).isMoving()) {
                    PBotUtils.sleep(10);
                }
                PBotUtils.sleep(time);
                stoping += time;
            }
            if (success = !interrupt) break;
        }
        return (success);
    }

    public void itemact(PBotGob gob) {
        gob.itemClick(1);
        int stoping = 0;
        int maxstoping = 5000;
        int time = 10;
        while (!PBotUtils.playerInventory(ui).getInventoryItemsByResnames(invobj).isEmpty() && !gob.stockpileIsFull() && !stop) {
            if (stoping >= maxstoping)
                break;
            PBotUtils.sleep(time);
            stoping += time;
        }
    }

    public boolean takeItem(String invobj) {
        int maxretry = 5;
        boolean success = false;

        for (int i = 0; i < maxretry; i++) {
            PBotItem item = PBotUtils.playerInventory(ui).getInventoryItemByResnames(invobj);
            if (item != null) {
                success = item.takeItem(1000);
                if (success) break;
            } else
                break;
        }
        return (success);
    }

    public boolean notmoving(int wait, Gob g) {
        int stoping = 0;
        int maxstoping = wait;
        int time = 10;
        while (!PBotUtils.isMoving(ui) && !stop) {
            if (PBotUtils.findObjectById(ui, g.id) == null)
                return false;
            if (stoping >= maxstoping)
                return true;
            PBotUtils.sleep(time);
            stoping += time;
        }
        return false;
    }
}