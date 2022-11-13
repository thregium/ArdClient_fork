package haven.purus.pbot;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import haven.Loading;
import haven.MCache;
import haven.UI;
import haven.automation.GobSelectCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static haven.OCache.posres;

public class PBotGobAPI {

    /**
     * Description for all method
     *
     * @param ui    UI for this script
     */

    private static boolean gobSelectWait = false;
    private static Gob selectedGob;

    public static final Map<String, String> gobWindowMap = new HashMap<>();

    static {
        gobWindowMap.put("gfx/terobjs/crate", "Crate");
        gobWindowMap.put("gfx/terobjs/dframe", "Frame");
        gobWindowMap.put("gfx/terobjs/kiln", "Kiln");
        gobWindowMap.put("gfx/terobjs/fineryforge", "Finery Forge");
        gobWindowMap.put("gfx/terobjs/steelcrucible", "Steelbox");
        gobWindowMap.put("gfx/terobjs/smelter", "Ore Smelter");
        gobWindowMap.put("gfx/terobjs/pow", "Fireplace");
        gobWindowMap.put("gfx/terobjs/oven", "Oven");
        gobWindowMap.put("gfx/terobjs/cauldron", "Cauldron");
        gobWindowMap.put("gfx/terobjs/woodbox", "Woodbox");
        gobWindowMap.put("gfx/terobjs/create", "Crate");
        gobWindowMap.put("gfx/terobjs/furn/table-stone", "Table");
        gobWindowMap.put("gfx/terobjs/furn/cottagetable", "Table");
        gobWindowMap.put("gfx/terobjs/wbasket", "Basket");
        gobWindowMap.put("gfx/terobjs/chickencoop", "Chicken Coop");
        gobWindowMap.put("gfx/terobjs/htable", "Herbalist Table");
        gobWindowMap.put("gfx/terobjs/studydesk", "Study Desk");
        gobWindowMap.put("gfx/terobjs/cupboard", "Cupboard");
        gobWindowMap.put("gfx/terobjs/ttub", "Tub");
        gobWindowMap.put("gfx/terobjs/chest", "Chest");
        gobWindowMap.put("gfx/terobjs/largechest", "Large Chest");
        gobWindowMap.put("gfx/terobjs/matalcabinet", "Metal Cabinet");
    }

    /**
     * List of all gobs visible to the client
     *
     * @return List of all gobs
     */
    public static List<PBotGob> getAllGobs(UI ui) {
        List<PBotGob> list = new ArrayList<>();
        synchronized (ui.sess.glob.oc) {
            for (Gob gob : ui.sess.glob.oc) {
                list.add(new PBotGob(gob));
            }
        }
        return list;
    }

    /**
     * List of all gobs in radius
     *
     * @return List of all gobs in radius
     */
    public static List<PBotGob> getGobsInRadius(UI ui, double radius) {
        Coord2d plc = player(ui).getRcCoords();
        double min = radius;

        List<PBotGob> list = new ArrayList<>();
        synchronized (ui.sess.glob.oc) {
            for (Gob gob : ui.sess.glob.oc) {
                if (gob.isplayer()) continue;
                double dist = gob.rc.dist(plc);
                if (dist < min) {
                    list.add(new PBotGob(gob));
                }
            }
        }
        return list;
    }

    /**
     * List of all gobs in radius
     *
     * @param radius search radius
     * @param center search center
     * @return List of all gobs in radius
     */
    public static List<PBotGob> getGobsInRadius(UI ui, Coord2d center, double radius) {
        double min = radius;

        List<PBotGob> list = new ArrayList<>();
        synchronized (ui.sess.glob.oc) {
            for (Gob gob : ui.sess.glob.oc) {
                if (gob.isplayer())
                    continue;
                double dist = gob.rc.dist(center);
                if (dist < min) {
                    list.add(new PBotGob(gob));
                }
            }
        }
        return list;
    }

    /**
     * List of all gobs in radius
     *
     * @param radius search radius
     * @param cx     search center x
     * @param cy     search center y
     * @return List of all gobs in radius
     */
    public static List<PBotGob> getGobsInRadius(UI ui, double cx, double cy, double radius) {
        Coord2d center = new Coord2d(cx, cy);
        double min = radius;

        List<PBotGob> list = new ArrayList<>();
        synchronized (ui.sess.glob.oc) {
            for (Gob gob : ui.sess.glob.oc) {
                if (gob.isplayer())
                    continue;
                double dist = gob.rc.dist(center);
                if (dist < min) {
                    list.add(new PBotGob(gob));
                }
            }
        }
        return list;
    }

    /**
     * List of all gobs in radius
     *
     * @param radius search radius
     * @param center search center
     * @param names  gobs names
     * @return List of all gobs in radius
     */
    public static List<PBotGob> findObjectsByNames(UI ui, Coord2d center, double radius, String... names) {
        double min = radius;

        List<PBotGob> list = new ArrayList<>();
        synchronized (ui.sess.glob.oc) {
            for (Gob gob : ui.sess.glob.oc) {
                double dist = gob.rc.dist(center);
                if (dist < min) {
                    boolean matches = false;
                    for (String name : names) {
                        if (gob.getres() != null && gob.getres().name.equals(name)) {
                            matches = true;
                            break;
                        }
                    }
                    if (matches) {
                        list.add(new PBotGob(gob));
                    }
                }
            }
        }
        return list;
    }

    public static List<PBotGob> findObjectsByNames(UI ui, Coord2d center, double radius, List<String> names) {
        return (findObjectsByNames(ui, center, radius, names.toArray(new String[0])));
    }

    /**
     * List of all gobs in radius
     *
     * @param radius search radius
     * @param cx     search center x
     * @param cy     search center y
     * @param names  gobs names
     * @return List of all gobs in radius
     */
    public static List<PBotGob> findObjectsByNames(UI ui, double cx, double cy, double radius, String... names) {
        Coord2d center = new Coord2d(cx, cy);
        double min = radius;

        List<PBotGob> list = new ArrayList<>();
        synchronized (ui.sess.glob.oc) {
            for (Gob gob : ui.sess.glob.oc) {
                double dist = gob.rc.dist(center);
                if (dist < min) {
                    boolean matches = false;
                    for (String name : names) {
                        if (gob.getres() != null && gob.getres().name.equals(name)) {
                            matches = true;
                            break;
                        }
                    }
                    if (matches) {
                        list.add(new PBotGob(gob));
                    }
                }
            }
        }
        return list;
    }

    public static List<PBotGob> findObjectsByNames(UI ui, double cx, double cy, double radius, List<String> names) {
        return (findObjectsByNames(ui, cx, cy, radius, names.toArray(new String[0])));
    }

    /**
     * List of all gobs in radius
     *
     * @param radius search radius
     * @param names  gobs names
     * @return List of all gobs in radius
     */
    public static List<PBotGob> findObjectsByNames(UI ui, double radius, String... names) {
        Coord2d plc = player(ui).getRcCoords();
        double min = radius;

        List<PBotGob> list = new ArrayList<>();
        synchronized (ui.sess.glob.oc) {
            for (Gob gob : ui.sess.glob.oc) {
                double dist = gob.rc.dist(plc);
                if (dist < min) {
                    boolean matches = false;
                    for (String name : names) {
                        if (gob.getres() != null && gob.getres().name.equals(name)) {
                            matches = true;
                            break;
                        }
                    }
                    if (matches) {
                        list.add(new PBotGob(gob));
                    }
                }
            }
        }
        return list;
    }

    public static List<PBotGob> findObjectsByNames(UI ui, double radius, List<String> names) {
        return (findObjectsByNames(ui, radius, names.toArray(new String[0])));
    }

    public static List<PBotGob> findObjectsByNames(UI ui, String... names) {
        List<PBotGob> list = new ArrayList<>();
        synchronized (ui.sess.glob.oc) {
            for (Gob gob : ui.sess.glob.oc) {
                boolean matches = false;
                for (String name : names) {
                    if (gob.getres() != null && gob.getres().name.equals(name)) {
                        matches = true;
                        break;
                    }
                }
                if (matches) {
                    list.add(new PBotGob(gob));
                }
            }
        }
        return list;
    }

    public static List<PBotGob> findObjectsByNames(UI ui, List<String> names) {
        return (findObjectsByNames(ui, names.toArray(new String[0])));
    }

//    public static List<PBotGob> getAllGobs() {
//        return getAllGobs(PBotAPI.modeui());
//    }

    /**
     * Waits for any gob to appear at the precisely given coordinates, excluding player
     *
     * @param x
     * @param y
     */
    public static void waitForGob(UI ui, double x, double y) {
        Coord2d expected = new Coord2d(x, y);
        while (true) {
            for (PBotGob gob : getAllGobs(ui)) {
                if (gob.getRcCoords().equals(expected) && player(ui).getGobId() != gob.getGobId())
                    return;
            }
            PBotUtils.sleep(25);
        }
    }

//    public static void waitForGob(double x, double y) {
//        waitForGob(PBotAPI.modeui(), x, y);
//    }

    public static PBotGob getClosestGobInRadius(UI ui, double radius) {
        Coord2d plc = player(ui).getRcCoords();
        double min = radius;
        Gob nearest = null;
        synchronized (ui.sess.glob.oc) {
            for (Gob gob : ui.sess.glob.oc) {
                double dist = gob.rc.dist(plc);
                if (dist < min) {
                    min = dist;
                    nearest = gob;
                }
            }
        }
        if (nearest == null)
            return (null);
        else
            return (new PBotGob(nearest));
    }

    public static PBotGob getClosestGobInRadius(UI ui, Coord2d center, double radius) {
        double min = radius;
        Gob nearest = null;
        synchronized (ui.sess.glob.oc) {
            for (Gob gob : ui.sess.glob.oc) {
                double dist = gob.rc.dist(center);
                if (dist < min) {
                    min = dist;
                    nearest = gob;
                }
            }
        }
        if (nearest == null)
            return (null);
        else
            return (new PBotGob(nearest));
    }

    /**
     * Finds the closest object that matches one of the given names
     *
     * @param radius  Radius to look for objects in tiles
     * @param pattern Regex pattern(s) to match resnames of the gobs
     * @return Gob of the object, or null if not found
     */
    public static PBotGob findGobByNames(UI ui, double radius, String... pattern) {
        Coord2d plc = player(ui).getRcCoords();
        double min = radius;
        Gob nearest = null;
        List<Pattern> patterns = Arrays.stream(pattern).map(Pattern::compile).collect(Collectors.toList());
        synchronized (ui.sess.glob.oc) {
            for (Gob gob : ui.sess.glob.oc) {
                double dist = gob.rc.dist(plc);
                if (dist < min) {
                    boolean matches = false;
                    try {
                        for (Pattern p : patterns) {
                            if (gob.getres() != null && p.matcher(gob.getres().name).matches()) {
                                matches = true;
                                break;
                            }
                        }
                    } catch (Loading l) {
                    }
                    if (matches) {
                        min = dist;
                        nearest = gob;
                    }
                }
            }
        }
        if (nearest == null)
            return null;
        else
            return new PBotGob(nearest);
    }

    public static PBotGob findGobByNames(UI ui, double radius, List<String> pattern) {
        return (findGobByNames(ui, radius, pattern.toArray(new String[0])));
    }

    public static PBotGob findGobByNames(UI ui, String... pattern) {
        Coord2d plc = player(ui).getRcCoords();
        double min = Double.MAX_VALUE;
        Gob nearest = null;
        List<Pattern> patterns = Arrays.stream(pattern).map(Pattern::compile).collect(Collectors.toList());
        synchronized (ui.sess.glob.oc) {
            for (Gob gob : ui.sess.glob.oc) {
                double dist = gob.rc.dist(plc);
                if (dist < min) {
                    boolean matches = false;
                    try {
                        for (Pattern p : patterns) {
                            if (gob.getres() != null && p.matcher(gob.getres().name).matches()) {
                                matches = true;
                                break;
                            }
                        }
                    } catch (Loading l) {
                    }
                    if (matches) {
                        min = dist;
                        nearest = gob;
                    }
                }
            }
        }
        if (nearest == null)
            return null;
        else
            return new PBotGob(nearest);
    }

    public static PBotGob findGobByNames(UI ui, List<String> pattern) {
        return (findGobByNames(ui, pattern.toArray(new String[0])));
    }

//    public static PBotGob findGobByNames(double radius, String... pattern) {
//        return findGobByNames(PBotAPI.modeui(), radius, pattern);
//    }

    /**
     * Returns gob with exactly the given coords or null if not found
     *
     * @param c Coords of gob
     * @return Gob with coordinates or null
     */
    public static PBotGob getGobWithCoords(UI ui, Coord2d c) {
        synchronized (ui.sess.glob.oc) {
            for (Gob gob : ui.sess.glob.oc) {
                if (gob.rc.x == c.x && gob.rc.y == c.y)
                    return new PBotGob(gob);
            }
        }
        return null;
    }

//    public static PBotGob getGobWithCoords(Coord2d c) {
//        return getGobWithCoords(PBotAPI.modeui(), c);
//    }

    /**
     * Returns the player gob
     *
     * @return Player gob
     */
    public static PBotGob player(UI ui) {
        return new PBotGob(ui.gui.map.player());
    }

//    public static PBotGob player() {
//        return player(PBotAPI.modeui());
//    }


    /**
     * Find object by ID
     *
     * @param id ID of the object to look for
     * @return Object, or null if not found
     */
    public static PBotGob findGobById(UI ui, long id) {
        synchronized (ui.sess.glob.oc) {
            for (Gob gob : ui.sess.glob.oc)
                if (gob.id == id)
                    return (new PBotGob(gob));
        }
        return (null);
    }

//    public static PBotGob findGobById(long id) {
//        return findGobById(PBotAPI.modeui(), id);
//    }


    /**
     * Next alt+click to gob returns the gob, the function will wait until this happens
     */
    public static PBotGob selectGob(UI ui) {
        gobSelectWait = true;
        synchronized (GobSelectCallback.class) {
            ui.gui.map.registerGobSelect(new GobCb());
        }
        while (gobSelectWait) {
            PBotUtils.sleep(25);
        }
        ui.gui.map.unregisterGobSelect();
        return new PBotGob(selectedGob);
    }

//    public static PBotGob selectGob() {
//        return selectGob(PBotAPI.modeui());
//    }

    private static class GobCb implements GobSelectCallback {

        public GobCb() {
        }

        @Override
        public void gobselect(Gob gob) {
            selectedGob = gob;
            gobSelectWait = false;
        }
    }

    /**
     * Itemact with item in hand, for example, to make a stockpile
     */
    public static boolean makePile(UI ui) {
        Coord c = new Coord();
        Coord2d cc = player(ui).getRcCoords();
        double tx = Math.ceil(cc.x / MCache.tilesz.x / MCache.cutsz.x);
        double ty = Math.ceil(cc.y / MCache.tilesz.y / MCache.cutsz.y);
        Coord ul = new Coord((int) (tx - ui.gui.map.view - 1) * MCache.cutsz.x, (int) (ty - ui.gui.map.view - 1) * MCache.cutsz.y);
        Coord size = ul.add(MCache.cutsz.mul(ui.gui.map.view * 2 + 1));
        for (c.y = ul.y; c.y < size.y; c.y++)
            for (c.x = ul.x; c.x < size.x; c.x++) {
                String tile = PBotUtils.tileResnameAt(ui, c.mul(11).x, c.mul(11).y);
                if (tile != null && !tile.equals("gfx/tiles/field")) {
                    ui.gui.map.wdgmsg("itemact", PBotUtils.getCenterScreenCoord(ui), c, 0);
                    return (true);
                }
            }
        return (false);
    }

    /**
     * Itemact with item in hand, for example, to make a stockpile
     * @param c2d - coord for click mouse
     */
    public static boolean makePile(UI ui, Coord2d c2d) {
        ui.gui.map.wdgmsg("itemact", PBotUtils.getCenterScreenCoord(ui), c2d.floor(posres), 0);
        return (true);
    }

//    public static void makePile() {
//        makePile(PBotAPI.modeui());
//    }

    /**
     * Use to place something, for example, a stockpile
     *
     * @param x x place stockpile to
     * @param y y place stockpile to
     */
    public static void placeThing(UI ui, double x, double y) {
        ui.gui.map.wdgmsg("place", new Coord2d(x, y).floor(posres), 0, 1, 0);
    }

    public static boolean placeThing(UI ui, double x, double y, int timeout) {
        ui.gui.map.wdgmsg("place", new Coord2d(x, y).floor(posres), 0, 1, 0);
        for (int i = 0, sleep = 10; ui.gui.map.placing != null && ui.gui.map.placing.done(); i += sleep) {
            if (i >= timeout) {
                return (false);
            }
            PBotUtils.sleep(sleep);
        }
        return (true);
    }

    public static void unplaceThing(UI ui) {
        ui.gui.map.wdgmsg("place", player(ui).getRcCoords().floor(posres), 0, 3, 0);
    }

    public static boolean unplaceThing(UI ui, int timeout) {
        ui.gui.map.wdgmsg("place", player(ui).getRcCoords().floor(posres), 0, 3, 0);
        for (int i = 0, sleep = 10; ui.gui.map.placing != null && ui.gui.map.placing.done(); i += sleep) {
            if (i >= timeout) {
                return (false);
            }
            PBotUtils.sleep(sleep);
        }
        return (true);
    }

//    public static void placeThing(double x, double y) {
//        placeThing(PBotAPI.modeui(), x, y);
//    }

    /**
     * Use to cancel stockpile placing for example
     */
    public static void cancelPlace(UI ui) {
        ui.gui.map.wdgmsg("place", new Coord2d(0, 0).floor(posres), 0, 3, 0);
    }

//    public static void cancelPlace() {
//        cancelPlace(PBotAPI.modeui());
//    }
}
