/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import haven.MapFile.*;
import integrations.mapv4.MappingClient;
import modification.configuration;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

import static haven.MCache.cmaps;
import static haven.Text.latin;

public class MapFileWidget extends Widget implements Console.Directory {
    public final MapFile file;
    public Location curloc;
    private Locator setloc;
    public boolean follow;
    private Area dext;
    private Segment dseg;
    private DisplayGrid[] display;
    private Collection<DisplayMarker> markers = null;
    private int markerseq = -1;
    private UI.Grab drag;
    private boolean dragging;
    private Coord dsc, dmc;
    private String biome, seginfo;
    public static int zoom = Utils.getprefi("zoomlmap", 0);
    public static int zoomlvls = 8;
    public static final double[] scaleFactors = new double[]{1 / 8.0, 1 / 4.0, 1 / 2.0, 1, 100 / 75.0, 100 / 50.0, 100 / 25.0, 100 / 15.0, 100 / 8.0}; //FIXME that his add more scale
    private static final Tex gridred = Resource.loadtex("gfx/hud/mmap/gridred");

    public static Map<String, Tex> cachedTextTex = new HashMap<>();
    public static Map<String, Tex> cachedImageTex = new HashMap<>();
    public static Map<String, Tex> cachedZoomImageTex = new HashMap<>();

    public static Tex getCachedTextTex(String text) {
        Tex tex = cachedTextTex.get(text);
        if (tex == null) {
            Text.Foundry fnd = new Text.Foundry(latin.deriveFont(Font.BOLD), 12).aa(true);
            tex = Text.renderstroked(text, Color.white, Color.BLACK, fnd).tex();
            cachedTextTex.put(text, tex);
        }
        return (tex);
    }


    public MapFileWidget(MapFile file, Coord sz) {
        super();
        this.file = file;
    }

    public static class Location {
        public final Segment seg;
        public final Coord tc;

        public Location(Segment seg, Coord tc) {
            Objects.requireNonNull(seg);
            Objects.requireNonNull(tc);
            this.seg = seg;
            this.tc = tc;
        }

        public String toString() {
            return "[" + seg + "[" + seg.id + "], " + tc + "]";
        }
    }

    public interface Locator {
        Location locate(MapFile file) throws Loading;
    }

    public static class MapLocator implements Locator {
        public final MapView mv;

        public MapLocator(MapView mv) {
            this.mv = mv;
        }

        public Location locate(MapFile file) {
            Coord mc = new Coord2d(mv.getcc()).floor(MCache.tilesz);
            if (mc == null)
                throw (new Loading("Waiting for initial location"));
            MCache.Grid plg = mv.ui.sess.glob.map.getgrid(mc.div(cmaps));
            GridInfo info = file.gridinfo.get(plg.id);
            if (info == null)
                throw (new Loading("No grid info, probably coming soon"));
            Segment seg = file.segments.get(info.seg);
            if (seg == null)
                throw (new Loading("No segment info, probably coming soon"));
            return (new Location(seg, info.sc.mul(cmaps.div(scalef())).add(mc.sub(plg.ul).div(scalef()))));
        }
    }

    public static class SpecLocator implements Locator {
        public final long seg;
        public final Coord tc;

        public SpecLocator(long seg, Coord tc) {
            this.seg = seg;
            this.tc = tc;
        }

        public Location locate(MapFile file) {
            Segment seg = file.segments.get(this.seg);
            if (seg == null)
                return (null);
            return (new Location(seg, tc.div(scalef())));
        }
    }

    public void center(Location loc) {
        curloc = loc;
    }

    public Location resolve(Locator loc) {
        if (!file.lock.readLock().tryLock())
            throw (new Loading("Map file is busy"));
        try {
            return (loc.locate(file));
        } finally {
            file.lock.readLock().unlock();
        }
    }

    public void tick(double dt) {
        if (setloc != null) {
            try {
                Location loc = resolve(setloc);
                center(loc);
                if (!follow)
                    setloc = null;
            } catch (Loading l) {
            }
        }
    }

    public static class DisplayGrid {
        public final Segment seg;
        public final Coord sc;
        public final Indir<Grid> gref;
        private Grid cgrid = null;
        private Defer.Future<TexI> img = null;
        private TexI tex = null;
        private final Map<String, Defer.Future<Tex>> olimg_c = new HashMap<>();

        private final Object fogSync = new Object();
        private Defer.Future<TexI> fogimg = null;

        private String[] tiles;
        private final Map<String[], Defer.Future<TexI>> highlightedMap = new HashMap<>();
        private final Map<String[], Boolean> highlightedHas = new HashMap<>();

        public DisplayGrid(Segment seg, Coord sc, Indir<Grid> gref) {
            this.seg = seg;
            this.sc = sc;
            this.gref = gref;
        }

        public Tex img() {
            Grid grid = gref.get();
            if (grid != cgrid) {
                if (img != null)
                    img.cancel();
                synchronized (olimg_c) {
                    if (!olimg_c.isEmpty()) {
                        olimg_c.forEach((s, d) -> d.cancel());
                        olimg_c.clear();
                    }
                }
                synchronized (highlightedHas) {
                    if (!highlightedHas.isEmpty()) {
                        highlightedHas.clear();
                    }
                }
                synchronized (highlightedMap) {
                    if (!highlightedMap.isEmpty()) {
                        highlightedMap.forEach((s, d) -> d.cancel());
                        highlightedMap.clear();
                    }
                }
                synchronized (fogSync) {
                    if (fogimg != null) {
                        if (!fogimg.done()) fogimg.cancel();
                        fogimg = null;
                    }
                }
                img = Defer.later(() -> new TexI(grid.render(sc.mul(cmaps.div(scalef())))));
                cgrid = grid;
            }

            if (img != null && img.done()) {
                try {
                    tex = img.get();
                } catch (Exception e) {
//                    dev.simpleLog(e);
                }
            }
            return tex;
        }

        public Tex olimg(String tag) {
            Tex tret = null;
            if (tex != null) {
                Defer.Future<Tex> ret;
                synchronized (olimg_c) {
                    ret = olimg_c.get(tag);
                    if (ret == null) {
                        ret = Defer.later(() -> new TexI(cgrid.olrender(sc.mul(cmaps.div(scalef())), tag)));
                        olimg_c.put(tag, ret);
                    }
                }

                if (ret != null && ret.done()) {
                    try {
                        tret = ret.get();
                    } catch (Exception e) {
//                    dev.resourceLog("DisplayGrid: " + e + " " + seg.id);
                    }
                }
            }
            return (tret);
        }

        public Tex olfog(final UI ui) {
            Tex tret = null;
            if (tex != null) {
                if (Glob.mapList.get(cgrid.id) != null) {
                    synchronized (fogSync) {
                        if (ui != null && ui.sess != null && ui.sess.alive()) {
                            Glob glob = ui.sess.glob;
                            if (glob.updatableMap.stream().anyMatch(l -> l.equals(cgrid.id))) {
                                clearfog();
                                glob.updatableMap.remove(cgrid.id);
                            }
                        }
                        Defer.Future<TexI> ret = fogimg;
                        if (ret == null) fogimg = ret = Defer.later(() -> new TexI(cgrid.olrenderfog(cgrid.id)));
                        if (ret != null && ret.done()) {
                            try {
                                tret = ret.get();
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            }
            return (tret);
        }

        public void clearfog() {
            synchronized (fogSync) {
                if (fogimg != null) {
                    if (!fogimg.done()) fogimg.cancel();
                    fogimg = null;
                }
            }
        }

        public Tex highlight(String... tileNames) {
            Tex tret = null;
            body:
            {
                if (tex == null) break body;
                if (tileNames == null || tileNames.length == 0) break body;

                if (!Arrays.equals(tileNames, tiles)) {
                    tiles = tileNames;
                }

                Boolean isHas;
                synchronized (highlightedHas) {
                    isHas = highlightedHas.computeIfAbsent(tiles, t -> cgrid.hasTiles(t));
                }

                if (!isHas) break body;

                Defer.Future<TexI> ret;
                synchronized (highlightedMap) {
                    ret = highlightedMap.computeIfAbsent(tiles, t -> Defer.later(() -> new TexI(cgrid.highlightOverlay(t))));
                }
                if (ret != null && ret.done()) {
                    try {
                        tret = ret.get();
                    } catch (Exception e) {
//                    dev.resourceLog("DisplayGrid: " + e + " " + seg.id);
                    }
                }
            }
            return (tret);
        }
    }

    public static class DisplayMarker {
        public static final Resource.Image flagbg, flagfg;
        public static final Coord flagcc;
        public final Marker m;
        public final Text tip;
        public Area hit;
        private Resource.Image img;
        private Coord cc;

        static {
            Resource flag = Resource.local().loadwait("gfx/hud/mmap/flag");
            flagbg = flag.layer(Resource.imgc, 1);
            flagfg = flag.layer(Resource.imgc, 0);
            flagcc = flag.layer(Resource.negc).cc;
        }

        public DisplayMarker(Marker marker) {
            this.m = marker;
            this.tip = Text.render(m.nm);
            if (marker instanceof PMarker)
                this.hit = Area.sized(flagcc.inv(), flagbg.sz);
        }

        public void draw(GOut g, Coord c) {
            if (m instanceof PMarker) {
                Coord ul = c.sub(flagcc);
                g.chcolor(((PMarker) m).color);
                g.image(flagfg, ul);
                g.chcolor();
                g.image(flagbg, ul);
                if (Config.mapdrawflags) {
//                    Tex tex = Text.renderstroked(m.nm, Color.white, Color.BLACK, fnd).tex();
                    Tex tex = getCachedTextTex(m.nm);
                    if (tex != null) {
                        g.aimage(tex, ul.add(flagfg.sz.x / 2, -20), 0.5, 0);
                    }
                }
            } else if (m instanceof SMarker) {
                SMarker sm = (SMarker) m;
                try {
                    if (cc == null) {
                        Resource res = MapFile.loadsaved(Resource.remote(), sm.res);
                        img = res.layer(Resource.imgc);
                        Resource.Neg neg = res.layer(Resource.negc);
                        cc = (neg != null) ? neg.cc : img.sz.div(2);
                        if (hit == null)
                            hit = Area.sized(cc.inv(), img.sz);
                    }
                } catch (Loading l) {
                } catch (Exception e) {
                    cc = Coord.z;
                }
                if (img != null) {
                    //((SMarker)m).res.name.startsWith("gfx/invobjs/small"));
                    int size = 20;
                    Tex itex = cachedImageTex.get(img.getres().name);
                    if (itex == null) {
                        itex = new TexI(img.img);
                        if ((itex.sz().x > size) || (itex.sz().y > size)) {
                            BufferedImage buf = img.img;
                            buf = PUtils.convolve(buf, new Coord(size, size), new PUtils.Hanning(1));
                            itex = new TexI(buf);
                        }
                        cachedImageTex.put(img.getres().name, itex);
                    }
                    g.aimage(itex, c, 0.5, 0.5);

                    if (Config.mapdrawquests) {
                        if (sm.res != null && (sm.res.name.startsWith("gfx/invobjs/small") || sm.res.name.contains("thingwall"))) {
//                            Tex tex = Text.renderstroked(sm.nm, Color.white, Color.BLACK, fnd).tex();
                            Tex ttex = getCachedTextTex(sm.nm);
                            if (ttex != null) {
                                g.aimage(ttex, c.add(0, -15), 0.5, 1);
                            }
                        }
                    }
                }
            }
        }
    }

    private void remark(Location loc, Area ext) {
        if (file.lock.readLock().tryLock()) {
            try {
                Collection<DisplayMarker> marks = new ArrayList<>();
                Area mext = ext.margin(cmaps);
                for (Marker mark : file.markers) {
                    if ((mark.seg == loc.seg.id) && mext.contains(mark.tc.div(cmaps)))
                        marks.add(new DisplayMarker(mark));
                }
                markers = marks;
                markerseq = file.markerseq;
            } finally {
                file.lock.readLock().unlock();
            }
        }
    }

    private void redisplay(Location loc) {
        Coord hsz = sz.div(2);
        Area next = Area.sized(loc.tc.sub(hsz).div(cmaps.div(scalef())),
                sz.add(cmaps.div(scalef())).sub(1, 1).div(cmaps.div(scalef())).add(1, 1));
        if ((display == null) || (loc.seg != dseg) || !next.equals(dext)) {
            DisplayGrid[] nd = new DisplayGrid[next.rsz()];
            if ((display != null) && (loc.seg == dseg)) {
                for (Coord c : dext) {
                    if (next.contains(c))
                        nd[next.ri(c)] = display[dext.ri(c)];
                }
            }
            display = nd;
            dseg = loc.seg;
            dext = next;
            markers = null;
        }
    }

    public Coord xlate(Location loc) {
        Location curloc = this.curloc;
        if (curloc == null || loc == null || curloc.seg.id != loc.seg.id/* || (curloc.seg != loc.seg)*/)
            return (null);
        return (loc.tc.add(sz.div(2)).sub(curloc.tc));
    }

    public void drawgrid(GOut g, Coord ul, DisplayGrid disp) {
        try {
            Tex img = disp.img();
            if (img != null) {
                g.image(img, ul, cmaps.div(scalef()));
            }
        } catch (Loading l) {
        }
    }

    public void refreshDisplayGrid() {
        Location loc = this.curloc;
        if (loc == null)
            return;
        redisplay(loc);
        if (file.lock.readLock().tryLock()) {
            try {
                for (Coord c : dext) {
                    display[dext.ri(c)] = null;
                }
            } finally {
                file.lock.readLock().unlock();
            }
        }
    }

    public void draw(GOut g) {
        Location loc = this.curloc;
        if (loc == null)
            return;
        Coord hsz = sz.div(2);
        redisplay(loc);
        if (file.lock.readLock().tryLock()) {
            try {
                for (Coord c : dext) {
                    if (display[dext.ri(c)] == null)
                        display[dext.ri(c)] = new DisplayGrid(loc.seg, c, loc.seg.grid(c));
                }
            } finally {
                file.lock.readLock().unlock();
            }
        }
        for (Coord c : dext) {
            Coord ul = hsz.add(c.mul(cmaps.div(scalef()))).sub(loc.tc);
            try {
                DisplayGrid disp = display[dext.ri(c)];
                if (disp == null)
                    continue;
                drawgrid(g, ul, disp);
            } catch (Loading l) {
                continue;
            }
            if (configuration.bigmapshowgrid) {
                g.image(gridred, ul, cmaps.div(scalef()));
//                g.chcolor(Color.RED);
//                Coord rect = cmaps.div(scalef());
//                g.dottedline(ul, ul.add(rect.x, 0), 1);
//                g.dottedline(ul, ul.add(0, rect.y), 1);
//                g.chcolor();
            }
        }
        if ((markers == null) || (file.markerseq != markerseq))
            remark(loc, dext);
        if (markers != null && !configuration.bigmaphidemarks) {
            for (DisplayMarker mark : markers) {
                if (ui != null && ui.gui != null && ui.gui.mapfile != null && ui.gui.mapfile.markers.contains(mark.m)) {
                    mark.draw(g, hsz.sub(loc.tc).add(mark.m.tc.div(scalef())));
                }
            }
        }
    }

    public void dumpTiles() {
        ui.gui.msg("Dumping map. Please wait...");

        Location loc = this.curloc;
        if (loc == null)
            return;

        LinkedList<DisplayGrid> grids = new LinkedList<>();
        if (file.lock.readLock().tryLock()) {
            try {
                for (Map.Entry<Coord, Long> entry : loc.seg.map.entrySet())
                    grids.add(new DisplayGrid(loc.seg, entry.getKey(), loc.seg.grid(entry.getKey())));
            } finally {
                file.lock.readLock().unlock();
            }
        }

        String session = (new SimpleDateFormat("yyyy-MM-dd HH.mm.ss")).format(new Date(System.currentTimeMillis()));
        (new File("map/" + session)).mkdirs();

        int c = 50;

        BufferedWriter ids = null;
        try {
            ids = new BufferedWriter(new FileWriter(String.format("map/%s/ids.txt", session), true));

            while (grids.size() > 0) {
                // just a fail-safe
                if (c-- == 0) {
                    ui.gui.error("WARNING: map dumper timed out");
                    break;
                }

                ListIterator<DisplayGrid> iter = grids.listIterator();
                while (iter.hasNext()) {
                    DisplayGrid disp = iter.next();
                    try {
                        Grid grid = disp.gref.get();
                        if (grid != null) {
                            BufferedImage img = grid.render(disp.sc.mul(cmaps));
                            File tilefile = new File(String.format("map/%s/tile_%d_%d.png", session, disp.sc.x, disp.sc.y));
                            ImageIO.write(img, "png", tilefile);
                            ids.write(String.format("%d,%d,%d\n", disp.sc.x, disp.sc.y, grid.id));
                        } else {
                            continue;
                        }
                    } catch (Loading l) {
                        continue;
                    }
                    iter.remove();
                }
            }
        } catch (IOException e) {
            ui.gui.error("ERROR: map dumper failure. See console for more info.");
            e.printStackTrace();
            return;
        } finally {
            if (ids != null) {
                try {
                    ids.close();
                } catch (IOException e) {
                }
            }
        }

        ui.gui.msg("Finished dumping map");
    }

    public void center(Locator loc) {
        setloc = loc;
        follow = false;
    }

    public void follow(Locator loc) {
        setloc = loc;
        follow = true;
    }

    public boolean clickloc(Location loc, int button) {
        return (false);
    }

    public boolean clickmarker(DisplayMarker mark, int button) {
        return (false);
    }

    public boolean deletemarker(DisplayMarker mark, int button) {
        return (false);
    }

    public void deletemarker(DisplayMarker mark) {
    }

    private DisplayMarker markerat(Coord tc) {
        if (markers != null) {
            for (DisplayMarker mark : markers) {
                if ((mark.hit != null) && mark.hit.contains(tc.sub(mark.m.tc.div(scalef()))))
                    return (mark);
            }
        }
        return (null);
    }

    Coord lastmousedown = Coord.z;

    public boolean mousedown(Coord c, int button) {
        Coord tc = null;
        lastmousedown = c;
        if (curloc != null)
            tc = c.sub(sz.div(2)).add(curloc.tc);
        if (tc != null) {
            if (button == 3 && ui.modflags() == (UI.MOD_CTRL | UI.MOD_META)) {
                file.lock.writeLock().lock();
                try {
                    Coord gridc = c.sub(sz.div(2)).mul(scalef()).add(curloc.tc.mul(scalef())).div(cmaps);
                    long gridid = curloc.seg.gridid(gridc);
                    curloc.seg.remove(gridc);

                    try {
                        String gridstring = String.format("grid-%x", gridid);
                        StringBuilder buf = new StringBuilder();
                        buf.append("map/");
                        if (!file.filename.equals("")) {
                            buf.append(file.filename);
                            buf.append('/');
                        }
                        buf.append(gridstring);
                        Path base = HashDirCache.findbase();
                        long h = configuration.namehash(configuration.namehash(0, Config.resurl.toString()), buf.toString());
                        Path gridfile = Utils.pj(base, String.format("%016x.0", h));
                        if (Files.deleteIfExists(gridfile))
                            System.out.println(gridfile.toFile().getAbsolutePath() + " deleted");
                        else
                            System.err.println(gridfile.toFile().getAbsolutePath() + " failed");
                    } catch (Exception er) {
                        er.printStackTrace();
                    }

                    file.updategrids(ui.sess.glob.map, ui.sess.glob.map.grids.values());
                    refreshDisplayGrid();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    file.lock.writeLock().unlock();
                }
            }
            if (clickloc(new Location(curloc.seg, tc.mul(scalef())), button))
                return (true);
            if (button == 1 && (ui.modctrl || ui.modmeta || ui.modshift)) {
                //Only works if we're on the same map segment as our player
                try {
                    //   tc = c.sub(sz.div(2)).add(curloc.tc);
                    final Location pl = resolve(new MapLocator(ui.gui.map));
                    if (curloc != null && curloc.seg.id == pl.seg.id) {
                        final Coord2d plc = new Coord2d(ui.sess.glob.oc.getgob(ui.gui.map.plgob).getc());
                        //Offset in terms of loftar map coordinates
                        //XXX: Previous worlds had randomized north/south/east/west directions, still the case? Assuming not for now.
                        final Coord2d offset = new Coord2d(pl.tc.sub(tc));
                        //Translate this to real map units and add to current map position
                        final Coord2d mc = plc.sub(offset.mul(MCache.tilesz).mul(scalef()));
                        if (ui.modmeta && !ui.modshift && !ui.modctrl) {
                            ui.gui.map.queuemove(mc);
                        } else if (ui.modshift && !ui.modmeta && !ui.modctrl)
                            ui.gui.map.pathto(mc);
                        else if (ui.modctrl && !ui.modmeta && !ui.modshift) {
                            ui.gui.map.moveto(mc);
                        }
                    }
                } catch (Exception e) {
                    ui.gui.syslog.append("Failed to resolve player location with map move", Color.white);
                }
                return true;
            }
        }
        if (button == 1 && ui.modflags() == 0) {
            Location loc = curloc;
            if ((drag == null) && (loc != null)) {
                drag = ui.grabmouse(this);
                dsc = c;
                dmc = loc.tc;
                dragging = false;
            }
            return (true);

        }
        return (super.mousedown(c, button));
    }

    public void mousemove(Coord c) {
        if (drag != null) {
            if (dragging) {
                setloc = null;
                follow = false;
                curloc = new Location(curloc.seg, dmc.add(dsc.sub(c)));
            } else if (c.dist(dsc) > 5) {
                dragging = true;
            }
        }
        super.mousemove(c);
    }

    public boolean mouseup(Coord c, int button) {
        if (button == 1) {
            if (drag != null) {
                drag.remove();
                drag = null;
            } else {
                Coord tc = null;
                if (curloc != null)
                    tc = c.sub(sz.div(2)).add(curloc.tc);
                if (tc != null && lastmousedown.equals(c)) {
                    DisplayMarker mark = markerat(tc);
                    if (mark != null)
                        if ((ui.modflags() == (UI.MOD_CTRL | UI.MOD_META) && deletemarker(mark, button)) || clickmarker(mark, button))
                            return (true);
                }
            }
        } else if (button == 3) {
            Coord tc = null;
            if (curloc != null)
                tc = c.sub(sz.div(2)).add(curloc.tc);
            if (tc != null && lastmousedown.equals(c)) {
                DisplayMarker mark = markerat(tc);
                if (mark != null && mark.m instanceof MapFile.SMarker) {
                    MapFile.SMarker sm = (MapFile.SMarker) mark.m;
                    if (ui.modflags() == 0) {
                        final FlowerMenu menu = new FlowerMenu((selection) -> {
                            if (selection == 0) {
                                sm.makeAutosend(!sm.autosend);
                                if (sm.autosend)
                                    uploadMarks();
                                file.defersave();
                            } else if (selection == 1) {
                                deletemarker(mark);
                            }
                        }, !sm.autosend ? "Enable sending to mapper" : "Disable sending to mapper", "Remove mark");
                        ui.root.getchilds(FlowerMenu.class).forEach(wdg -> wdg.choose(null));
                        ui.root.add(menu, ui.mc);
                    }
                }
            }
        }

        return (super.mouseup(c, button));
    }

    public boolean mousewheel(Coord c, int amount) {
        try {
            if (amount > 0) {
                if (MapFileWidget.zoom < zoomlvls - 1) {
                    ui.gui.mapfile.zoomtex = null;
                    Coord tc = curloc.tc.mul(MapFileWidget.scalef());
                    MapFileWidget.zoom++;
                    Utils.setprefi("zoomlmap", MapFileWidget.zoom);
                    tc = tc.div(MapFileWidget.scalef());
                    if (curloc != null) {
                        curloc.tc.x = tc.x;
                        curloc.tc.y = tc.y;
                    }
                }
            } else {
                if (MapFileWidget.zoom > 0) {
                    ui.gui.mapfile.zoomtex = null;
                    Coord tc = curloc.tc.mul(MapFileWidget.scalef());
                    MapFileWidget.zoom--;
                    Utils.setprefi("zoomlmap", MapFileWidget.zoom);
                    tc = tc.div(MapFileWidget.scalef());
                    if (curloc != null) {
                        curloc.tc.x = tc.x;
                        curloc.tc.y = tc.y;
                    }
                }
            }
        } catch (Exception e) {
        }
        return (true);
    }

    public Object tooltip(Coord c, Widget prev) {
        if (curloc != null) {
            Coord tc = c.sub(sz.div(2)).add(curloc.tc);
            DisplayMarker mark = markerat(tc);
            if (mark != null) {
                return (mark.tip);
            } else {
                if (ui.modshift)
                    return (segmentinfo(c));
                return (biomeat(c));
            }
        }
        return (super.tooltip(c, prev));
    }

    public static double scalef() {
        return scaleFactors[zoom];
    }


    private Object biomeat(Coord c) {
        final Coord tc = c.sub(sz.div(2)).mul(scalef()).add(curloc.tc.mul(scalef()));
        final Coord gc = tc.div(cmaps);
        String newbiome;
        try {
            newbiome = prettybiome(curloc.seg.gridtilename(tc, gc));
        } catch (Exception e) {
            newbiome = "Void";
        }
        if (!newbiome.equals(biome)) {
            biome = newbiome;
            return Text.render(newbiome);
        }
        return Text.render(biome);
    }

    private Object segmentinfo(Coord c) {
        final Coord tc = c.sub(sz.div(2)).mul(scalef()).add(curloc.tc.mul(scalef()));
        final Coord gc = tc.div(cmaps);
        String newsegment;
        try {
            newsegment = curloc.seg.gridid(gc) + " " + gc;
        } catch (Exception e) {
            newsegment = "null";
        }
        if (!newsegment.equals(seginfo))
            seginfo = newsegment;
        return Text.render(seginfo);
    }

    private static String prettybiome(String biome) {
        int k = biome.lastIndexOf("/");
        biome = biome.substring(k + 1);
        biome = biome.substring(0, 1).toUpperCase() + biome.substring(1);
        return biome;
    }

    public static class ExportWindow extends Window implements MapFile.ExportStatus {
        private Thread th;
        private volatile String prog = "Exporting map...";

        public ExportWindow() {
            super(new Coord(300, 65), "Exporting map...", true);
            adda(new Button(100, "Cancel", false, this::cancel), asz.x / 2, 40, 0.5, 0.0);
        }

        public void run(Thread th) {
            (this.th = th).start();
        }

        public void cdraw(GOut g) {
            g.text(prog, new Coord(10, 10));
        }

        public void cancel() {
            th.interrupt();
        }

        public void tick(double dt) {
            if (!th.isAlive())
                destroy();
        }

        public void grid(int cs, int ns, int cg, int ng) {
            info(String.format("Exporting map cut %,d/%,d in segment %,d/%,d", cg, ng, cs, ns));
        }

        public void info(String text) {
            this.prog = text;
        }

        public void mark(int cm, int nm) {
            this.prog = String.format("Exporting marker", cm, nm);
        }
    }

    public static class ImportWindow extends Window {
        private Thread th;
        private volatile String prog = "Initializing";
        private double sprog = -1;

        public ImportWindow() {
            super(new Coord(300, 65), "Importing map...", true);
            adda(new Button(100, "Cancel", false, this::cancel), asz.x / 2, 40, 0.5, 0.0);
        }

        public void run(Thread th) {
            (this.th = th).start();
        }

        public void cdraw(GOut g) {
            String prog = this.prog;
            if (sprog >= 0)
                prog = String.format("%s: %d%%", prog, (int) Math.floor(sprog * 100));
            else
                prog = prog + "...";
            g.text(prog, new Coord(10, 10));
        }

        public void cancel() {
            th.interrupt();
        }

        public void tick(double dt) {
            if (!th.isAlive())
                destroy();
        }

        public void prog(String prog) {
            this.prog = prog;
            this.sprog = -1;
        }

        public void sprog(double sprog) {
            this.sprog = sprog;
        }
    }

    public void exportmap(boolean errors, int v, File path) {
        GameUI gui = getparent(GameUI.class);
        ExportWindow prog = new ExportWindow();
        Thread th = new HackThread(() -> {
            try {
                try (OutputStream out = new BufferedOutputStream(new FileOutputStream(path))) {
                    file.export(errors, v, out, MapFile.ExportFilter.all, prog);
                }
            } catch (IOException e) {
                e.printStackTrace(ui.cons.out);
                gui.error("Unexpected error occurred when exporting map.");
            } catch (InterruptedException e) {
            }
        }, "Mapfile exporter");
        prog.run(th);
        gui.adda(prog, gui.sz.div(2), 0.5, 1.0);
    }

    public void importmap(boolean errors, File path) {
        GameUI gui = getparent(GameUI.class);
        ImportWindow prog = new ImportWindow();
        Thread th = new HackThread(() -> {
            long size = path.length();
            class Updater extends CountingInputStream {
                Updater(InputStream bk) {
                    super(bk);
                }

                protected void update(long val) {
                    super.update(val);
                    prog.sprog((double) pos / (double) size);
                }
            }
            try {
                prog.prog("Validating map data");
                try (InputStream in = new Updater(new FileInputStream(path))) {
                    file.reimport(errors, in, MapFile.ImportFilter.readonly);
                }
                prog.prog("Importing map data");
                try (InputStream in = new Updater(new FileInputStream(path))) {
                    file.reimport(errors, in, MapFile.ImportFilter.all);
                }
            } catch (InterruptedException e) {
            } catch (Exception e) {
                e.printStackTrace(ui.cons.out);
                e.printStackTrace();
                gui.error("Could not import map: " + e.getMessage());
            }
        }, "Mapfile importer");
        prog.run(th);
        gui.adda(prog, gui.sz.div(2), 0.5, 1.0);
    }

    public void exportmap(boolean errors, int v) {
        java.awt.EventQueue.invokeLater(() -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("Exported Haven map data", "hmap"));
            if (fc.showSaveDialog(null) != JFileChooser.APPROVE_OPTION)
                return;
            File path = fc.getSelectedFile();
            if (path.getName().indexOf('.') < 0)
                path = new File(path + ".hmap");
            exportmap(errors, v, path);
        });
    }

    public void importmap(boolean errors) {
        java.awt.EventQueue.invokeLater(() -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("Exported Haven map data", "hmap"));
            if (fc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
                return;
            importmap(errors, fc.getSelectedFile());
        });
    }

    private final Map<String, Console.Command> cmdmap = new TreeMap<>();

    {
        cmdmap.put("exportmap", (cons, args) -> {
            if (args.length == 2)
                exportmap(false, -1, new File(args[1]));
            else if (args.length == 3)
                exportmap(false, Integer.parseInt(args[2]), new File(args[1]));
            else
                exportmap(false, -1);
        });
        cmdmap.put("importmap", (cons, args) -> {
            if (args.length > 1)
                importmap(false, new File(args[1]));
            else
                importmap(false);
        });
        cmdmap.put("rmseg", new Console.Command() {
            public void run(Console cons, String[] args) {
                MapFileWidget.Location loc = curloc;
                if (loc != null) {
                    file.removeSegment(loc);
                }
            }
        });
    }

    @Override
    public Map<String, Console.Command> findcmds() {
        return (cmdmap);
    }

    public void uploadMarks() {
        if (ui.sess != null && ui.sess.alive() && ui.sess.username != null) {
            if (configuration.loadMapSetting(ui.sess.username, "mapper")) {
                MappingClient.getInstance(ui.sess.username).ProcessMap(file, (m) -> {
                    if (m instanceof MapFile.SMarker) {
                        return (((MapFile.SMarker) m).autosend);
                    }
                    if (m instanceof MapFile.PMarker) {
                        return ((MapFile.PMarker) m).color.equals(Color.GREEN) && configuration.loadMapSetting(ui.sess.username, "green") && !m.name().equals("");
                    }
                    return false;
                });
            }
        }
    }
}
