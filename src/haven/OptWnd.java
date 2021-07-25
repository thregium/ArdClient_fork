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


import haven.purus.pathfinder.Pathfinder;
import haven.purus.pbot.PBotDiscord;
import haven.purus.pbot.PBotUtils;
import haven.resutil.BPRadSprite;
import haven.resutil.FoodInfo;
import haven.resutil.WaterTile;
import haven.sloth.gfx.GobSpeedSprite;
import haven.sloth.gfx.HitboxMesh;
import haven.sloth.gfx.SnowFall;
import haven.sloth.gob.Movable;
import haven.sloth.gob.Type;
import integrations.mapv4.MappingClient;
import modification.configuration;
import modification.dev;
import modification.resources;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import static haven.DefSettings.ALLWATERCOL;
import static haven.DefSettings.AMBERMENU;
import static haven.DefSettings.ANIMALDANGERCOLOR;
import static haven.DefSettings.ANIMALPATHCOL;
import static haven.DefSettings.BEEHIVECOLOR;
import static haven.DefSettings.BTNCOL;
import static haven.DefSettings.BUGGEDMENU;
import static haven.DefSettings.CHEESERACKMISSINGCOLOR;
import static haven.DefSettings.CLOSEFORMENU;
import static haven.DefSettings.DARKMODE;
import static haven.DefSettings.DEBUG;
import static haven.DefSettings.DEEPWATERCOL;
import static haven.DefSettings.DRAWGRIDRADIUS;
import static haven.DefSettings.ERRORTEXTCOLOR;
import static haven.DefSettings.GARDENPOTDONECOLOR;
import static haven.DefSettings.GOBPATHCOL;
import static haven.DefSettings.GUIDESCOLOR;
import static haven.DefSettings.HIDDENCOLOR;
import static haven.DefSettings.HITBOXCOLOR;
import static haven.DefSettings.HUDTHEME;
import static haven.DefSettings.KEEPGOBS;
import static haven.DefSettings.KEEPGRIDS;
import static haven.DefSettings.LIMITPATHFINDING;
import static haven.DefSettings.MAPTYPE;
import static haven.DefSettings.MINIMAPTYPE;
import static haven.DefSettings.NVAMBIENTCOL;
import static haven.DefSettings.NVDIFFUSECOL;
import static haven.DefSettings.NVSPECCOC;
import static haven.DefSettings.OCEANWATERCOL;
import static haven.DefSettings.PATHFINDINGTIER;
import static haven.DefSettings.PLAYERPATHCOL;
import static haven.DefSettings.RESEARCHUNTILGOAL;
import static haven.DefSettings.SHALLOWOCEANWATERCOL;
import static haven.DefSettings.SHALLOWWATERCOL;
import static haven.DefSettings.SHOWANIMALPATH;
import static haven.DefSettings.SHOWFKBELT;
import static haven.DefSettings.SHOWGOBPATH;
import static haven.DefSettings.SHOWGOBS;
import static haven.DefSettings.SHOWHALO;
import static haven.DefSettings.SHOWHALOONHEARTH;
import static haven.DefSettings.SHOWMAP;
import static haven.DefSettings.SHOWNBELT;
import static haven.DefSettings.SHOWNPBELT;
import static haven.DefSettings.SHOWPLAYERPATH;
import static haven.DefSettings.SLIDERCOL;
import static haven.DefSettings.SUPPORTDANGERCOLOR;
import static haven.DefSettings.SYMMETRICOUTLINES;
import static haven.DefSettings.THEMES;
import static haven.DefSettings.TROUGHCOLOR;
import static haven.DefSettings.TXBCOL;
import static haven.DefSettings.WATERCOL;
import static haven.DefSettings.WIREFRAMEMODE;
import static haven.DefSettings.WNDCOL;


public class OptWnd extends Window {
    public static final int VERTICAL_MARGIN = 10;
    public static final int HORIZONTAL_MARGIN = 5;
    public static final int VERTICAL_AUDIO_MARGIN = 5;
    private static final Text.Foundry fonttest = new Text.Foundry(Text.sans, 10).aa(true);
    private static final List<Integer> caveindust = Arrays.asList(1, 2, 5, 10, 15, 30, 45, 60, 120);
    private static final Pair[] combatkeys = new Pair[]{
            new Pair<>("[1-5] and [shift + 1-5]", 0),
            new Pair<>("[1-5] and [F1-F5]", 1),
            new Pair<>("[F1-F10]", 2),
            new Pair<>("[1-10]", 3)
    };
    private static final List<Integer> fontSize = Arrays.asList(10, 11, 12, 13, 14, 15, 16);
    private static final List<String> statSize = Arrays.asList("1", "2", "5", "10", "25", "50", "100", "200", "500", "1000");
    private static final List<String> afkTime = Arrays.asList("0", "5", "10", "15", "20", "25", "30", "45", "60");
    private static final List<String> AutoDrinkTime = Arrays.asList("1", "3", "5", "10", "15", "20", "25", "30", "45", "60");
    private static final List<String> menuSize = Arrays.asList("4", "5", "6", "7", "8", "9", "10");
    private static List<String> pictureList = configuration.findFiles(configuration.picturePath, Arrays.asList(".png", ".jpg", ".gif"));
    public final Panel main, video, audio, display, map, general, combat, control, uis, uip, quality, mapping, flowermenus, quickactionsettings, soundalarms, hidesettings, studydesksettings, autodropsettings, keybindsettings, chatsettings, clearboulders, clearbushes, cleartrees, clearhides, discord, additions, modification;
    public Panel waterPanel, qualityPanel, mapPanel, devPanel;
    public Panel current;
    public CheckBox discordcheckbox, menugridcheckbox;
    CheckBox sm = null, rm = null, lt = null, bt = null, ltl, discordrole, discorduser;

    public OptWnd(boolean gopts) {
        super(new Coord(620, 400), "Options", true);

        main = add(new Panel());
        video = add(new VideoPanel(main));
        audio = add(new Panel());
        display = add(new Panel());
        map = add(new Panel());
        general = add(new Panel());
        combat = add(new Panel());
        control = add(new Panel());
        uis = add(new Panel());
        uip = add(new Panel());
        quality = add(new Panel());
        flowermenus = add(new Panel());
        quickactionsettings = add(new Panel());
        soundalarms = add(new Panel());
        hidesettings = add(new Panel());
        studydesksettings = add(new Panel());
        autodropsettings = add(new Panel());
        keybindsettings = add(new Panel());
        chatsettings = add(new Panel());
        clearboulders = add(new Panel());
        clearbushes = add(new Panel());
        cleartrees = add(new Panel());
        clearhides = add(new Panel());
        additions = add(new Panel());
        discord = add(new Panel());
        mapping = add(new Panel());
        modification = add(new Panel());
        waterPanel = add(new Panel());
        qualityPanel = add(new Panel());
        mapPanel = add(new Panel());
        devPanel = add(new Panel());

        initMain(gopts);
        initAudio();
        initDisplay();
        initMap();
        initGeneral();
        initCombat();
        initControl();
        initUis();
        initTheme();
        initQuality();
        initFlowermenus();
        initquickactionsettings();
        initSoundAlarms();
        initHideMenu();
        initstudydesksettings();
        initautodropsettings();
        initkeybindsettings();
        initchatsettings();
        initAdditions();
        initMapping();
        initDiscord();
        initModification();
        initWater();
        initQualityPanel();
        initMapPanel();
        initDevPanel();

        chpanel(main);
    }

    public OptWnd() {
        this(true);
    }

    static private Scrollport.Scrollcont withScrollport(Widget widget, Coord sz) {
        final Scrollport scroll = new Scrollport(sz);
        widget.add(scroll, new Coord(0, 0));
        return scroll.cont;
    }

    static private Scrollport withScroll(Widget widget, Coord sz) {
        final Scrollport scroll = new Scrollport(sz);
        widget.add(scroll, new Coord(0, 0));
        return scroll;
    }

    public void chpanel(Panel p) {
        if (current != null)
            current.hide();
        (current = p).show();
    }

    private Widget ColorPreWithLabel(final String text, final IndirSetting<Color> cl) {
        final Widget container = new Widget();
        final Label lbl = new Label(text);
        final IndirColorPreview pre = new IndirColorPreview(new Coord(16, 16), cl);
        final int height = Math.max(lbl.sz.y, pre.sz.y) / 2;
        container.add(lbl, new Coord(0, height - lbl.sz.y / 2));
        container.add(pre, new Coord(lbl.sz.x, height - pre.sz.y / 2));
        container.pack();
        return container;
    }

    private Widget ColorPreWithLabel(final String text, final IndirSetting<Color> cl, final Consumer<Color> cb) {
        final Widget container = new Widget();
        final Label lbl = new Label(text);
        final IndirColorPreview pre = new IndirColorPreview(new Coord(16, 16), cl, cb);
        final int height = Math.max(lbl.sz.y, pre.sz.y) / 2;
        container.add(lbl, new Coord(0, height - lbl.sz.y / 2));
        container.add(pre, new Coord(lbl.sz.x, height - pre.sz.y / 2));
        container.pack();
        return container;
    }

    private void initMapping() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(mapping, new Coord(620, 350)));
        appender.setVerticalMargin(VERTICAL_MARGIN);
        appender.setHorizontalMargin(HORIZONTAL_MARGIN);
        appender.add(new Label("Online Auto-Mapper Service:"));

        appender.addRow(new Label("Server URL:"),
                new TextEntry(240, Utils.getpref("vendan-mapv4-endpoint", "")) {
                    public boolean keydown(KeyEvent ev) {
                        if (!parent.visible)
                            return false;
                        Utils.setpref("vendan-mapv4-endpoint", text);
                        if (ui.sess != null && ui.sess.alive() && ui.sess.username != null) {
                            MappingClient.getInstance(ui.sess.username).SetEndpoint(Utils.getpref("vendan-mapv4-endpoint", ""));
                        }
                        return buf.key(ev);
                    }
                }
        );

        appender.add(new CheckBox("Enable mapv4 mapper") {
            public void set(boolean val) {
                if (ui.sess != null && ui.sess.alive() && ui.sess.username != null) {
                    configuration.saveMapSetting(ui.sess.username, val, "mapper");
                    MappingClient.getInstance(ui.sess.username).EnableGridUploads(val);
//                    MappingClient.getInstance(ui.sess.username).EnableTracking(val);
                    a = val;
                }
            }

            public void tick(double dt) {
                super.tick(dt);
                if (ui.sess != null && ui.sess.alive() && ui.sess.username != null) {
                    boolean b = configuration.loadMapSetting(ui.sess.username, "mapper");
                    if (a != b) {
                        a = b;
                        MappingClient.getInstance(ui.sess.username).EnableGridUploads(a);
//                        MappingClient.getInstance(ui.sess.username).EnableTracking(a);
                    }
                }
            }
        });

//        appender.add(new CheckBox("Hide character name") {
//            {
//                a = Config.mapperHashName;
//            }
//
//            public void set(boolean val) {
//                Utils.setprefb("mapperHashName", val);
//                Config.mapperHashName = val;
//                a = val;
//            }
//        });
        appender.add(new CheckBox("Enable navigation tracking") {
            public void set(boolean val) {
                if (ui.sess != null && ui.sess.alive() && ui.sess.username != null) {
                    configuration.saveMapSetting(ui.sess.username, val, "track");
                    MappingClient.getInstance(ui.sess.username).EnableTracking(val);
                    a = val;
                }
            }

            public void tick(double dt) {
                super.tick(dt);
                if (ui.sess != null && ui.sess.alive() && ui.sess.username != null) {
                    boolean b = configuration.loadMapSetting(ui.sess.username, "track");
                    if (a != b) {
                        a = b;
                        MappingClient.getInstance(ui.sess.username).EnableTracking(a);
                    }
                }
            }
        });
        appender.add(new CheckBox("Upload custom GREEN markers to map") {
            public void set(boolean val) {
                if (ui.sess != null && ui.sess.alive() && ui.sess.username != null) {
                    configuration.saveMapSetting(ui.sess.username, val, "green");
                    a = val;
                }
            }

            public void tick(double dt) {
                super.tick(dt);
                if (ui.sess != null && ui.sess.alive() && ui.sess.username != null) {
                    boolean b = configuration.loadMapSetting(ui.sess.username, "green");
                    if (a != b) {
                        a = b;
                    }
                }
            }
        });

        mapping.add(new PButton(200, "Back", 27, main), new Coord(210, 360));
        mapping.pack();
    }

    private void initMain(boolean gopts) {
        main.add(new PButton(200, "Video", 'v', video), new Coord(0, 0));
        main.add(new PButton(200, "Audio", 'a', audio), new Coord(0, 30));
        main.add(new PButton(200, "Display", 'd', display), new Coord(0, 60));
        main.add(new PButton(200, "Map", 'm', map), new Coord(0, 90));
        main.add(new PButton(200, "General", 'g', general), new Coord(210, 0));
        main.add(new PButton(200, "Combat", 'c', combat), new Coord(210, 30));
        main.add(new PButton(200, "Control", 'k', control), new Coord(210, 60));
        main.add(new PButton(200, "UI", 'u', uis), new Coord(210, 90));
        main.add(new PButton(200, "Item Overlay", 'q', quality), new Coord(420, 0));
        main.add(new PButton(200, "Pop-up Menu", 'f', flowermenus), new Coord(420, 30));
        main.add(new PButton(200, "Quick Actions", 'b', quickactionsettings), new Coord(420, 60));
        main.add(new PButton(200, "Sound Alarms", 's', soundalarms), new Coord(420, 90));
        main.add(new PButton(200, "World Overlay", 'h', hidesettings), new Coord(420, 120));
        main.add(new PButton(200, "Study Desk", 'o', studydesksettings), new Coord(0, 120));
        main.add(new PButton(200, "Keybinds", 'p', keybindsettings), new Coord(210, 120));
        main.add(new PButton(200, "Chat", 'c', chatsettings), new Coord(420, 150));
        main.add(new PButton(200, "Theme", 't', uip), new Coord(0, 150));
        main.add(new PButton(200, "Autodrop", 's', autodropsettings), new Coord(420, 180));
        main.add(new PButton(200, "Additional settings", 'z', additions), new Coord(0, 180));
        main.add(new PButton(200, "PBotDiscord", 'z', discord), new Coord(0, 210));
        main.add(new PButton(200, "Mapping", 'z', mapping), new Coord(420, 210));
        main.add(new PButton(200, "Modification", 'z', modification), new Coord(0, 240));
        if (gopts) {
//            main.add(new Button(200, "Disconnect Discord") {
//                public void click() {
//                    ui.gui.discordconnected = false;
//                    if (Discord.jdalogin != null) {
//                        PBotUtils.sysMsg(ui, "Discord Disconnected", Color.white);
//                        ui.gui.discordconnected = false;
//                        Discord.jdalogin.shutdownNow();
//                        Discord.jdalogin = null;
//                        for (int i = 0; i < 15; i++) {
//                            for (Widget w = ui.gui.chat.lchild; w != null; w = w.prev) {
//                                if (w instanceof ChatUI.DiscordChat)
//                                    w.destroy();
//                            }
//                        }
//                    } else
//                        PBotUtils.sysMsg(ui, "Not currently connected.", Color.white);
//                }
//            }, new Coord(210, 150));
//            main.add(new Button(200, "Join Village Discord") {
//                public void click() {
//                    if (!ui.gui.discordconnected) {
//                        if (Config.discordtoken != null) {
//                            new Thread(new Discord(ui.gui, "normal")).start();
//                            ui.gui.discordconnected = true;
//                        } else
//                            PBotUtils.sysMsg(ui, "No Key Detected, if there is one in chat settings you might need to relog.", Color.white);
//                    } else
//                        PBotUtils.sysMsg(ui, "Already connected.", Color.white);
//                }
//            }, new Coord(210, 180));
//            main.add(new Button(200, "Join Ingame Discord") {
//                public void click() {
//                    if (ui.gui.discordconnected)
//                        PBotUtils.sysMsg(ui, "Already Connected.", Color.white);
//                    else {
//                        new Thread(new Discord(ui.gui, "ard")).start();
//                        ui.gui.discordconnected = true;
//                    }
//                }
//            }, new Coord(210, 210));
            /*
            main.add(new Button(200, "Join ArdClient Discord") {
                public void click() {
                    try {
                        WebBrowser.self.show(new URL(String.format("https://disc"+"ord.gg/Rx"+"gVh5j")));
                    } catch (WebBrowser.BrowserException e) {
                        getparent(GameUI.class).error("Could not launch web browser.");
                    } catch (MalformedURLException e) {
                    }
                }
            }, new Coord(210, 240));
            */

            main.add(new Button(200, "Changelog") {
                public void click() {
                    showChangeLog();
                }
            }, new Coord(210, 270));

            main.add(new Button(200, "Switch character") {
                public void click() {
                    ui.gui.act("lo", "cs");
                    if (ui.gui != null && ui.gui.map != null)
                        ui.gui.map.canceltasks();
                }
            }, new Coord(210, 300));
            main.add(new Button(200, "Log out") {
                public void click() {
                    ui.gui.act("lo");
                    if (ui.gui != null && ui.gui.map != null)
                        ui.gui.map.canceltasks();
                    //MainFrame.instance.p.closeSession(ui);
                }
            }, new Coord(210, 330));
        }
        main.add(new Button(200, "Close") {
            public void click() {
                OptWnd.this.hide();
            }
        }, new Coord(210, 360));
        main.pack();
    }

    private void initAudio() {
        initAudioFirstColumn();
        audio.add(new PButton(200, "Back", 27, main), new Coord(210, 360));
        audio.pack();
    }

    private void initAudioFirstColumn() {
        final WidgetVerticalAppender appender2 = new WidgetVerticalAppender(audio);
        appender2.setVerticalMargin(0);

        resources.sfxlist = new HSliderListbox(140, 17);
        synchronized (resources.sfxmenus) {
            Utils.loadprefsliderlist("customsfxvol", resources.sfxmenus);
            resources.sfxmenus.forEach(s -> resources.sfxlist.addItem(configuration.createSFXSlider(s)));
        }
        appender2.add(resources.sfxlist);
        resources.sfxsearch = new TextEntry(140, "") {
            @Override
            public void changed() {
                update();
            }

            @Override
            public boolean mousedown(Coord mc, int btn) {
                if (btn == 3) {
                    settext("");
                    update();
                    return true;
                } else {
                    return super.mousedown(mc, btn);
                }
            }

            public void update() {
                synchronized (resources.sfxmenus) {
                    resources.sfxlist.clear();
                    resources.sfxmenus.forEach(s -> {
                        if (s.contains(text))
                            resources.sfxlist.addItem(configuration.createSFXSlider(s));
                    });
                }
            }

            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Right Click to clear entry").tex();
            }
        };
        appender2.add(resources.sfxsearch);
        appender2.add(new Button(140, "Clear") {
            @Override
            public boolean mousedown(Coord mc, int btn) {
                if (ui.modctrl && btn == 1) {
                    synchronized (resources.sfxmenus) {
                        resources.sfxmenus.clear();
                        resources.sfxlist.clear();
                        Utils.setprefsliderlst("customsfxvol", resources.sfxmenus);
                    }
                }
                return (true);
            }

            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Clear all list if something went wrong (CTRL + LMB). Don't click!").tex();
            }
        });

        Scrollport scroll = withScroll(audio, new Coord(620 - 150, 350));
        scroll.move(Coord.of(150, 0));
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(scroll.cont);

        appender.add(new Label("Master audio volume"));
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, (int) (Audio.volume * 1000)) {
            public void changed() {
                Audio.setvolume(val / 1000.0);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new Label("In-game event volume"));
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (ui.audio.pos.volume * 1000);
            }

            public void changed() {
                ui.audio.pos.setvolume(val / 1000.0);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new Label("Ambient volume"));
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (ui.audio.amb.volume * 1000);
            }

            public void changed() {
                ui.audio.amb.setvolume(val / 1000.0);
            }
        });
        appender.addRow(new Label("Cleave Sound"), makeDropdownCleave());
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void added() {
                super.added();
                val = (int) (Config.cleavesoundvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.cleavesoundvol = vol;
                Utils.setprefd("cleavesoundvol", vol);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new Label("Timers alarm volume"));
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void added() {
                super.added();
                val = (int) (Config.timersalarmvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.timersalarmvol = vol;
                Utils.setprefd("timersalarmvol", vol);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new Label("Alerted gobs sound volume"));
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void added() {
                super.added();
                val = (int) (Config.alertsvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.alertsvol = vol;
                Utils.setprefd("alertsvol", vol);
            }
        });
        appender.add(new Label("'Ding' sound volume"));
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void added() {
                super.added();
                val = (int) (Config.sfxdingvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.sfxdingvol = vol;
                Utils.setprefd("sfxdingvol", vol);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new Label("'Whip' sound volume"));
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void added() {
                super.added();
                val = (int) (Config.sfxwhipvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.sfxwhipvol = vol;
                Utils.setprefd("sfxwhipvol", vol);
            }
        });
        appender.setVerticalMargin(0);
//        appender.add(new Label("Fireplace sound volume (req. restart)"));
//        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
//        appender.add(new HSlider(200, 0, 1000, 0) {
//            protected void added() {
//                super.added();
//                val = (int) (Config.sfxfirevol * 1000);
//            }
//
//            public void changed() {
//                double vol = val / 1000.0;
//                Config.sfxfirevol = vol;
//                Utils.setprefd("sfxfirevol", vol);
//            }
//        });
//        appender.setVerticalMargin(0);
//        appender.add(new Label("Cauldron sound volume - Changes are not immediate, will trigger on next cauldon sound start."));
//        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
//        appender.add(new HSlider(200, 0, 1000, 0) {
//            protected void added() {
//                super.added();
//                val = (int) (Config.sfxcauldronvol * 1000);
//            }
//
//            public void changed() {
//                double vol = val / 1000.0;
//                Config.sfxcauldronvol = vol;
//                Utils.setprefd("sfxcauldronvol", vol);
//            }
//        });
//        appender.setVerticalMargin(0);
//        appender.add(new Label("Beehive sound volume"));
//        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
//        appender.add(new HSlider(200, 0, 1000, 0) {
//            protected void added() {
//                super.added();
//                val = (int) (Config.sfxbeehivevol * 1000);
//            }
//
//            public void changed() {
//                double vol = val / 1000.0;
//                Config.sfxbeehivevol = vol;
//                Utils.setprefd("sfxbeehivevol", vol);
//            }
//        });

        appender.add(new CheckBox("Enable error sounds.") {
            {
                a = Config.errorsounds;
            }

            public void set(boolean val) {
                Utils.setprefb("errorsounds", val);
                Config.errorsounds = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Enable Cleave sound.") {
            {
                a = Config.cleavesound;
            }

            public void set(boolean val) {
                Utils.setprefb("cleavesound", val);
                Config.cleavesound = val;
                a = val;
            }
        });
    }

    private void initDisplay() {
        initDisplayFirstColumn();
        display.add(new PButton(200, "Back", 27, main), new Coord(210, 360));
        display.pack();
    }

    private void initTheme() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(uip, new Coord(620, 350)));
        appender.setVerticalMargin(VERTICAL_MARGIN);
        { //Theme
            final IndirRadioGroup<String> rgrp = new IndirRadioGroup<>("Main Hud Theme (requires restart)", HUDTHEME);
            for (final String name : THEMES.get()) {
                rgrp.add(name, name);
            }
            appender.add(rgrp);
            appender.add(new IndirLabel(() -> String.format("Settings for %s", HUDTHEME.get())));
            appender.add(ColorPreWithLabel("Window Color: ", WNDCOL));
            appender.add(ColorPreWithLabel("Button Color: ", BTNCOL));
            appender.add(ColorPreWithLabel("Textbox Color: ", TXBCOL));
            appender.add(ColorPreWithLabel("Slider Color: ", SLIDERCOL));
            uip.add(new PButton(200, "Back", 27, main), new Coord(210, 380));
            uip.pack();
        }
    }

    private void initDisplayFirstColumn() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(display, new Coord(620, 350)));
        appender.setVerticalMargin(VERTICAL_MARGIN);

        appender.add(new CheckBox("Show Session Display") {
            {
                a = Config.sessiondisplay;
            }

            public void set(boolean val) {
                Utils.setprefb("sessiondisplay", val);
                Config.sessiondisplay = val;
                a = val;

                ui.root.sessionDisplay.unlink();
                if (Config.sessiondisplay)
                    if (ui.gui != null)
                        ui.gui.add(ui.root.sessionDisplay);
                    else
                        ui.root.add(ui.root.sessionDisplay);
            }
        });

        appender.add(new CheckBox("Big Animals (required for Small World)") {
            {
                a = Config.biganimals;
            }

            public void set(boolean val) {
                Utils.setprefb("biganimals", val);
                Config.biganimals = val;
                a = val;
            }
        });

        appender.add(new CheckBox("Show IMeter Text") {
            {
                a = Config.showmetertext;
            }

            public void set(boolean val) {
                Utils.setprefb("showmetertext", val);
                Config.showmetertext = val;
                a = val;
            }
        });

        appender.add(new CheckBox("Flatten Cupboards - Requires Restart") {
            {
                a = Config.flatcupboards;
            }

            public void set(boolean val) {
                Utils.setprefb("flatcupboards", val);
                Config.flatcupboards = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Flatten Palisades/Bwalls") {
            {
                a = Config.flatwalls;
            }

            public void set(boolean val) {
                Utils.setprefb("flatwalls", val);
                Config.flatwalls = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Flatten Cave Walls") {
            {
                a = Config.flatcaves;
            }

            public void set(boolean val) {
                Utils.setprefb("flatcaves", val);
                Config.flatcaves = val;
                a = val;
                if (ui.sess != null) {
                    ui.sess.glob.map.invalidateAll();
                }
            }
        });
        appender.add(new CheckBox("Display stage 1 (fresh planted) crops when crop stage overlay enabled.") {
            {
                a = Config.showfreshcropstage;
            }

            public void set(boolean val) {
                Utils.setprefb("showfreshcropstage", val);
                Config.showfreshcropstage = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Always display long tooltips.") {
            {
                a = Config.longtooltips;
            }

            public void set(boolean val) {
                Utils.setprefb("longtooltips", val);
                Config.longtooltips = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Display Avatar Equipment tooltips.") {
            {
                a = Config.avatooltips;
            }

            public void set(boolean val) {
                Utils.setprefb("avatooltips", val);
                Config.avatooltips = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Display kin names") {
            {
                a = Config.showkinnames;
            }

            public void set(boolean val) {
                Utils.setprefb("showkinnames", val);
                Config.showkinnames = val;
                a = val;
            }
        });

        appender.add(new CheckBox("Show hourglass percentage") {
            {
                a = Config.showprogressperc;
            }

            public void set(boolean val) {
                Utils.setprefb("showprogressperc", val);
                Config.showprogressperc = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show attributes & softcap values in craft window") {
            {
                a = Config.showcraftcap;
            }

            public void set(boolean val) {
                Utils.setprefb("showcraftcap", val);
                Config.showcraftcap = val;
                a = val;
            }
        });
        appender.add(new IndirCheckBox("Toggle halo pointers", SHOWHALO));
        appender.add(new IndirCheckBox("Toggle halo pointers on hearthing", SHOWHALOONHEARTH));
        appender.add(new CheckBox("Show objects health - Useful for mine supports/boats") {
            {
                a = Config.showgobhp;
            }

            public void set(boolean val) {
                Utils.setprefb("showgobhp", val);
                Config.showgobhp = val;
                a = val;

                if (ui.gui != null && ui.gui.map != null) {
                    if (val)
                        ui.gui.map.addHealthSprites();
                    else
                        ui.gui.map.removeCustomSprites(Sprite.GOB_HEALTH_ID);
                }
            }
        });
        appender.add(new CheckBox("Show inspected qualities of objects - only until the object unloads.") {
            {
                a = Config.showgobquality;
            }

            public void set(boolean val) {
                Utils.setprefb("showgobquality", val);
                Config.showgobquality = val;
                a = val;

                if (ui.gui != null && ui.gui.map != null) {
                    if (val)
                        ui.gui.map.addQualitySprites();
                    else
                        ui.gui.map.removeCustomSprites(Sprite.GOB_QUALITY_ID);
                }
            }
        });
        appender.add(new IndirCheckBox("Show Your Movement Path", SHOWPLAYERPATH));
        appender.add(ColorPreWithLabel("Your path color: ", PLAYERPATHCOL, val -> GobPath.clrst = new States.ColState(val)));
        appender.add(new IndirCheckBox("Show Other Player Paths - Kinned player's paths will be their kin color.", SHOWGOBPATH));
        appender.add(ColorPreWithLabel("Unknown player path color: ", GOBPATHCOL, val -> Movable.unknowngobcol = new States.ColState(val)));
        appender.add(new IndirCheckBox("Show Mob Paths", SHOWANIMALPATH));
        appender.add(ColorPreWithLabel("Animal path color: ", ANIMALPATHCOL, val -> Movable.animalpathcol = new States.ColState(val)));

        appender.add(new CheckBox("Colorful Cave Dust") {
            {
                a = Config.colorfulcaveins;
            }

            public void set(boolean val) {
                Utils.setprefb("colorfulcaveins", val);
                Config.colorfulcaveins = val;
                a = val;
            }
        });
        appender.addRow(new Label("Cave-in Warning Dust Duration in Minutes"), makeCaveInDropdown());
        appender.add(new CheckBox("Show animal radius") {
            {
                a = Config.showanimalrad;
            }

            public void set(boolean val) {
                Utils.setprefb("showanimalrad", val);
                Config.showanimalrad = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Double animal radius size.") {
            {
                a = Config.doubleradius;
            }

            public void set(boolean val) {
                Utils.setprefb("doubleradius", val);
                Config.doubleradius = val;
                a = val;
            }
        });
        final Consumer<Color> fog = val -> {
            WaterTile.updateFog();
            if (ui.sess != null && ui.sess.glob != null && ui.sess.glob.map != null) {
                ui.sess.glob.map.clearTiles();
                ui.sess.glob.map.invalidateAll();
            }

        };
        appender.add(ColorPreWithLabel("Deep Ocean Color: ", DEEPWATERCOL, fog));
        appender.add(ColorPreWithLabel("Ocean Color: ", OCEANWATERCOL, fog));
        appender.add(ColorPreWithLabel("Shallow Water Ocean Color: ", SHALLOWOCEANWATERCOL, fog));
        appender.add(ColorPreWithLabel("Water Color: ", WATERCOL, fog));
        appender.add(ColorPreWithLabel("Shallow Water Color: ", SHALLOWWATERCOL, fog));
        appender.add(ColorPreWithLabel("All Other Water Color: ", ALLWATERCOL, fog));

        appender.add(ColorPreWithLabel("Beehive radius color: ", BEEHIVECOLOR, val -> {
            BPRadSprite.smatBeehive = new States.ColState(val);
            if (ui.gui != null) {
                if (ui.gui.map != null) {
//                    MapView.rovlbeehive = new Gob.Overlay(new BPRadSprite(151.0F, -10.0F, BPRadSprite.smatBeehive));
                    ui.gui.map.refreshGobsAll();
                }
            }
        }));
        appender.add(ColorPreWithLabel("Trough radius color: ", TROUGHCOLOR, val -> {
            BPRadSprite.smatTrough = new States.ColState(val);
            if (ui.gui != null) {
                if (ui.gui.map != null) {
//                    MapView.rovltrough = new Gob.Overlay(new BPRadSprite(200.0F, -10.0F, BPRadSprite.smatTrough));
                    ui.gui.map.refreshGobsAll();
                }
            }
        }));
        appender.add(ColorPreWithLabel("Dangerous animal radius color: ", ANIMALDANGERCOLOR, val -> {
            BPRadSprite.smatDanger = new States.ColState(val);
            if (ui.gui != null) {
                if (ui.gui.map != null) {
//                    Gob.animalradius = new Gob.Overlay(new BPRadSprite(100.0F, -10.0F, BPRadSprite.smatDanger));
//                    Gob.doubleanimalradius = new Gob.Overlay(new BPRadSprite(200.0F, -20.0F, BPRadSprite.smatDanger));
                    ui.gui.map.refreshGobsAll();
                }
            }
        }));
        appender.add(ColorPreWithLabel("Mine support radius color: ", SUPPORTDANGERCOLOR, val -> {
            BPRadSprite.smatSupports = new States.ColState(val);
            if (ui.gui != null) {
                if (ui.gui.map != null) {
//                    MapView.rovlsupport = new Gob.Overlay(new BPRadSprite(100.0F, 0, BPRadSprite.smatSupports));
//                    MapView.rovlcolumn = new Gob.Overlay(new BPRadSprite(125.0F, 0, BPRadSprite.smatSupports));
//                    MapView.rovlbeam = new Gob.Overlay(new BPRadSprite(150.0F, 0, BPRadSprite.smatSupports));
                    ui.gui.map.refreshGobsAll();
                }
            }
        }));
        appender.add(ColorPreWithLabel("Error message text color: ", ERRORTEXTCOLOR));
        appender.add(new CheckBox("Highlight empty/finished drying frames and full/empty tanning tubs. Requires restart.") {
            {
                a = Config.showdframestatus;
            }

            public void set(boolean val) {
                Utils.setprefb("showdframestatus", val);
                Config.showdframestatus = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Highlight chicken coops based on food/water needs.") {
            {
                a = Config.showcoopstatus;
            }

            public void set(boolean val) {
                Utils.setprefb("showcoopstatus", val);
                Config.showcoopstatus = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Highlight rabbit hutches based on food/water needs.") {
            {
                a = Config.showhutchstatus;
            }

            public void set(boolean val) {
                Utils.setprefb("showhutchstatus", val);
                Config.showhutchstatus = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Highlight storages based on amount of contents.") {
            {
                a = Config.showcupboardstatus;
            }

            public void set(boolean val) {
                Utils.setprefb("showcupboardstatus", val);
                Config.showdframestatus = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Highlight sheds based on amount of contents.") {
            {
                a = Config.showshedstatus;
            }

            public void set(boolean val) {
                Utils.setprefb("showshedstatus", val);
                Config.showshedstatus = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Highlight empty/full cheese racks.") {
            {
                a = Config.showrackstatus;
            }

            public void set(boolean val) {
                Utils.setprefb("showrackstatus", val);
                Config.showrackstatus = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Highlight partially full cheese racks.") {
            {
                a = Config.cRackmissing;
            }

            public void set(boolean val) {
                Utils.setprefb("cRackmissing", val);
                Config.cRackmissing = val;
                a = val;
            }
        });
        appender.add(ColorPreWithLabel("Cheese rack missing color: ", CHEESERACKMISSINGCOLOR, val -> BPRadSprite.cRackMissing = new Material.Colors(CHEESERACKMISSINGCOLOR.get())));
        appender.add(new CheckBox("Highlight finished garden pots.") {
            {
                a = Config.highlightpots;
            }

            public void set(boolean val) {
                Utils.setprefb("highlightpots", val);
                Config.highlightpots = val;
                a = val;
            }
        });
        appender.add(ColorPreWithLabel("Garden Pot Finished Color (Requires restart)", GARDENPOTDONECOLOR));
        appender.add(new CheckBox("Draw circles around party members.") {
            {
                a = Config.partycircles;
            }

            public void set(boolean val) {
                Utils.setprefb("partycircles", val);
                Config.partycircles = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Draw circles around kinned players") {
            {
                a = Config.kincircles;
            }

            public void set(boolean val) {
                Utils.setprefb("kincircles", val);
                Config.kincircles = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Draw circle on ground around yourself.") {
            {
                a = Config.playercircle;
            }

            public void set(boolean val) {
                Utils.setprefb("playercircle", val);
                Config.playercircle = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Draw green circle around paving stranglevines") {
            {
                a = Config.stranglevinecircle;
            }

            public void set(boolean val) {
                Utils.setprefb("stranglevinecircle", val);
                Config.stranglevinecircle = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show last used curios in study window") {
            {
                a = Config.studyhist;
            }

            public void set(boolean val) {
                Utils.setprefb("studyhist", val);
                Config.studyhist = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Display buff icon when study has free slots") {
            {
                a = Config.studybuff;
            }

            public void set(boolean val) {
                Utils.setprefb("studybuff", val);
                Config.studybuff = val;
                a = val;
            }
        });

        /**bt = new CheckBox("Miniature trees (req. logout)") {
         {
         a = Config.bonsai;
         }

         public void set(boolean val) {
         Utils.setprefb("bonsai", val);
         Config.bonsai = val;
         a = val;
         lt.a = false;
         Config.largetree = false;
         ltl.a = false;
         Config.largetreeleaves = false;
         }
         };

         lt = new CheckBox("LARP trees (req. logout)") {
         {
         a = Config.largetree;
         }

         public void set(boolean val) {
         Utils.setprefb("largetree", val);
         Config.largetree = val;
         a = val;
         bt.a = false;
         Config.bonsai = false;
         ltl.a = false;
         Config.largetreeleaves = false;
         }
         };

         ltl = new CheckBox("LARP trees w/ leaves (req. logout)") {
         {
         a = Config.largetreeleaves;
         }

         public void set(boolean val) {
         Utils.setprefb("largetreeleaves", val);
         Config.largetreeleaves = val;
         a = val;
         bt.a = false;
         Config.bonsai = false;
         lt.a = false;
         Config.largetree = false;
         }
         };**/

        appender.addRow(new CheckBox("Scalable trees: ") {
            {
                this.a = configuration.scaletree;
            }

            @Override
            public void set(boolean val) {
                Utils.setprefb("scaletree", val);
                configuration.scaletree = val;
                this.a = val;
                if (ui.sess != null) {
                    ui.sess.glob.oc.refreshallresdraw();
                }
            }
        }, new HSlider(200, 0, 255, configuration.scaletreeint) {

            @Override
            protected void added() {
                super.added();
            }

            @Override
            public void changed() {
                configuration.scaletreeint = val;
                Utils.setprefi("scaletreeint", configuration.scaletreeint);
                if (ui.sess != null) {
                    ui.sess.glob.oc.refreshallresdraw();
                }
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Scale tree and brush : " + configuration.scaletreeint + "%").tex();
            }
        });

        appender.add(new CheckBox("It's a small world") {
            {
                a = Config.smallworld;
            }

            public void set(boolean val) {
                Utils.setprefb("smallworld", val);
                Config.smallworld = val;
                a = val;
            }
        });
        /** appender.add(lt);
         appender.add(bt);
         appender.add(ltl);**/

        Button OutputSettings = new Button(220, "Output Light Settings to System Tab") {
            @Override
            public void click() {
                PBotUtils.sysLogAppend(ui, "Ambient Red " + DefSettings.NVAMBIENTCOL.get().getRed() + " Green - " + DefSettings.NVAMBIENTCOL.get().getGreen() + " Blue - " + NVAMBIENTCOL.get().getBlue(), "white");
                PBotUtils.sysLogAppend(ui, "Diffuse Red " + DefSettings.NVDIFFUSECOL.get().getRed() + " Green - " + DefSettings.NVDIFFUSECOL.get().getGreen() + " Blue - " + NVDIFFUSECOL.get().getBlue(), "white");
                PBotUtils.sysLogAppend(ui, "Specular Red " + DefSettings.NVSPECCOC.get().getRed() + " Green - " + DefSettings.NVSPECCOC.get().getGreen() + " Blue - " + NVSPECCOC.get().getBlue(), "white");
            }
        };
        appender.add(OutputSettings);
        appender.add(new Label("Ghandhi Lighting Presets"));
        Button Preset1 = new Button(220, "Friday Evening") {
            @Override
            public void click() {
                DefSettings.NVAMBIENTCOL.set(new Color(51, 59, 119));
                DefSettings.NVDIFFUSECOL.set(new Color(20, 28, 127));
                DefSettings.NVSPECCOC.set(new Color(167, 117, 103));
            }
        };
        appender.add(Preset1);
        Button Preset2 = new Button(220, "Thieving Night") {
            @Override
            public void click() {
                DefSettings.NVAMBIENTCOL.set(new Color(5, 10, 51));
                DefSettings.NVDIFFUSECOL.set(new Color(0, 31, 50));
                DefSettings.NVSPECCOC.set(new Color(138, 64, 255));
            }
        };
        appender.add(Preset2);
        Button Preset3 = new Button(220, "Hunting Dusk") {
            @Override
            public void click() {
                DefSettings.NVAMBIENTCOL.set(new Color(165, 213, 255));
                DefSettings.NVDIFFUSECOL.set(new Color(160, 193, 255));
                DefSettings.NVSPECCOC.set(new Color(138, 64, 255));
            }
        };
        appender.add(Preset3);
        Button Preset4 = new Button(220, "Sunny Morning") {
            @Override
            public void click() {
                DefSettings.NVAMBIENTCOL.set(new Color(211, 180, 72));
                DefSettings.NVDIFFUSECOL.set(new Color(255, 178, 169));
                DefSettings.NVSPECCOC.set(new Color(255, 255, 255));
            }
        };
        appender.add(Preset4);
        appender.add(new Label("Default Lighting"));
        Button Preset5 = new Button(220, "Amber Default") {
            @Override
            public void click() {
                DefSettings.NVAMBIENTCOL.set(new Color(200, 200, 200));
                DefSettings.NVDIFFUSECOL.set(new Color(200, 200, 200));
                DefSettings.NVSPECCOC.set(new Color(255, 255, 255));
            }
        };
        appender.add(Preset5);
        appender.add(new IndirCheckBox("Dark Mode (overrides custom global light)", DARKMODE));
        appender.add(ColorPreWithLabel("Ambient Color", NVAMBIENTCOL));
        appender.add(ColorPreWithLabel("Diffuse Color", NVDIFFUSECOL));
        appender.add(ColorPreWithLabel("Specular Color", NVSPECCOC));
    }

    private void initMap() {
        map.add(new Label("Show boulders:"), new Coord(10, 0));
        map.add(new Label("Show bushes:"), new Coord(165, 0));
        map.add(new Label("Show trees:"), new Coord(320, 0));
        map.add(new Label("Hide icons:"), new Coord(475, 0));
//        map.add(new Button(200, "Icon update (donotpress)") {
//            public void click() {
//                Iconfinder.updateConfig();
//            }
//        }, new Coord(425, 360));

        map.add(new CheckBox("Draw party members/names") {
            {
                a = Config.mapdrawparty;
            }

            public void set(boolean val) {
                Utils.setprefb("mapdrawparty", val);
                Config.mapdrawparty = val;
                a = val;
            }
        }, 10, 370);

        map.add(new CheckBox("Show names above questgivers") {
            {
                a = Config.mapdrawquests;
            }

            public void set(boolean val) {
                Utils.setprefb("mapdrawquests", val);
                Config.mapdrawquests = val;
                a = val;
            }
        }, 10, 330);
        map.add(new CheckBox("Show names above marker flags") {
            {
                a = Config.mapdrawflags;
            }

            public void set(boolean val) {
                Utils.setprefb("mapdrawflags", val);
                Config.mapdrawflags = val;
                a = val;
            }
        }, 10, 350);
        map.add(new CheckBox("Disable map updating(dontwork)") {
            {
                a = Config.stopmapupdate;
            }

            public void set(boolean val) {
                Utils.setprefb("stopmapupdate", val);
                Config.stopmapupdate = val;
                a = val;
            }
        }, 425, 350);

        map.add(new PButton(200, "Back", 27, main), new Coord(210, 380));
        map.pack();
    }

    private void initGeneral() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(general, new Coord(620, 350)));

        appender.setVerticalMargin(VERTICAL_MARGIN);
        appender.setHorizontalMargin(HORIZONTAL_MARGIN);
        appender.add(new CheckBox("Confirmation popup box on game exit.") {
            {
                a = Config.confirmclose;
            }

            public void set(boolean val) {
                Utils.setprefb("confirmclose", val);
                Config.confirmclose = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Save chat logs to disk") {
            {
                a = Config.chatsave;
            }

            public void set(boolean val) {
                Utils.setprefb("chatsave", val);
                Config.chatsave = val;
                a = val;
                if (!val && Config.chatlog != null) {
                    try {
                        Config.chatlog.close();
                        Config.chatlog = null;
                    } catch (Exception e) {
                    }
                }
            }
        });
        appender.add(new CheckBox("Save map tiles to disk - No performance benefit, this is only for creating your own maps or uploading.") {
            {
                a = Config.savemmap;
            }

            public void set(boolean val) {
                Utils.setprefb("savemmap", val);
                Config.savemmap = val;
                MapGridSave.mgs = null;
                a = val;
            }
        });
        appender.add(new CheckBox("Show timestamps in chats") {
            {
                a = Config.chattimestamp;
            }

            public void set(boolean val) {
                Utils.setprefb("chattimestamp", val);
                Config.chattimestamp = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Notify when kin comes online") {
            {
                a = Config.notifykinonline;
            }

            public void set(boolean val) {
                Utils.setprefb("notifykinonline", val);
                Config.notifykinonline = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Autosort kin list by online status.") {
            {
                a = Config.autosortkinlist;
            }

            public void set(boolean val) {
                Utils.setprefb("autosortkinlist", val);
                Config.autosortkinlist = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Abandon quests on right click") {
            {
                a = Config.abandonrightclick;
            }

            public void set(boolean val) {
                Utils.setprefb("abandonrightclick", val);
                Config.abandonrightclick = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Disable swimming automatically after 30 seconds.") {
            {
                a = Config.temporaryswimming;
            }

            public void set(boolean val) {
                Utils.setprefb("temporaryswimming", val);
                Config.temporaryswimming = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Auto hearth on unknown/red players") {
            {
                a = Config.autohearth;
            }

            public void set(boolean val) {
                Utils.setprefb("autohearth", val);
                Config.autohearth = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Auto logout on unknown/red players") {
            {
                a = Config.autologout;
            }

            public void set(boolean val) {
                Utils.setprefb("autologout", val);
                Config.autologout = val;
                a = val;
            }
        });
        appender.addRow(new Label("Auto Logout after x Minutes - 0 means never"), makeafkTimeDropdown());
        appender.add(new CheckBox("Auto remove damaged tableware items") {
            {
                a = Config.savecutlery;
            }

            public void set(boolean val) {
                Utils.setprefb("savecutlery", val);
                Config.savecutlery = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Repeat Starvation Alert Warning/Sound") {
            {
                a = Config.StarveAlert;
            }

            public void set(boolean val) {
                Utils.setprefb("StarveAlert", val);
                Config.StarveAlert = val;
                a = val;
            }
        });
        appender.addRow(new Label("Attribute Increase per mouse scroll"), makeStatGainDropdown());
        appender.add(new CheckBox("Run on login") {
            {
                a = Config.runonlogin;
            }

            public void set(boolean val) {
                Utils.setprefb("runonlogin", val);
                Config.runonlogin = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show server time") {
            {
                a = Config.showservertime;
            }

            public void set(boolean val) {
                Utils.setprefb("showservertime", val);
                Config.showservertime = val;
                a = val;
            }
        });
//        appender.add(new CheckBox("Show polowners info") {
//            {
//                a = configuration.showpolownersinfo;
//            }
//
//            public void set(boolean val) {
//                Utils.setprefb("showpolownersinfo", val);
//                configuration.showpolownersinfo = val;
//                a = val;
//            }
//        });
        appender.add(new CheckBox("Drop leeches automatically") {
            {
                a = Config.leechdrop;
            }

            public void set(boolean val) {
                Utils.setprefb("leechdrop", val);
                Config.leechdrop = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Auto switch to speed 3 on horse") {
            {
                a = Config.horseautorun;
            }

            public void set(boolean val) {
                Utils.setprefb("horseautorun", val);
                Config.horseautorun = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Enable tracking on login") {
            {
                a = Config.enabletracking;
            }

            public void set(boolean val) {
                Utils.setprefb("enabletracking", val);
                Config.enabletracking = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Enable swimming on login") {
            {
                a = Config.enableswimming;
            }

            public void set(boolean val) {
                Utils.setprefb("enableswimming", val);
                Config.enableswimming = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Enable criminal acts on login") {
            {
                a = Config.enablecrime;
            }

            public void set(boolean val) {
                Utils.setprefb("enablecrime", val);
                Config.enablecrime = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Shoo animals with Ctrl+Left Click") {
            {
                a = Config.shooanimals;
            }

            public void set(boolean val) {
                Utils.setprefb("shooanimals", val);
                Config.shooanimals = val;
                a = val;
            }
        });
        general.add(new PButton(200, "Back", 27, main), new Coord(210, 360));
        general.pack();
    }

    private void initCombat() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(combat, new Coord(620, 350)));

        appender.setVerticalMargin(VERTICAL_MARGIN);
        appender.setHorizontalMargin(HORIZONTAL_MARGIN);
//        appender.add(new CheckBox("Display damage") {
//            {
//                a = Config.showdmgop;
//            }
//
//            public void set(boolean val) {
//                Utils.setprefb("showdmgop", val);
//                Config.showdmgop = val;
//                a = val;
//            }
//        });
        appender.add(new CheckBox("Notify in the absence of a shield") {
            {
                a = configuration.shieldnotify;
            }

            public void set(boolean val) {
                Utils.setprefb("shieldnotify", val);
                configuration.shieldnotify = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Auto Clear Damage") {
            {
                a = configuration.autocleardamage;
            }

            public void set(boolean val) {
                Utils.setprefb("autocleardamage", val);
                configuration.autocleardamage = val;
                a = val;
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Auto Clear Damage after fight").tex();
            }
        });
        appender.add(new CheckBox("Show combat widgets border") {
            {
                a = configuration.showcombatborder;
            }

            public void set(boolean val) {
                Utils.setprefb("showcombatborder", val);
                configuration.showcombatborder = val;
                a = val;
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Show combat widgets border for move").tex();
            }
        });
        appender.add(new Label("Chat Exempt will force the fight session to have focus unless the chat box has focus."));
        appender.add(new CheckBox("Force Fight Session Focus - Chat Exempt") {
            {
                a = Config.forcefightfocus;
            }

            public void set(boolean val) {
                Utils.setprefb("forcefightfocus", val);
                Config.forcefightfocus = val;
                a = val;
            }
        });
        appender.add(new Label("Chat Included will force fight session to have focus at all times, this will prevent talking in combat."));
        appender.add(new CheckBox("Force Fight Session Focus - Chat Included") {
            {
                a = Config.forcefightfocusharsh;
            }

            public void set(boolean val) {
                Utils.setprefb("forcefightfocusharsh", val);
                Config.forcefightfocusharsh = val;
                a = val;
            }
        });

        appender.add(new CheckBox("Display info above untargeted enemies") {
            {
                a = Config.showothercombatinfo;
            }

            public void set(boolean val) {
                Utils.setprefb("showothercombatinfo", val);
                Config.showothercombatinfo = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Display info above current enemies", (val) -> Utils.setprefb("showcurrentenemieinfo", configuration.showcurrentenemieinfo = val), configuration.showcurrentenemieinfo));
        appender.add(new CheckBox("Display additional info about actions and the enemy") {
            {
                a = configuration.showactioninfo;
            }

            public void set(boolean val) {
                Utils.setprefb("showactioninfo", val);
                configuration.showactioninfo = val;
                a = val;
            }
        });
        appender.addRow(new Label("Combat Start Sound"), makeDropdownCombat());
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void added() {
                super.added();
                val = (int) (Config.attackedvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.attackedvol = vol;
                Utils.setprefd("attackedvol", vol);
            }
        });
        appender.add(new CheckBox("Highlight current opponent") {
            {
                a = Config.hlightcuropp;
            }

            public void set(boolean val) {
                Utils.setprefb("hlightcuropp", val);
                Config.hlightcuropp = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Display cooldown time") {
            {
                a = Config.showcooldown;
            }

            public void set(boolean val) {
                Utils.setprefb("showcooldown", val);
                Config.showcooldown = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show arrow vectors") {
            {
                a = Config.showarchvector;
            }

            public void set(boolean val) {
                Utils.setprefb("showarchvector", val);
                Config.showarchvector = val;
                a = val;
            }
        });
        /*appender.add(new CheckBox("Show attack cooldown delta") {
            {
                a = Config.showcddelta;
            }

            public void set(boolean val) {
                Utils.setprefb("showcddelta", val);
                Config.showcddelta = val;
                a = val;
            }
        });*/
        appender.add(new CheckBox("Log combat actions to system log") {
            {
                a = Config.logcombatactions;
            }

            public void set(boolean val) {
                Utils.setprefb("logcombatactions", val);
                Config.logcombatactions = val;
                a = val;
            }
        });
//        appender.add(new CheckBox("Alternative combat UI") {
//            {
//                a = Config.altfightui;
//            }
//
//            public void set(boolean val) {
//                Utils.setprefb("altfightui", val);
//                Config.altfightui = val;
//                a = val;
//            }
//        });
        appender.add(new CheckBox("Simplified opening indicators") {
            {
                a = Config.combaltopenings;
            }

            public void set(boolean val) {
                Utils.setprefb("combaltopenings", val);
                Config.combaltopenings = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show key bindings in combat UI") {
            {
                a = Config.combshowkeys;
            }

            public void set(boolean val) {
                Utils.setprefb("combshowkeys", val);
                a = val;
            }
        });
        appender.add(new CheckBox("Aggro players in proximity to the mouse cursor") {
            {
                a = Config.proximityaggropvp;
            }

            public void set(boolean val) {
                Utils.setprefb("proximityaggropvp", val);
                Config.proximityaggropvp = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Aggro animals in proximity to the mouse cursor") {
            {
                a = Config.proximityaggro;
            }

            public void set(boolean val) {
                Utils.setprefb("proximityaggro", val);
                Config.proximityaggro = val;
                a = val;
            }
        });
        appender.addRow(new Label("Combat key bindings:"), combatkeysDropdown());

        combat.add(new PButton(200, "Back", 27, main), new Coord(210, 360));
        combat.pack();
    }

    private void initControl() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(control, new Coord(620, 350)));

        appender.setVerticalMargin(VERTICAL_MARGIN);
        appender.setHorizontalMargin(HORIZONTAL_MARGIN);

        appender.addRow(new Label("Bad/Top camera scrolling sensitivity"),
                new HSlider(200, 0, 50, Config.badcamsensitivity) {
                    protected void added() {
                        super.added();
                        val = Config.badcamsensitivity;
                    }

                    public void changed() {
                        Config.badcamsensitivity = val;
                        Utils.setprefi("badcamsensitivity", val);
                    }

                    @Override
                    public Object tooltip(Coord c0, Widget prev) {
                        return Text.render("Bad camera scrolling sensitivity : " + val).tex();
                    }
                });
        appender.addRow(new CheckBox("Lock bad camera elevator") {
            {
                a = configuration.badcamelevlock;
            }

            public void set(boolean val) {
                Utils.setprefb("badcamelevlock", val);
                configuration.badcamelevlock = val;
                a = val;
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Override with shift").tex();
            }
        });
        appender.add(new CheckBox("Use French (AZERTY) keyboard layout") {
            {
                a = Config.userazerty;
            }

            public void set(boolean val) {
                Utils.setprefb("userazerty", val);
                Config.userazerty = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Reverse bad camera MMB x-axis") {
            {
                a = Config.reversebadcamx;
            }

            public void set(boolean val) {
                Utils.setprefb("reversebadcamx", val);
                Config.reversebadcamx = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Reverse bad camera MMB y-axis") {
            {
                a = Config.reversebadcamy;
            }

            public void set(boolean val) {
                Utils.setprefb("reversebadcamy", val);
                Config.reversebadcamy = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Force hardware cursor") {
            {
                a = Config.hwcursor;
            }

            public void set(boolean val) {
                Utils.setprefb("hwcursor", val);
                Config.hwcursor = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Disable game cursors") {
            {
                a = configuration.nocursor;
            }

            public void set(boolean val) {
                Utils.setprefb("nocursor", val);
                configuration.nocursor = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Disable dropping items over water (overridable with Ctrl)") {
            {
                a = Config.nodropping;
            }

            public void set(boolean val) {
                Utils.setprefb("nodropping", val);
                Config.nodropping = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Disable dropping items over anywhere (overridable with Ctrl)") {
            {
                a = Config.nodropping_all;
            }

            public void set(boolean val) {
                Utils.setprefb("nodropping_all", val);
                Config.nodropping_all = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Enable full zoom-out in Ortho cam") {
            {
                a = Config.enableorthofullzoom;
            }

            public void set(boolean val) {
                Utils.setprefb("enableorthofullzoom", val);
                Config.enableorthofullzoom = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Disable hotkey (tilde/back-quote key) for drinking") {
            {
                a = Config.disabledrinkhotkey;
            }

            public void set(boolean val) {
                Utils.setprefb("disabledrinkhotkey", val);
                Config.disabledrinkhotkey = val;
                a = val;
            }
        });
        appender.add(new Label("Disable Shift Right Click for :"));
        CheckListbox disableshiftclick = new CheckListbox(320, Math.min(8, Config.disableshiftclick.values().size()), 18 + Config.fontadd) {
            @Override
            protected void itemclick(CheckListboxItem itm, int button) {
                super.itemclick(itm, button);
                Utils.setprefchklst("disableshiftclick", Config.disableshiftclick);
            }
        };
        disableshiftclick.items.addAll(Config.disableshiftclick.values());
        appender.add(disableshiftclick);


        control.add(new PButton(200, "Back", 27, main), new Coord(210, 360));
        control.pack();
    }

    private void initUis() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(uis, new Coord(620, 310)));

        appender.setVerticalMargin(VERTICAL_MARGIN);
        appender.setHorizontalMargin(HORIZONTAL_MARGIN);

        appender.addRow(new Label("Language (req. restart):"), langDropdown());
        menugridcheckbox = new CheckBox("Disable all menugrid hotkeys (Bottom Right grid)") {
            {
                a = Config.disablemenugrid;
            }

            public void set(boolean val) {
                Utils.setprefb("disablemenugrid", val);
                Config.disablemenugrid = val;
                a = val;
            }
        };
        appender.add(menugridcheckbox);
        appender.add(new CheckBox("Disable menugrid magic hotkeys") {
            {
                a = Config.disablemagaicmenugrid;
            }

            public void set(boolean val) {
                Utils.setprefb("disablemagaicmenugrid", val);
                Config.disablemagaicmenugrid = val;
                a = val;
            }
        });

        appender.add(new CheckBox("Always show Main Menu (Requires relog)") {
            {
                a = Config.lockedmainmenu;
            }

            public void set(boolean val) {
                Utils.setprefb("lockedmainmenu", val);
                Config.lockedmainmenu = val;
                a = val;
            }
        });

        appender.add(new CheckBox("Display skills split into base+bonus") {
            {
                a = Config.splitskills;
            }

            public void set(boolean val) {
                Utils.setprefb("splitskills", val);
                Config.splitskills = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show PBot Menugrid icon (Requires relog)") {
            {
                a = Config.showPBot;
            }

            public void set(boolean val) {
                Utils.setprefb("showPBot", val);
                Config.showPBot = val;
                a = val;
            }
        });
//        appender.add(new CheckBox("Show Old PBot Menugrid icon (Requires relog)") {
//            {
//                a = Config.showPBotOld;
//            }
//
//            public void set(boolean val) {
//                Utils.setprefb("showPBotOld", val);
//                Config.showPBotOld = val;
//                a = val;
//            }
//        });
        appender.add(new CheckBox("Detailed Shift+Mouseover tooltips - Negative FPS Impact when holding shift.") {
            {
                a = Config.detailedresinfo;
            }

            public void set(boolean val) {
                Utils.setprefb("detailedresinfo", val);
                Config.detailedresinfo = val;
                a = val;
            }
        });
        appender.add(new CheckBox("More Detailed Shift+Mouseover tooltips - Request Detailed tooltip") {
            {
                a = configuration.moredetails;
            }

            public void set(boolean val) {
                Utils.setprefb("moredetails", val);
                configuration.moredetails = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show quick hand slots") {
            {
                a = Config.quickslots;
            }

            public void set(boolean val) {
                Utils.setprefb("quickslots", val);
                Config.quickslots = val;
                a = val;

                Set<GameUI> guis = ui.root.children(GameUI.class);
                if (!guis.isEmpty()) {
                    GameUI gui = guis.iterator().next();
                    Widget qs = configuration.newQuickSlotWdg ? gui.newquickslots : gui.quickslots;

                    if (qs != null) {
                        if (val) {
                            qs.show();
                        } else {
                            qs.hide();
                        }
                    }
                }
            }
        });
        appender.add(new CheckBox("Disable ctrl clicking to drop items from quick hand slots.") {
            {
                a = Config.disablequickslotdrop;
            }

            public void set(boolean val) {
                Utils.setprefb("disablequickslotdrop", val);
                Config.disablequickslotdrop = val;
                a = val;
            }
        });
        appender.add(new IndirCheckBox("Amber flowermenus", AMBERMENU));
        appender.add(new CheckBox("Alternative equipment belt window") {
            {
                a = Config.quickbelt;
            }

            public void set(boolean val) {
                Utils.setprefb("quickbelt", val);
                Config.quickbelt = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Stack cupboard windows on top of eachother") {
            {
                a = Config.stackwindows;
            }

            public void set(boolean val) {
                Utils.setprefb("stackwindows", val);
                Config.stackwindows = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Hide Calendar Widget on login.") {
            {
                a = Config.hidecalendar;
            }

            public void set(boolean val) {
                Utils.setprefb("hidecalendar", val);
                Config.hidecalendar = val;
                a = val;
                if (ui.gui != null)
                    ui.gui.cal.visible = !Config.hidecalendar;
            }
        });
        appender.add(new CheckBox("Close windows with escape key.") {
            {
                a = Config.escclosewindows;
            }

            public void set(boolean val) {
                Utils.setprefb("escclosewindows", val);
                Config.escclosewindows = val;
                a = val;
            }
        });
        appender.add(new IndirCheckBox("Show F Key Belt", SHOWFKBELT, val -> {
            if (ui.gui != null && ui.gui.fbelt != null) {
                ui.gui.fbelt.setVisibile(val);
            }
        }));
        appender.add(new IndirCheckBox("Show NumPad Key Belt", SHOWNPBELT, val -> {
            if (ui.gui != null && ui.gui.npbelt != null) {
                ui.gui.npbelt.setVisibile(val);
            }
        }));
        appender.add(new IndirCheckBox("Show Number Key Belt", SHOWNBELT, val -> {
            if (ui.gui != null && ui.gui.nbelt != null) {
                ui.gui.nbelt.setVisibile(val);
            }
        }));
        appender.add(new CheckBox("Show hungermeter") {
            {
                a = Config.hungermeter;
            }

            public void set(boolean val) {
                Utils.setprefb("hungermeter", val);
                Config.hungermeter = val;
                a = val;
                if (ui.gui != null) {
                    ui.gui.hungermeter.show(val);
                }
            }
        });
        appender.add(new CheckBox("Show fepmeter") {
            {
                a = Config.fepmeter;
            }

            public void set(boolean val) {
                Utils.setprefb("fepmeter", val);
                Config.fepmeter = val;
                a = val;
                if (ui.gui != null) {
                    ui.gui.fepmeter.show(val);
                }
            }
        });
        appender.add(new CheckBox("Show Craft/Build history toolbar") {
            {
                a = Config.histbelt;
            }

            public void set(boolean val) {
                Utils.setprefb("histbelt", val);
                Config.histbelt = val;
                a = val;
                if (ui.gui != null) {
                    CraftHistoryBelt histbelt = ui.gui.histbelt;
                    if (histbelt != null) {
                        if (val)
                            histbelt.show();
                        else
                            histbelt.hide();
                    }
                }
            }
        });
        appender.add(new CheckBox("Display confirmation dialog when using magic") {
            {
                a = Config.confirmmagic;
            }

            public void set(boolean val) {
                Utils.setprefb("confirmmagic", val);
                Config.confirmmagic = val;
                a = val;
            }
        });

        appender.addRow(new Label("Chat font size (req. restart):"), makeFontSizeChatDropdown());
        appender.add(new CheckBox("Font antialiasing") {
            {
                a = Config.fontaa;
            }

            public void set(boolean val) {
                Utils.setprefb("fontaa", val);
                Config.fontaa = val;
                a = val;
            }
        });
        appender.addRow(new CheckBox("Custom interface font (req. restart):") {
                            {
                                a = Config.usefont;
                            }

                            public void set(boolean val) {
                                Utils.setprefb("usefont", val);
                                Config.usefont = val;
                                a = val;
                            }
                        },
                makeFontsDropdown());
        final Label fontAdd = new Label("");
        appender.addRow(
                new Label("Increase font size by (req. restart):"),
                new HSlider(160, 0, 3, Config.fontadd) {
                    public void added() {
                        super.added();
                        updateLabel();
                    }

                    public void changed() {
                        Utils.setprefi("fontadd", val);
                        Config.fontadd = val;
                        updateLabel();
                    }

                    private void updateLabel() {
                        fontAdd.settext(String.format("%d", val));
                    }
                },
                fontAdd
        );

        appender.add(new Label("Open selected windows on login."));
        CheckListbox autoopenlist = new CheckListbox(320, Config.autowindows.values().size(), 18 + Config.fontadd) {
            @Override
            protected void itemclick(CheckListboxItem itm, int button) {
                super.itemclick(itm, button);
                Utils.setprefchklst("autowindows", Config.autowindows);
            }
        };
        Utils.loadprefchklist("autowindows", Config.autowindows);
        autoopenlist.items.addAll(Config.autowindows.values());
        appender.add(autoopenlist);


//        Button resetWndBtn = new Button(220, "Reset Windows (req. logout)") {
//            @Override
//            public void click() {
//                try {
//                    for (String key : Utils.prefs().keys()) {
//                        if (key.endsWith("_c")) {
//                            Utils.delpref(key);
//                        }
//                    }
//                } catch (BackingStoreException e) {
//                }
//                Utils.delpref("mmapc");
//                Utils.delpref("mmapwndsz");
//                Utils.delpref("mmapsz");
//                Utils.delpref("quickslotsc");
//                Utils.delpref("chatsz");
//                Utils.delpref("chatvis");
//                Utils.delpref("menu-visible");
//                Utils.delpref("fbelt_vertical");
//                Utils.delpref("haven.study.position");
//            }
//        };
//        uis.add(resetWndBtn, new Coord(620 / 2 - resetWndBtn.sz.x / 2, 320));
        uis.add(new PButton(200, "Back", 27, main), new Coord(210, 360));
        uis.pack();
    }

    private void initQuality() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(quality, new Coord(620, 350)));
        appender.setHorizontalMargin(5);

        List<String> qualityposlist = new ArrayList<>(Arrays.asList("Left-Top", "Top-Center", "Right-Top", "Right-Center", "Right-Bottom", "Bottom-Center", "Left-Bottom", "Left-Center", "Center"));
        appender.addRow(new CheckBox("Show item quality") {
            {
                a = Config.showquality;
            }

            public void set(boolean val) {
                Utils.setprefb("showquality", val);
                Config.showquality = val;
                a = val;
            }
        }, new Dropbox<String>(qualityposlist.size(), qualityposlist) {
            {
                super.change(configuration.qualitypos);
            }

            @Override
            protected String listitem(int i) {
                return qualityposlist.get(i);
            }

            @Override
            protected int listitems() {
                return qualityposlist.size();
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }

            @Override
            public void change(String item) {
                super.change(item);
                configuration.qualitypos = item;
                Utils.setpref("qualitypos", item);
            }
        }, new PButton(50, "Color Quality", 'c', qualityPanel));
        appender.add(new CheckBox("Round item quality to a whole number") {
            {
                a = Config.qualitywhole;
            }

            public void set(boolean val) {
                Utils.setprefb("qualitywhole", val);
                Config.qualitywhole = val;
                a = val;
            }
        });
        appender.addRow(new CheckBox("Draw background for quality values:") {
            {
                a = Config.qualitybg;
            }

            public void set(boolean val) {
                Utils.setprefb("qualitybg", val);
                Config.qualitybg = val;
                a = val;
            }
        }, new HSlider(200, 0, 255, Config.qualitybgtransparency) {
            public void changed() {
                Utils.setprefi("qualitybgtransparency", val);
                Config.qualitybgtransparency = val;
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                return Text.render(val + "").tex();
            }
        });
        appender.addRow(new CheckBox("Show numeric info") {
            {
                a = configuration.shownumeric;
            }

            public void set(boolean val) {
                Utils.setprefb("shownumeric", val);
                configuration.shownumeric = val;
                a = val;
            }
        }, new Dropbox<String>(qualityposlist.size(), qualityposlist) {
            {
                super.change(configuration.numericpos);
            }

            @Override
            protected String listitem(int i) {
                return qualityposlist.get(i);
            }

            @Override
            protected int listitems() {
                return qualityposlist.size();
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }

            @Override
            public void change(String item) {
                super.change(item);
                configuration.numericpos = item;
                Utils.setpref("numericpos", item);
            }
        });
        appender.add(new CheckBox("Display item completion progress bar") {
            {
                a = Config.itemmeterbar;
            }

            public void set(boolean val) {
                Utils.setprefb("itemmeterbar", val);
                Config.itemmeterbar = val;
                a = val;
            }
        });
        appender.addRow(new CheckBox("Show study time") {
            {
                a = configuration.showstudytime;
            }

            public void set(boolean val) {
                Utils.setprefb("showstudytime", val);
                configuration.showstudytime = val;
                a = val;
            }
        }, new Dropbox<String>(qualityposlist.size(), qualityposlist) {
            {
                super.change(configuration.studytimepos);
            }

            @Override
            protected String listitem(int i) {
                return qualityposlist.get(i);
            }

            @Override
            protected int listitems() {
                return qualityposlist.size();
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }

            @Override
            public void change(String item) {
                super.change(item);
                configuration.studytimepos = item;
                Utils.setpref("studytimepos", item);
            }
        });
        appender.add(new CheckBox("Draw old mountbar") {
            {
                a = configuration.oldmountbar;
            }

            public void set(boolean val) {
                Utils.setprefb("oldmountbar", val);
                configuration.oldmountbar = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Draw new mountbar") {
            {
                a = configuration.newmountbar;
            }

            public void set(boolean val) {
                Utils.setprefb("newmountbar", val);
                configuration.newmountbar = val;
                a = val;
            }
        });

        appender.add(new CheckBox("Show wear bars") {
            {
                a = Config.showwearbars;
            }

            public void set(boolean val) {
                Utils.setprefb("showwearbars", val);
                Config.showwearbars = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Larger quality/quantity text") {
            {
                a = Config.largeqfont;
            }

            public void set(boolean val) {
                Utils.setprefb("largeqfont", val);
                Config.largeqfont = val;
                a = val;
            }
        });

        quality.add(new PButton(200, "Back", 27, main), new Coord(210, 360));
        quality.pack();
    }

    private void initAdditions() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(additions, new Coord(620, 350)));

        appender.setVerticalMargin(VERTICAL_MARGIN);
        appender.setHorizontalMargin(HORIZONTAL_MARGIN);

        appender.add(new Label("Additional Client Features"));

        appender.add(new CheckBox("Straight cave wall (requires new chunk render)") {
            {
                a = Config.straightcavewall;
            }

            public void set(boolean val) {
                Utils.setprefb("straightcavewall", val);
                Config.straightcavewall = val;
                a = val;
                if (ui.sess != null) {
                    ui.sess.glob.map.invalidateAll();
                }
            }
        });

        additions.add(new PButton(200, "Back", 27, main), new Coord(210, 360));
        additions.pack();
    }

    private void initDiscord() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(discord, new Coord(620, 350)));

        appender.setVerticalMargin(VERTICAL_MARGIN);
        appender.setHorizontalMargin(HORIZONTAL_MARGIN);

        appender.addRow(new CheckBox("Auto Connect") {
            {
                a = Config.autoconnectdiscord;
            }

            public void set(boolean val) {
                Utils.setprefb("autoconnectdiscord", val);
                Config.autoconnectdiscord = val;
                a = val;
            }
        }, new Button("Connect") {
            @Override
            public void click() {
                PBotDiscord.initalize();
            }
        });

        appender.addRow(new Label("Discord Token: "),
                new TextEntry(240, Utils.getpref("discordtoken", "")) {
                    @Override
                    public boolean keydown(KeyEvent ev) {
                        if (!parent.visible)
                            return false;
                        Utils.setpref("discordtoken", text);
                        System.out.println(text);
                        Config.discordtoken = text;
                        System.out.println(Utils.getpref("discordtoken", ""));

                        return buf.key(ev);
                    }
                }
        );

        appender.addRow(new Label("Discord Channel: "),
                new TextEntry(240, Utils.getpref("discordchannel", "")) {
                    @Override
                    public boolean keydown(KeyEvent ev) {
                        if (!parent.visible)
                            return false;
                        Utils.setpref("discordchannel", text);
                        System.out.println(text);
                        Config.discordchannel = text;
                        System.out.println(Utils.getpref("discordchannel", ""));

                        return buf.key(ev);
                    }
                }
        );

        appender.add(new CheckBox("Vendan Discord Player Alert") {
            {
                a = Config.discordplayeralert;
            }

            public void set(boolean val) {
                Utils.setprefb("discordplayeralert", val);
                Config.discordplayeralert = val;
                a = val;
            }
        });

        appender.add(new CheckBox("Vendan Discord Non-Player Alert") {
            {
                a = Config.discordalarmalert;
            }

            public void set(boolean val) {
                Utils.setprefb("discordalarmalert", val);
                Config.discordalarmalert = val;
                a = val;
            }
        });

        Frame f = new Frame(new Coord(300, 100), false);

        discorduser = new CheckBox("Message a specific user.") {
            {
                a = Config.discorduser;
            }

            public void set(boolean val) {
                Utils.setprefb("discorduser", val);
                Config.discorduser = val;
                a = val;
                Config.discordrole = false;
                discordrole.a = false;
            }
        };

        discordrole = new CheckBox("Message a specific role.") {
            {
                a = Config.discordrole;
            }

            public void set(boolean val) {
                Utils.setprefb("discordrole", val);
                Config.discordrole = val;
                a = val;
                Config.discorduser = false;
                discorduser.a = false;
            }
        };

        appender.add(f);
        f.add(new Label("Messages everyone by default."), 2, 0);
        f.add(discorduser, 0, 20);
        f.add(discordrole, 0, 40);

        f.add(new Label("User Name/Role ID to Alert:"), 2, 60);
        f.add(new TextEntry(80, Utils.getpref("discordalertstring", "")) {
                  @Override
                  public boolean keydown(KeyEvent ev) {
                      if (!parent.visible)
                          return false;
                      Utils.setpref("discordalertstring", text);
                      Config.discordalertstring = text;
                      System.out.println(text);
                      System.out.println(Utils.getpref("discordalertstring", ""));
                      return buf.key(ev);
                  }
              }
                , new Coord(180, 60));


        discord.add(new PButton(200, "Back", 27, main), new Coord(210, 360));
        discord.pack();
    }

    private void initModification() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(modification, new Coord(620, 350)));
        appender.setHorizontalMargin(5);

        appender.add(new Label("Strange or unreal modifications"));

        appender.addRow(new PButton(50, "Water", 'w', waterPanel),
                new PButton(50, "Map", 'm', mapPanel),
                new PButton(50, "Dev", 'w', devPanel)
        );

        appender.addRow(new CheckBox("Custom title: ") {
                            {
                                a = configuration.customTitleBoolean;
                            }

                            public void set(boolean val) {
                                Utils.setprefb("custom-title-bol", val);
                                configuration.customTitleBoolean = val;
                                a = val;

                                MainFrame.instance.setTitle(configuration.tittleCheck(ui.sess));
                            }
                        },
                new ResizableTextEntry(configuration.defaultUtilsCustomTitle) {
                    @Override
                    public void changed() {
                        Utils.setpref("custom-title", text);
                        configuration.defaultUtilsCustomTitle = text;
                        MainFrame.instance.setTitle(configuration.tittleCheck(ui.sess));
                    }
                });
        appender.addRow(new CheckBox("Custom login background: ") {
                            {
                                a = resources.defaultUtilsCustomLoginScreenBgBoolean;
                            }

                            public void set(boolean val) {
                                Utils.setprefb("custom-login-background-bol", val);
                                resources.defaultUtilsCustomLoginScreenBgBoolean = val;
                                a = val;
                                LoginScreen.bg = resources.bgCheck();
                                if (ui != null && ui.root != null && ui.root.getchild(LoginScreen.class) != null)
                                    ui.uimsg(1, "bg");
                            }
                        },
                pictureList != null ? makePictureChoiseDropdown() : new Label("The modification folder has no pictures") {
                    @Override
                    public Object tooltip(Coord c0, Widget prev) {
                        return Text.render("Create modification folder and add in pictures or launch updater").tex();
                    }
                });

        appender.add(new Label(""));

        appender.addRow(new Label("Broken hat replacer"), new Button(50, "Configurate") {
            public void click() {
                Window w = new Window(Coord.z, "Hat wardrobe");
                WidgetVerticalAppender wva = new WidgetVerticalAppender(w);
                final CustomWidgetList list = new CustomWidgetList(resources.customHats, "CustomHats") {
                    public void wdgmsg(Widget sender, String msg, Object... args) {
                        if (!msg.equals("changed")) {
                            super.wdgmsg(sender, msg, args);
                        } else {
                            String name = (String) args[0];
                            boolean val = (Boolean) args[1];
                            synchronized (customlist) {
                                customlist.put(name, val);
                            }
                            if (val) {
                                for (Map.Entry<String, Boolean> entry : customlist.entrySet()) {
                                    if (entry.getValue() && !entry.getKey().equals(name)) {
                                        synchronized (customlist) {
                                            customlist.put(entry.getKey(), false);
                                        }
                                    }
                                }
                                resources.hatreplace = name;
                                Utils.setpref("hatreplace", name);
                            } else {
                                resources.hatreplace = resources.defaultbrokenhat;
                                Utils.setpref("hatreplace", resources.defaultbrokenhat);
                            }
                            Utils.saveCustomList(customlist, jsonname);
                        }
                    }
                };
                final TextEntry value = new TextEntry(150, "") {
                    @Override
                    public void activate(String text) {
                        list.add(text);
                        settext("");
                    }
                };
                wva.add(list);
                wva.addRow(value, new Button(45, "Add") {
                    @Override
                    public void click() {
                        list.put(value.text, false);
                        value.settext("");
                    }
                }, new Button(45, "Load Default") {
                    @Override
                    public void click() {
                        for (String dmark : resources.normalhatslist) {
                            boolean exist = false;
                            for (String mark : resources.customHats.keySet()) {
                                if (dmark.equalsIgnoreCase(mark)) {
                                    exist = true;
                                    break;
                                }
                            }
                            if (!exist)
                                list.put(dmark, false);
                        }
                    }
                });
                w.pack();

                ui.root.adda(w, ui.root.sz.div(2), 0.5, 0.5);
            }

            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Hats works! It is now an unused function. Suggest your changes for it revival").tex();
            }
        });
        appender.addRow(new CheckBox("Cloth painter") {
            {
                a = resources.paintcloth;
            }

            public void set(boolean val) {
                Utils.setprefb("paintcloth", val);
                resources.paintcloth = val;
                a = val;
            }
        }, new Button(50, "Configurate") {
            public void click() {
                Window w = new Window(Coord.z, "Cloth Painter") {{
                    WidgetVerticalAppender wva = new WidgetVerticalAppender(this);
                    CustomWidgetList cwl = new CustomWidgetList(resources.painedcloth, "PaintedClothList", true) {
                        public void wdgmsg(Widget sender, String msg, Object... args) {
                            if (msg.equals("option")) {
                                String name = (String) args[0];
                                Window settings = win(name, getHashJSON(name));
                                ui.root.adda(settings, ui.root.sz.div(2), 0.5, 0.5);
                            } else {
                                super.wdgmsg(sender, msg, args);
                                savejson();
                            }
                        }

                        public JSONObject getHashJSON(String name) {
                            JSONObject jo = new JSONObject();
                            try {
                                jo = resources.painedclothjson.getJSONObject(name);
                            } catch (JSONException ignored) {
                            }
                            return (jo);
                        }

                        public Window win(String name, JSONObject json) {
                            return (new Window(Coord.z, name) {{
                                WidgetVerticalAppender wva = new WidgetVerticalAppender(this);
                                wva.setHorizontalMargin(2);
                                for (String f : resources.clothfilters) {
                                    boolean check = false;
                                    try {
                                        check = json.getBoolean(f);
                                    } catch (JSONException ignored) {
                                    }
                                    wva.add(cbox(f, check, name));
                                }
                                JSONArray colar = new JSONArray();
                                boolean check = false;
                                int a = -1, d = -1, s = -1, e = -1, shine = 0;
                                try {
                                    colar = json.getJSONArray(resources.clothcol);
                                } catch (JSONException ignored) {
                                }
                                if (colar.length() > 0) {
                                    try {
                                        check = colar.getBoolean(0);
                                    } catch (JSONException ignored) {
                                    }
                                    JSONObject colorj = new JSONObject();
                                    try {
                                        colorj = colar.getJSONObject(1);
                                    } catch (JSONException i) {
                                    }
                                    if (colorj.length() > 0) {
                                        try {
                                            a = colorj.getInt("Ambient");
                                        } catch (JSONException ignored) {
                                        }
                                        try {
                                            d = colorj.getInt("Diffuse");
                                        } catch (JSONException ignored) {
                                        }
                                        try {
                                            s = colorj.getInt("Specular");
                                        } catch (JSONException ignored) {
                                        }
                                        try {
                                            e = colorj.getInt("Emission");
                                        } catch (JSONException ignored) {
                                        }
                                        try {
                                            shine = colorj.getInt("Shine");
                                        } catch (JSONException ignored) {
                                        }
                                    }
                                }
                                wva.addRow(cbox(resources.clothcol, check, name),
                                        ccol(a, "Ambient", name, this),
                                        ccol(d, "Diffuse", name, this),
                                        ccol(s, "Specular", name, this),
                                        ccol(e, "Emission", name, this),
                                        new HSlider(100, -100, 100, shine) {
                                            public void changed() {
                                                savejson(name, parent);
                                            }

                                            public Object tooltip(Coord c0, Widget prev) {
                                                return Text.render("Shine: " + val).tex();
                                            }
                                        });
                                pack();
                            }});
                        }

                        public CheckBox cbox(String name, boolean b, String wname) {
                            return (new CheckBox(name) {
                                {
                                    a = b;
                                }

                                public void set(boolean val) {
                                    super.set(val);
                                    savejson(wname, parent);
                                }
                            });
                        }

                        public ColorPreview ccol(int i, String name, String wname, Window pa) {
                            return (new ColorPreview(new Coord(20, 20), new Color(i, true), val -> {
                                savejson(wname, pa);
                            }, name) {

                            });
                        }

                        public JSONObject wjson(Widget json) {
                            JSONObject jo = new JSONObject();
                            List<CheckBox> cbl = json.getchilds(CheckBox.class);
                            for (CheckBox cb : cbl) {
                                for (String f : resources.clothfilters) {
                                    if (cb.lbl.text.equals(f)) {
                                        jo.put(cb.lbl.text, cb.a);
                                        break;
                                    }
                                }
                                if (cb.lbl.text.equals(resources.clothcol)) {
                                    JSONArray ja = new JSONArray();
                                    ja.put(cb.a);
                                    JSONObject co = new JSONObject();
                                    List<ColorPreview> cpl = json.getchilds(ColorPreview.class);
                                    for (ColorPreview cp : cpl) {
                                        co.put(cp.name, cp.getColor().hashCode());
                                    }
                                    HSlider hsl = json.getchild(HSlider.class);
                                    co.put("Shine", hsl.val);
                                    ja.put(co);
                                    jo.put(cb.lbl.text, ja);
                                }
                            }
                            return (jo);
                        }

                        public void createjson(String name, Widget parent) {
                            JSONObject nall = new JSONObject();
                            for (Map.Entry<String, Boolean> entry : customlist.entrySet()) {
                                JSONObject o = new JSONObject();
                                try {
                                    o = resources.painedclothjson.getJSONObject(entry.getKey());
                                } catch (JSONException ignored) {
                                }
                                nall.put(entry.getKey(), entry.getKey().equals(name) ? wjson(parent) : o);
                            }
                            resources.painedclothjson = nall;
                        }

                        public void createjson() {
                            JSONObject nall = new JSONObject();
                            for (Map.Entry<String, Boolean> entry : customlist.entrySet()) {
                                JSONObject o = new JSONObject();
                                try {
                                    o = resources.painedclothjson.getJSONObject(entry.getKey());
                                } catch (JSONException ignored) {
                                }
                                nall.put(entry.getKey(), o);
                            }
                            resources.painedclothjson = nall;
                        }

                        public void savejson(String name, Widget parent) {
                            createjson(name, parent);
                            FileWriter jsonWriter = null;
                            try {
                                jsonWriter = new FileWriter("PaintedCloth.json");
                                jsonWriter.write(resources.painedclothjson.toString());
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                try {
                                    if (jsonWriter != null) {
                                        jsonWriter.flush();
                                        jsonWriter.close();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        public void savejson() {
                            createjson();
                            FileWriter jsonWriter = null;
                            try {
                                jsonWriter = new FileWriter("PaintedCloth.json");
                                jsonWriter.write(resources.painedclothjson.toString());
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                try {
                                    if (jsonWriter != null) {
                                        jsonWriter.flush();
                                        jsonWriter.close();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    };
                    TextEntry search = new TextEntry(cwl.sz.x, "");
                    TextEntry addentry = new TextEntry(0, "") {
                        public void activate(String text) {
                            if (!text.equals("")) {
                                cwl.put(text, false);
                                settext("");
                            }
                        }
                    };
                    Button addbtn = new Button(45, "Add") {
                        public void click() {
                            if (!addentry.text.equals("")) {
                                cwl.put(addentry.text, false);
                                addentry.settext("");
                            }
                        }
                    };
                    addentry.resize(cwl.sz.x - addbtn.sz.x - 1, addentry.sz.y);

                    wva.add(cwl);
                    //wva.add(search);
                    wva.addRow(addentry, addbtn);
                    pack();
                }};

                ui.root.adda(w, ui.root.sz.div(2), 0.5, 0.5);
            }
        });
        appender.add(new CheckBox("Gob resizer") {
            {
                a = configuration.resizegob;
            }

            public void set(boolean val) {
                Utils.setprefb("resizegob", val);
                configuration.resizegob = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Player Status tooltip") {
            {
                a = configuration.statustooltip;
            }

            public void set(boolean val) {
                Utils.setprefb("statustooltip", val);
                configuration.statustooltip = val;
                a = val;
            }
        });
        appender.add(new CheckBox("New gilding window") {
            {
                a = configuration.newgildingwindow;
            }

            public void set(boolean val) {
                Utils.setprefb("newgildingwindow", val);
                configuration.newgildingwindow = val;
                a = val;
            }
        });
        appender.add(new CheckBox("New overlay for plant stage") {
            {
                a = configuration.newCropStageOverlay;
            }

            public void set(boolean val) {
                Utils.setprefb("newCropStageOverlay", val);
                configuration.newCropStageOverlay = val;
                a = val;
            }
        });
        appender.add(new CheckBox("New quick hand slots") {
            {
                a = configuration.newQuickSlotWdg;
            }

            public void set(boolean val) {
                Utils.setprefb("newQuickSlotWdg", val);
                configuration.newQuickSlotWdg = val;
                a = val;

                try {
                    if (ui != null && ui.gui != null) {
                        Widget qs = ui.gui.quickslots;
                        Widget nqs = ui.gui.newquickslots;

                        if (qs != null && nqs != null) {
                            if (val) {
                                nqs.show();
                                qs.hide();
                            } else {
                                nqs.hide();
                                qs.show();
                            }
                        }
                    }
                } catch (ClassCastException e) { // in case we are at the login screen
                }
            }
        });
        appender.add(new CheckBox("Autoclick DiabloLike move") {
            {
                a = configuration.autoclick;
            }

            public void set(boolean val) {
                Utils.setprefb("autoclick", val);
                configuration.autoclick = val;
                a = val;
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Bad works with the old system movement. Turn on only by interest.").tex();
            }
        });

        appender.addRow(new Label("Custom grid size: "), makeCustomMenuGrid(0), makeCustomMenuGrid(1));

        appender.add(new CheckBox("Special menu alt+RMC in proximity to the mouse cursor") {
            {
                a = configuration.proximityspecial;
            }

            public void set(boolean val) {
                Utils.setprefb("proximityspecial", val);
                configuration.proximityspecial = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show trough status") {
            {
                a = configuration.showtroughstatus;
            }

            public void set(boolean val) {
                Utils.setprefb("showtroughstatus", val);
                configuration.showtroughstatus = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show beehive status") {
            {
                a = configuration.showbeehivestatus;
            }

            public void set(boolean val) {
                Utils.setprefb("showbeehivestatus", val);
                configuration.showbeehivestatus = val;
                a = val;
            }
        });
        appender.addRow(new CheckBox("Show tree berry status") {
            {
                a = configuration.showtreeberry;
            }

            public void set(boolean val) {
                Utils.setprefb("showtreeberry", val);
                configuration.showtreeberry = val;
                a = val;
            }
        }, new ColorPreview(new Coord(20, 20), new Color(configuration.showtreeberryamb, true), val -> {
            configuration.showtreeberryamb = val.hashCode();
            Utils.setprefi("showtreeberryamb", val.hashCode());
        }), new ColorPreview(new Coord(20, 20), new Color(configuration.showtreeberrydif, true), val -> {
            configuration.showtreeberrydif = val.hashCode();
            Utils.setprefi("showtreeberrydif", val.hashCode());
        }), new ColorPreview(new Coord(20, 20), new Color(configuration.showtreeberryspc, true), val -> {
            configuration.showtreeberryspc = val.hashCode();
            Utils.setprefi("showtreeberryspc", val.hashCode());
        }), new ColorPreview(new Coord(20, 20), new Color(configuration.showtreeberryemi, true), val -> {
            configuration.showtreeberryemi = val.hashCode();
            Utils.setprefi("showtreeberryemi", val.hashCode());
        }));
        appender.addRow(new CheckBox("Show player distance border", val -> Utils.setprefb("playerbordersprite", configuration.playerbordersprite = val), configuration.playerbordersprite), new ColorPreview(new Coord(20, 20), new Color(configuration.playerbordercolor, true), val -> {
            configuration.playerbordercolor = val.hashCode();
            Utils.setprefi("playerbordercolor", val.hashCode());
        }));
        appender.addRow(new CheckBox("Show player distance box", val -> Utils.setprefb("playerboxsprite", configuration.playerboxsprite = val), configuration.playerboxsprite), new ColorPreview(new Coord(20, 20), new Color(configuration.playerboxcolor, true), val -> {
            configuration.playerboxcolor = val.hashCode();
            Utils.setprefi("playerboxcolor", val.hashCode());
        }));
        appender.addRow(new CheckBox("Resizable World") {
            {
                a = configuration.resizableworld;
            }

            public void set(boolean val) {
                Utils.setprefb("resizableworld", val);
                configuration.resizableworld = val;
                a = val;
            }
        }, new HSlider(200, 1, 500, (int) (configuration.worldsize * 100)) {
            @Override
            public void changed() {
                configuration.worldsize = val / 100f;
                Utils.setprefd("worldsize", configuration.worldsize);
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("World size density: " + configuration.worldsize + "x").tex();
            }
        });
        appender.addRow(new CheckBox("Rotate World") {
            {
                a = configuration.rotateworld;
            }

            public void set(boolean val) {
                Utils.setprefb("rotateworld", val);
                configuration.rotateworld = val;
                a = val;
            }
        }, new HSlider(100, 0, 36000, (int) (configuration.rotateworldvalx * 100)) {
            @Override
            public void changed() {
                configuration.rotateworldvalx = val / 100f;
                Utils.setprefd("rotateworldvalx", configuration.rotateworldvalx);
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Rotate angle x: " + configuration.rotateworldvalx + "%").tex();
            }
        }, new HSlider(100, 0, 36000, (int) (configuration.rotateworldvaly * 100)) {
            @Override
            public void changed() {
                configuration.rotateworldvaly = val / 100f;
                Utils.setprefd("rotateworldvaly", configuration.rotateworldvaly);
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Rotate angle y: " + configuration.rotateworldvaly + "%").tex();
            }
        }, new HSlider(100, 0, 36000, (int) (configuration.rotateworldvalz * 100)) {
            @Override
            public void changed() {
                configuration.rotateworldvalz = val / 100f;
                Utils.setprefd("rotateworldvalz", configuration.rotateworldvalz);
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Rotate angle z: " + configuration.rotateworldvalz + "%").tex();
            }
        });
        appender.add(new CheckBox("Transparency World") {
            {
                a = configuration.transparencyworld;
            }

            public void set(boolean val) {
                Utils.setprefb("transparencyworld", val);
                configuration.transparencyworld = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show base quality fep on food") {
            {
                a = FoodInfo.showbaseq;
            }

            public void set(boolean val) {
                Utils.setprefb("showbaseq", val);
                FoodInfo.showbaseq = val;
                a = val;
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Shows base quality fep on food: fep (basefep) - %").tex();
            }
        });
        appender.add(new CheckBox("Straight ridges") {
            {
                a = configuration.straightridges;
            }

            public void set(boolean val) {
                Utils.setprefb("straightridges", val);
                configuration.straightridges = val;
                a = val;
                if (ui.sess != null) {
                    ui.sess.glob.map.invalidateAll();
                }
            }
        });
        appender.add(new CheckBox("Show distance on Point") {
            {
                a = configuration.showpointdist;
            }

            public void set(boolean val) {
                Utils.setprefb("showpointdist", val);
                configuration.showpointdist = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show number in inventory") {
            {
                a = configuration.showinvnumber;
            }

            public void set(boolean val) {
                Utils.setprefb("showinvnumber", val);
                configuration.showinvnumber = val;
                a = val;
            }
        });
        appender.add(new CheckBox("New livestock manager - need open Livestock Manager Sloth from Xtensions") {
            {
                a = configuration.forcelivestock;
            }

            public void set(boolean val) {
                Utils.setprefb("forcelivestock", val);
                configuration.forcelivestock = val;
                a = val;
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("After inspect an animal open Livestock Manager Sloth").tex();
            }
        });
        appender.add(new CheckBox("New livestock manager autoopen - Request New livestock manager") {
            {
                a = configuration.forcelivestockopen;
            }

            public void set(boolean val) {
                Utils.setprefb("forcelivestockopen", val);
                configuration.forcelivestockopen = val;
                a = val;
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Auto Open Livestock Manager Sloth").tex();
            }
        });
        appender.add(new CheckBox("Show player id in Kith & Kin") {
            {
                a = configuration.kinid;
            }

            public void set(boolean val) {
                Utils.setprefb("kinid", val);
                configuration.kinid = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Enable speed sprite") {
            {
                a = configuration.gobspeedsprite;
            }

            public void set(boolean val) {
                Utils.setprefb("gobspeedsprite", val);
                configuration.gobspeedsprite = val;
                a = val;
                if (ui != null && ui.gui != null && ui.sess != null && ui.sess.glob != null && ui.sess.glob.oc != null) {
                    synchronized (ui.sess.glob.oc) {
                        for (Gob g : ui.sess.glob.oc) {
                            if (val) {
                                if (g.findol(GobSpeedSprite.id) == null && (g.type == Type.HUMAN || g.type == Type.ANIMAL || g.name().startsWith("gfx/kritter/")))
                                    g.addol(new Gob.Overlay(GobSpeedSprite.id, new GobSpeedSprite(g)));
                            } else {
                                Gob.Overlay speed = g.findol(GobSpeedSprite.id);
                                if (speed != null)
                                    g.ols.remove(speed);
                            }
                        }
                    }
                }
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Show speed over head (HUMAN, ANIMAL, gfx/kritter/)").tex();
            }
        });
        appender.add(new CheckBox("Enable snow fall") {
            {
                a = configuration.snowfalloverlay;
            }

            public void set(boolean val) {
                Utils.setprefb("snowfalloverlay", val);
                configuration.snowfalloverlay = val;
                a = val;
                if (ui != null && ui.gui != null) {
                    Gob player = PBotUtils.player(ui);
                    if (player != null) {
                        if (val) {
                            if (player.findol(-4921) == null)
                                player.addol(new Gob.Overlay(-4921, new SnowFall(player)));
                        } else {
                            Gob.Overlay snow = player.findol(-4921);
                            if (snow != null)
                                player.ols.remove(snow);
                        }
                    }
                }
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Cosmetic Effect around the player").tex();
            }
        });
        appender.addRow(new CheckBox("Enable blizzard") {
                            {
                                a = configuration.blizzardoverlay;
                            }

                            public void set(boolean val) {
                                Utils.setprefb("blizzardoverlay", val);
                                configuration.blizzardoverlay = val;
                                a = val;
                                if (ui != null && ui.gui != null && ui.sess != null && ui.sess.glob != null && ui.sess.glob.oc != null) {
                                    synchronized (ui.sess.glob.oc) {
                                        if (val) {
                                            if (configuration.snowThread == null)
                                                configuration.snowThread = new configuration.SnowThread(ui.sess.glob.oc);
                                            if (!configuration.snowThread.isAlive())
                                                configuration.snowThread.start();
                                        } else {
                                            if (configuration.snowThread != null && configuration.snowThread.isAlive())
                                                configuration.snowThread.kill();
                                        }
                                    }
                                }
                            }

                            @Override
                            public Object tooltip(Coord c0, Widget prev) {
                                return Text.render("Cosmetic Effect around other gobs, fps drops").tex();
                            }
                        },
                new HSlider(200, 1, 20, configuration.blizzarddensity) {
                    @Override
                    public void changed() {
                        configuration.blizzarddensity = val;
                        Utils.setprefi("blizzarddensity", configuration.blizzarddensity);

                        if (configuration.blizzardoverlay && ui != null && ui.gui != null && ui.sess != null && ui.sess.glob != null && ui.sess.glob.oc != null) {
                            synchronized (ui.sess.glob.oc) {
                                OCache oc = ui.sess.glob.oc;

                                if (configuration.getCurrentsnow(oc) < val)
                                    configuration.addsnow(oc);
                                else
                                    configuration.deleteSnow(oc);
                            }
                        }
                    }

                    @Override
                    public Object tooltip(Coord c0, Widget prev) {
                        return Text.render("Blizzard density: " + configuration.blizzarddensity).tex();
                    }
                });
        appender.add(new IndirCheckBox("Never delete grids", KEEPGRIDS));
        appender.add(new IndirCheckBox("Never delete gobs", KEEPGOBS));

        appender.add(new CheckBox("Caching Gemstone") {
            {
                a = configuration.cachedGem;
            }

            public void set(boolean val) {
                Utils.setprefb("cachedGem", val);
                configuration.cachedGem = val;
                a = val;
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Caching Gemstone for fast loading").tex();
            }
        });

        appender.add(new Label("Pathfinder"));
        final String[] tiers = {"Perfect", "Medium", "Fastest"};
        appender.addRow(new IndirLabel(() -> String.format("Pathfinding Tier: %s", tiers[PATHFINDINGTIER.get()])), new IndirHSlider(200, 0, 2, PATHFINDINGTIER));
        appender.add(new IndirCheckBox("Limit pathfinding search to 40 tiles", LIMITPATHFINDING));
        appender.add(new IndirCheckBox("Research if goal was not found (requires Limited pathfinding)", RESEARCHUNTILGOAL));

        appender.add(new Label("Flowermenu"));
        appender.addRow(new Label("Instant Flowermenu: "),
                new CheckBox("Opening") {
                    {
                        a = configuration.instflmopening;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("instflmopening", val);
                        configuration.instflmopening = val;
                        a = val;
                    }
                }, new CheckBox("Chosen") {
                    {
                        a = configuration.instflmchosen;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("instflmchosen", val);
                        configuration.instflmchosen = val;
                        a = val;
                    }
                }, new CheckBox("Cancel") {
                    {
                        a = configuration.instflmcancel;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("instflmcancel", val);
                        configuration.instflmcancel = val;
                        a = val;
                    }
                });
        appender.add(new IndirCheckBox("Don't close flowermenu on clicks", BUGGEDMENU));
        appender.add(new IndirCheckBox("Close button to each flowermenu", CLOSEFORMENU));

        appender.add(new Label("Camera"));
        appender.addRow(new Label("Minimal distance for free camera: "),
                new HSlider(200, -200, 200, (int) configuration.badcamdistminimaldefault) {
                    @Override
                    public void changed() {
                        configuration.badcamdistminimaldefault = val;
                        Utils.setpreff("badcamdistminimaldefault", configuration.badcamdistminimaldefault);
                    }

                    @Override
                    public Object tooltip(Coord c0, Widget prev) {
                        return Text.render("Minimal distance for free camera : " + configuration.badcamdistminimaldefault).tex();
                    }
                }
        );

        modification.add(new PButton(200, "Back", 27, main), new Coord(210, 360));
        modification.pack();
    }

    private void initWater() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(waterPanel, new Coord(620, 350)));
        appender.setVerticalMargin(5);
        appender.setHorizontalMargin(5);

        appender.addRow(new CheckBox("Autodrink below threshold") {
            {
                a = Config.autodrink;
            }

            public void set(boolean val) {
                Utils.setprefb("autodrink", val);
                Config.autodrink = val;
                a = val;
            }
        }, new CheckBox("Drink or sip (off/on)") {
            {
                a = configuration.drinkorsip;
            }

            public void set(boolean val) {
                Utils.setprefb("drinkorsip", val);
                configuration.drinkorsip = val;
                a = val;
            }

            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("New type of drinking so as not to drink everything like wine").tex();
            }
        }, new CheckBox("Sip once") {
            {
                a = configuration.siponce;
            }

            public void set(boolean val) {
                Utils.setprefb("siponce", val);
                configuration.siponce = val;
                a = val;
            }

            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Sip once instead of drinking a lot").tex();
            }
        }, new CheckBox("Auto drink'o'sip") {
            {
                a = configuration.autodrinkosip;
            }

            public void set(boolean val) {
                Utils.setprefb("autodrinkosip", val);
                configuration.autodrinkosip = val;
                a = val;
            }

            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Automatically choose to drink or sip (Water = Drink, Other = Sip)").tex();
            }
        });

        appender.addRow(new Label("Liquid"), makeSelectAutoDrinkLiquid(), new CheckBox("Autodrink whatever i find") {
            {
                a = configuration.autoDrinkWhatever;
            }

            public void set(boolean val) {
                Utils.setprefb("autoDrinkWhatever", val);
                configuration.autoDrinkWhatever = val;
                a = val;
            }
        });

        appender.addRow(new Label("Autodrink Threshold"), new HSlider(130, 0, 100, Config.autodrinkthreshold) {
            protected void added() {
                super.added();
                val = (Config.autodrinkthreshold);
            }

            public void changed() {
                Utils.setprefi("autodrinkthreshold", val);
                Config.autodrinkthreshold = val;
            }

            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Autodrink Threshold : " + val + " Percent").tex();
            }
        });

        appender.addRow(new Label("Autodrink check frequency (Seconds)"), makeAutoDrinkTimeDropdown());

        appender.addRow(new Label("Autosip Threshold to this position"), new HSlider(130, 0, 100, configuration.autosipthreshold) {
            protected void added() {
                super.added();
                val = (configuration.autosipthreshold);
            }

            public void changed() {
                Utils.setprefi("autosipthreshold", val);
                configuration.autosipthreshold = val;
            }

            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Autosip Threshold : " + val + " Percent").tex();
            }
        });

        appender.addRow(new Label("Error waiting time"), new HSlider(130, 0, 10000, configuration.sipwaiting) {
            protected void added() {
                super.added();
                val = (configuration.sipwaiting);
            }

            public void changed() {
                Utils.setprefi("sipwaiting", val);
                configuration.sipwaiting = val;
            }

            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Autosip time waiting before error : " + val + " ms").tex();
            }
        });
        appender.add(new CheckBox("Show error message") {
            {
                a = configuration.drinkmessage;
            }

            public void set(boolean val) {
                Utils.setprefb("drinkmessage", val);
                configuration.drinkmessage = val;
                a = val;
            }
        });

        waterPanel.add(new PButton(200, "Back", 27, modification), new Coord(210, 360));
        waterPanel.pack();
    }

    private void initQualityPanel() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(qualityPanel, new Coord(620, 350)));
        appender.setHorizontalMargin(5);

        appender.add(new CheckBox("Item Quality Coloring") {
            {
                a = Config.qualitycolor;
            }

            public void set(boolean val) {
                Utils.setprefb("qualitycolor", val);
                Config.qualitycolor = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Item Quality Coloring Transfer ASC") {
            {
                a = Config.transfercolor;
            }

            public void set(boolean val) {
                Utils.setprefb("transfercolor", val);
                Config.transfercolor = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Drop Color Identical") {
            {
                a = Config.dropcolor;
            }

            public void set(boolean val) {
                Utils.setprefb("dropcolor", val);
                Config.dropcolor = val;
                a = val;
            }
        });

        Frame f = new Frame(new Coord(200, 100), false);
        f.add(new Label("Uncommon below:"), 5, 10);
        f.add(new TextEntry(40, String.valueOf(Config.uncommonq)) {
            @Override
            public boolean keydown(KeyEvent e) {
                return !(e.getKeyCode() >= KeyEvent.VK_F1 && e.getKeyCode() <= KeyEvent.VK_F12);
            }

            @Override
            public boolean type(char c, KeyEvent ev) {
                if (c >= KeyEvent.VK_0 && c <= KeyEvent.VK_9 && buf.line.length() < 3 || c == '\b') {
                    return buf.key(ev);
                } else if (c == '\n') {
                    try {
                        Config.uncommonq = Integer.parseInt(dtext());
                        Utils.setprefi("uncommonq", Config.uncommonq);
                        return true;
                    } catch (NumberFormatException e) {
                    }
                }
                return false;
            }
        }, new Coord(140, 10));

        f.add(new Label("Rare below:"), 5, 30);
        f.add(new TextEntry(40, String.valueOf(Config.rareq)) {
            @Override
            public boolean keydown(KeyEvent e) {
                return !(e.getKeyCode() >= KeyEvent.VK_F1 && e.getKeyCode() <= KeyEvent.VK_F12);
            }

            @Override
            public boolean type(char c, KeyEvent ev) {
                if (c >= KeyEvent.VK_0 && c <= KeyEvent.VK_9 && buf.line.length() < 3 || c == '\b') {
                    return buf.key(ev);
                } else if (c == '\n') {
                    try {
                        Config.rareq = Integer.parseInt(dtext());
                        Utils.setprefi("rareq", Config.rareq);
                        return true;
                    } catch (NumberFormatException e) {
                    }
                }
                return false;
            }
        }, new Coord(140, 30));

        f.add(new Label("Epic below:"), 5, 50);
        f.add(new TextEntry(40, String.valueOf(Config.epicq)) {
            @Override
            public boolean keydown(KeyEvent e) {
                return !(e.getKeyCode() >= KeyEvent.VK_F1 && e.getKeyCode() <= KeyEvent.VK_F12);
            }

            @Override
            public boolean type(char c, KeyEvent ev) {
                if (c >= KeyEvent.VK_0 && c <= KeyEvent.VK_9 && buf.line.length() < 3 || c == '\b') {
                    return buf.key(ev);
                } else if (c == '\n') {
                    try {
                        Config.epicq = Integer.parseInt(dtext());
                        Utils.setprefi("epicq", Config.epicq);
                        return true;
                    } catch (NumberFormatException e) {
                    }
                }
                return false;
            }
        }, new Coord(140, 50));

        f.add(new Label("Legendary below:"), 5, 70);
        f.add(new TextEntry(40, String.valueOf(Config.legendaryq)) {
            @Override
            public boolean keydown(KeyEvent e) {
                return !(e.getKeyCode() >= KeyEvent.VK_F1 && e.getKeyCode() <= KeyEvent.VK_F12);
            }

            @Override
            public boolean type(char c, KeyEvent ev) {
                if (c >= KeyEvent.VK_0 && c <= KeyEvent.VK_9 && buf.line.length() < 3 || c == '\b') {
                    return buf.key(ev);
                } else if (c == '\n') {
                    try {
                        Config.legendaryq = Integer.parseInt(dtext());
                        Utils.setprefi("legendaryq", Config.legendaryq);
                        return true;
                    } catch (NumberFormatException e) {
                    }
                }
                return false;
            }
        }, new Coord(140, 70));

        appender.add(f);
        appender.add(new CheckBox("Insane Item Alert (Above Legendary)") {
            {
                a = Config.insaneitem;
            }

            public void set(boolean val) {
                Utils.setprefb("insaneitem", val);
                Config.insaneitem = val;
                a = val;
            }
        });

        appender.setX(310 + 10);
        appender.setY(0);

        appender.add(new Label("Choose/add item quality color:"));
        appender.add(new CheckBox("Custom quality below") {
            {
                a = configuration.customquality;
            }

            public void set(boolean val) {
                Utils.setprefb("customquality", val);
                configuration.customquality = val;
                a = val;
            }
        });

        final CustomQualityList list = new CustomQualityList();
        appender.add(list);

        appender.addRow(new CheckBox("Quality color more than last") {
            {
                a = configuration.morethanquility;
            }

            public void set(boolean val) {
                Utils.setprefb("morethanquility", val);
                configuration.morethanquility = val;
                a = val;
            }
        }, new ColorPreview(new Coord(20, 20), new Color(configuration.morethancolor, true), val -> {
            configuration.morethancolor = val.hashCode();
            Utils.setprefi("morethancolor", val.hashCode());
        }), new ColorPreview(new Coord(20, 20), new Color(configuration.morethancoloroutline, true), val -> {
            configuration.morethancoloroutline = val.hashCode();
            Utils.setprefi("morethancoloroutline", val.hashCode());
        }));
        final ColorPreview colPre = new ColorPreview(new Coord(20, 20), Color.WHITE, val -> CustomQualityList.NewColor = val);
        final TextEntry value = new TextEntry(120, "") {
            @Override
            public void activate(String text) {
                try {
                    list.add(Double.parseDouble(text), Double.parseDouble(text), CustomQualityList.NewColor, true);
                } catch (Exception e) {
                    System.out.println("Color Quality TextEntry " + e);
                }
                settext("");
            }
        };
        appender.addRow(value, colPre, new Button(45, "Add") {
            @Override
            public void click() {
                try {
                    if (!value.text.isEmpty())
                        list.add(Double.parseDouble(value.text), Double.parseDouble(value.text), CustomQualityList.NewColor, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                value.settext("");
            }
        });

        qualityPanel.add(new PButton(200, "Back", 27, quality), new Coord(210, 360));
        qualityPanel.pack();
    }

    private void initMapPanel() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(mapPanel, new Coord(620, 350)));
        appender.setVerticalMargin(5);
        appender.setHorizontalMargin(5);

        appender.add(new Label("Both"));
        appender.addRow(new Label("Simple map color"), new HSlider(100, 0, 100, (int) (configuration.simplelmapintens * 100)) {
            public void changed() {
                configuration.simplelmapintens = val / 100f;
                Utils.setpreff("simplelmapintens", val / 100f);
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Simple map blend: " + val / 100f).tex();
            }
        });
        appender.add(new CheckBox("Draw cave tiles on map") {
            {
                a = configuration.cavetileonmap;
            }

            public void set(boolean val) {
                Utils.setprefb("cavetileonmap", val);
                configuration.cavetileonmap = val;
                a = val;
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Draw cave tiles on large map. Outline nust be disable.").tex();
            }
        });

        appender.add(new Label(""));
        appender.add(new Label("Minimap"));
        final String[] tiers = {"Default", "Blend", "Simple"};
        appender.addRow(new IndirLabel(() -> String.format("Minimap type: %s", tiers[MINIMAPTYPE.get()])), new IndirHSlider(100, 0, 2, MINIMAPTYPE));
        appender.add(new CheckBox("Disable black lines on minimap") {
            {
                a = Config.disableBlackOutLinesOnMap;
            }

            public void set(boolean val) {
                Utils.setprefb("disableBlackOutLinesOnMap", val);
                Config.disableBlackOutLinesOnMap = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Map Scale") {
            {
                a = Config.mapscale;
            }

            public void set(boolean val) {
                Utils.setprefb("mapscale", val);
                Config.mapscale = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Trollex Map Binds") {
            {
                a = Config.trollexmap;
            }

            public void set(boolean val) {
                Utils.setprefb("trollexmap", val);
                Config.trollexmap = val;
                a = val;
            }
        });

        appender.add(new Label(""));
        appender.add(new Label("Map"));
        appender.addRow(new IndirLabel(() -> String.format("Map type: %s", tiers[MAPTYPE.get()])), new IndirHSlider(100, 0, 2, MAPTYPE));
        appender.addRow(new CheckBox("Additional marks on the map") {
            {
                a = resources.customMarkObj;
            }

            public void set(boolean val) {
                Utils.setprefb("customMarkObj", val);
                resources.customMarkObj = val;
                a = val;
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Automatically places markrs on the map: caves, dungeons, tarpits.").tex();
            }
        }, new Button(50, "Configurate") {
            public void click() {
                Window w = new Window(Coord.z, "Map Marks Configurate");
                WidgetVerticalAppender wva = new WidgetVerticalAppender(w);
                final CustomWidgetList list = new CustomWidgetList(resources.customMarks, "CustomMarks");
                final TextEntry value = new TextEntry(150, "") {
                    @Override
                    public void activate(String text) {
                        list.add(text);
                        settext("");
                    }
                };
                wva.add(list);
                wva.addRow(value, new Button(45, "Add") {
                    @Override
                    public void click() {
                        list.add(value.text);
                        value.settext("");
                    }
                }, new Button(45, "Load Default") {
                    @Override
                    public void click() {
                        for (String dmark : resources.customMarkObjs) {
                            boolean exist = false;
                            for (String mark : resources.customMarks.keySet()) {
                                if (dmark.equalsIgnoreCase(mark)) {
                                    exist = true;
                                    break;
                                }
                            }
                            if (!exist)
                                list.put(dmark, false);
                        }
                    }
                });
                w.pack();

                ui.root.adda(w, ui.root.sz.div(2), 0.5, 0.5);
            }
        });
        appender.add(new CheckBox("Scaling marks from zoom") {
            {
                a = configuration.scalingmarks;
            }

            public void set(boolean val) {
                Utils.setprefb("scalingmarks", val);
                configuration.scalingmarks = val;
                a = val;
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("On a large map the marks will look small").tex();
            }
        });
        appender.add(new CheckBox("Allow texture map") {
            {
                a = configuration.allowtexturemap;
            }

            public void set(boolean val) {
                Utils.setprefb("allowtexturemap", val);
                configuration.allowtexturemap = val;
                a = val;
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Draw textures on large map").tex();
            }
        });
        appender.addRow(new CheckBox("Allow outline map") {
            {
                a = configuration.allowoutlinemap;
            }

            public void set(boolean val) {
                Utils.setprefb("allowoutlinemap", val);
                configuration.allowoutlinemap = val;
                a = val;
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Draw outline on large map").tex();
            }
        }, new HSlider(255, 0, 255, configuration.mapoutlinetransparency) {
            public void changed() {
                configuration.mapoutlinetransparency = val;
                Utils.setprefi("mapoutlinetransparency", val);
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                return Text.render(val + "").tex();
            }
        });
        appender.add(new CheckBox("Allow ridges map") {
            {
                a = configuration.allowridgesmap;
            }

            public void set(boolean val) {
                Utils.setprefb("allowridgesmap", val);
                configuration.allowridgesmap = val;
                a = val;
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Draw ridges on large map").tex();
            }
        });

        appender.add(new Label(""));
        appender.add(new Label("Other"));
        appender.addRow(new Label("Distance view color"), new ColorPreview(new Coord(20, 20), new Color(configuration.distanceviewcolor, true), val -> {
            configuration.distanceviewcolor = val.hashCode();
            Utils.setprefi("distanceviewcolor", val.hashCode());
        }));
        appender.addRow(new Label("Pathfinding color"), new ColorPreview(new Coord(20, 20), new Color(configuration.pfcolor, true), val -> {
            configuration.pfcolor = val.hashCode();
            Utils.setprefi("pfcolor", val.hashCode());
        }));
        appender.addRow(new Label("Dowse color"), new ColorPreview(new Coord(20, 20), new Color(configuration.dowsecolor, true), val -> {
            configuration.dowsecolor = val.hashCode();
            Utils.setprefi("dowsecolor", val.hashCode());
        }));
        appender.addRow(new Label("Questline color"), new ColorPreview(new Coord(20, 20), new Color(configuration.questlinecolor, true), val -> {
            configuration.questlinecolor = val.hashCode();
            Utils.setprefi("questlinecolor", val.hashCode());
        }));

        appender.add(new Label(""));
        appender.addRow(new CheckBox("Temporary marks") {
            {
                a = configuration.tempmarks;
            }

            public void set(boolean val) {
                Utils.setprefb("tempmarks", val);
                configuration.tempmarks = val;
                a = val;
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Draw checked icons on map for a while").tex();
            }
        }, new CheckBox("All Temporary marks") {
            {
                a = configuration.tempmarksall;
            }

            public void set(boolean val) {
                Utils.setprefb("tempmarksall", val);
                configuration.tempmarksall = val;
                a = val;
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Draw all icons on map for a while").tex();
            }
        }, new Label("Total: ") {
            public void tick(double dt) {
                super.tick(dt);
                if (ui != null && ui.gui != null && ui.gui.mapfile != null)
                    settext("Total: " + ui.gui.mapfile.getTempMarkList().size());
            }
        });

        appender.addRow(new HSlider(200, 0, 5000, configuration.tempmarkstime) {
            @Override
            protected void added() {
                super.added();
            }

            @Override
            public void changed() {
                configuration.tempmarkstime = val;
                Utils.setprefi("tempmarkstime", configuration.tempmarkstime);
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Marks time : " + configuration.tempmarkstime + "s").tex();
            }
        }, new HSlider(200, 0, 5000, configuration.tempmarksfrequency) {
            @Override
            protected void added() {
                super.added();
            }

            @Override
            public void changed() {
                configuration.tempmarksfrequency = val;
                Utils.setprefi("tempmarksfrequency", configuration.tempmarksfrequency);
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Frequency time : " + configuration.tempmarksfrequency + "ms").tex();
            }
        });

        mapPanel.add(new PButton(200, "Back", 27, modification), new Coord(210, 360));
        mapPanel.pack();
    }

    private void initDevPanel() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(devPanel, new Coord(620, 350)));
        appender.setVerticalMargin(5);
        appender.setHorizontalMargin(5);

        appender.add(new CheckBox("Log for developer") {
            {
                a = dev.logging;
            }

            public void set(boolean val) {
                Utils.setprefb("msglogging", val);
                dev.logging = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Decode code") {
            {
                a = dev.decodeCode;
            }

            public void set(boolean val) {
                Utils.setprefb("decodeCode", val);
                dev.decodeCode = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Skip exceptions") {
            {
                a = dev.skipexceptions;
            }

            public void set(boolean val) {
                Utils.setprefb("skipexceptions", val);
                dev.skipexceptions = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Resource loading debug log") {
            {
                a = dev.reslog;
            }

            public void set(boolean val) {
                Utils.setprefb("reslog", val);
                dev.reslog = val;
                a = val;
            }
        });
        appender.add(new IndirCheckBox("Debug sloth pathfinding", DEBUG));
        appender.add(new CheckBox("Debug purus pathfinding") {
            {
                a = Pathfinder.DEBUG;
            }

            public void set(boolean val) {
                Pathfinder.DEBUG = val;
                a = val;
            }
        });
        appender.addRow(new Button(50, "Resource") {
                            public void click() {
                                if (ui.sess != null) {
                                    ui.sess.allCache();
                                }
                            }
                        },
                new Button(50, "Clear Memory") {
                    public void click() {
                        System.gc();
                    }
                },
                new Button(50, "Resource Cleaner") {
                    public void click() {
                        getCleaner("Resource Cleaner", "res/").start();
                    }

                    public Object tooltip(Coord c0, Widget prev) {
                        return Text.render("Delete only resources excluding other files such as for example maps. After completing the game turns off").tex();
                    }
                },
                new Button(50, "Map Cleaner") {
                    public void click() {
                        getCleaner("Map Cleaner", "map/").start();
                    }

                    public Object tooltip(Coord c0, Widget prev) {
                        return Text.render("Delete only maps excluding other files such as for example resources. After completing the game turns off").tex();
                    }
                });


        appender.add(new Label(""));
        TextEntry baseurl = new TextEntry(200, Config.resurl.toString()) {
            {
                sz = new Coord(TextEntry.fnd.render(text).sz().x + 10, sz.y);
            }

            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("This is base url. Сhange this if necessary.").tex();
            }
        };
        TextEntry hashid = new TextEntry(200, "") {
            @Override
            public void changed() {
                sz = new Coord(TextEntry.fnd.render(text).sz().x + 10, sz.y);
            }
        };
        TextEntry textEntry = new TextEntry(200, "") {
            @Override
            public boolean type(char c, KeyEvent ev) {
                if (c == '\n') {
                    String hash = String.format("%016x.0", namehash(namehash(0, baseurl.text), "res/" + text)); //-8944751680107289605
                    hashid.settext(hash);

                    PBotUtils.sysMsg(ui, hash);
                    System.out.println(hash);
                } else {
                    return buf.key(ev);
                }
                return false;
            }

            private long namehash(long h, String name) {
                for (int i = 0; i < name.length(); i++)
                    h = (h * 31) + name.charAt(i);
                return (h);
            }

            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Enter resource name and get its hash (press ENTER)").tex();
            }
        };
        appender.addRow(new Label("Base URL: "), baseurl);
        appender.addRow(new Label("res/"), textEntry,
                new Button(30, "ENTER") {
                    public void click() {
                        String hash = String.format("%016x.0", namehash(namehash(0, baseurl.text), "res/" + textEntry.text)); //-8944751680107289605
                        hashid.settext(hash);

                        PBotUtils.sysMsg(ui, hash);
                        System.out.println(hash);
                    }

                    private long namehash(long h, String name) {
                        for (int i = 0; i < name.length(); i++)
                            h = (h * 31) + name.charAt(i);
                        return (h);
                    }
                }, new Button(50, "Download") {
                    public void click() {
                        try {
                            Resource res = Resource.remote(baseurl.text).loadwait(textEntry.text);
                            dev.resourceLog("Resource", "DOWNLOAD", res);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
        appender.addRow(new Label("%appdata%\\Haven and Hearth\\data\\"), hashid, new Button(50, "Remove") {
            public void click() {
                if (hashid.text != null && !hashid.text.equals("")) {
                    try {
                        File basedir = HashDirCache.findbase();
                        File file = new File(basedir, hashid.text);
                        if (!file.exists()) {
                            dev.resourceLog("Resource", "NOT FOUND", file.getAbsolutePath());
                        } else {
                            if (file.delete()) {
                                dev.resourceLog("Resource", "DELETED", file.getAbsolutePath());
                            } else {
                                dev.resourceLog("Resource", "NOT DELETED", file.getAbsolutePath());
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    dev.resourceLog("Resource", "TEXT NOT FOUND");
                }
            }
        });

        final WidgetVerticalAppender appender3 = new WidgetVerticalAppender(devPanel);
        appender3.setX(620 - 140 - 10);

        appender3.add(new CheckBox("Skip msg!") {
            {
                a = dev.msg_log_skip_boolean;
            }

            public void set(boolean val) {
                Utils.setprefb("skiplogmsg", val);
                dev.msg_log_skip_boolean = val;
                a = val;
            }
        });

        dev.msglist = new CheckListbox(140, 15) {
            @Override
            protected void itemclick(CheckListboxItem itm, int button) {
                super.itemclick(itm, button);
                Utils.setprefchklst("msgsel", dev.msgmenus);
            }
        };
        Utils.loadprefchklist("msgsel", dev.msgmenus);
        dev.msglist.items.addAll(dev.msgmenus.values());
        dev.msglist.items.sort(Comparator.comparing(o -> o.name));
        appender3.add(dev.msglist);

        devPanel.add(new PButton(200, "Back", 27, modification), new Coord(210, 360));
        devPanel.pack();
    }

    private void initFlowermenus() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(flowermenus);
        final WidgetVerticalAppender appender2 = new WidgetVerticalAppender(flowermenus);

        appender.setVerticalMargin(VERTICAL_MARGIN);
        appender.setHorizontalMargin(HORIZONTAL_MARGIN);
        appender2.setVerticalMargin(VERTICAL_MARGIN);
        appender2.setHorizontalMargin(HORIZONTAL_MARGIN);
        appender2.setX(150);

        appender2.add(new Label("Autopick Clusters:"));
        CheckListbox clusterlist = new CheckListbox(140, 17) {
            @Override
            protected void itemclick(CheckListboxItem itm, int button) {
                super.itemclick(itm, button);
                Utils.setprefchklst("clustersel", Config.autoclusters);
            }
        };
        Utils.loadprefchklist("clustersel", Config.autoclusters);
        clusterlist.items.addAll(Config.autoclusters.values());
        // clusterlist.items.addAll(Config.autoclusters.values());
        appender2.add(clusterlist);

        appender.add(new CheckBox("Automatic selecton:") {
            {
                a = configuration.autoflower;
            }

            public void set(boolean val) {
                Utils.setprefb("autoflower", val);
                configuration.autoflower = val;
                a = val;
            }
        });
        Config.flowerlist = new CheckListbox(140, 17) {
            @Override
            protected void itemclick(CheckListboxItem itm, int button) {
                super.itemclick(itm, button);
                Utils.setprefchklst("flowersel", Config.flowermenus);
            }

            protected void drawitemname(GOut g, CheckListboxItem itm) {
                Text t = Text.render(Resource.getLocString(Resource.BUNDLE_FLOWER, itm.name));
                Tex T = t.tex();
                g.image(T, new Coord(2, 2), t.sz());
                T.dispose();
            }
        };
        Utils.loadprefchklist("flowersel", Config.flowermenus);
        Config.flowerlist.items.addAll(Config.flowermenus.values());
        Config.flowerlist.items.sort(Comparator.comparing(o -> Resource.getLocString(Resource.BUNDLE_FLOWER, o.name)));
        //  flowerlist.items.addAll(Config.flowermenus.values());
        appender.add(Config.flowerlist);
        Config.petalsearch = new TextEntry(140, "") {
            @Override
            public void changed() {
                update();
            }

            @Override
            public boolean mousedown(Coord mc, int btn) {
                if (btn == 3) {
                    settext("");
                    update();
                    return true;
                } else {
                    return super.mousedown(mc, btn);
                }
            }

            public void update() {
                Config.flowerlist.items.clear();
                for (Map.Entry<String, CheckListboxItem> entry : Config.flowermenus.entrySet()) {
                    if (Resource.getLocString(Resource.BUNDLE_FLOWER, entry.getKey()).toLowerCase().contains(text.toLowerCase()))
                        Config.flowerlist.items.add(entry.getValue());
                }
                Config.flowerlist.items.sort(Comparator.comparing(o -> Resource.getLocString(Resource.BUNDLE_FLOWER, o.name)));
            }

            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Right Click to clear entry").tex();
            }
        };
        appender.add(Config.petalsearch);
        appender.add(new Button(140, "Clear") {
            @Override
            public boolean mousedown(Coord mc, int btn) {
                if (ui.modctrl && btn == 1) {
                    Config.flowermenus.clear();
                    Config.flowerlist.items.clear();
                    Utils.setcollection("petalcol", Config.flowermenus.keySet());
                    Utils.setprefchklst("flowersel", Config.flowermenus);
                }
                return (true);
            }

            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Clear all list if something went wrong (CTRL + LMB). Don't click!").tex();
            }
        });

        flowermenus.add(new PButton(200, "Back", 27, main), new Coord(210, 360));
        flowermenus.pack();
    }

    private void initquickactionsettings() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(quickactionsettings, new Coord(620, 350)));

//        appender.setVerticalMargin(VERTICAL_MARGIN);
        appender.setHorizontalMargin(HORIZONTAL_MARGIN);

        appender.add(new Label("Choose/add gobs for quick action (Q) (Pattern type):"));
        final CustomWidgetList list = new CustomWidgetList(resources.customQuickActions, "QuickPattern");

        appender.add(list);
        final TextEntry value = new TextEntry(150, "") {
            @Override
            public void activate(String text) {
                list.add(text);
                settext("");
            }
        };

        appender.add(value);
        appender.addRow(new Button(45, "Add") {
            @Override
            public void click() {
                list.add(value.text);
                value.settext("");
            }
        }, new Button(45, "Load Default") {
            @Override
            public void click() {
                for (String dact : resources.defaultQuickActions) {
                    boolean exist = false;
                    for (String act : resources.customQuickActions.keySet()) {
                        if (dact.equalsIgnoreCase(act)) {
                            exist = true;
                            break;
                        }
                    }
                    if (!exist)
                        list.put(dact, true);
                }
            }
        });

        appender.setX(list.sz.x);
        appender.setY(list.c.y);
        appender.addRow(new Label("Quick radius"), new HSlider(200, 1, 100, configuration.quickradius) {
            @Override
            public void changed() {
                configuration.quickradius = val;
                Utils.setprefd("quickradius", configuration.quickradius);
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Quick action radius: " + configuration.quickradius + " tiles").tex();
            }
        });
        appender.add(new CheckBox("Autochoice single petal") {
            {
                a = configuration.quickactionauto;
            }

            public void set(boolean val) {
                Utils.setprefb("quickactionauto", val);
                configuration.quickactionauto = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Disable pick forage keybind (Q by Default) opening/closing gates.") {
            {
                a = Config.disablegatekeybind;
            }

            public void set(boolean val) {
                Utils.setprefb("disablegatekeybind", val);
                Config.disablegatekeybind = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Disable pick forage keybind (Q by Default) opening/closing visitor gates.") {
            {
                a = Config.disablevgatekeybind;
            }

            public void set(boolean val) {
                Utils.setprefb("disablevgatekeybind", val);
                Config.disablevgatekeybind = val;
                a = val;
            }
        });
//        appender.add(new CheckBox("Disable pick forage keybind (Q by Default) picking up/dropping carts.") {
//            {
//                a = Config.disablecartkeybind;
//            }
//
//            public void set(boolean val) {
//                Utils.setprefb("disablecartkeybind", val);
//                Config.disablecartkeybind = val;
//                a = val;
//            }
//        });

        quickactionsettings.add(new PButton(200, "Back", 27, main), new Coord(210, 360));
        quickactionsettings.pack();
    }

    private void initstudydesksettings() {
        int x = 0;
        int y = 0, my = 0;
        studydesksettings.add(new Label("Choose curios to check your studydesk for:"), x, y);
        y += 15;
        final CurioList list = studydesksettings.add(new CurioList(), x, y);

        y += list.sz.y + 5;
        final TextEntry value = studydesksettings.add(new TextEntry(150, "") {
            @Override
            public void activate(String text) {
                list.add(text);
                settext("");
            }
        }, x, y);

        studydesksettings.add(new Button(45, "Add") {
            @Override
            public void click() {
                list.add(value.text);
                value.settext("");
            }
        }, x + 155, y - 2);

        my = Math.max(my, y);

        studydesksettings.add(new PButton(200, "Back", 27, main), 0, my + 35);
        studydesksettings.pack();
    }

    private void initautodropsettings() {
        int x = 0;
        int y = 0;
        autodropsettings.add(new Label("Choose/add inventory items to automatically drop:"), x, y);
        y += 15;
        final AutodropList list = autodropsettings.add(new AutodropList(), x, y);

        y += list.sz.y + 5;
        final TextEntry value = autodropsettings.add(new TextEntry(150, "") {
            @Override
            public void activate(String text) {
                list.add(text);
                settext("");
            }
        }, x, y);

        autodropsettings.add(new Button(45, "Add") {
            @Override
            public void click() {
                list.add(value.text);
                value.settext("");
            }
        }, x + 155, y - 2);


        y = 15;
        autodropsettings.add(new CheckBox("Drop mined stones") {
            {
                a = Config.dropMinedStones;
            }

            public void set(boolean val) {
                Utils.setprefb("dropMinedStones", val);
                Config.dropMinedStones = val;
                a = val;
            }
        }, new Coord(list.sz.x + 10, y));
        y += 20;
        autodropsettings.add(new CheckBox("Drop mined ore") {
            {
                a = Config.dropMinedOre;
            }

            public void set(boolean val) {
                Utils.setprefb("dropMinedOre", val);
                Config.dropMinedOre = val;
                a = val;
            }
        }, new Coord(list.sz.x + 10, y));
        y += 20;
        autodropsettings.add(new CheckBox("Drop mined silver/gold ore") {
            {
                a = Config.dropMinedOrePrecious;
            }

            public void set(boolean val) {
                Utils.setprefb("dropMinedOrePrecious", val);
                Config.dropMinedOrePrecious = val;
                a = val;
            }
        }, new Coord(list.sz.x + 10, y));
        y += 20;
        autodropsettings.add(new CheckBox("Drop mined Cat Gold.") {
            {
                a = Config.dropMinedCatGold;
            }

            public void set(boolean val) {
                Utils.setprefb("dropMinedCatGold", val);
                Config.dropMinedCatGold = val;
                a = val;
            }
        }, new Coord(list.sz.x + 10, y));
        y += 20;
        autodropsettings.add(new CheckBox("Drop mined Petrified SeaShells.") {
            {
                a = Config.dropMinedSeaShells;
            }

            public void set(boolean val) {
                Utils.setprefb("dropMinedSeaShells", val);
                Config.dropMinedSeaShells = val;
                a = val;
            }
        }, new Coord(list.sz.x + 10, y));
        y += 20;
        autodropsettings.add(new CheckBox("Drop mined Strange Crystals.") {
            {
                a = Config.dropMinedCrystals;
            }

            public void set(boolean val) {
                Utils.setprefb("dropMinedCrystals", val);
                Config.dropMinedCrystals = val;
                a = val;
            }
        }, new Coord(list.sz.x + 10, y));
        y += 20;
        autodropsettings.add(new CheckBox("Drop mined Quarryartz.") {
            {
                a = Config.dropMinedQuarryquartz;
            }

            public void set(boolean val) {
                Utils.setprefb("dropMinedQuarryquartz", val);
                Config.dropMinedQuarryquartz = val;
                a = val;
            }
        }, new Coord(list.sz.x + 10, y));
        autodropsettings.add(new PButton(200, "Back", 27, main), new Coord(210, 360));
        autodropsettings.pack();
    }

    private void initkeybindsettings() {
        WidgetList<KeyBinder.ShortcutWidget> list = keybindsettings.add(new WidgetList<KeyBinder.ShortcutWidget>(new Coord(300, 24), 16) {
            @Override
            public boolean mousedown(Coord c0, int button) {
                boolean result = super.mousedown(c0, button);
                KeyBinder.ShortcutWidget item = itemat(c0);
                if (item != null) {
                    c0 = c0.add(0, sb.val * itemsz.y);
                    item.mousedown(c0.sub(item.parentpos(this)), button);
                }
                return result;
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                KeyBinder.ShortcutWidget item = itemat(c0);
                if (item != null) {
                    c0 = c0.add(0, sb.val * itemsz.y);
                    return item.tooltip(c0, prev);
                }
                return super.tooltip(c, prev);
            }
        });
        list.canselect = false;
        KeyBinder.makeWidgets(() -> {
            for (int i = 0; i < list.listitems(); i++) {
                list.listitem(i).update();
            }
            return null;
        }).forEach(list::additem);


        keybindsettings.pack();
        keybindsettings.add(new PButton(200, "Back", 27, main), new Coord(410, 360));
        keybindsettings.pack();
    }

    private void initchatsettings() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(chatsettings, new Coord(620, 310)));

//        appender.setVerticalMargin(VERTICAL_MARGIN);
        appender.setHorizontalMargin(HORIZONTAL_MARGIN);

        appender.add(new CheckBox("Enable chat alert sounds") {
            {
                a = Config.chatsounds;
            }

            public void set(boolean val) {
                Utils.setprefb("chatsounds", val);
                Config.chatsounds = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Enable discord chat alert sounds") {
            {
                a = Config.discordsounds;
            }

            public void set(boolean val) {
                Utils.setprefb("discordsounds", val);
                Config.discordsounds = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Enable public realm chat alert sounds") {
            {
                a = Config.realmchatalerts;
            }

            public void set(boolean val) {
                Utils.setprefb("realmchatalerts", val);
                Config.realmchatalerts = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Autoselect new chat") {
            {
                a = configuration.autoselectchat;
            }

            public void set(boolean val) {
                Utils.setprefb("autoselectchat", val);
                configuration.autoselectchat = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Enable private chat alert sounds") {
            {
                a = configuration.privatechatalerts;
            }

            public void set(boolean val) {
                Utils.setprefb("privatechatalerts", val);
                configuration.privatechatalerts = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Ignore unknown private message") {
            {
                a = configuration.ignorepm;
            }

            public void set(boolean val) {
                Utils.setprefb("ignorepm", val);
                configuration.ignorepm = val;
                a = val;
            }
        });
//        appender.addRow(new Label("Enter Village name for Chat Alert sound, and village chat relay."),
//                new TextEntry(150, Config.chatalert) {
//                    @Override
//                    public boolean type(char c, KeyEvent ev) {
//                        if (!parent.visible)
//                            return false;
//
//                        boolean ret = buf.key(ev);
//                        if (text.length() > 0) {
//                            Utils.setpref("chatalert", text);
//                            Config.chatalert = text;
//                        }
//
//                        return ret;
//                    }
//                }
//        );
//        appender.addRow(new Label("Enter Discord Channel for Alerts to be sent to."),
//                new TextEntry(150, Config.AlertChannel) {
//                    @Override
//                    public boolean type(char c, KeyEvent ev) {
//                        if (!parent.visible)
//                            return false;
//
//                        boolean ret = buf.key(ev);
//                        if (text.length() > 0) {
//                            Utils.setpref("AlertChannel", text);
//                            Config.AlertChannel = text;
//                        }
//
//                        return ret;
//                    }
//                }
//        );
//        appender.addRow(new Label("Enter Discord Bot Key"),
//                new TextEntry(475, Config.discordtoken) {
//                    @Override
//                    public boolean type(char c, KeyEvent ev) {
//                        if (!parent.visible)
//                            return false;
//
//                        boolean ret = buf.key(ev);
//                        if (text.length() > 0) {
//                            Utils.setpref("discordtoken", text);
//                            Config.discordtoken = text;
//                        }
//
//                        return ret;
//                    }
//                }
//        );
//        appender.add(new CheckBox("Connect to Discord on Login") {
//            {
//                a = Config.autoconnectdiscord;
//            }
//
//            public void set(boolean val) {
//                Utils.setprefb("autoconnectdiscord", val);
//                Config.autoconnectdiscord = val;
//                a = val;
//            }
//        });
//        discordcheckbox = new CheckBox("Log village chat to Discord - Warning, best used if only one person is using on an alt.") {
//            {
//                a = Config.discordchat;
//            }
//
//            public void set(boolean val) {
//                final String charname = ui.gui.chrid;
//                Utils.setprefb("discordchat_" + charname, val);
//                Config.discordchat = val;
//                a = val;
//            }
//        };
//        appender.add(discordcheckbox);
//        appender.addRow(new Label("Enter Discord channel name for village chat output."),
//                new TextEntry(150, Config.discordchannel) {
//                    @Override
//                    public boolean type(char c, KeyEvent ev) {
//                        if (!parent.visible)
//                            return false;
//
//                        boolean ret = buf.key(ev);
//                        if (text.length() > 0) {
//                            Utils.setpref("discordchannel", text);
//                            Config.discordchannel = text;
//                        }
//
//                        return ret;
//                    }
//                }
//        );
//
//        appender.addRow(new Label("Enter Discord Name For Bot."),
//                new TextEntry(150, Config.charname) {
//                    @Override
//                    public boolean type(char c, KeyEvent ev) {
//                        if (!parent.visible)
//                            return false;
//
//                        boolean ret = buf.key(ev);
//                        if (text.length() > 0) {
//                            Utils.setpref("charname", text);
//                            Config.charname = text;
//                        }
//
//                        return ret;
//                    }
//                }
//        );


//Maybe someday he will return
//        appender.add(new CheckBox("Connection to ArdZone Discord on login."){
//            {
//                a = Config.autoconnectarddiscord;
//            }
//
//            public void set(boolean val) {
//                Utils.setprefb("autoconnectarddiscord", val);
//                Config.autoconnectarddiscord = val;
//                a = val;
//            }
//        });
        chatsettings.add(new PButton(200, "Back", 27, main), new Coord(210, 360));
        chatsettings.pack();
    }

    private void initHideMenu() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(hidesettings, new Coord(620, 350)));
        appender.setVerticalMargin(2);
        appender.setHorizontalMargin(HORIZONTAL_MARGIN);

        appender.addRow(
                new CheckBox("Show tile grid", val -> {
                    Utils.setprefb("showgridlines", Config.showgridlines = val);
                    GameUI gui = getparent(GameUI.class);
                    if (gui != null && gui.map != null)
                        gui.map.togglegrid();
                }, Config.showgridlines),
                new CheckBox("Accurate", val -> Utils.setprefb("showaccgridlines", configuration.showaccgridlines = val), configuration.showaccgridlines),
                new CheckBox("Grid type (old/new)", val -> Utils.setprefb("slothgrid", Config.slothgrid = val), Config.slothgrid),
                new IndirColorPreview(Coord.of(20, 20), GUIDESCOLOR, val -> TileOutline.color = new States.ColState(val.getRed(), val.getGreen(), val.getBlue(), (int) (val.getAlpha() * 0.5)))
        );
        appender.addRow(
                new CheckBox("Display hitboxes", val -> {
                    Utils.setprefb("showboundingboxes", Config.showboundingboxes = val);
                    GameUI gui = getparent(GameUI.class);
                    if (gui != null && gui.map != null) {
                        gui.map.refreshGobsAll();
                    }
                }, Config.showboundingboxes),
                new CheckBox("Accurate", val -> Utils.setprefb("showaccboundingboxes", configuration.showaccboundingboxes = val), configuration.showaccboundingboxes),
                new IndirColorPreview(Coord.of(20, 20), HITBOXCOLOR, val -> {
                    GobHitbox.bbclrstate = new States.ColState(val);
                    if (ui.sess != null) {
                        ui.sess.glob.oc.changeAllGobs();
                    }
                })
        );
        Label placegridtext = new Label("Place Grid (" + Utils.getprefi("placegridval", 8) + "): ");
        appender.addRow(
                placegridtext,
                new HSlider(200, 0, 255, Utils.getprefi("placegridval", 8)) {
                    @Override
                    public void changed() {
                        placegridtext.settext("Place Grid (" + val + "): ");
                        try {
                            ui.cons.run(new String[]{"placegrid", Integer.toString(val)});
                        } catch (Exception e) {
                            e.printStackTrace();
                            Utils.setprefi("placegridval", val);
                        }
                    }

                    @Override
                    public Object tooltip(Coord c0, Widget prev) {
                        return Text.render("Object placement grid: " + val).tex();
                    }
                }
        );
        appender.add(
                new CheckBox("Tile centering", val -> {
                    Utils.setprefb("tilecenter", Config.tilecenter = val);
                    GameUI gui = getparent(GameUI.class);
                    if (gui != null)
                        gui.msg("Tile centering is now turned " + (Config.tilecenter ? "on." : "off."), Color.WHITE);
                }, Config.tilecenter)
        );
        appender.addRow(
                new CheckBox("Hide game objects", val -> {
                    Utils.setprefb("hidegobs", Config.hidegobs = val);
                    GameUI gui = getparent(GameUI.class);
                    if (gui != null && gui.map != null) {
                        gui.map.refreshGobsAll();
                        gui.msg("Gobs are now" + (Config.hidegobs ? " " : " NOT ") + "hidden.", Color.WHITE);
                    }
                }, Config.hidegobs),
                new CheckBox("Hide trees", val -> Utils.setprefb("hideTrees", Config.hideTrees = val), Config.hideTrees),
                new CheckBox("Hide boulders", val -> Utils.setprefb("hideboulders", Config.hideboulders = val), Config.hideboulders),
                new CheckBox("Hide crops", val -> Utils.setprefb("hideCrops", Config.hideCrops = val), Config.hideCrops),
                new CheckBox("Hide bushes", val -> Utils.setprefb("hideBushes", Config.hideBushes = val), Config.hideBushes)
        );
        appender.addRow(
                new CheckBox("Hide unique game objects", val -> {
                    Utils.setprefb("hideuniquegobs", Config.hideuniquegobs = val);
                    GameUI gui = getparent(GameUI.class);
                    if (gui != null && gui.map != null) {
                        gui.map.refreshGobsAll();
                        gui.msg("Unique gobs are now" + (Config.hideuniquegobs ? " " : " NOT ") + "hidden.", Color.WHITE);
                    }
                }, Config.hideuniquegobs).wsettip("Toggle bulk hide by pressing the keybind you assign in Keybind Settings"),
                new IndirColorPreview(Coord.of(20, 20), HIDDENCOLOR, val -> {
                    GobHitbox.fillclrstate = new States.ColState(val);
                    HitboxMesh.updateColor(new States.ColState(val));
                    if (ui.sess != null) {
                        ui.sess.glob.oc.changeAllGobs();
                    }
                }),
                new Button("New Hidden System", () -> {
                    if (ui.gui != null)
                        ui.gui.toggleHidden();
                }).wsettip("These hides are for all objects of this type, to hide individual ones instead please utilize the alt + right click menu."),
                new Button("New Deleted System", () -> {
                    if (ui.gui != null)
                        ui.gui.toggleDeleted();
                })
        );
        appender.add(new CheckBox("Draw colored overlay for hidden objects. Hide will need to be toggled", val -> Utils.setprefb("showoverlay", Config.showoverlay = val), Config.showoverlay));
        appender.add(new CheckBox("Show game object overlays while hidden", val -> Utils.setprefb("showhiddenoverlay", configuration.showhiddenoverlay = val), configuration.showhiddenoverlay));
        hidesettings.add(new PButton(200, "Back", 27, main), new Coord(210, 360));
        hidesettings.pack();
    }

    private void initSoundAlarms() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(soundalarms, new Coord(620, 350)));
        appender.add(new Label("Individual alarms are now set by alt+right clicking an object, or navigating to the alert menu and adding manually."));
        appender.add(new Label("The alert menu can be found by navigating through the bottom right menugrid using 'Game Windows'"));
        appender.setVerticalMargin(VERTICAL_MARGIN);
        appender.setHorizontalMargin(HORIZONTAL_MARGIN);
        appender.add(new CheckBox("Ping on ant dungeon key drops.") {
            {
                a = Config.dungeonkeyalert;
            }

            public void set(boolean val) {
                Utils.setprefb("dungeonkeyalert", val);
                Config.dungeonkeyalert = val;
                a = val;
            }
        });
        appender.setVerticalMargin(0);
        appender.addRow(new Label("Unknown Player Alarm"), makeAlarmDropdownUnknown());
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void added() {
                super.added();
                val = (int) (Config.alarmunknownvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.alarmunknownvol = vol;
                Utils.setprefd("alarmunknownvol", vol);
            }
        });
        appender.setVerticalMargin(0);
        appender.addRow(new Label("Red Player Alarm"), makeAlarmDropdownRed());
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void added() {
                super.added();
                val = (int) (Config.alarmredvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.alarmredvol = vol;
                Utils.setprefd("alarmredvol", vol);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new CheckBox("Alarm on new private/party chat") {
            {
                a = Config.chatalarm;
            }

            public void set(boolean val) {
                Utils.setprefb("chatalarm", val);
                Config.chatalarm = val;
                a = val;
            }
        });
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void added() {
                super.added();
                val = (int) (Config.chatalarmvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.chatalarmvol = vol;
                Utils.setprefd("chatalarmvol", vol);
            }
        });
        appender.setVerticalMargin(0);
        appender.addRow(new Label("Study Finish Alarm"), makeAlarmDropdownStudy());
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void added() {
                super.added();
                val = (int) (Config.studyalarmvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.studyalarmvol = vol;
                Utils.setprefd("studyalarmvol", vol);
            }
        });
        appender.add(new Button(200, "New Alerts System", false) {
            public void click() {
                if (ui.gui != null)
                    ui.gui.toggleAlerted();
            }
        });

        soundalarms.add(new PButton(200, "Back", 27, main), new Coord(210, 360));
        soundalarms.pack();
    }

    private Dropbox<Integer> makeCaveInDropdown() {
        List<String> values = new ArrayList<>();
        for (Integer x : caveindust) {
            String s = x.toString();
            values.add(s);
        }
        return new Dropbox<Integer>(9, values) {
            {
                super.change(null);
            }

            @Override
            protected Integer listitem(int i) {
                return caveindust.get(i);
            }

            @Override
            protected int listitems() {
                return caveindust.size();
            }

            @Override
            protected void drawitem(GOut g, Integer item, int i) {
                g.text(item.toString(), Coord.z);
            }

            @Override
            public void change(Integer item) {
                super.change(item);
                Config.caveinduration = item;
                Utils.setprefi("caveinduration", item);
            }
        };
    }

    private Dropbox<Locale> langDropdown() {
        List<Locale> languages = enumerateLanguages();
        List<String> values = languages.stream().map(Locale::getDisplayName).collect(Collectors.toList());
        return new Dropbox<Locale>(10, values) {
            {
                super.change(new Locale(Resource.language));
            }

            @Override
            protected Locale listitem(int i) {
                return languages.get(i);
            }

            @Override
            protected int listitems() {
                return languages.size();
            }

            @Override
            protected void drawitem(GOut g, Locale item, int i) {
                g.text(item.getDisplayName(), Coord.z);
            }

            @Override
            public void change(Locale item) {
                super.change(item);
                Utils.setpref("language", item.toString());
            }
        };
    }

    private Dropbox<String> makeFontsDropdown() {
        final List<String> fonts = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        return new Dropbox<String>(8, fonts) {
            {
                super.change(Config.font);
            }

            @Override
            protected String listitem(int i) {
                return fonts.get(i);
            }

            @Override
            protected int listitems() {
                return fonts.size();
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }

            @Override
            public void change(String item) {
                super.change(item);
                Config.font = item;
                Utils.setpref("font", item);
            }
        };
    }

    private List<Locale> enumerateLanguages() {
        Set<Locale> languages = new HashSet<>();
        languages.add(new Locale("en"));

        Enumeration<URL> en;
        try {
            en = this.getClass().getClassLoader().getResources("l10n");
            if (en.hasMoreElements()) {
                URL url = en.nextElement();
                JarURLConnection urlcon = (JarURLConnection) (url.openConnection());
                try (JarFile jar = urlcon.getJarFile()) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        String name = entries.nextElement().getName();
                        // we assume that if tooltip localization exists then the rest exist as well
                        // up to dev to make sure that it's true
                        if (name.startsWith("l10n/" + Resource.BUNDLE_TOOLTIP))
                            languages.add(new Locale(name.substring(13, 15)));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ArrayList<>(languages);
    }

    @SuppressWarnings("unchecked")
    private Dropbox<Pair<String, Integer>> combatkeysDropdown() {
        List<String> values = Arrays.stream(combatkeys).map(x -> x.a.toString()).collect(Collectors.toList());
        Dropbox<Pair<String, Integer>> modes = new Dropbox<Pair<String, Integer>>(combatkeys.length, values) {
            @Override
            protected Pair<String, Integer> listitem(int i) {
                return combatkeys[i];
            }

            @Override
            protected int listitems() {
                return combatkeys.length;
            }

            @Override
            protected void drawitem(GOut g, Pair<String, Integer> item, int i) {
                g.text(item.a, Coord.z);
            }

            @Override
            public void change(Pair<String, Integer> item) {
                super.change(item);
                Config.combatkeys = item.b;
                Utils.setprefi("combatkeys", item.b);
            }
        };
        modes.change(combatkeys[Config.combatkeys]);
        return modes;
    }

    private Dropbox<Integer> makeFontSizeChatDropdown() {
        List<String> values = fontSize.stream().map(Object::toString).collect(Collectors.toList());
        return new Dropbox<Integer>(fontSize.size(), values) {
            {
                change(Config.fontsizechat);
            }

            @Override
            protected Integer listitem(int i) {
                return fontSize.get(i);
            }

            @Override
            protected int listitems() {
                return fontSize.size();
            }

            @Override
            protected void drawitem(GOut g, Integer item, int i) {
                g.text(item.toString(), Coord.z);
            }

            @Override
            public void change(Integer item) {
                super.change(item);
                Config.fontsizechat = item;
                Utils.setprefi("fontsizechat", item);
            }
        };
    }

    private Dropbox<String> makeStatGainDropdown() {
        return new Dropbox<String>(statSize.size(), statSize) {
            {
                super.change(Integer.toString(Config.statgainsize));
            }

            @Override
            protected String listitem(int i) {
                return statSize.get(i);
            }

            @Override
            protected int listitems() {
                return statSize.size();
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }

            @Override
            public void change(String item) {
                super.change(item);
                Config.statgainsize = Integer.parseInt(item);
                Utils.setpref("statgainsize", item);
            }
        };
    }

    private Dropbox<String> makeafkTimeDropdown() {
        return new Dropbox<String>(afkTime.size(), afkTime) {
            {
                super.change(Integer.toString(Config.afklogouttime));
            }

            @Override
            protected String listitem(int i) {
                return afkTime.get(i);
            }

            @Override
            protected int listitems() {
                return afkTime.size();
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }

            @Override
            public void change(String item) {
                super.change(item);
                Config.afklogouttime = Integer.parseInt(item);
                Utils.setpref("afklogouttime", item);
            }
        };
    }

    private Dropbox<String> makeAutoDrinkTimeDropdown() {
        return new Dropbox<String>(AutoDrinkTime.size(), AutoDrinkTime) {
            {
                super.change(Integer.toString(Config.autodrinktime));
            }

            @Override
            protected String listitem(int i) {
                return AutoDrinkTime.get(i);
            }

            @Override
            protected int listitems() {
                return AutoDrinkTime.size();
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }

            @Override
            public void change(String item) {
                super.change(item);
                Config.autodrinktime = Integer.parseInt(item);
                Utils.setpref("autodrinktime", item);
            }
        };
    }

    private Dropbox<String> makeSelectAutoDrinkLiquid() {
        return new Dropbox<String>(configuration.liquids.size(), configuration.liquids) {
            {
                super.change(configuration.autoDrinkLiquid);
            }

            @Override
            protected String listitem(int i) {
                return configuration.liquids.get(i);
            }

            @Override
            protected int listitems() {
                return configuration.liquids.size();
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }

            @Override
            public void change(String item) {
                super.change(item);
                configuration.autoDrinkLiquid = item;
                Utils.setpref("autoDrinkLiquid", item);
            }
        };
    }

    public void setMapSettings() {
        final String charname = ui.gui.chrid;

        CheckListbox boulderlist = new CheckListbox(140, 16) {
            @Override
            protected void itemclick(CheckListboxItem itm, int button) {
                super.itemclick(itm, button);
                Utils.setprefchklst("boulderssel_" + charname, Config.boulders);
            }
        };
        boulderlist.items.addAll(Config.boulders.values());
        boulderlist.items.sort(Comparator.comparing(a -> a.name));
        map.add(boulderlist, new Coord(10, 15));

        CheckListbox bushlist = new CheckListbox(140, 16) {
            @Override
            protected void itemclick(CheckListboxItem itm, int button) {
                super.itemclick(itm, button);
                Utils.setprefchklst("bushessel_" + charname, Config.bushes);
            }
        };
        bushlist.items.addAll(Config.bushes.values());
        bushlist.items.sort(Comparator.comparing(a -> a.name));
        map.add(bushlist, new Coord(165, 15));

        CheckListbox treelist = new CheckListbox(140, 16) {
            @Override
            protected void itemclick(CheckListboxItem itm, int button) {
                super.itemclick(itm, button);
                Utils.setprefchklst("treessel_" + charname, Config.trees);
            }
        };
        treelist.items.addAll(Config.trees.values());
        treelist.items.sort(Comparator.comparing(a -> a.name));
        map.add(treelist, new Coord(320, 15));

        CheckListbox iconslist = new CheckListbox(140, 16) {
            @Override
            protected void itemclick(CheckListboxItem itm, int button) {
                super.itemclick(itm, button);
                Utils.setprefchklst("iconssel_" + charname, Config.icons);
            }
        };
        iconslist.items.addAll(Config.icons.values());
        iconslist.items.sort(Comparator.comparing(a -> a.name));
        map.add(iconslist, new Coord(475, 15));

        map.add(new CheckBox("Show road Endpoints") {
            {
                a = Config.showroadendpoint;
            }

            public void set(boolean val) {
                Utils.setprefb("showroadendpoint", val);
                Config.showroadendpoint = val;
                a = val;
            }
        }, 240, 330);

        map.add(new CheckBox("Show road Midpoints") {
            {
                a = Config.showroadmidpoint;
            }

            public void set(boolean val) {
                Utils.setprefb("showroadmidpoint", val);
                Config.showroadmidpoint = val;
                a = val;
            }
        }, 240, 350);

        map.add(new CheckBox("Hide ALL Icons") {
            {
                a = Config.hideallicons;
            }

            public void set(boolean val) {
                Utils.setprefb("hideallicons", val);
                Config.hideallicons = val;
                a = val;
            }
        }, 425, 330);


        map.add(new PButton(140, "Clear Boulders", 27, clearboulders), new Coord(10, 302));
        map.add(new PButton(140, "Clear Bushes", 27, clearbushes), new Coord(165, 302));
        map.add(new PButton(140, "Clear Trees", 27, cleartrees), new Coord(320, 302));
        map.add(new PButton(140, "Clear Icons", 27, clearhides), new Coord(475, 302));


        map.pack();
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && (msg == "close")) {
            hide();
            if (ui.gui != null)
                setfocus(ui.gui.invwnd);
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    public void show() {
        chpanel(main);
        super.show();
    }

    private void showChangeLog() {
        Window log = ui.root.add(new Window(new Coord(50, 50), "Changelog"), new Coord(100, 50));
        log.justclose = true;
        Textlog txt = log.add(new Textlog(new Coord(450, 500)));
        txt.quote = false;
        int maxlines = txt.maxLines = 200;
        log.pack();
        try {
            InputStream in = LoginScreen.class.getResourceAsStream("/CHANGELOG.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            File f = Config.getFile("CHANGELOG.txt");
            FileOutputStream out = new FileOutputStream(f);
            String strLine;
            int count = 0;
            while ((count < maxlines) && (strLine = br.readLine()) != null) {
                txt.append(strLine);
                out.write((strLine + Config.LINE_SEPARATOR).getBytes());
                count++;
            }
            br.close();
            out.close();
            in.close();
        } catch (IOException ignored) {
        }
        txt.setprog(0);
    }

    private Dropbox<String> makeAlarmDropdownUnknown() {
        final List<String> alarms = Config.alarms.values().stream().map(String::toString).collect(Collectors.toList());
        return new Dropbox<String>(Config.alarms.size(), alarms) {
            {
                super.change(Config.alarmunknownplayer);
            }

            @Override
            protected String listitem(int i) {
                return alarms.get(i);
            }

            @Override
            protected int listitems() {
                return alarms.size();
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }

            @Override
            public void change(String item) {
                super.change(item);
                Config.alarmunknownplayer = item;
                Utils.setpref("alarmunknownplayer", item);
                if (!item.equals("None"))
                    Audio.play(Resource.local().loadwait(item), Config.alarmunknownvol);
            }
        };
    }

    private Dropbox<String> makeAlarmDropdownRed() {
        final List<String> alarms = Config.alarms.values().stream().map(String::toString).collect(Collectors.toList());
        return new Dropbox<String>(Config.alarms.size(), alarms) {
            {
                super.change(Config.alarmredplayer);
            }

            @Override
            protected String listitem(int i) {
                return alarms.get(i);
            }

            @Override
            protected int listitems() {
                return alarms.size();
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }

            @Override
            public void change(String item) {
                super.change(item);
                Config.alarmredplayer = item;
                Utils.setpref("alarmredplayer", item);
                if (!item.equals("None"))
                    Audio.play(Resource.local().loadwait(item), Config.alarmredvol);
            }
        };
    }

    private Dropbox<String> makeAlarmDropdownStudy() {
        final List<String> alarms = Config.alarms.values().stream().map(String::toString).collect(Collectors.toList());
        return new Dropbox<String>(Config.alarms.size(), alarms) {
            {
                super.change(Config.alarmstudy);
            }

            @Override
            protected String listitem(int i) {
                return alarms.get(i);
            }

            @Override
            protected int listitems() {
                return alarms.size();
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }

            @Override
            public void change(String item) {
                super.change(item);
                Config.alarmstudy = item;
                Utils.setpref("alarmstudy", item);
                if (!item.equals("None"))
                    Audio.play(Resource.local().loadwait(item), Config.studyalarmvol);
            }
        };
    }

    private Dropbox<String> makeDropdownCleave() {
        final List<String> alarms = Config.alarms.values().stream().map(String::toString).collect(Collectors.toList());
        return new Dropbox<String>(Config.alarms.size(), alarms) {
            {
                super.change(Config.cleavesfx);
            }

            @Override
            protected String listitem(int i) {
                return alarms.get(i);
            }

            @Override
            protected int listitems() {
                return alarms.size();
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }

            @Override
            public void change(String item) {
                super.change(item);
                Config.cleavesfx = item;
                Utils.setpref("cleavesfx", item);
                if (!item.equals("None"))
                    Audio.play(Resource.local().loadwait(item), Config.cleavesoundvol);
            }
        };
    }

    private Dropbox<String> makeDropdownCombat() {
        final List<String> alarms = Config.alarms.values().stream().map(String::toString).collect(Collectors.toList());
        return new Dropbox<String>(Config.alarms.size(), alarms) {
            {
                super.change(Config.attackedsfx);
            }

            @Override
            protected String listitem(int i) {
                return alarms.get(i);
            }

            @Override
            protected int listitems() {
                return alarms.size();
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }

            @Override
            public void change(String item) {
                super.change(item);
                Config.attackedsfx = item;
                Utils.setpref("attackedsfx", item);
                if (!item.equals("None"))
                    Audio.play(Resource.local().loadwait(item), Config.attackedvol);
            }
        };
    }

    private Dropbox<String> makePictureChoiseDropdown() {
        return new Dropbox<String>(pictureList.size(), pictureList) {
            {
                super.change(resources.defaultUtilsCustomLoginScreenBg);
            }

            @Override
            protected String listitem(int i) {
                return pictureList.get(i);
            }

            @Override
            protected int listitems() {
                return pictureList.size();
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
//                g.text(item, Coord.z);
                g.text(item.replace(configuration.picturePath + "\\", ""), Coord.z);
            }

            @Override
            public void change(String item) {
                super.change(item);
                resources.defaultUtilsCustomLoginScreenBg = item;
                Utils.setpref("custom-login-background", item);
                LoginScreen.bg = resources.bgCheck();
                if (ui != null && ui.root != null && ui.root.getchild(LoginScreen.class) != null)
                    ui.uimsg(1, "bg");
            }

            @Override
            public boolean mousedown(Coord c, int btn) {
                if (btn == 3) {
                    pictureList = configuration.findFiles(configuration.picturePath, Arrays.asList(".png", ".jpg", ".gif"));
                }
                super.mousedown(c, btn);
                return true;
            }

            @Override
            public Object tooltip(Coord c0, Widget prev) {
                return Text.render("Right click to reload folder").tex();
            }
        };
    }

    private Dropbox<String> makeCustomMenuGrid(int n) {
        return new Dropbox<String>(menuSize.size(), menuSize) {
            {
                super.change(configuration.customMenuGrid[n]);
            }

            @Override
            protected String listitem(int i) {
                return menuSize.get(i);
            }

            @Override
            protected int listitems() {
                return menuSize.size();
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }

            @Override
            public void change(String item) {
                super.change(item);
                configuration.customMenuGrid[n] = item;
                Utils.setpref("customMenuGrid" + n, item);

                if (ui != null && ui.gui != null && ui.gui.menu != null) {
                    ui.gui.menu.gsz = configuration.getMenuGrid();
                    ui.gui.menu.cap = (ui.gui.menu.gsz.x * ui.gui.menu.gsz.y) - 2;
                    ui.gui.menu.layout = new MenuGrid.PagButton[configuration.getMenuGrid().x][configuration.getMenuGrid().y];
                    ui.gui.menu.updlayout();
                    ui.gui.menu.resize(ui.gui.menu.bgsz.mul(ui.gui.menu.gsz).add(1, 1));
                    ui.gui.brpanel.pack();
                    ui.gui.brpanel.move();
                }
            }
        };
    }

    public class PButton extends Button {
        public final Panel tgt;
        public final int key;

        public PButton(int w, String title, int key, Panel tgt) {
            super(w, title);
            this.tgt = tgt;
            this.key = key;
        }

        public void click() {
            if (tgt == clearboulders) {
                final String charname = ui.gui.chrid;
                for (CheckListboxItem itm : Config.boulders.values())
                    itm.selected = false;
                Utils.setprefchklst("boulderssel_" + charname, Config.boulders);
            } else if (tgt == clearbushes) {
                final String charname = ui.gui.chrid;
                for (CheckListboxItem itm : Config.bushes.values())
                    itm.selected = false;
                Utils.setprefchklst("bushessel_" + charname, Config.bushes);
            } else if (tgt == cleartrees) {
                final String charname = ui.gui.chrid;
                for (CheckListboxItem itm : Config.trees.values())
                    itm.selected = false;
                Utils.setprefchklst("treessel_" + charname, Config.trees);
            } else if (tgt == clearhides) {
                final String charname = ui.gui.chrid;
                for (CheckListboxItem itm : Config.icons.values())
                    itm.selected = false;
                Utils.setprefchklst("iconssel_" + charname, Config.icons);
            } else
                chpanel(tgt);
        }

        public boolean type(char key, java.awt.event.KeyEvent ev) {
            if ((this.key != -1) && (key == this.key)) {
                click();
                return (true);
            }
            return (false);
        }
    }

    public class Panel extends Widget {
        public Panel() {
            visible = false;
            c = Coord.z;
        }
    }

    public class VideoPanel extends Panel {
        private CPanel curcf = null;

        public VideoPanel(Panel back) {
            super();
            add(new PButton(200, "Back", 27, back), new Coord(210, 360));
            resize(new Coord(620, 400));
        }

        public void draw(GOut g) {
            if ((curcf == null) || (g.gc.pref != curcf.cf)) {
                if (curcf != null)
                    curcf.destroy();
                curcf = add(new CPanel(g.gc.pref), Coord.z);
            }
            super.draw(g);
        }

        public class CPanel extends Widget {
            public final GLSettings cf;

            public CPanel(GLSettings gcf) {
                this.cf = gcf;
                final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(this, new Coord(620, 350)));
                appender.setVerticalMargin(2);
                appender.setHorizontalMargin(HORIZONTAL_MARGIN);
                appender.add(new CheckBox("Show Entering/Leaving Messages in Sys Log instead of large Popup - FPS increase?") {
                    {
                        a = Config.DivertPolityMessages;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("DivertPolityMessages", val);
                        Config.DivertPolityMessages = val;
                        a = val;
                    }
                });
                appender.add(new CheckBox("Per-fragment lighting") {
                    {
                        a = cf.flight.val;
                    }

                    public void set(boolean val) {
                        if (val) {
                            try {
                                cf.flight.set(true);
                            } catch (GLSettings.SettingException e) {
                                if (ui.gui != null)
                                    ui.gui.error(e.getMessage());
                                return;
                            }
                        } else {
                            cf.flight.set(false);
                        }
                        a = val;
                        cf.dirty = true;
                    }
                });
                appender.add(new CheckBox("Cel shading") {
                    {
                        a = cf.cel.val;
                    }

                    public void set(boolean val) {
                        if (val) {
                            try {
                                cf.cel.set(true);
                            } catch (GLSettings.SettingException e) {
                                if (ui.gui != null)
                                    ui.gui.error(e.getMessage());
                                return;
                            }
                        } else {
                            cf.cel.set(false);
                        }
                        a = val;
                        cf.dirty = true;
                    }
                });
                appender.add(new CheckBox("Render shadows") {
                    {
                        a = cf.lshadow.val;
                    }

                    public void set(boolean val) {
                        if (val) {
                            try {
                                cf.lshadow.set(true);
                            } catch (GLSettings.SettingException e) {
                                if (ui.gui != null)
                                    ui.gui.error(e.getMessage());
                                return;
                            }
                        } else {
                            cf.lshadow.set(false);
                        }
                        a = val;
                        cf.dirty = true;
                    }
                });
                appender.add(new CheckBox("Antialiasing") {
                    {
                        a = cf.fsaa.val;
                    }

                    public void set(boolean val) {
                        try {
                            cf.fsaa.set(val);
                        } catch (GLSettings.SettingException e) {
                            if (ui.gui != null)
                                ui.gui.error(e.getMessage());
                            return;
                        }
                        a = val;
                        cf.dirty = true;
                    }
                });
                appender.add(new CheckBox("Alpha to coverage") {
                    {
                        a = cf.alphacov.val;
                    }

                    public void set(boolean val) {
                        if (val) {
                            try {
                                cf.alphacov.set(true);
                            } catch (GLSettings.SettingException e) {
                                if (ui.gui != null)
                                    ui.gui.error(e.getMessage());
                                return;
                            }
                        } else {
                            cf.alphacov.set(false);
                        }
                        a = val;
                        cf.dirty = true;
                    }
                });
                appender.add(new CheckBox("Outline") {
                    {
                        a = cf.outline.val;
                    }

                    public void set(boolean val) {
                        try {
                            cf.outline.set(val);
                        } catch (GLSettings.SettingException e) {
                            if (ui.gui != null)
                                ui.gui.error(e.getMessage());
                            return;
                        }
                        a = val;
                        cf.dirty = true;
                    }
                });
                appender.addRow(new Label("Outlines COLOR"), new ColorPreview(new Coord(20, 20), new Color(configuration.outlinecolor, true), val -> {
                    configuration.outlinecolor = val.hashCode();
                    Utils.setprefi("outlinecolor", val.hashCode());
                    Outlines.shadersupdate();
                }));
                appender.addRow(new Label("Outline height ()"), new HSlider(100, 0, 10, (int) (configuration.outlineh)) {
                    @Override
                    public void changed() {
                        configuration.outlineh = val;
                        Utils.setprefd("outlineh", configuration.outlineh);
                        Outlines.shadersupdate();
                    }

                    @Override
                    public Object tooltip(Coord c0, Widget prev) {
                        return Text.render("Outline height: " + configuration.outlineh).tex();
                    }
                });
                appender.add(new IndirCheckBox("Symmetric Outlines", SYMMETRICOUTLINES));
                appender.add(new CheckBox("Instancing") {
                    {
                        a = cf.instancing.val;
                    }

                    public void set(boolean val) {
                        try {
                            cf.instancing.set(val);
                        } catch (GLSettings.SettingException e) {
                            if (ui.gui != null)
                                ui.gui.error(e.getMessage());
                            return;
                        }
                        a = val;
                        cf.dirty = true;
                    }
                });

                Label fpsBackgroundLimitLbl = new Label("Background FPS limit: " + (Config.fpsBackgroundLimit == -1 ? "unlimited" : Config.fpsBackgroundLimit));
                appender.add(fpsBackgroundLimitLbl);
                appender.add(new HSlider(200, 0, 49, 0) {
                    protected void added() {
                        super.added();
                        if (Config.fpsBackgroundLimit == -1) {
                            val = 49;
                        } else {
                            val = Config.fpsBackgroundLimit / 5;
                        }
                    }

                    public void changed() {
                        if (val == 0) {
                            Config.fpsBackgroundLimit = 1;
                        } else if (val == 49) {
                            Config.fpsBackgroundLimit = -1; // Unlimited
                        } else {
                            Config.fpsBackgroundLimit = val * 5;
                        }
                        Utils.setprefi("fpsBackgroundLimit", Config.fpsBackgroundLimit);
                        HavenPanel.bgfd = 1000 / Config.fpsBackgroundLimit;
                        if (Config.fpsBackgroundLimit == -1) {
                            fpsBackgroundLimitLbl.settext("Background FPS limit: unlimited");
                        } else {
                            fpsBackgroundLimitLbl.settext("Background FPS limit: " + Config.fpsBackgroundLimit);
                        }
                    }
                });

                Label fpsLimitLbl = new Label("FPS limit: " + (Config.fpsLimit == -1 ? "unlimited" : Config.fpsLimit));
                appender.add(fpsLimitLbl);
                appender.add(new HSlider(200, 0, 49, 0) {
                    protected void added() {
                        super.added();
                        if (Config.fpsLimit == -1) {
                            val = 49;
                        } else {
                            val = Config.fpsLimit / 5;
                        }
                    }

                    public void changed() {
                        if (val == 0) {
                            Config.fpsLimit = 1;
                        } else if (val == 49) {
                            Config.fpsLimit = -1; // Unlimited
                        } else {
                            Config.fpsLimit = val * 5;
                        }
                        Utils.setprefi("fpsLimit", Config.fpsLimit);
                        HavenPanel.fd = 1000 / Config.fpsLimit;
                        if (Config.fpsLimit == -1) {
                            fpsLimitLbl.settext("FPS limit: unlimited");
                        } else {
                            fpsLimitLbl.settext("FPS limit: " + Config.fpsLimit);
                        }
                    }
                });
                appender.add(new Label("Anisotropic filtering"));
                if (cf.anisotex.max() <= 1) {
                    appender.add(new Label("(Not supported)"));
                } else {
                    final Label dpy = new Label("");
                    appender.addRow(
                            new HSlider(160, (int) (cf.anisotex.min() * 2), (int) (cf.anisotex.max() * 2), (int) (cf.anisotex.val * 2)) {
                                protected void added() {
                                    super.added();
                                    dpy();
                                }

                                void dpy() {
                                    if (val < 2)
                                        dpy.settext("Off");
                                    else
                                        dpy.settext(String.format("%.1f\u00d7", (val / 2.0)));
                                }

                                public void changed() {
                                    try {
                                        cf.anisotex.set(val / 2.0f);
                                    } catch (GLSettings.SettingException e) {
                                        getparent(GameUI.class).error(e.getMessage());
                                        return;
                                    }
                                    dpy();
                                    cf.dirty = true;
                                }
                            },
                            dpy);
                }
                appender.add(new CheckBox("Add flared lip to top of ridges to make them obvious") {
                    {
                        a = Config.obviousridges;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("obviousridges", val);
                        Config.obviousridges = val;
                        a = val;
                        if (ui.sess != null) {
                            ui.sess.glob.map.invalidateAll();
                        }
                    }
                });
                appender.add(new CheckBox("Disable Animations (Big Performance Boost, makes some animations look weird.)") {
                    {
                        a = Config.disableAllAnimations;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("disableAllAnimations", val);
                        Config.disableAllAnimations = val;
                        a = val;
                    }
                });
                Function<Integer, String> animtext = i -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Animation ");
                    if (i == 0)
                        sb.append("on");
                    else if (i == 5000)
                        sb.append("off");
                    else
                        sb.append(i);
                    sb.append(" : ");
                    return (sb.toString());
                };
                Label frequencytext = new Label(animtext.apply(configuration.animationfrequency));
                appender.addRow(
                        frequencytext,
                        new HSlider(200, 0, 5000, configuration.animationfrequency) {
                            @Override
                            public void changed() {
                                frequencytext.settext(animtext.apply(val));
                                Utils.setprefi("animationfrequency", configuration.animationfrequency = val);
                            }

                            @Override
                            public Object tooltip(Coord c0, Widget prev) {
                                return RichText.render(RichText.Parser.quote("Animation cooldown: " + val + " ms\n0 - always on, 5000 - always off"), 300).tex();
                            }
                        }
                );
//                appender.add(new CheckBox("Lower terrain draw distance - Will increase performance, but look like shit. (requires logout)") {
//                    {
//                        a = Config.lowerterraindistance;
//                    }
//
//                    public void set(boolean val) {
//                        Config.lowerterraindistance = val;
//                        Utils.setprefb("lowerterraindistance", val);
//                        a = val;
//                    }
//                });
                appender.addRow(new IndirLabel(() -> String.format("Map View Distance: %d",
                        DRAWGRIDRADIUS.get())), new IndirHSlider(200, 1, 6, DRAWGRIDRADIUS, val -> {
                    if (ui.gui != null && ui.gui.map != null) {
                        ui.gui.map.view = val;
                    }
                }));
                appender.add(new IndirCheckBox("Show Map", SHOWMAP));
                appender.add(new IndirCheckBox("Show Gobs", SHOWGOBS));
                appender.add(new CheckBox("Disable biome tile transitions") {
                    {
                        a = Config.disabletiletrans;
                    }

                    public void set(boolean val) {
                        Config.disabletiletrans = val;
                        Utils.setprefb("disabletiletrans", val);
                        a = val;
                        if (ui.sess != null) {
                            ui.sess.glob.map.invalidateAll();
                        }
                    }
                });
                appender.add(new CheckBox("Disable terrain smoothing") {
                    {
                        a = Config.disableterrainsmooth;
                    }

                    public void set(boolean val) {
                        Config.disableterrainsmooth = val;
                        Utils.setprefb("disableterrainsmooth", val);
                        a = val;
                        if (ui.sess != null) {
                            ui.sess.glob.map.invalidateAll();
                        }
                    }
                });
                appender.add(new CheckBox("Disable terrain elevation") {
                    {
                        a = Config.disableelev;
                    }

                    public void set(boolean val) {
                        Config.disableelev = val;
                        Utils.setprefb("disableelev", val);
                        a = val;
                        if (ui.sess != null) {
                            ui.sess.glob.map.invalidateAll();
                        }
                    }
                });
                appender.add(new CheckBox("Disable flavor objects including ambient sounds") {
                    {
                        a = Config.hideflocomplete;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("hideflocomplete", val);
                        Config.hideflocomplete = val;
                        a = val;
                    }
                });
                appender.add(new IndirCheckBox("Wireframe mode", WIREFRAMEMODE));
                appender.add(new IndirCheckBox("Render water surface", cf.WATERSURFACE));
                appender.add(new CheckBox("Hide flavor objects but keep sounds (requires logout)") {
                    {
                        a = Config.hideflovisual;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("hideflovisual", val);
                        Config.hideflovisual = val;
                        a = val;
                    }
                });
                appender.add(new CheckBox("Show weather - This will also enable/disable Weed/Opium effects") {
                    {
                        a = Config.showweather;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("showweather", val);
                        Config.showweather = val;
                        a = val;
                    }
                });
                appender.add(new CheckBox("Simple crops (req. logout)") {
                    {
                        a = Config.simplecrops;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("simplecrops", val);
                        Config.simplecrops = val;
                        a = val;
                    }
                });
                appender.add(new CheckBox("Show skybox (Potential Performance Impact)") {
                    {
                        a = Config.skybox;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("skybox", val);
                        Config.skybox = val;
                        a = val;
                    }
                });

                appender.add(new CheckBox("Simple foragables (req. logout)") {
                    {
                        a = Config.simpleforage;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("simpleforage", val);
                        Config.simpleforage = val;
                        a = val;
                    }
                });
                appender.add(new CheckBox("Show FPS") {
                    {
                        a = Config.showfps;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("showfps", val);
                        Config.showfps = val;
                        a = val;
                    }
                });
                appender.add(new CheckBox("Disable black load screens. - Can cause issues loading the map, setting not for everyone.") {
                    {
                        a = Config.noloadscreen;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("noloadscreen", val);
                        Config.noloadscreen = val;
                        a = val;
                    }
                });

                appender.add(new Label("Disable animations:"));
                CheckListbox disanimlist = new CheckListbox(320, Config.disableanim.values().size(), 18 + Config.fontadd) {
                    @Override
                    protected void itemclick(CheckListboxItem itm, int button) {
                        super.itemclick(itm, button);
                        Utils.setprefchklst("disableanim", Config.disableanim);
                    }
                };
                disanimlist.items.addAll(Config.disableanim.values());
                appender.add(disanimlist);

                pack();
            }
        }
    }

    public Thread getCleaner(String name, String part) {
        return new Thread(() -> {
            Label search = new Label("", Text.num14boldFnd) {
                public void draw(GOut g) {
                    g.chcolor(0, 0, 0, 120);
                    g.frect(Coord.z, sz);
                    g.chcolor();
                    super.draw(g);
                }
            };
            ui.root.add(search);
            Label[] lbl = new Label[20];
            for (int i = 0; i < lbl.length; i++)
                ui.root.add(lbl[i] = new Label("", Text.num14boldFnd) {
                    public void draw(GOut g) {
                        g.chcolor(0, 0, 0, 120);
                        g.frect(Coord.z, sz);
                        g.chcolor();
                        super.draw(g);
                    }
                });
            File file = HashDirCache.findbase();
            File[] listFiles = file.listFiles(new configuration.MyFileNameFilter(".0"));
            ArrayList<File> files = new ArrayList<>();
            boolean success = false;
            if (listFiles != null)
                for (int i = 0; i < listFiles.length; i++) {
                    if (!success)
                        success = true;
                    try {
                        final RandomAccessFile fp = HashDirCache.open2(listFiles[i], "r");
                        HashDirCache.Header head = ((HashDirCache) ResCache.global).readhead(fp);
                        fp.close();
                        if (Config.resurl.toString().equals(head.cid) && head.name.startsWith(part)) {
                            files.add(listFiles[i]);
                            search.settext("Searching files for deleting: " + ((i + 1) + "/" + listFiles.length) + " - " + files.size() + " added");
                            search.move(ui.root.sz.div(2), 0.5, 0.5);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            search.reqdestroy();
            for (int i = 0; i < files.size(); i++) {
                try {
                    final RandomAccessFile fp = HashDirCache.open2(files.get(i), "r");
                    HashDirCache.Header head = ((HashDirCache) ResCache.global).readhead(fp);
                    fp.close();
                    String text;
                    Color clr;
                    if (files.get(i).delete()) {
                        clr = Color.GREEN;
                        text = String.format("Resource %s: [%s] [%s] DELETED", (i + 1) + "/" + files.size(), head.name, files.get(i).getName());
                    } else {
                        clr = Color.RED;
                        text = String.format("Resource %s: [%s] [%s] NOT DELETED", (i + 1) + "/" + files.size(), head.name, files.get(i).getName());
                    }
                    String finalText = text;
                    Color finalClr = clr;
                    dev.resourceLog(finalText);
                    for (int j = 0; j < lbl.length; j++) {
                        if (lbl[j].text.text.equals("")) {
                            lbl[j].settext(finalText, finalClr);
                            lbl[j].move(ui.root.sz.div(2), 0.5, 6 - j);
                            break;
                        } else {
                            if (j + 1 == lbl.length)
                                for (int k = 0; k < lbl.length; k++) {
                                    if (k + 1 == lbl.length)
                                        lbl[k].settext(finalText, finalClr);
                                    else
                                        lbl[k].settext(lbl[k + 1].text);
                                    lbl[k].move(ui.root.sz.div(2), 0.5, 6 - k);
                                }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (success)
                HackThread.tg().interrupt();
            Arrays.stream(lbl).forEach(Widget::reqdestroy);
        }, name);
    }
}
