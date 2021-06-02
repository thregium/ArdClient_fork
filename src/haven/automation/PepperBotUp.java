package haven.automation;

import haven.Button;
import haven.CheckBox;
import haven.Coord;
import haven.FastMesh;
import haven.Gob;
import haven.Label;
import haven.MapView;
import haven.Text;
import haven.Widget;
import haven.Window;
import haven.purus.pbot.PBotUtils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class PepperBotUp extends Window implements GobSelectCallback {
    private Coord ca, cb;
    private static final Text.Foundry infof = new Text.Foundry(Text.sans, 10).aa(true);
    private Gob barrel, hfire, water, cauldron, htable, grinder;
    private ArrayList<Gob> crops = new ArrayList<Gob>();
    private ArrayList<Gob> storages = new ArrayList<Gob>();
    private ArrayList<Gob> tables = new ArrayList<Gob>();

    private Thread selectingarea;
    private String cropName = "gfx/terobjs/plants/pepper";
    private static final List<String> storagesTypes = new ArrayList<String>() {{
        add("gfx/terobjs/woodbox");
        add("gfx/terobjs/create");
        add("gfx/terobjs/wbasket");
        add("gfx/terobjs/cupboard");
        add("gfx/terobjs/chest");
        add("gfx/terobjs/largechest");
        add("gfx/terobjs/matalcabinet");
    }};
    public boolean allowrun;
    private Label sec;
    private CheckBox onlyStorage = new CheckBox("Only Storages");

    public PepperBotUp() {
        super(new Coord(300, 200), "Pepper Bot Updated");
    }

    public void added() {
        int y = 0;
        final Label seclbl = new Label("Section : ", infof);
        add(seclbl, new Coord(20, y));

        y += 15;
        sec = new Label("", Text.num12boldFnd, Color.white);
        add(sec, new Coord(0, y));

        if (PBotUtils.findObjectByNames(ui, 100, "gfx/terobjs/quern") != null) {
            grinder = PBotUtils.findObjectByNames(ui, 100, "gfx/terobjs/quern");
            if (!MapView.markedGobs.contains(grinder.id))
                MapView.markedGobs.add(grinder.id);
            PBotUtils.sysLogAppend(ui, "Auto added Quern", "white");
        }
        if (PBotUtils.findObjectByNames(ui, 100, "gfx/terobjs/well") != null) {
            water = PBotUtils.findObjectByNames(ui, 100, "gfx/terobjs/well");
            if (!MapView.markedGobs.contains(water.id))
                MapView.markedGobs.add(water.id);
            PBotUtils.sysLogAppend(ui, "Auto added water source", "white");
        }
        if (PBotUtils.findObjectByNames(ui, 100, "gfx/terobjs/cauldron") != null) {
            cauldron = PBotUtils.findObjectByNames(ui, 100, "gfx/terobjs/cauldron");
            if (!MapView.markedGobs.contains(cauldron.id))
                MapView.markedGobs.add(cauldron.id);
            PBotUtils.sysLogAppend(ui, "Auto added cauldron", "white");
        }
        if (PBotUtils.findObjectByNames(ui, 100, "gfx/terobjs/pow") != null) {
            hfire = PBotUtils.findObjectByNames(ui, 100, "gfx/terobjs/pow");
            if (!MapView.markedGobs.contains(hfire.id))
                MapView.markedGobs.add(hfire.id);
            PBotUtils.sysLogAppend(ui, "Auto added hearthfire", "white");
        }
        if (PBotUtils.findObjectByNames(ui, 100, "gfx/terobjs/barrel") != null) {
            barrel = PBotUtils.findObjectByNames(ui, 100, "gfx/terobjs/barrel");
            if (!MapView.markedGobs.contains(barrel.id))
                MapView.markedGobs.add(barrel.id);
            PBotUtils.sysLogAppend(ui, "Auto added barrel", "white");
        }

        y += 25;
        add(onlyStorage, new Coord(20, y));

        y += 25;
        Button trelHarBtn = new Button(140, "Trellis harvest") {
            @Override
            public void click() {
                allowrun = true;
                if (hfire == null) {
                    PBotUtils.sysMsg(ui, "No Hearthfire Selected.", Color.white);
                    allowrun = false;
                }
                if (barrel == null) {
                    PBotUtils.sysMsg(ui, "No barrel Selected.", Color.white);
                    allowrun = false;
                }
                if (water == null) {
                    PBotUtils.sysMsg(ui, "No water source Selected.", Color.white);
                    allowrun = false;
                }
                if (cauldron == null) {
                    PBotUtils.sysMsg(ui, "No cauldron Selected.", Color.white);
                    allowrun = false;
                }


                if (ca != null && cb != null && allowrun) {
                    PepperBotUpRun bf = new PepperBotUpRun(crops, storages, tables, onlyStorage.a, true, barrel, water, cauldron, hfire);

                    ui.gui.add(bf, new Coord(ui.gui.sz.x / 2 - bf.sz.x / 2, ui.gui.sz.y / 2 - bf.sz.y / 2 - 200));
                    new Thread(bf).start();
                    this.parent.destroy();
                } else if (allowrun) {
                    PBotUtils.sysMsg(ui, "Area not selected!", Color.WHITE);
                }
            }
        };
        add(trelHarBtn, new Coord(20, y));

        y += 35;
        Button GrindBtn = new Button(140, "Grind Pepper") {
            @Override
            public void click() {
                allowrun = true;
                if (grinder == null) {
                    PBotUtils.sysMsg(ui, "No grinder Selected.", Color.white);
                    allowrun = false;
                }
                if (ca != null && cb != null && allowrun) {
                    PepperGrinderUpRun bf = new PepperGrinderUpRun(ca, cb, grinder, onlyStorage.a);
                    ui.gui.add(bf, new Coord(ui.gui.sz.x / 2 - bf.sz.x / 2, ui.gui.sz.y / 2 - bf.sz.y / 2 - 200));
                    new Thread(bf).start();
                    this.parent.destroy();
                } else if (allowrun) {
                    PBotUtils.sysMsg(ui, "Area not selected!", Color.WHITE);
                }
            }
        };
        add(GrindBtn, new Coord(20, y));

        y += 35;
        Button areaSelBtn = new Button(140, "Select Area") {
            @Override
            public void click() {
                PBotUtils.sysMsg(ui, "Drag area over crops", Color.WHITE);
                selectingarea = new Thread(new PepperBotUp.selectingarea(), "Pepper Bot");
                selectingarea.start();
            }
        };
        add(areaSelBtn, new Coord(20, y));
    }

    public void gobselect(Gob gob) {
        if (gob.getres().basename().contains("barrel")) {
            barrel = gob;
            PBotUtils.sysMsg(ui, "Barrel selected!x : " + gob.rc.x + " y : " + gob.rc.y, Color.WHITE);
        } else if (gob.getres().basename().contains("well") || (gob.getres().basename().contains("cistern"))) {
            water = gob;
            PBotUtils.sysMsg(ui, "Well/Cistern selected! x : " + gob.rc.x + " y : " + gob.rc.y, Color.white);
        } else if (gob.getres().basename().contains("pow")) {
            hfire = gob;
            PBotUtils.sysMsg(ui, "Hearthfire selected!x : " + gob.rc.x + " y : " + gob.rc.y, Color.white);
        } else if (gob.getres().basename().contains("cauldron")) {
            cauldron = gob;
            PBotUtils.sysMsg(ui, "Cauldron Selected!x : " + gob.rc.x + " y : " + gob.rc.y, Color.white);
        } else if (gob.getres().basename().contains("htable")) {
            htable = gob;
            int stage = gob.getStage();
            PBotUtils.sysMsg(ui, "table selected : " + gob.rc.x + " y : " + gob.rc.y + " stage : " + stage + " overlay : " + gob.ols.size(), Color.white);
        } else if (gob.getres().basename().contains("quern")) {
            grinder = gob;
            PBotUtils.sysMsg(ui, "grinder selected : " + gob.rc.x + " y : " + gob.rc.y, Color.white);
        }
    }

    private class selectingarea implements Runnable {
        @Override
        public void run() {
            ca = null;
            cb = null;
            PBotUtils.selectArea(ui);
            Coord selectedAreaA = PBotUtils.getSelectedAreaA();
            Coord selectedAreaB = PBotUtils.getSelectedAreaB();
            areaselect(selectedAreaA, selectedAreaB);
        }
    }

    public ArrayList<Gob> Crops(boolean checkStage, Coord rc1, Coord rc2) {
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
        gobs.sort(new CoordSort());

        return gobs;
    }

    public ArrayList<Gob> Storages(Coord rc1, Coord rc2) {
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
        gobs.sort(new CoordSort());

        return gobs;
    }

    public ArrayList<Gob> Tables(Coord rc1, Coord rc2) {
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
        gobs.sort(new CoordSort());

        return gobs;
    }

    class CoordSort implements Comparator<Gob> {
        public int compare(Gob a, Gob b) {
            if (a.rc.x == b.rc.x) {
                if (a.rc.x % 2 == 0)
                    return Double.compare(b.rc.y, a.rc.y);
                else
                    return Double.compare(a.rc.y, b.rc.y);
            } else
                return Double.compare(b.rc.x, a.rc.x);
        }
    }


    private void registerGobSelect() {
        synchronized (GobSelectCallback.class) {
            ui.gui.map.registerGobSelect(this);
        }
    }

    public void areaselect(Coord a, Coord b) {
        try {
            System.out.println("Area select triggered.");
            this.ca = a;
            this.cb = b;
            crops = Crops(true, this.ca, this.cb);
            storages = Storages(this.ca, this.cb);
            tables = Tables(this.ca, this.cb);
            sec.settext("Crops : " + crops.size() + " Storages : " + storages.size() + " Tables : " + tables.size());
            PBotUtils.mapInteractLeftClick(ui, 0);
        } catch (Exception e) {
            PBotUtils.sysMsg(ui, e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender == cbtn)
            reqdestroy();
        else
            super.wdgmsg(sender, msg, args);
    }
}
