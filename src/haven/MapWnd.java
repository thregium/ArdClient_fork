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

import haven.BuddyWnd.GroupSelector;
import haven.MapFile.Marker;
import haven.MapFile.PMarker;
import haven.MapFile.SMarker;
import haven.MapFileWidget.Locator;
import haven.MapFileWidget.MapLocator;
import haven.MapFileWidget.SpecLocator;
import haven.purus.pbot.PBotGob;
import haven.purus.pbot.PBotGobAPI;
import haven.purus.pbot.PBotUtils;
import haven.res.ui.obj.buddy.Buddy;
import haven.sloth.gob.Type;
import haven.sloth.gui.DowseWnd;
import haven.sloth.gui.ResizableWnd;
import integrations.mapv4.MappingClient;
import modification.configuration;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;

public class MapWnd extends ResizableWnd {
    public static final Resource markcurs = Resource.remote().loadwait("gfx/hud/curs/flag");
    public static final Tex party = Resource.loadtex("custom/mm/pl/party");
    private static final Tex gridblue = Resource.loadtex("gfx/hud/mmap/gridblue");

    public final MapFileWidget view;
    public final MapView mv;
    public final Toolbox tool;
    public final Widget zoombar, toolbar2;
    public boolean decohide = false;
    private final Locator player;
    private final Widget toolbar;
    private final Frame viewf;
    private TextEntry namesel;
    private GroupSelector colsel;
    private Button mremove;
    private Comparator<Marker> mcmp = namecmp;
    public List<Marker> markers = Collections.emptyList();
    public final Collection<String> overlays = new java.util.concurrent.CopyOnWriteArraySet<>();
    private int markerseq = -1;
    private boolean domark = false;
    private int olalpha = 64;
    public Tex zoomtex = null;
    private final Collection<Runnable> deferred = new LinkedList<>();
    private static final Tex plx = Text.renderstroked("\u2716", Color.red, Color.BLACK, Text.num12boldFnd).tex();
    private Predicate<Marker> filter = (m -> true);
    private static final Comparator<Marker> namecmp = (Comparator.comparing(a -> a.nm));
    private final Map<Color, Tex> xmap = new HashMap<>(6);
    private final Map<Long, Tex> namemap = new HashMap<>(50);
    private final Map<Coord, Coord> questlinemap = new HashMap<>();

    private final List<TempMark> tempMarkList = new ArrayList<TempMark>() {
        public synchronized TempMark remove(int index) {
            return super.remove(index);
        }

        public synchronized boolean add(TempMark tempMark) {
            return super.add(tempMark);
        }
    };

    public synchronized List<TempMark> getTempMarkList() {
        return (tempMarkList);
    }

    private long lastMarkCheck = System.currentTimeMillis();

    public static class TempMark {
        String name;
        boolean dead;
        boolean near = true;
        long start;
        final long id;
        Coord2d rc;
        Coord gc;
        MapFileWidget.Location loc;
        TexI icon;
        GobIcon gobIcon;
        Tex tooltip;

        public TempMark(String name, GobIcon gobIcon, boolean dead, long id, Coord2d rc, Coord gc, MapFileWidget.Location loc, TexI icon) {
            start = System.currentTimeMillis();
            this.name = name;
            this.gobIcon = gobIcon;
            this.dead = dead;
            this.id = id;
            this.rc = rc;
            this.gc = gc;
            this.loc = loc;
            this.icon = icon;
            this.tooltip = Text.render(configuration.getShortName(name)).tex();
        }
    }

    public MapWnd(MapFile file, MapView mv, Coord sz, String title) {
        super(sz, title, true);
        makeHidable();
        this.mv = mv;
        viewf = add(new Frame(Coord.z, true));
        view = viewf.add(new View(file));
        this.player = new MapLocator(view, mv);
        recenter();
        toolbar2 = add(new ToolBar());
        toolbar = add(new Widget(Coord.z));
        toolbar.add(new Img(Resource.loadtex("gfx/hud/mmap/fgwdg")), Coord.z);
        toolbar.add(new IButton("gfx/hud/mmap/home", "", "-d", "-h") {
            {
                tooltip = RichText.render("Follow ($col[255,255,0]{Home})\nUpdate grids with CTRL", 0);
            }

            @Override
            public void click() {
                if (ui.modflags() == UI.MOD_CTRL) {
                    view.file.updategrids(ui.sess.glob.map, ui.sess.glob.map.grids.values());
                    view.refreshDisplayGrid();
                } else {
                    questlinemap.clear();
                    recenter();
                }
            }
        }, Coord.z);
        toolbar.add(new ICheckBox("gfx/hud/mmap/mark", "", "-d", "-h", "-dh") {
            {
                tooltip = RichText.render("Add marker", 0);
            }
        }, Coord.z).state(() -> domark).set(a -> domark = a);
        toolbar.add(new ICheckBox("gfx/hud/mmap/hmark", "", "-d", "-h", "-dh") {
            {
                tooltip = RichText.render("Hide markers", 0);
            }
        }).state(() -> configuration.bigmaphidemarks).set(a -> configuration.bigmaphidemarks = a);
        toolbar.add(new ICheckBox("gfx/hud/mmap/wnd", "", "-d", "-h", "-dh") {
            {
                tooltip = RichText.render("Compact mode", 0);
            }
        }).state(this::decohide).set(a -> {
            compact(a);
            Utils.setprefb("compact-map", a);
        });
        toolbar.add(new ICheckBox("gfx/hud/mmap/prov", "", "-d", "-h", "-dh") {
                    public boolean mousewheel(Coord c, int amount) {
                        if (!checkhit(c) || !ui.modshift)
                            return (super.mousewheel(c, amount));
                        olalpha = Utils.clip(olalpha + (amount * -32), 32, 256);
                        return (true);
                    }
                })
                .changed(a -> toggleol("realm", a))
                .settip("Display provinces");
        toolbar.pack();
        zoombar = add(new ZoomBar());
        tool = add(new Toolbox());
        compact(Utils.getprefb("compact-map", false));
        resize(sz);
    }

    public void toggleol(String tag, boolean a) {
        if (a)
            overlays.add(tag);
        else
            overlays.remove(tag);
    }

    public class Toolbox extends Widget {
        public final MarkerList list;
        private final Frame listf, fdropf;
        private final Button mebtn, mibtn;
        private final Dropbox<Pair<String, String>> fdrop;
        private TextEntry namesel;

        private Toolbox() {
            super(UI.scale(200, 200));
            fdrop = add(markersFilter());
            fdropf = Frame.around(this, Collections.singletonList(fdrop));

            listf = add(new Frame(UI.scale(200, 200), false));
            list = listf.add(new MarkerList(listf.inner().x, 0));

            mebtn = add(new Button(UI.scale(95), "Export...", false) {
                public void click() {
                    boolean errors = ui.modflags() == UI.MOD_CTRL;
                    view.exportmap(errors, -1);
                }

                @Override
                public Object tooltip(Coord c, Widget prev) {
                    return Text.render("Ctrl for Export with errors").tex();
                }
            });
            mibtn = add(new Button(UI.scale(95), "Import...", false) {
                public void click() {
                    boolean errors = ui.modflags() == UI.MOD_CTRL;
                    view.importmap(errors);
                }

                @Override
                public Object tooltip(Coord c, Widget prev) {
                    return Text.render("Ctrl for Import with errors").tex();
                }
            });
        }

        public void resize(int h) {
            super.resize(new Coord(sz.x, h));

            fdropf.c = new Coord(sz.x - UI.scale(200), 0);
            fdrop.c = new Coord(fdropf.c.x + UI.scale(5), fdropf.c.y + UI.scale(5));

            listf.resize(listf.sz.x, sz.y - UI.scale(140));
            listf.c = new Coord(sz.x - listf.sz.x, fdropf.c.y + fdropf.sz.y);
            list.resize(listf.inner());
            mebtn.c = new Coord(sz.x - UI.scale(200), sz.y - mebtn.sz.y);
            mibtn.c = new Coord(sz.x - UI.scale(95), sz.y - mibtn.sz.y);

            if (namesel != null) {
                namesel.c = listf.c.add(0, listf.sz.y + UI.scale(10));
                if (colsel != null)
                    colsel.c = namesel.c.add(0, namesel.sz.y + UI.scale(10));
                mremove.c = new Coord(namesel.c.x, sz.y - mremove.sz.y);
            }
        }

        @SuppressWarnings("unchecked")
        private Dropbox<Pair<String, String>> markersFilter() {
            Dropbox<Pair<String, String>> modes = new Dropbox<Pair<String, String>>(UI.scale(195), filters.length, Math.max(Text.render(filters[0].a.toString()).sz().y, UI.scale(20))) {
                @Override
                protected Pair<String, String> listitem(int i) {
                    return filters[i];
                }

                @Override
                protected int listitems() {
                    return filters.length;
                }

                @Override
                protected void drawitem(GOut g, Pair<String, String> item, int i) {
                    g.text(item.a, Dropbox.itemtextc);
                }

                @Override
                public void change(Pair<String, String> item) {
                    super.change(item);
                    if (item.b == null)
                        filter = (m -> true);
                    else if (item.b.equals("flg"))
                        filter = (m -> m instanceof PMarker);
                    else if (item.b.equals("qst"))
                        filter = (m -> m instanceof SMarker && (((SMarker) m).res.name.startsWith("gfx/invobjs/small/bush") || ((SMarker) m).res.name.startsWith("gfx/invobjs/small/bumbling") || ((SMarker) m).res.name.startsWith("gfx/terobjs/mm/gianttoad")));
                    else
                        filter = (m -> m instanceof SMarker && ((SMarker) m).res.name.endsWith(item.b));
                    markerseq = -1;
                    // reset scrollbar
                    if (list != null)
                        list.sb.val = list.sb.min;
//                for (MapFile.SMarker marker : view.file.smarkers.values()) {
//                    System.out.println(marker + " [" + marker.nm + "] [" + marker.oid + "] [" + marker.res + " " + marker.res.name + " " + marker.res.ver + "]");
//                }
                }
            };
            modes.change(filters[0]);
            return modes;
        }

        @Override
        public void gotfocus() {
            super.gotfocus();
            setfocus(list);
        }
    }

    private static final int btnsz = UI.scale(21);

    public class ZoomBar extends Widget {
        public ZoomBar() {
            super(new Coord(btnsz * 2 + UI.scale(20), btnsz));
            add(new IButton("gfx/hud/worldmap/minus", "", "", "") {
                @Override
                public void click() {
                    if (view.zoom < MapFileWidget.zoommax) {
                        zoomtex = null;
                        Coord tc = view.curloc.tc.mul(view.scalef());
                        view.zoom++;
                        Utils.setprefi("zoomlmap", view.zoom);
                        tc = tc.div(view.scalef());
                        view.curloc.tc.x = tc.x;
                        view.curloc.tc.y = tc.y;
                    }
                }
            }, Coord.z);
            add(new IButton("gfx/hud/worldmap/plus", "", "", "") {
                @Override
                public void click() {
                    MapFileWidget.Location loc = view.curloc;
                    if (loc != null && view.zoom > MapFileWidget.zoommin) {
                        zoomtex = null;
                        Coord tc = loc.tc.mul(view.scalef());
                        view.zoom--;
                        Utils.setprefi("zoomlmap", view.zoom);
                        tc = tc.div(view.scalef());
                        loc.tc.x = tc.x;
                        loc.tc.y = tc.y;
                    }
                }
            }, new Coord(btnsz + UI.scale(20), 0));
        }

        @Override
        public void draw(GOut g) {
            super.draw(g);
            g.image(renderz(), new Coord((btnsz * 2 + UI.scale(20)) / 2 - zoomtex.sz().x / 2, btnsz / 2 - zoomtex.sz().y / 2));
        }

        private Tex renderz() {
            if (zoomtex == null)
                zoomtex = Text.renderstroked("x" + view.scalef(), Color.WHITE, Color.BLACK).tex();
            return zoomtex;
        }
    }

    private volatile boolean fogA = false;
    private volatile boolean fogT = false;

    public class ToolBar extends Widget {
        public ToolBar() {
            super(Coord.z);
            final int spacer = UI.scale(5);
            final ICheckBox pclaim = new ICheckBox("gfx/hud/wndmap/btns/claim", "", "-d", "", "") {
                public boolean mousewheel(Coord c, int amount) {
                    if (!checkhit(c) || !ui.modshift)
                        return (super.mousewheel(c, amount));
                    olalpha = Utils.clip(olalpha + (amount * -32), 32, 256);
                    return (true);
                }
            };
            add(pclaim).changed(a -> toggleol("cplot", a)).settip(Resource.getLocString(Resource.BUNDLE_LABEL, "Display personal claims"));
            final ICheckBox vclaim = new ICheckBox("gfx/hud/wndmap/btns/vil", "", "-d", "", "") {
                public boolean mousewheel(Coord c, int amount) {
                    if (!checkhit(c) || !ui.modshift)
                        return (super.mousewheel(c, amount));
                    olalpha = Utils.clip(olalpha + (amount * -32), 32, 256);
                    return (true);
                }
            };
            add(vclaim, pclaim.c.add(pclaim.sz.x + spacer, 0)).changed(a -> toggleol("vlg", a)).settip(Resource.getLocString(Resource.BUNDLE_LABEL, "Display village claims"));
//            final ICheckBox realm = new ICheckBox("gfx/hud/wndmap/btns/realm", "", "-d", "", "");
//            add(realm, vclaim.c.add(vclaim.sz.x + spacer, 0)).changed(a -> toggleol("realm", a)).settip(Resource.getLocString(Resource.BUNDLE_LABEL, "Display realms"));
            final IButton geoloc = new IButton("gfx/hud/wndmap/btns/geoloc", "", "", "") {
                private BufferedImage green = Resource.loadimg("hud/geoloc-green");
                private BufferedImage red = Resource.loadimg("hud/geoloc-red");

                private int state = 0;

                @Override
                public Object tooltip(Coord c, Widget prev) {
                    search:
                    {
                        if (!configuration.endpoint.isEmpty() && ui.sess != null && ui.sess.alive() && ui.sess.username != null && ui.gui != null) {
                            if (!ui.gui.chrid.isEmpty()) {
                                String username = ui.sess.username + "/" + ui.gui.chrid;
                                if (configuration.loadMapSetting(username, "mapper")) {
                                    MappingClient map = MappingClient.getInstance(username);
                                    MappingClient.MapRef mr = map.lastMapRef;
                                    if (mr != null) {
                                        tooltip = Text.render("Coordinates: " + mr);
                                        break search;
                                    }
                                }
                            }
                        }
                        tooltip = Text.render("Unable to determine your current location.");
                    }
                    return super.tooltip(c, prev);
                }

                @Override
                public void click() {
                    if (!configuration.endpoint.isEmpty() && ui.sess != null && ui.sess.alive() && ui.sess.username != null && ui.gui != null) {
                        if (!ui.gui.chrid.isEmpty()) {
                            String username = ui.sess.username + "/" + ui.gui.chrid;
                            if (configuration.loadMapSetting(username, "mapper")) {
                                MappingClient map = MappingClient.getInstance(username);
                                MappingClient.MapRef mr = map.GetMapRef(true);
                                if (mr != null) {
                                    map.OpenMap(mr);
                                    return;
                                }
                            }
                        }
                    }
                }

                @Override
                public void draw(GOut g) {
                    boolean redraw = false;
                    fin:
                    {
                        if (!configuration.endpoint.isEmpty() && ui.sess != null && ui.sess.alive() && ui.sess.username != null && ui.gui != null) {
                            if (!ui.gui.chrid.isEmpty()) {
                                String username = ui.sess.username + "/" + ui.gui.chrid;
                                if (configuration.loadMapSetting(username, "mapper")) {
                                    MappingClient map = MappingClient.getInstance(username);
                                    MappingClient.MapRef mr = map.lastMapRef;
                                    if (mr != null) {
                                        if (state != 2) {
                                            state = 2;
                                            redraw = true;
                                        }
                                        break fin;
                                    }
                                }
                            }
                        }
                        if (state != 0) {
                            state = 0;
                            redraw = true;
                        }
                    }
                    if (redraw) this.redraw();
                    super.draw(g);
                }

                @Override
                public void draw(BufferedImage buf) {
                    Graphics2D g = (Graphics2D) buf.getGraphics();
                    if (state == 2) {
                        g.drawImage(green, 0, 0, null);
                    } else if (state == 1) {
                        g.drawImage(red, 0, 0, null);
                    } else {
                        g.drawImage(up, 0, 0, null);
                    }
                    g.dispose();
                }
            };
            add(geoloc, vclaim.c.add(vclaim.sz.x + spacer, 0));
//            final IButton oddigeoloc = new IButton("gfx/hud/wndmap/btns/geoloc", "", "", "") {
//                private Coord coords = null;
//                private BufferedImage green = Resource.loadimg("hud/geoloc-green");
//                private BufferedImage red = Resource.loadimg("hud/geoloc-red");
//
//                private boolean state = false;
//
//                @Override
//                public Object tooltip(Coord c, Widget prev) {
//                    Coord coords = getCurCoords();
//                    String oddi = "";
//                    String vatsul = "";
//                    if (coords != null) {
//                        this.coords = coords;
//                        oddi = String.format("Current location: %d x %d", coords.x, coords.y);
//                    } else
//                        oddi = "Location not found";
//
//                    MCache.Grid g = getGrid();
//                    if (g != null) {
//                        vatsul = String.format("Current grid id: %d", g.id);
//                    } else
//                        vatsul = "Grid not found";
//                    String addinfo = "Click to open the Odditown map\nShfit+Click to open the Vatsul map";
//
//                    tooltip = RichText.render(oddi + "\n" + vatsul + "\n" + addinfo, 300).tex();
//                    return (super.tooltip(c, prev));
//                }
//
//                @Override
//                public void click() {
//                    if (ui.modflags() != UI.MOD_SHIFT) {
//                        Coord coords = getCurCoords();
//                        if (coords != null) {
//                            this.coords = coords;
//                            try {
//                                WebBrowser.self.show(new URL(String.format("http://odditown.com/haven/map/#x=%d&y=%d&zoom=9", coords.x, coords.y)));
//                            } catch (WebBrowser.BrowserException e) {
//                                getparent(GameUI.class).error("Could not launch web browser.");
//                            } catch (MalformedURLException e) {
//                            }
//                        } else {
//                            getparent(GameUI.class).error("Unable to determine your current location.");
//                        }
//                    } else {
//                        try {
//                            MCache.Grid g = getGrid();
//                            if (g != null)
//                                WebBrowser.self.show(new URL("https://vatsul.com/HnHMap/whereis/" + g.id));
//                        } catch (NullPointerException | MalformedURLException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//
//                @Override
//                public void draw(GOut g) {
//                    boolean redraw = false;
//
//                    Coord coords = getCurCoords();
//                    if (coords != null) {
//                        this.coords = coords;
//                        if (!state) {
//                            state = true;
//                            redraw = true;
//                        }
//                    } else if (state) {
//                        state = false;
//                        redraw = true;
//                    }
//
//
//                    if (redraw) this.redraw();
//                    super.draw(g);
//                }
//
//                @Override
//                public void draw(BufferedImage buf) {
//                    Graphics2D g = (Graphics2D) buf.getGraphics();
//                    if (state) {
//                        g.drawImage(green, 0, 0, null);
//                    } else {
//                        g.drawImage(red, 0, 0, null);
//                    }
//                    g.dispose();
//                }
//
//                private MCache.Grid getGrid() {
//                    try {
//                        Coord mpc = new Coord2d(mv.getcc()).floor(tilesz);
//                        return (ui.sess.glob.map.getgrid(mpc.div(cmaps)));
//                    } catch (Exception e) {
//                    }
//                    return (null);
//                }
//
//                private Coord getCurCoords() {
//                    try {
//                        MCache.Grid obg = getGrid();
//                        return (Config.gridIdsMap.get(obg.id));
//                    } catch (Exception e) {
//                    }
//                    return (null);
//                }
//            };
//            add(oddigeoloc, geoloc.c.add(geoloc.sz.x + spacer, 0));
            final IButton grid = add(new IButton("gfx/hud/wndmap/btns/grid", "Toggle grid on minimap", MapWnd.this::toggleMapGrid),
                    geoloc.c.add(geoloc.sz.x + spacer, 0));
            final IButton viewdist = add(new IButton("gfx/hud/wndmap/btns/viewdist", null, MapWnd.this::toggleMapViewDist) {
                @Override
                public boolean mouseup(Coord c, int button) {
                    if (button == 3) {
                        if (!configuration.savingFogOfWar) {
                            if (fogA) fogA = false;
                            if (fogT) fogT = false;
                            else PBotUtils.sysMsg(ui, "First enable the FogOfWar option in the map settings.");
                        } else {
                            if (ui.modflags() == UI.MOD_CTRL) fogT = !fogT;
                            else if (ui.modflags() == UI.MOD_META) {
                                final Widget parent = MapWnd.this.parent;
                                if (parent != null) {
                                    final Window wnd = new Window(Coord.z, "Confirm clear");
                                    final Button yes = new Button("Yes", () -> {
                                        ui.sess.glob.clearTemp();
                                        wnd.reqdestroy();
                                    });
                                    final Button no = new Button("No", wnd::reqdestroy);
                                    final WidgetVerticalAppender wva = new WidgetVerticalAppender(wnd);
                                    wva.add(new Label("Please confirm a clearing!"));
                                    wva.addRow(yes, no);
                                    wnd.pack();
                                    parent.adda(wnd, parent.sz.div(2), 0.5, 0.5);
                                }
                            } else fogA = !fogA;
                        }
                        return (true);
                    } else {
                        return (super.mouseup(c, button));
                    }
                }

                @Override
                public Object tooltip(Coord c, Widget prev) {
                    return (RichText.render("Click - Toggle view range" + "\n" + "Right-Click - Display fog of war" + "\n" + "Ctrl+Right-Click - Display temp fog of war" + "\n" + "Alt+Right-Click - Clear temp fog of war", UI.scale(300)).tex());
                }
            }, grid.c.add(grid.sz.x + spacer, 0));
            final IButton iconbtn = add(new IButton("gfx/hud/wndmap/btns/lbtn-ico", "Icon settings", () -> {
                GameUI gui = getparent(GameUI.class);
                if (gui != null) {
                    if (gui.iconconf == null)
                        return;
                    if (gui.iconwnd == null) {
                        gui.iconwnd = new GobIcon.SettingsWindow(gui.iconconf, () -> Utils.defer(gui::saveiconconf));
                        gui.fitwdg(gui.add(gui.iconwnd, Utils.getprefc("wndc-icon", UI.scale(200, 200))));
                    } else {
                        gui.iconwnd.toggleVisibility();
                        if (gui.iconwnd.visible()) {
                            gui.iconwnd.parent.setfocus(gui.iconwnd);
                            gui.iconwnd.raise();
                        }
                    }
                }
            }), viewdist.c.add(viewdist.sz.x + spacer, 0));
            final Button setting = add(new Button(0, "⚙", MapWnd.this::toggleMapSettings) {
                public Object tooltip(Coord c, Widget prev) {
                    return (Text.render("Settings").tex());
                }
            }, iconbtn.c.add(iconbtn.sz.x + spacer, 0));
            pack();
        }
    }

    private class View extends MapFileWidget {
        View(MapFile file) {
            super(file, Coord.z);
        }

        private long timer = System.currentTimeMillis();

        public void drawgrid(GOut g, Coord ul, DisplayGrid disp) {
            super.drawgrid(g, ul, disp);
            if (fogA) {
                try {
                    Tex img = disp.olfog(ui);
                    if (img != null) {
                        final Color color = new Color(configuration.fogOfWarColor, true);
                        g.chcolor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
                        g.image(img, ul, cmaps.div(scalef()));
                        g.chcolor();
                    }
                } catch (Loading l) {
                }
            }
            if (fogT) {
                try {
                    Tex img = disp.olfogtemp(ui);
                    if (img != null) {
                        final Color color = new Color(configuration.fogOfWarColorTemp, true);
                        g.chcolor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
                        g.image(img, ul, cmaps.div(scalef()));
                        g.chcolor();
                    }
                } catch (Loading l) {
                }
            }

            if (!overlays.isEmpty()) {
                for (String tag : overlays) {
                    try {
                        Tex img = disp.olimg(tag);
                        if (img != null) {
                            g.chcolor(255, 255, 255, olalpha);
                            g.image(img, ul, cmaps.div(scalef()));
                            g.chcolor();
                        }
                    } catch (Loading l) {
                    }
                }
                g.chcolor();
            }

            if (!highlighed.isEmpty()) {
                int period = configuration.highlightTilePeriod;
                long now = System.currentTimeMillis();
                long dt = now - timer;
                double opac = dt % period / (period / 2.0);
                if (dt > period) timer = now;
                double ropca = opac > 1.0 ? 1.0 - (opac % 1.0) : opac;
                try {
                    Tex img = disp.highlight(highlighed.toArray(new String[0]));
                    if (img != null) {
                        Color color = MapFile.highlightColor;
                        g.chcolor(color.getRed(), color.getGreen(), color.getBlue(), (int) (ropca * 255.0 * (color.getAlpha() / 255.0)));
                        g.image(img, ul, cmaps.div(scalef()));
                        g.chcolor();
                    }
                } catch (Loading l) {
                }
            }
        }

        public boolean clickmarker(DisplayMarker mark, int button) {
            if (!decohide() && button == 1 && ui.modflags() == 0) {
                focus(mark.m);
                return (true);
            }
            return (false);
        }

        public boolean deletemarker(DisplayMarker mark, int button) {
            if (button == 1 && ui.modflags() == (UI.MOD_CTRL | UI.MOD_META)) {
                view.file.remove(mark.m);
                tool.list.change2(null);
                setfocus(tool.list);
                return (true);
            }
            return (false);
        }

        public void deletemarker(DisplayMarker mark) {
            view.file.remove(mark.m);
            tool.list.change2(null);
            setfocus(tool.list);
        }

        public boolean clickloc(Location loc, int button) {
            if (domark && (button == 1)) {
                Marker nm = new PMarker(loc.seg.id, loc.tc, "", BuddyWnd.gc[0]);
                file.add(nm);
                focus(nm);
                domark = false;
//                uploadMarks();
                return (true);
            }
            return (false);
        }

        public boolean mousedown(Coord c, int button) {
            if (domark && (button == 3)) {
                domark = false;
                return (true);
            }
            if (configuration.tempmarks) {
                int flags = ui.modflags();
                Location loc = this.curloc;
                if (loc != null) {
                    Coord hsz = sz.div(2);
                    Coord mc = c.sub(hsz).add(loc.tc);
                    Coord hiconsize = UI.scale(20, 20).div(2);
                    full:
                    for (TempMark cm : getTempMarkList()) {
                        if (check.test(cm)) {
                            for (int x = -hiconsize.x; x < hiconsize.x; x++)
                                for (int y = -hiconsize.y; y < hiconsize.y; y++)
                                    if ((cm.gc.div(scalef())).add(new Coord(x, y)).equals(mc)) {
                                        PBotGob gob = PBotGobAPI.findGobById(ui, cm.id);
                                        if (gob != null) {
                                            gob.doClick(button, flags);
                                            return (true);
                                        }
                                        break full;
                                    }
                        }
                    }
                }
            }
            return (super.mousedown(c, button));
        }

        private void drawTracking(GOut g) {
            final double dist = 90000.0D;
            for (final DowseWnd wnd : new ArrayList<>(ui.gui.dowsewnds)) {
                if (wnd.mapc == null || wnd.mapc.equals(Coord.z))
                    wnd.mapc = getRealCoord(new Coord2d(wnd.startc).floor(tilesz));
                Location loc = this.curloc;
                if (loc != null && wnd.mapc != null && !wnd.mapc.equals(Coord.z)) {
                    Coord hsz = sz.div(2);
                    final Coord gc = hsz.sub(loc.tc).add(wnd.mapc.div(scalef()));
                    final Coord mlc = gc.add((int) (Math.cos(Math.toRadians(wnd.a1())) * dist), (int) (Math.sin(Math.toRadians(wnd.a1())) * dist));
                    final Coord mrc = gc.add((int) (Math.cos(Math.toRadians(wnd.a2())) * dist), (int) (Math.sin(Math.toRadians(wnd.a2())) * dist));
                    g.chcolor(new Color(configuration.dowsecolor, true));
                    g.dottedline(gc, mlc, 1);
                    g.dottedline(gc, mrc, 1);
                    g.chcolor();
                }
            }
        }

        private Set<Long> drawparty(GOut g) {
            final Set<Long> ignore = new HashSet<>();
            double angle;
            try {
                synchronized (ui.sess.glob.party) {
                    final Coord psz = party.sz();
                    for (Party.Member m : ui.sess.glob.party.memb.values()) {
                        Coord2d ppc = m.getc();

                        if (ppc == null) // chars are located in different worlds
                            continue;

                        if (ui.sess.glob.party.memb.size() == 1) //don't do anything if you don't have a party
                            continue;

                        final Coord pc = new Coord2d(ppc).floor(tilesz);
                        final Coord mc = getRealCoord(pc);
                        Location loc = this.curloc;
                        if (loc != null) {
                            Coord hsz = sz.div(2);
                            final Coord gc = mc.equals(Coord.z) ? getNotRealCoord(pc) : hsz.sub(loc.tc).add(mc.div(scalef()));
                            ignore.add(m.gobid);
                            Gob gob = m.getgob();

                            if (gob == null) {//party member not in draw distance, draw a party colored X instead.
                                Tex tex = xmap.get(m.col);
                                if (tex == null) {
                                    tex = Text.renderstroked("\u2716", m.col, m.col, Text.num12boldFnd).tex();
                                    xmap.put(m.col, tex);
                                }
                                g.chcolor(m.col);
                                g.image(tex, gc.sub(psz.div(2)), psz);
                                Tex nametex = namemap.get(m.gobid);
                                if (nametex != null) { //if we have a nametex for this gobid because we've seen them before, go ahead and apply it
                                    g.chcolor(Color.WHITE);
                                    g.image(nametex, gc.sub(psz.div(2).add(UI.scale(-5, -10))));
                                }
                                continue;
                            }
                            angle = gob.geta();
                            final Coord front = UI.scale(8, 0).rotate(angle).add(gc);
                            final Coord right = UI.scale(-5, 5).rotate(angle).add(gc);
                            final Coord left = UI.scale(-5, -5).rotate(angle).add(gc);
                            final Coord notch = UI.scale(-2, 0).rotate(angle).add(gc);
                            Buddy buddy = gob.getattr(Buddy.class);

                            Tex tex = namemap.get(m.gobid);
                            if (tex == null && buddy != null) { //if we don't already have this nametex in memory, set one up.
                                System.out.println("tex null buddy not null");
                                tex = buddy.oname().map(name -> {
                                    Tex tex1 = Text.renderstroked(name, Color.WHITE, Color.BLACK, Text.delfnd2).tex();
                                    namemap.put(m.gobid, tex1);
                                    return (tex1);
                                }).orElse(null);
                            }
                            if (tex != null) { //apply texture if it's been successfully setup.
                                g.chcolor(Color.WHITE);
                                g.image(tex, gc.sub(psz.div(2).add(new Coord(-5, -10))));
                            }


                            g.chcolor(m.col);
                            g.poly(front, right, notch, left);
                            g.chcolor(Color.BLACK);
                            g.polyline(1, front, right, notch, left);
                            g.chcolor();
                        }
                    }
                }
            } catch (Loading l) {
                //Fail silently
            }
            return ignore;
        }

        /**
         * Ideally this will be a line -> X -> line -> X
         * Where X is some icon for destinations
         * Start at map.moveto
         * Then follow map.movequeue
         * XXX: does it need an icon?
         */
        private void drawmovement(GOut g) {
            final Coord2d movingto = mv.movingto();
            final Iterator<Coord2d> queue = mv.movequeue();
            Coord last;
            if (movingto != null) {
                g.chcolor(new Color(configuration.pfcolor, true));
                Location loc = this.curloc;
                if (loc != null) {
                    Coord hsz = sz.div(2);

                    final Coord mpc = new Coord2d(mv.getcc()).floor(tilesz);
                    final Coord rpc = getRealCoord(mpc);
                    final Coord pc = rpc.equals(Coord.z) ? getNotRealCoord(mpc) : hsz.sub(loc.tc).add(rpc.div(scalef()));

                    final Coord lc = movingto.floor(tilesz);
                    final Coord rlast = getRealCoord(lc);
                    last = rlast.equals(Coord.z) ? getNotRealCoord(lc) : hsz.sub(loc.tc).add(rlast.div(scalef()));

                    g.dottedline(pc, last, 2);
                    if (queue.hasNext()) {
                        while (queue.hasNext()) {
                            final Coord n = queue.next().floor(tilesz);
                            final Coord rnext = getRealCoord(n);
                            final Coord next = rnext.equals(Coord.z) ? getNotRealCoord(n) : hsz.sub(loc.tc).add(rnext.div(scalef()));

                            g.dottedline(last, next, 2);
                            last = next;
                        }
                    }
                }
                g.chcolor();
            }
        }

        private void questgiverLines(GOut g) {
            final double dist = 90000.0D;
            List<Coord2d> questQueue = new ArrayList<>(mv.questQueue());
            try {
                if (questQueue.size() > 0) {
                    for (Coord2d coord : questQueue) {
//                        ui.gui.mapfile.view.follow = false;
                        final Gob player = mv.player();
                        double angle = player.rc.angle(coord);
                        final Coord mc = getRealCoord(player.rc.floor(tilesz));
                        final Coord lc = mc.add((int) (Math.cos(angle) * dist), (int) (Math.sin(angle) * dist));
                        questlinemap.put(mc, lc);
                    }
                    questQueue.clear();
                    mv.questQueue().clear();
                }
                if (questlinemap.size() > 0) {
                    for (Map.Entry<Coord, Coord> entry : questlinemap.entrySet()) {
                        Location loc = this.curloc;
                        if (loc != null) {
                            Coord hsz = sz.div(2);
                            final Coord gc = hsz.sub(loc.tc).add(entry.getKey().div(scalef()));
                            final Coord mlc = hsz.sub(loc.tc).add(entry.getValue().div(scalef()));
                            g.chcolor(new Color(configuration.questlinecolor, true));
                            g.dottedline(gc, mlc, 2);
                            g.chcolor();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private Coord getRealCoord(Coord c) {
            try {
                MCache.Grid obg = ui.sess.glob.map.getgrid(c.div(cmaps));
                if (!file.lock.writeLock().tryLock())
                    throw (new Loading());
                try {
                    MapFile.GridInfo info = file.gridinfo.get(obg.id);
                    if (info == null) {
                        throw (new Loading());
                    } else
                        return (c.add(info.sc.sub(obg.gc).mul(cmaps)));
                } finally {
                    file.lock.writeLock().unlock();
                }
            } catch (Exception ignore) {
            }
            return (Coord.z);
        }

        private Coord getNotRealCoord(Coord c) {
            Coord plc = new Coord2d(mv.getcc()).floor(tilesz);
            Location ploc = resolve(player);
            Coord cloc = xlate(ploc);
            if (cloc != null) {
                return (cloc.add(c.sub(plc).div(scalef())));
            }
            return (Coord.z);
        }

        private void drawmarks(GOut g) {
            Location loc = this.curloc;
            if (loc != null) {
                Coord hsz = sz.div(2);
                for (TempMark cm : getTempMarkList()) {
                    if (loc.seg.id == cm.loc.seg.id && check.test(cm)) {
                        Tex tex = cachedzoomtex(cm.icon, cm.name, zoom);
                        if (tex != null) {
                            if (!cm.gc.equals(Coord.z)) {
                                Coord gc = hsz.sub(loc.tc).add(cm.gc.div(scalef()));
                                g.aimage(tex, gc, 0.5, 0.5);
                            }
                        }
                    }
                }
            }
        }

        Predicate<TempMark> check = (mark) -> {
            /*if (!Config.hideallicons && (Config.additonalicons.containsKey(mark.name))) {
                CheckListboxItem itm = Config.icons.get(mark.name.substring(mark.name.lastIndexOf("/") + 1));
                if (itm != null && !itm.selected)
                    return (true);
            }*/
            if (mark.gobIcon != null && mark.gobIcon.res != null && mark.gobIcon.res.get() != null) {
                GobIcon.Setting conf = ui.gui.mmap.iconconf.get(mark.gobIcon.res.get());
                if (conf != null && conf.show)
                    return (true);
            }

            if (Type.getType(mark.name) == Type.ROAD && Config.showroadmidpoint) {
                return (true);
            } else if (Type.getType(mark.name) == Type.ROADENDPOINT && Config.showroadendpoint) {
                return (true);
            } else if (Type.getType(mark.name) == Type.DUNGEONDOOR) {
                return (true);
            }
            return (false);
        };

        public void draw(GOut g) {
            g.chcolor(0, 0, 0, 128);
            g.frect(Coord.z, sz);
            g.chcolor();
            super.draw(g);
            try {
                drawmovement(g);
                questgiverLines(g);
                drawTracking(g);
                if (configuration.tempmarks && !configuration.bigmaphidemarks)
                    drawmarks(g);

                final Location loc = resolve(player);
                Coord ploc = xlate(loc);
                if (ploc != null) {
                    double angle = 0;
                    Gob player = mv.player();
                    if (player != null)
                        angle = player.geta();

                    g.chcolor(255, 0, 0, 255);
//                    g.image(plx, ploc.sub(plx.sz().div(2)));

                    final Coord coord1 = UI.scale(8, 0).rotate(angle).add(ploc);
                    final Coord coord2 = UI.scale(0, -5).rotate(angle).add(ploc);
                    final Coord coord3 = UI.scale(0, -1).rotate(angle).add(ploc);
                    final Coord coord4 = UI.scale(-8, -1).rotate(angle).add(ploc);
                    final Coord coord5 = UI.scale(-8, 1).rotate(angle).add(ploc);
                    final Coord coord6 = UI.scale(0, 1).rotate(angle).add(ploc);
                    final Coord coord7 = UI.scale(0, 5).rotate(angle).add(ploc);
                    g.poly(coord1, coord2, coord3, coord4, coord5, coord6, coord7);
                    g.chcolor(Color.BLACK);
                    g.polyline(1, coord1, coord2, coord3, coord4, coord5, coord6, coord7);
                    g.chcolor();

                    if (configuration.bigmapshowviewdist)
                        drawview(g);
                }

                final Set<Long> ignore;
                if (Config.mapdrawparty)
                    ignore = drawparty(g);
                g.chcolor();
            } catch (Exception l) {
            }
        }

        public void drawview(GOut g) {
            Coord2d sgridsz = new Coord2d(MCache.cmaps);
            Gob player = ui.gui.map.player();
            if (player != null) {
                Location loc = this.curloc;
                if (loc != null) {
                    Coord hsz = sz.div(2);
                    Coord rcd = getRealCoord(player.rc.floor(sgridsz).mul(sgridsz).sub(sgridsz.mul(4)).floor(tilesz));
                    Coord rc = hsz.sub(loc.tc).add(rcd.div(scalef()));
                    g.chcolor(new Color(configuration.distanceviewcolor, true));
                    Coord rect = sgridsz.mul(9).div(tilesz).div(scalef()).floor();
                    g.dottedline(rc, rc.add(rect.x - 1, 0), 1);
                    g.dottedline(rc.add(rect.x - 1, 0), rc.add(rect), 1);
                    g.dottedline(rc.add(rect).sub(1, 1), rc.add(0, rect.y - 1), 1);
                    g.dottedline(rc.add(0, rect.y - 1), rc, 1);
                    g.chcolor();
                }
            }
        }

        public Resource getcurs(Coord c) {
            if (domark)
                return (markcurs);
            return (super.getcurs(c));
        }

        public void tick(double dt) {
            super.tick(dt);
            try {
                if (configuration.tempmarks && System.currentTimeMillis() - lastMarkCheck > configuration.tempmarksfrequency) {
                    checkmarks();
                    lastMarkCheck = System.currentTimeMillis();
                }
            } catch (Loading l) {
            }
        }

        public Object tooltip(Coord c, Widget prev) {
            if (configuration.tempmarks) {
                Location loc = this.curloc;
                if (loc != null) {
                    Coord hsz = sz.div(2);
                    Coord mc = c.sub(hsz).add(loc.tc);
                    Coord hiconsize = UI.scale(20, 20).div(2);
                    for (TempMark cm : getTempMarkList()) {
                        if (check.test(cm)) {
                            for (int x = -hiconsize.x; x < hiconsize.x; x++)
                                for (int y = -hiconsize.y; y < hiconsize.y; y++)
                                    if ((cm.gc.div(scalef())).add(new Coord(x, y)).equals(mc))
                                        return (cm.tooltip);
                        }
                    }
                }
            }
            return (super.tooltip(c, prev));
        }

        public void checkmarks() {
            Function<Gob, Tex> setTex = (gob) -> {
                Tex tex = null;
                try {
                    GobIcon icon = gob.getattr(GobIcon.class);
                    Optional<Resource> ores = gob.res();
                    if (ores.isPresent() && ui.gui.mmap.iconconf != null) {
                        Resource res = ores.get();
                        boolean check = false;
                        if (icon != null && icon.res != null && icon.res.get() != null) {
                            GobIcon.Setting conf = ui.gui.mmap.iconconf.get(icon.res.get());
                            if (conf != null && conf.show)
                                check = true;
                        }
                        /*if (!Config.hideallicons && (icon != null || Config.additonalicons.containsKey(res.name))) {
                            CheckListboxItem itm = Config.icons.get(res.basename());
                            if (configuration.tempmarksall || (itm != null && !itm.selected) || check) {
                                if (icon != null)
                                    tex = cachedtex(gob);
                                else
                                    tex = Config.additonalicons.get(res.name).get();
                            }
                        } else */
                        if (icon != null) {
                            if (configuration.tempmarksall || check) {
                                tex = cachedtex(gob);
                            }
                        } else if (gob.type == Type.ROAD && Config.showroadmidpoint) {
                            tex = LocalMiniMap.roadicn;
                        } else if (gob.type == Type.ROADENDPOINT && Config.showroadendpoint) {
                            tex = LocalMiniMap.roadicn;
                        } else if (gob.type == Type.DUNGEONDOOR) {
                            int stage = 0;
                            if (gob.getattr(ResDrawable.class) != null)
                                stage = gob.getattr(ResDrawable.class).sdt.peekrbuf(0);
                            if (stage == 10 || stage == 14)
                                tex = LocalMiniMap.dooricn;
                        }
                    }
                } catch (Loading ignored) {
                }
                return (tex);
            };
            final List<TempMark> marks = new ArrayList<>(getTempMarkList());
            for (TempMark cm : marks) {
                PBotGob g = PBotGobAPI.findGobById(ui, cm.id);
                if (g == null) {
                    if (System.currentTimeMillis() - cm.start > configuration.tempmarkstime * 1000L) {
                        tempMarkList.remove(cm);
                    } else if (cm.near) {
                        Gob player = mv.player();
                        if (player != null) {
                            Coord2d cmap = new Coord2d(cmaps);
                            Coord2d vs = player.rc.floord(cmap).mul(cmap).sub(cmap.mul(4));
                            if (cm.rc.isect(vs.add(11, 11), cmap.mul(9).sub(22, 22))) {
                                tempMarkList.remove(cm);
                            } else {
                                cm.near = false;
                            }
                        }
                    }
                } else {
                    Location loc = this.curloc;
                    if (loc != null) {
                        for (TempMark customMark : getTempMarkList()) {
                            if (cm.id == customMark.id) {
                                if (!customMark.near) {
                                    customMark.near = true;
                                }
                                customMark.start = System.currentTimeMillis();
                                if (!customMark.rc.equals(g.getRcCoords())) {
                                    customMark.rc = g.getRcCoords();
                                    customMark.gc = getRealCoord(g.getRcCoords().floor(tilesz));
                                }
                                if (customMark.gc.equals(Coord.z)) {
                                    customMark.gc = getRealCoord(g.getRcCoords().floor(tilesz));
                                }
                                if (customMark.loc.seg != loc.seg) {
                                    customMark.loc = loc;
                                }
                                if (customMark.dead != g.isKnocked()) {
                                    customMark.dead = g.isKnocked();
                                    customMark.icon = (TexI) setTex.apply(g.gob);
                                }
                                if (customMark.gobIcon == null) {
                                    GobIcon gobIcon = g.gob.getattr(GobIcon.class);
                                    if (gobIcon != null)
                                        customMark.gobIcon = gobIcon;
                                }
                                break;
                            }
                        }
                    }
                }
            }

            marks.clear();
            marks.addAll(getTempMarkList());

            for (Gob gob : ui.sess.glob.oc.getallgobs()) {
                if (marks.stream().noneMatch(m -> m.id == gob.id)) {
                    TexI tex = (TexI) setTex.apply(gob);
                    GobIcon icon = gob.getattr(GobIcon.class);
                    Location loc = this.curloc;
                    if (tex != null && loc != null)
                        tempMarkList.add(new TempMark(gob.getres().name, icon, gob.isDead(), gob.id, gob.rc, getRealCoord(gob.rc.floor(tilesz)), loc, tex));
                }
            }
        }

        private Tex cachedtex(Gob gob) {
            GobIcon icon = gob.getattr(GobIcon.class);
            if (icon != null) {
                boolean isdead = gob.isDead();
                int size = UI.scale(20);
                Indir<Resource> indir = icon.res;
                if (indir != null) {
                    Resource res = indir.get();
                    if (res != null) {
                        Tex itex = cachedImageTex.computeIfAbsent(res.name + (isdead ? "-dead" : ""), name -> {
                            GobIcon.Image img = icon.img();
                            Tex tex = isdead ? img.texgrey() : img.tex();
                            if ((tex.sz().x > size) || (tex.sz().y > size)) {
                                tex = img.tex(Coord.of(size), isdead);
                            }
                            return (tex);
                        });
                        return (itex);
                    }
                }
            }
            return (null);
        }

        private Tex cachedzoomtex(Tex tex, String name, int z) {
            Tex itex;
            synchronized (cachedZoomImageTex) {
                itex = cachedZoomImageTex.get(name + "_zoom" + z);
                if (itex == null && tex != null) {
                    Coord zoomc = new Coord2d(tex.sz()).round();
                    itex = new TexI(PUtils.uiscale(((TexI) tex).back, zoomc));
                    cachedZoomImageTex.put(name + "_zoom" + z, itex);
                }
            }
            return (itex);
        }
    }

    public void resolveNames() {//used to load name textures even while the map is closed
        try {
            synchronized (ui.sess.glob.party) {
                for (Party.Member m : ui.sess.glob.party.memb.values()) {
                    Coord2d ppc = m.getc();
                    if (ppc == null) // chars are located in different worlds
                        continue;
                    if (ui.sess.glob.party.memb.size() == 1) //don't do anything if you don't have a party
                        continue;
                    Gob gob = m.getgob();
                    if (gob != null) {
                        Buddy buddy = gob.getattr(Buddy.class);
                        Tex tex = namemap.get(m.gobid);
                        if (tex == null && buddy != null) { //if we don't already have this nametex in memory, set one up.
                            tex = buddy.oname().map(name -> {
                                Tex tex1 = Text.renderstroked(name, Color.WHITE, Color.BLACK, Text.delfnd2).tex();
                                namemap.put(m.gobid, tex1);
                                return (tex1);
                            }).orElse(null);
                        }
                    }
                }
            }
        } catch (Loading l) {
            //Fail silently
        }
    }

    public void tick(double dt) {
        super.tick(dt);
        if (Config.mapdrawparty)
            resolveNames();
        synchronized (deferred) {
            for (Iterator<Runnable> i = deferred.iterator(); i.hasNext(); ) {
                Runnable task = i.next();
                try {
                    task.run();
                } catch (Loading l) {
                    continue;
                }
                i.remove();
            }
        }
        if (visible && (markerseq != view.file.markerseq)) {
            if (view.file.lock.readLock().tryLock()) {
                try {
                    this.markers = view.file.markers.stream().filter(filter).sorted(mcmp).collect(Collectors.toList());
                    markerseq = view.file.markerseq;
                } finally {
                    view.file.lock.readLock().unlock();
                }
            }
        }
    }

    public void selectMarker(String name) {
        if (markers != null && !markers.isEmpty()) {
            for (Marker marker : markers) {
                if (marker.nm.equals(name)) {
                    tool.list.change(marker);
//                    view.center(new SpecLocator(marker.seg, marker.tc));
                }
            }
        }
    }

    public static final Color every = new Color(255, 255, 255, 16), other = new Color(255, 255, 255, 32), found = new Color(255, 255, 0, 32);

    public void toggleMapGrid() {
        configuration.bigmapshowgrid = !configuration.bigmapshowgrid;
        Utils.setprefb("bigmapshowgrid", configuration.bigmapshowgrid);
    }

    public void toggleMapViewDist() {
        configuration.bigmapshowviewdist = !configuration.bigmapshowviewdist;
        Utils.setprefb("bigmapshowviewdist", configuration.bigmapshowviewdist);
    }

    public void toggleMapSettings() {
        GameUI gui = getparent(GameUI.class);
        if (gui != null) {
            if (!gui.opts.visible) {
                gui.opts.show();
                gui.opts.raise();
                gui.fitwdg(gui.opts);
                setfocus(gui.opts);
                gui.opts.chpanel(gui.opts.mapPanel);
            } else {
                gui.opts.show(false);
            }
        }
    }

    private static final Pair[] filters = new Pair[]{
            new Pair<>("-- All --", null),
            new Pair<>("--- Custom ---", "flg"),
            new Pair<>("Abyssal Chasm", "abyssalchasm"),
            new Pair<>("Ancient Windthrow", "windthrow"),
            new Pair<>("Cave Organ", "caveorgan"),
            new Pair<>("Clay Pit", "claypit"),
            new Pair<>("Coral Reef", "coralreef"),
            new Pair<>("Fairy Stone", "fairystone"),
            new Pair<>("Crystal Rock", "crystalpatch"),
            new Pair<>("Geyser", "geyser"),
            new Pair<>("Great Cave Organ", "caveorgan"),
            new Pair<>("Guano Pile", "guanopile"),
            new Pair<>("Headwaters", "headwaters"),
            new Pair<>("Heart of the Woods", "woodheart"),
            new Pair<>("Ice Spire", "icespire"),
            new Pair<>("Irminsul", "irminsul"),
            new Pair<>("Jotun Mussel", "jotunmussel"),
            new Pair<>("Kelp Island", "algaeblob"),
            new Pair<>("Lilypad Lotus", "lilypadlotus"),
            new Pair<>("Monolith", "monolith"),
            new Pair<>("Quest Givers", "qst"),
            new Pair<>("Rock Crystal", "crystalpatch"),
            new Pair<>("Salt Basin", "saltbasin"),
            new Pair<>("Swirling Vortex", "watervortex"),
            new Pair<>("Tarpit", "tarpit"),
            new Pair<>("Thingwall", "thingwall"),
            new Pair<>("Cave", "cave")
    };

    public class MarkerList extends Searchbox<Marker> {
        private final Text.Foundry fnd = CharWnd.attrf;

        public Marker listitem(int idx) {
            return (markers.get(idx));
        }

        public int listitems() {
            return (markers.size());
        }

        public boolean searchmatch(int idx, String txt) {
            return (markers.get(idx).nm.toLowerCase().contains(txt.toLowerCase()));
        }

        public MarkerList(int w, int n) {
            super(w, n, UI.scale(20));
        }

        private Function<String, Text> names = new CachedFunction<>(500, fnd::render);

        protected void drawbg(GOut g) {
        }

        public void drawitem(GOut g, Marker mark, int idx) {
            if (soughtitem(idx)) {
                g.chcolor(found);
                g.frect(Coord.z, g.sz());
            }
            g.chcolor(((idx % 2) == 0) ? every : other);
            g.frect(Coord.z, g.sz);
            if (mark instanceof PMarker)
                g.chcolor(((PMarker) mark).color);
            else
                g.chcolor();
            g.aimage(names.apply(mark.nm).tex(), new Coord(UI.scale(5), itemh / 2), 0, 0.5);
        }

        protected void itemclick(Marker mark, int button) {
            if (button == 1) {
                if (ui.modflags() == (UI.MOD_CTRL | UI.MOD_META)) {
                    view.file.remove(mark);
                    change2(null);
                    setfocus(MapWnd.MarkerList.this);
                } else
                    change(mark);
            }
            if (button == 3)
                change2(mark);
        }

        public void change(Marker mark) {
            //change2(mark);
            if (mark != null)
                view.center(new SpecLocator(view, mark.seg, mark.tc));
        }

        public void change2(Marker mark) {
            this.sel = mark;

            if (tool.namesel != null) {
                ui.destroy(tool.namesel);
                tool.namesel = null;
                if (colsel != null) {
                    ui.destroy(colsel);
                    colsel = null;
                }
                if (mremove != null) {
                    ui.destroy(mremove);
                    mremove = null;
                }
            }

            if (mark != null) {
                if (tool.namesel == null) {
                    tool.namesel = tool.add(new TextEntry(UI.scale(200), "") {
                        {
                            dshow = true;
                        }

                        @Override
                        public boolean close() {
                            change2(null);
                            MarkerList.this.parent.setfocus(MarkerList.this);
                            return (true);
                        }

                        @Override
                        public void activate(String text) {
                            mark.nm = text;
                            view.file.update(mark);
                            commit();
                            change2(null);
                            MarkerList.this.parent.setfocus(MarkerList.this);
                            uploadMark(mark);
                        }
                    });
                }
                setfocus(tool.namesel);
                tool.namesel.settext(mark.nm);
                tool.namesel.buf.point(mark.nm.length());
                tool.namesel.commit();
                if (mark instanceof PMarker) {
                    PMarker pm = (PMarker) mark;
                    colsel = tool.add(new GroupSelector(0) {
                        public void changed(int group) {
                            this.group = group;
                            pm.color = BuddyWnd.gc[group];
                            view.file.update(mark);
                            uploadMark(mark);
                        }
                    });
                    if ((colsel.group = Utils.index(BuddyWnd.gc, pm.color)) < 0)
                        colsel.group = 0;
                }
                mremove = tool.add(new Button(UI.scale(200), "Remove", false) {
                    public void click() {
                        view.file.remove(mark);
                        setfocus(MarkerList.this);
                        change2(null);
                    }
                });
                MapWnd.this.resize(asz);
            }
        }
    }

    public void resize(Coord sz) {
        super.resize(sz);
        tool.resize(sz.y);
        if (!decohide()) {
            tool.c = new Coord(sz.x - tool.sz.x, 0);
            viewf.resize(new Coord(sz.x - tool.sz.x - UI.scale(10), sz.y));
        } else {
            viewf.resize(sz);
            tool.c = Coord.z;
        }
        view.resize(viewf.inner());
        toolbar2.c = viewf.c.add(viewf.sz.x / 2 - toolbar2.sz.x / 2, 0).sub(0, UI.scale(7));
        toolbar.c = viewf.c.add(0, viewf.sz.y - toolbar.sz.y).add(UI.scale(2), UI.scale(-2));
        zoombar.c = viewf.c.add(viewf.sz.x - zoombar.sz.x, viewf.sz.y - zoombar.sz.y).sub(UI.scale(7, 7));
    }

    public void compact(boolean a) {
        tool.show(!a);
        if (a)
            delfocusable(tool);
        else
            newfocusable(tool);
        decohide(a);
        resize(asz);
    }

    public void decohide(boolean h) {
        this.decohide = h;
    }

    public boolean decohide() {
        return (decohide);
    }

    public void recenter() {
        view.follow(player);
    }

    public void focus(Marker m) {
        tool.list.change2(m);
        tool.list.display(m);
    }

    @Override
    public void gotfocus() {
        super.gotfocus();
        setfocus(tool);
    }

    private static final Tex sizer = Resource.loadtex("gfx/hud/wnd/sizer");

    protected void drawframe(GOut g) {
        g.image(sizer, ctl.add(csz).sub(sizer.sz()));
        super.drawframe(g);
    }

    public boolean keydown(KeyEvent ev) {
        if (super.keydown(ev))
            return (true);
        if (key_esc.match(ev)) {
            show(false);
            return (true);
        }
        if (ev.getKeyCode() == KeyEvent.VK_HOME) {
            if (ui.modflags() == UI.MOD_CTRL) {
                view.file.updategrids(ui.sess.glob.map, ui.sess.glob.map.grids.values());
                view.refreshDisplayGrid();
            } else {
                questlinemap.clear();
                recenter();
            }
            return (true);
        }
        return (false);
    }

    public void markobj(long gobid, long oid, Indir<Resource> resid, String nm) {
        configuration.treesMap.put(gobid, nm);
        synchronized (deferred) {
            deferred.add(new Runnable() {
                double f = 0;

                public void run() {
                    Resource res = resid.get();
                    String rnm = nm;
                    if (rnm == null) {
                        Resource.Tooltip tt = res.layer(Resource.tooltip);
                        if (tt == null)
                            return;
                        rnm = tt.origt;
                    }
                    double now = Utils.rtime();
                    if (f == 0)
                        f = now;
                    Gob gob = ui.sess.glob.oc.getgob(gobid);
                    if (gob == null) {
                        if (now - f < 1.0)
                            throw (new Loading());
                        return;
                    }
                    Coord tc = gob.rc.floor(tilesz);
                    MCache.Grid obg = ui.sess.glob.map.getgrid(tc.div(cmaps));
                    if (!view.file.lock.writeLock().tryLock())
                        throw (new Loading());
                    try {
                        MapFile.GridInfo info = view.file.gridinfo.get(obg.id);
                        if (info == null)
                            throw (new Loading());
                        Coord sc = tc.add(info.sc.sub(obg.gc).mul(cmaps));
                        SMarker prev = view.file.smarkers.get(oid);
                        if (prev == null) {
                            SMarker nsm = new SMarker(info.seg, sc, rnm, oid, new Resource.Spec(Resource.remote(), res.name, res.ver));
                            view.file.add(nsm);
                            uploadMark(nsm);
                        } else {
                            if ((prev.seg != info.seg) || !prev.tc.equals(sc)) {
                                prev.seg = info.seg;
                                prev.tc = sc;
                                view.file.update(prev);
                                uploadMark(prev);
                            }
                        }
                    } finally {
                        view.file.lock.writeLock().unlock();
                    }
                }
            });
        }
    }

    public void markobj(long gobid, Gob gob, String nm, boolean sendeable) {
        synchronized (deferred) {
            deferred.add(new Runnable() {
                double f = 0;

                public void run() {
                    GobIcon icon = gob.getattr(GobIcon.class);
                    Resource iconRes;
                    if (icon == null) {
                        iconRes = Resource.local().loadwait("gfx/hud/wndmap/btns/center", 1);
                    } else {
                        iconRes = icon.res.get();
                    }

                    Resource res = gob.getres();
                    String rnm = nm;
                    if (rnm == null) {
                        Resource.Tooltip tt = res.layer(Resource.tooltip);
                        if (tt == null)
                            return;
                        rnm = tt.t;
                    }
                    double now = Utils.rtime();
                    if (f == 0)
                        f = now;
                    Gob gob = ui.sess.glob.oc.getgob(gobid);
                    if (gob == null) {
                        if (now - f < 1.0)
                            throw (new Loading());
                        return;
                    }
                    Coord tc = gob.rc.floor(tilesz);
                    MCache.Grid obg = ui.sess.glob.map.getgrid(tc.div(cmaps));
                    if (!view.file.lock.writeLock().tryLock())
                        throw (new Loading());
                    try {
                        MapFile.GridInfo info = view.file.gridinfo.get(obg.id);
                        if (info == null)
                            throw (new Loading());
                        Coord sc = tc.add(info.sc.sub(obg.gc).mul(cmaps));

                        DoubleUnaryOperator offset = (s) -> {
                            int n = 100 * 11;
                            return (s % n < 0 ? s % n + n : s % n);
                        };
                        double x = offset.applyAsDouble(gob.getc().x);
                        double y = offset.applyAsDouble(gob.getc().y);

                        long oid = Objects.hash(res.name, obg.id, x, y, gob.getc().z);

                        SMarker prev = view.file.smarkers.get(oid);
                        if (prev == null) {
                            SMarker mark = new SMarker(info.seg, sc, rnm, oid, new Resource.Spec(Resource.remote(), iconRes.name, iconRes.ver));
                            mark.makeAutosend(sendeable);
                            view.file.add(mark);
                            if (sendeable)
                                uploadMark(mark);
                        } else {
                            if ((prev.seg != info.seg) || !prev.tc.equals(sc)) {
                                prev.seg = info.seg;
                                prev.tc = sc;
                                view.file.update(prev);
                                if (sendeable)
                                    uploadMark(prev);
                            }
                        }
                    } finally {
                        view.file.lock.writeLock().unlock();
                    }
                }
            });
        }
    }

    public void markobj(long gobid, Gob gob, String nm) {
        markobj(gobid, gob, nm, true);
    }

    public void uploadMarks() {
        view.uploadMarks();
    }

    public void uploadMark(Marker marker) {
        view.uploadMark(marker);
    }

    @Override
    public void close() {
        show(false);
        mv.questQueue().clear();
        questlinemap.clear();
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender == cbtn) {
            show(false);
        } else
            super.wdgmsg(sender, msg, args);
    }

    @Override
    public boolean type(char key, KeyEvent ev) {
        return (true);
    }

    private final List<String> highlighed = Collections.synchronizedList(new ArrayList<>());

    public void highlight(final String tilename, final boolean toggle) {
        synchronized (highlighed) {
            if (toggle) {
                if (!highlighed.contains(tilename))
                    highlighed.add(tilename);
            } else {
                highlighed.remove(tilename);
            }
        }
    }
}
