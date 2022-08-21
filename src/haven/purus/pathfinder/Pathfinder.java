package haven.purus.pathfinder;

import haven.Coord;
import haven.Coord2d;
import haven.GameUI;
import haven.Gob;
import haven.Resource;
import haven.purus.pbot.PBotUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static haven.OCache.posres;
import haven.sloth.script.pathfinding.Hitbox;

public class Pathfinder extends Thread {

    private int button, mod, meshid;
    private GameUI gui;
    private Coord2d dest;
    private Gob destGob;
    private String action;
    private boolean stop;
    public static boolean DEBUG = false;

    // Move to center of the location tile
    public Pathfinder(GameUI gui, Coord2d dest, String action) {
        this.gui = gui;
        this.dest = dest;
        this.action = action;
    }

    // Move close to the gob and right click it
    public Pathfinder(GameUI gui, Gob destGob, int button, int mod, int meshid, String action) {
        this.gui = gui;
        this.dest = destGob.rc;
        this.destGob = destGob;
        this.button = button;
        this.mod = mod;
        this.meshid = meshid;
        this.action = action;
    }

    public static Line segmentToLine(Coord2d a, Coord2d b) {
        if (a.y == b.y) { // Horizontal
            if (a.x == b.x)
                throw new Error("Segment must have different start and end points!");
            else
                return new Line(a.y, 0);
        } else if (a.x == b.x) { // Vertical
            return new Line(a.x, Double.POSITIVE_INFINITY);
        } else {
            double deltaX = a.x - b.x;
            double deltaY = a.y - b.y;
            double slope = deltaY / deltaX;
            double constant = a.y - slope * a.x;
            return new Line(constant, deltaY / deltaX);
        }
    }

    HashSet<String> inaccessibleTiles = new HashSet<String>() {{
        add("gfx/tiles/nil");
        add("gfx/tiles/deep");
        add("gfx/tiles/cave");
        add("gfx/tiles/rocks/.*");
    }};

    HashSet<String> whitelistedGobs = new HashSet<String>();

    private Coord coordToTile(Coord2d c) {
        return c.div(11, 11).floor();
    }

    // True if tile is within the given matrix and its not inaccessible
    private boolean isAccessible(Coord c, int[][] accessibilityMatrix) {
        if (c.x > 0 && c.x < accessibilityMatrix.length && c.y > 0 && c.y < accessibilityMatrix[0].length && accessibilityMatrix[c.x][c.y] <= 0)
            return true;
        else
            return false;
    }

    // Click tile at its center point
    private void clickTile(Coord tile, Coord2d origin) {
        gui.map.wdgmsg("click", PBotUtils.getCenterScreenCoord(gui.ui), origin.add(tile.x * 11, tile.y * 11).add(11 / 2.0, 11 / 2.0).floor(posres), 1, 0);
        gui.map.pllastcc = origin.add(tile.x * 11, tile.y * 11).add(11 / 2.0, 11 / 2.0);
    }

    private boolean moveToTileAndWait(Coord tile, Coord2d origin) {
        if (coordToTile(gui.map.player().rc.sub(origin)).equals(tile))
            return (true);
        clickTile(tile, origin);
        for (int i = 0, sleep = 25; (gui.map.player().isMoving() || !coordToTile(gui.map.player().rc.sub(origin)).equals(tile)) && !stop; ) { // For now lets assume that player starts from different tile so we only have to check that he has moved to correct tile and is not walking anymore
            if (!gui.map.player().isMoving()) {
                if (i > 1000) {
                    return (false);
                } else {
                    i += sleep;
                }
            } else {
                i = 0;
            }
            sleep(sleep);
            if (stop)
                return (true);
        }
        return (true);
    }

    @Override
    public void run() {
        run:
        while (!stop) {
            try {
                BufferedImage bMap = null;
                Graphics g = null;
                if (DEBUG) {
                    bMap = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_RGB);
                    g = bMap.getGraphics();
                }
                int[][] accessMatrix = new int[111][111]; //  0 <= accessible, 0 < inaccessible, 1 = object blocking, 2 = innaccessible tile, -1 = destinationgob
                Coord2d origin = new Coord2d(gui.map.player().rc.div(11).floor().mul(11));
                // Get rid of negative coordinates
                origin = origin.sub(11 * 45, 11 * 45);

                Predicate<String> inaccess = s -> {
                    for (String act : inaccessibleTiles) {
                        Pattern pattern = Pattern.compile(act);
                        if (pattern.matcher(s).matches())
                            return (true);
                    }
                    return (false);
                };

                for (int i = 1; i < 90; i++) {
                    for (int j = 1; j < 90; j++) {
                        int t = gui.map.glob.map.gettile(origin.div(11).floor().add(i, j));
                        Resource res = gui.map.glob.map.tilesetr(t);
                        if (res != null && inaccess.test(res.name)) {
                            accessMatrix[i][j] = 2;
                        }
                    }
                }
                long start = System.currentTimeMillis();
                synchronized (gui.ui.sess.glob.oc) {
                    for (Gob gob : gui.ui.sess.glob.oc) {
                        if (gob.isplayer())
                            continue;
                        Hitbox[] box = Hitbox.hbfor(gob);
                        if (box != null && box.length == 1 && box[0].points.length == 4 && !whitelistedGobs.contains(gob.getres().name) && box[0].ishitable()) {//FIXME
                            Coord2d rel = gob.rc.sub(origin);

                            Coord2d[] points = new Coord2d[4];

                            points[0] = box[0].points[0].rotate(gob.a).add(rel);
                            points[1] = box[0].points[1].rotate(gob.a).add(rel);
                            points[2] = box[0].points[2].rotate(gob.a).add(rel);
                            points[3] = box[0].points[3].rotate(gob.a).add(rel);

                            double maxY = Double.max(Double.max(points[0].y, points[1].y), Double.max(points[2].y, points[3].y));
                            double minY = Double.min(Double.min(points[0].y, points[1].y), Double.min(points[2].y, points[3].y));
                            for (int i = (int) Math.floor(minY); i <= (int) Math.ceil(maxY); i++) {
                                List<Double> plist = new ArrayList<>();
                                for (int j = 0; j < points.length; j++) {
                                    if (Math.min(points[j].y, points[(j + 1) % points.length].y) <= i && i <= Math.max(points[j].y, points[(j + 1) % points.length].y)) {
                                        Line l = segmentToLine(points[j], points[(j + 1) % points.length]);
                                        if (l.isVertical())
                                            plist.add(l.constant);
                                        else if (l.isHorizontal()) {
                                            plist.add(Math.min(points[j].x, points[(j + 1) % points.length].x));
                                            plist.add(Math.max(points[j].x, points[(j + 1) % points.length].x));
                                        } else {
                                            plist.add((l.xAtY(i)));
                                        }
                                    }
                                }
                                Collections.sort(plist);
                                for (int j = 1; j < plist.size(); j += 2) {
                                    int left = (int) Math.ceil(plist.get(j - 1));
                                    int right = (int) Math.floor(plist.get(j));
                                    for (int k = left; k <= right; k++) {
                                        if (j < 0 || k < 0 || j / 11 > 110 || k / 11 > 110)
                                            continue;
                                        try {
                                            if (destGob != null && gob.id == destGob.id)
                                                accessMatrix[(k) / 11][(i) / 11] = -1;
                                            else
                                                accessMatrix[(k) / 11][(i) / 11] = 1;
                                        } catch (ArrayIndexOutOfBoundsException a) {
                                            a.printStackTrace();
                                        }
                                    }
                                    if (left == right) {
                                        j--;
                                        continue;
                                    }
                                }
                            }

                        }
                    }
                }
                Coord startTile = coordToTile(gui.map.player().rc.sub(origin));
                Coord destTile = coordToTile(dest.sub(origin));

                if (DEBUG) {
                    g.setColor(Color.orange);
                    g.setColor(new Color(104, 0, 98));
                    g.fillRect(startTile.x * 11, startTile.y * 11, 11, 11);

                    g.setColor(Color.WHITE);
                    g.fillRect(destTile.x * 11, destTile.y * 11, 11, 11);
                }
                // Only pathfind within 37 tiles to every distance around player TODO: Find the precise distance where objects are loaded
                Coord playerTile = coordToTile(gui.map.player().rc.sub(origin));
                for (int i = -38; i <= 38; i++) {
                    accessMatrix[playerTile.x + 38][playerTile.y + i] = 2;
                    accessMatrix[playerTile.x - 38][playerTile.y + i] = 2;
                    accessMatrix[playerTile.x + i][playerTile.y + 38] = 2;
                    accessMatrix[playerTile.x + i][playerTile.y - 38] = 2;
                }
                // Color tiles
                if (DEBUG) {
                    for (int i = 0; i < 100; i++) {
                        for (int j = 0; j < 100; j++) {
                            if (accessMatrix[i][j] == 0)
                                continue;
                            else if (accessMatrix[i][j] == -1)
                                g.setColor(new Color(255, 116, 0));
                            else if (accessMatrix[i][j] == 1)
                                g.setColor(Color.cyan);
                            else if (accessMatrix[i][j] == 2)
                                g.setColor(Color.orange);
                            g.fillRect((i) * 11, (j) * 11, 11, 11);
                        }
                    }
                }

                // While player is in tile that is inaccessible, try to move player to accessible tile
                Random rng = new Random(1337);
                int timeout = 10000;
                long startTime = System.currentTimeMillis();

                /** Unstack cycle
                 * while player is standing on inaccessible tile do a random click within 11 pixels
                 */
                while (true) {
                    // Move player to tile that is accessible
                    playerTile = coordToTile(gui.map.player().rc.sub(origin));
                    int t = gui.map.glob.map.gettile(origin.div(11).floor().add(playerTile.x, playerTile.y));
                    Resource res = gui.map.glob.map.tilesetr(t);
                    if (res != null && inaccess.test(res.name)) {
                        accessMatrix[playerTile.x][playerTile.y] = 2;
                    }

                    if (accessMatrix[playerTile.x][playerTile.y] > 0) {
                        if (System.currentTimeMillis() - startTime > timeout) {
                            // Timeout after 10 seconds
                            return;
                        }
                        // Randomly click around and hope that player moves to correct position, timeout after few retries
                        gui.map.wdgmsg("click", PBotUtils.getCenterScreenCoord(gui.ui), gui.map.player().rc.add(rng.nextInt() % 11, rng.nextInt() % 11).floor(posres), 1, 0);
                        gui.map.pllastcc = gui.map.player().rc.add(rng.nextInt() % 11, rng.nextInt() % 11);
                        do {
                            sleep(250);
                            if (stop)
                                return;
                        } while (gui.map.player().getv() != 0);
                    } else {
                        break;
                    }
                }
                clickTile(playerTile, origin);
                // Find route
                if (!playerTile.equals(destTile)) { // If player is already in the destination tile, skip this
                    Coord route[];
                    Coord[][] toHere = new Coord[accessMatrix.length][accessMatrix[0].length];
                    int dist[][] = new int[accessMatrix.length][accessMatrix[0].length];
                    LinkedList<Coord> q = new LinkedList<>();
                    q.addFirst(new Coord(coordToTile(gui.map.player().rc.sub(origin))));
                    while (!q.isEmpty()) {
                        Coord cur = q.pollLast();
                        if (cur.equals(destTile))
                            break;
                        Coord[] possibleMoves = new Coord[4];
                        possibleMoves[0] = new Coord(cur.x + 1, cur.y);
                        possibleMoves[1] = new Coord(cur.x - 1, cur.y);
                        possibleMoves[2] = new Coord(cur.x, cur.y + 1);
                        possibleMoves[3] = new Coord(cur.x, cur.y - 1);
                        for (int i = 0; i < possibleMoves.length; i++) {
                            if (isAccessible(possibleMoves[i], accessMatrix) && toHere[possibleMoves[i].x][possibleMoves[i].y] == null) {
                                toHere[possibleMoves[i].x][possibleMoves[i].y] = new Coord(cur);
                                q.addFirst(possibleMoves[i]);
                                dist[possibleMoves[i].x][possibleMoves[i].y] = dist[cur.x][cur.y] + 1;
                            }
                        }
                    }
                    if (toHere[destTile.x][destTile.y] == null) { // A route does not exist!
                        synchronized (gui.map) {
                            gui.map.foundPath = false;
                        }
                        return;
                    }

                    route = new Coord[dist[destTile.x][destTile.y]];
                    Coord cur = playerTile;
                    route[route.length - 1] = destTile;

                    if (DEBUG)
                        g.setColor(Color.green);
                    for (int i = route.length - 2; i >= 0; i--) {
                        route[i] = toHere[route[i + 1].x][route[i + 1].y];
                        if (DEBUG)
                            g.fillRect((route[i].x) * 11, (route[i].y) * 11, 11, 11);
                    }
                    if (DEBUG) {
                        try {
                            ImageIO.write(bMap, "png", new File(System.currentTimeMillis() + ".png"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    System.out.println("Time taken for finding the route: " + (System.currentTimeMillis() - start));
                    synchronized (gui.map) {
                        gui.map.foundPath = true;
                    }
                    // Walk the route through
                    Coord clickDest = playerTile;
                    for (int i = 0; i < route.length; i++) {
                        if (accessMatrix[route[i].x][route[i].y] == -1)
                            break;
                        if (playerTile.x == route[i].x || playerTile.y == route[i].y) {
                            clickDest = clickDest.add(route[i].sub(clickDest));
                        } else {
                            if (!moveToTileAndWait(clickDest, origin))
                                continue run;
                            if (stop)
                                return;
                            playerTile = coordToTile(gui.map.player().rc.sub(origin));
                            clickDest = route[i];
                        }
                    }
                    if (destGob == null && action != null && action.length() > 0)
                        gui.act(action);
                    if (!moveToTileAndWait(clickDest, origin))
                        continue run;
                    if (stop)
                        return;
                }

                if (destGob != null) {
                    if (action != null && action.length() > 0)
                        gui.act(action);
                    if (destGob != null && destGob.rc != null) {
                        gui.map.wdgmsg("click", Coord.z, destGob.rc.floor(posres), button, mod, 0, (int) destGob.id, destGob.rc.floor(posres), 0, meshid);
                        gui.map.pllastcc = destGob.rc;
                    }
                    //	gui.map.wdgmsg("click", gob.sc, mc, clickb, modflags, 0, (int) gob.id, gob.rc.floor(posres), 0, meshid);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            break;
        }
    }

    private void sleep(int timeInMillis) {
        try {
            Thread.sleep(timeInMillis);
        } catch (InterruptedException ie) {
            stop = true;
        }
    }
}
