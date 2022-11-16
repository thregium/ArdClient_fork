package haven.purus.pbot;

import haven.Button;
import haven.Coord;
import haven.FlowerMenu;
import haven.GOut;
import haven.GameUI;
import haven.Listbox;
import haven.Text;
import haven.TextEntry;
import haven.UI;
import haven.Utils;
import haven.Widget;
import haven.WidgetList;
import modification.configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PBotScriptlist extends GameUI.Hidewnd {
    public static String scriptFolder = "scripts";
    public static final Path defPath = Paths.get(scriptFolder + File.separator);
    public static final String[] exps = new String[]{".pbot", ".py"};

    private TextEntry search;
    private Button change;
    private ScriptList list;
    private ThreadList threadList;

    boolean dragging;
    PBotScriptlistItem draggedItem;

    public PBotScriptlist() {
        super(new Coord(228, 280), "PBot Scripts");

        change = new Button(0, "T") {
            @Override
            public void click() {
                list.show(!list.visible);
                threadList.show(!threadList.visible);
                search.settext("");
                if (list.visible) {
                    list.refreshItemList();
                    list.sb.val = 0;
                } else if (threadList.visible) {
                    threadList.refreshItemList();
                    threadList.sb.val = 0;
                }
            }
        };
        search = new TextEntry(210 - change.sz.x, "", t -> {
            list.changeFilter(t);
        }, t -> {
            setfocus(list);
            list.changeFilter(t);
        }) {
            @Override
            public boolean mousedown(Coord mc, int btn) {
                if (btn == 3) {
                    settext("");
                    list.refreshItemList();
                    return (true);
                } else {
                    return (super.mousedown(mc, btn));
                }
            }
        };
        add(search, new Coord(10, 5));
        add(change, new Coord(asz.x - change.sz.x - 10, 5));

        list = new ScriptList(reloadList(ui, defPath), 200, 24, 10);
        add(list, new Coord(10, 35));

        threadList = new ThreadList(210, 10);
        add(threadList, new Coord(10, 35));
        threadList.hide();
    }

    @Override
    public void draw(GOut g) {
        super.draw(g);
        ui.drawafter(g1 -> {
            if (dragging && draggedItem != null) {
                g1.image(draggedItem.getIconTex(), ui.mc.add(new Coord(32, 32).div(2).inv()), new Coord(32, 32));
            }
        });
    }

    @Override
    public boolean show(boolean show) {
        if (show) {
            search.settext("");
            list.refreshItemList();
        }
        return super.show(show);
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (msg.equals("dragstart")) {
            dragging = true;
            draggedItem = (PBotScriptlistItem) args[0];
        } else if (msg.equals("dragstop")) {
            dragging = false;
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    private static class ScriptList extends WidgetList<PBotScriptlistItem> {
        public final List<PBotScriptlistItem> itemList = new ArrayList<>();

        private UI.Grab grab;
        PBotScriptlistItem mdItem;

        public PBotScriptlistItem getScript(String filename) {
            Path f = Paths.get(filename);
            return (new PBotScriptlistItem(ui, f));
        }

        public ScriptList(List<PBotScriptlistItem> initList, int w, int h, int items) {
            super(Coord.of(w, h), items);
            list.addAll(initList);
            pack();
        }

        public void changeFilter(String filter) {
            itemList.clear();
            itemList.addAll(reloadList(ui, defPath, filter));
        }

        @Override
        protected PBotScriptlistItem listitem(int i) {
            return (itemList.get(i));
        }

        @Override
        protected int listitems() {
            return (itemList.size());
        }

        @Override
        protected void drawitem(GOut g, PBotScriptlistItem item, int i) {
            item.draw(g);
        }

        @Override
        public boolean mousedown(Coord c, int button) {
            Coord cc = xlate(sb.c, true);
            if (button == 3) {
                PBotScriptlistItem script = itemat(c);
                if (script != null && !script.isFolder()) {
                    String fileName = script.scriptFile.toAbsolutePath().toString().substring(System.getProperty("user.dir").length() + 1).replace("/", File.separator).replace("\\", File.separator).replace(File.separator, ":");
                    FlowerMenu menu = new FlowerMenu(ch -> {
                        if (ch == -1) return;
                        if (ch == 0) {
                            if (configuration.autorunscripts.contains(fileName)) {
                                configuration.autorunscripts.remove(fileName);
                            } else {
                                configuration.autorunscripts.add(fileName);
                            }
                            Utils.setcollection("autorunscripts", configuration.autorunscripts.items());
                        }
                    }, configuration.autorunscripts.contains(fileName) ? "Remove from autorun" : "Add to autorun");
                    ui.root.getchilds(FlowerMenu.class).forEach(wdg -> wdg.choose(null));
                    ui.root.add(menu, ui.mc);
                    menu.z(1);
                    return (true);
                }
            } else if (!(c.isect(cc, sb.sz) && sb.mousedown(c.add(cc.inv()), button))) {
                mdItem = itemat(c);
                if (button == 1 && mdItem != null && !mdItem.isFolder()) {
                    grab = ui.grabmouse(this);
                    super.wdgmsg("dragstart", mdItem);
                }
            }
            return (true);
        }

        @Override
        public boolean mouseup(Coord c, int button) {
            PBotScriptlistItem item = itemat(c);
            if (item != null && itemat(c) == mdItem) {
                if (item.isFolder())
                    refreshItemList(mdItem.scriptFile);
                else
                    item.runScript();
            }
            if (mdItem != null && grab != null) {
                ui.dropthing(ui.root, ui.mc, mdItem);
            }
            if (button == 1 && grab != null) {
                grab.remove();
                grab = null;
                super.wdgmsg("dragstop");
            }
            mdItem = null;
            return (true);
        }

        @Override
        protected void drawbg(GOut g) {
            g.chcolor(0, 0, 0, 120);
            g.frect(Coord.z, sz);
            g.chcolor();
        }

        public void refreshItemList(Path path) {
            itemList.clear();
            itemList.addAll(reloadList(ui, path));
        }

        public void refreshItemList() {
            refreshItemList(defPath);
        }
    }

    public static List<PBotScriptlistItem> reloadList(UI ui, Path path) {
        final List<PBotScriptlistItem> list = new ArrayList<>();
        try {
            if (!path.equals(defPath))
                list.add(new PBotScriptlistItem(ui, path.getParent(), true));
            try (Stream<Path> stream = Files.list(path)) {
                stream.filter(p -> (Files.isDirectory(p) && !exclude(p)) || endsWith(p, exps)).forEach(p -> list.add(new PBotScriptlistItem(ui, p)));
            }
            list.sort(PBotScriptlistItem::compareTo);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return (list);
    }

    public static List<PBotScriptlistItem> reloadList(UI ui, Path path, String text) {
        if (text.equals(""))
            return (reloadList(ui, path));
        final List<PBotScriptlistItem> list = new ArrayList<>();
        try {
            if (!path.equals(defPath))
                list.add(new PBotScriptlistItem(ui, path.getParent(), true));
            try (Stream<Path> stream = Files.find(path, 100, (p, bfa) -> {
                String name = p.toString().toLowerCase();
                return (!Files.isDirectory(p) && endsWith(p, exps) && name.contains(text.toLowerCase().trim()));
            })) {
                stream.forEach(p -> list.add(new PBotScriptlistItem(ui, p)));
            }
            list.sort(PBotScriptlistItem::compareTo);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return (list);
    }

    public static boolean endsWith(Path p, String[] ends) {
        String text = p.toString().toLowerCase();
        for (String end : ends)
            if (text.endsWith(end) && !exclude(p))
                return (true);
        return (false);
    }

    public static boolean exclude(Path p) {
        String text = p.toFile().getName().toLowerCase();
        return (text.startsWith("__") || p.equals(Paths.get(scriptFolder, "py", "loader.py")) || p.equals(Paths.get(scriptFolder, "py", "py4j")));
    }

    private static class ThreadList extends Listbox<Map.Entry<String, PBotScript>> {
        private static final Coord nameoff = new Coord(5, 5);

        List<Map.Entry<String, PBotScript>> itemList;
        List<Map.Entry<String, PBotScript>> filteredItemList;
        Map.Entry<String, PBotScript> mdItem;

        public ThreadList(int w, int h) {
            super(w, h, 24);
            refreshItemList();
        }

        public void changeFilter(String filter) {
            filteredItemList = itemList.stream()
                    .filter(item -> item.getKey().toLowerCase().contains(filter.toLowerCase()))
                    .collect(Collectors.toList());
        }

        @Override
        protected Map.Entry<String, PBotScript> listitem(int i) {
            if (i < 0 || i >= filteredItemList.size())
                return null;
            return filteredItemList.get(i);
        }

        @Override
        protected int listitems() {
            return filteredItemList.size();
        }

        @Override
        protected void drawitem(GOut g, Map.Entry<String, PBotScript> item, int i) {
            g.image(Text.render(item.getValue().name() + "_" + item.getValue().id).tex(), nameoff);
        }

        @Override
        protected Object itemtooltip(Coord c, Map.Entry<String, PBotScript> item) {
            return Text.render(item.getValue().name() + "_" + item.getValue().id).tex();
        }

        @Override
        public boolean mousedown(Coord c, int button) {
            mdItem = itemat(c);
            return true;
        }

        @Override
        public boolean mouseup(Coord c, int button) {
            Map.Entry<String, PBotScript> item = itemat(c);
            if (item != null && itemat(c) == mdItem) {
                deleteItem(item);
                refreshItemList();
            }
            mdItem = null;
            return true;
        }

        @Override
        protected void drawbg(GOut g) {
            g.chcolor(0, 0, 0, 120);
            g.frect(Coord.z, sz);
            g.chcolor();
        }

        public void refreshItemList() {
            itemList = new ArrayList<>();
            itemList.addAll(PBotScriptmanager.scripts.entrySet());
            itemList = itemList.stream()
                    .sorted(Comparator.comparing(Map.Entry<String, PBotScript>::getKey))
                    .collect(Collectors.toList());
            filteredItemList = itemList;
        }

        public void deleteItem(Map.Entry<String, PBotScript> item) {
            item.getValue().kill();
        }

        public void removeFromList(Map.Entry<String, PBotScript> item) {
            if (item != null) {
                filteredItemList.remove(item);
                itemList.remove(item);
            }
        }

        public void removeFromList(PBotScript script) {
            removeFromList(getEntry(script));
        }

        public Map.Entry<String, PBotScript> getEntry(PBotScript script) {
            for (Map.Entry<String, PBotScript> item : itemList)
                if (item.getValue().equals(script))
                    return (item);
            return (null);
        }
    }

    public PBotScriptlistItem getScript(String name) {
        return (list.getScript(name));
    }

    public void threadsUpdate() {
        threadList.refreshItemList();
    }

    public void removeFromList(PBotScript script) {
        threadList.removeFromList(script);
    }

    @Override
    public boolean mousedown(Coord c, int button) {
        if (button == 3 && ui.modflags() == UI.MOD_META) {
            FlowerMenu menu = new FlowerMenu(ch -> {
                if (ch == -1) return;
                if (ch == 0) {
                    Utils.setprefb("autorunscriptsenable", configuration.autorunscriptsenable = !configuration.autorunscriptsenable);
                }
            }, configuration.autorunscriptsenable ? "Disable autorun" : "Enable autorun");
            ui.root.getchilds(FlowerMenu.class).forEach(wdg -> wdg.choose(null));
            ui.root.add(menu, ui.mc);
            return (true);
        }
        return (super.mousedown(c, button));
    }
}
