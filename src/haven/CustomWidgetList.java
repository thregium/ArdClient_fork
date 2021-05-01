package haven;

import haven.sloth.util.ObservableMap;
import haven.sloth.util.ObservableMapListener;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CustomWidgetList extends WidgetList<CustomWidgetList.Item> implements ObservableMapListener<String, Boolean> {
    public final ObservableMap<String, Boolean> customlist;
    public final String jsonname;
    public int width;
    public boolean options = false;

    public static final Comparator<Item> ITEM_COMPARATOR = Comparator.comparing(o -> o.name);

    public CustomWidgetList(ObservableMap<String, Boolean> list, String jsonname) {
        super(new Coord(calcWidth(list.keySet()) + 50 + 2, 25), 10);
        width = calcWidth(list.keySet()) + 50 + 2;
        customlist = list;
        customlist.addListener(this);
        this.jsonname = jsonname;
    }

    public CustomWidgetList(ObservableMap<String, Boolean> list, String jsonname, boolean options) {
        super(new Coord(calcWidth(list.keySet()) + 50 + 2 + 25, 25), 10);
        this.options = options;
        width = calcWidth(list.keySet()) + 50 + 2 + 25;
        customlist = list;
        customlist.addListener(this);
        this.jsonname = jsonname;
    }

    public void enableOptions(boolean b) {
        options = b;
    }

    private static int calcWidth(Set<String> names) {
        if (names.size() == 0)
            return 0;
        List<Integer> widths = names.stream().map((v) -> Text.render(v).sz().x).collect(Collectors.toList());
        return widths.stream().reduce(Integer::max).get();
    }

    private static int calcHeight(List<String> values) {
        return Math.max(Text.render(values.get(0)).sz().y, 16);
    }

    @SuppressWarnings("SynchronizeOnNonFinalField")
    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        switch (msg) {
            case "changed": {
                String name = (String) args[0];
                boolean val = (Boolean) args[1];
                synchronized (customlist) {
                    customlist.put(name, val);
                }
                Utils.saveCustomList(customlist, jsonname);
                break;
            }
            case "delete": {
                String name = (String) args[0];
                synchronized (customlist) {
                    customlist.remove(name);
                }
                Utils.saveCustomList(customlist, jsonname);
                removeitem((Item) sender, true);
                update();
                break;
            }
            case "option": {
                break;
            }
            default:
                super.wdgmsg(sender, msg, args);
                break;
        }
    }

    @SuppressWarnings("SynchronizeOnNonFinalField")
    public void add(String name) {
        if (name != null && !name.isEmpty()) {
            Utils.saveCustomList(customlist, jsonname);
            additem(new Item(name));
            update();
        }
    }

    private void update() {
        list.sort(ITEM_COMPARATOR);
        int n = listitems();
        for (int i = 0; i < n; i++) {
            listitem(i).c = itempos(i);
        }
    }

    @Override
    public void init(Map<String, Boolean> base) {
        for (Map.Entry<String, Boolean> entry : customlist.entrySet()) {
            additem(new Item(entry.getKey()));
        }

        update();
    }

    @Override
    public void put(String key, Boolean val) {
        Item item = getItem(key);
        if (item != null) {
            if (item.cb.a != val) {
                item.update(val);
            }
        } else {
            add(key);
        }
    }

    @Override
    public void remove(String key) {
        Item item = getItem(key);
        if (item != null) {
            list.remove(item);
            if (customlist.containsKey(key)) {
                synchronized (customlist) {
                    customlist.remove(key);
                }
                Utils.saveCustomList(customlist, jsonname);
            }
        }
    }

    public class Item extends Widget {

        public final String name;
        private final CheckBox cb;
        private boolean a = false;
        private UI.Grab grab;
        private Button cl, opt;

        public Item(String name) {
            super(new Coord(width, 25));
            this.name = name;

            cb = add(new CheckBox(name), 3, 3);
            Boolean b = customlist.get(name);
            cb.a = b != null && b;
            cb.canactivate = true;

            add(cl = new Button(24, "X") {
                @Override
                public void click() {
                    super.wdgmsg("activate", name);
                }

                @Override
                public boolean mouseup(Coord c, int button) {
                    //FIXME:a little hack, because WidgetList does not pass correct click coordinates if scrolled
                    return super.mouseup(Coord.z, button);
                }
            }, width - 25, 0);
            if (options)
                add(opt = new Button(24, "⚙") {
                    public void click() {
                        super.wdgmsg("opt", name);
                    }

                    public boolean mouseup(Coord c, int button) {
                        //FIXME:a little hack, because WidgetList does not pass correct click coordinates if scrolled
                        return super.mouseup(Coord.z, button);
                    }
                }, width - 50, 0);
        }

        public void draw(GOut g) {
            super.draw(g);
            if (cl != null && cl.c != null && cl.c.x != width - 25)
                cl.c.x = width - 25;
            if (opt != null && opt.c != null && opt.c.x != width - 50)
                opt.c.x = width - 50;
        }

        @Override
        public boolean mousedown(Coord c, int button) {
            if (super.mousedown(c, button)) {
                return true;
            }
            if (button != 1)
                return (false);
            a = true;
            grab = ui.grabmouse(this);
            return (true);
        }

        @Override
        public boolean mouseup(Coord c, int button) {
            if (a && button == 1) {
                a = false;
                if (grab != null) {
                    grab.remove();
                    grab = null;
                }
                if (c.isect(new Coord(0, 0), sz))
                    click();
                return (true);
            }
            return (false);
        }

        private void click() {
            cb.a = !cb.a;
            wdgmsg("changed", name, cb.a);
        }

        @Override
        public void wdgmsg(Widget sender, String msg, Object... args) {
            switch (msg) {
                case "ch":
                    wdgmsg("changed", name, (int) args[0] > 0);
                    break;
                case "activate":
                    wdgmsg("delete", name);
                    break;
                case "opt":
                    wdgmsg("option", name);
                default:
                    super.wdgmsg(sender, msg, args);
                    break;
            }
        }

        public void update(boolean a) {
            this.cb.a = a;
        }
    }

    public boolean contains(String name) {
        for (Item item : list) {
            if (item.name.equals(name))
                return true;
        }
        return false;
    }

    public Item getItem(String name) {
        for (Item item : list) {
            if (item.name.equals(name))
                return item;
        }
        return null;
    }
}
