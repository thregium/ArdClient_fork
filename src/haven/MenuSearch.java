package haven;

import haven.sloth.util.ObservableListener;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MenuSearch extends Window implements ObservableListener<MenuGrid.Pagina> {
    private static final int WIDTH = 200;
    private final TextEntry entry;
    private final List<MenuGrid.Pagina> all = new ArrayList<>();
    private final ActList list;
    public boolean ignoreinit;

    public MenuSearch(String caption) {
        super(Coord.z, caption, caption);
        setcanfocus(true);
        setfocusctl(true);
        addBtn_base("gfx/hud/helpbtn", "Show Filter Help", () -> ItemFilter.showHelp(ui, ItemFilter.FILTER_HELP));
        entry = add(new TextEntry(WIDTH, "") {
            public void activate(String text) {
                if (list.sel != null)
                    act(list.sel.pagina);
                MenuSearch.this.hide();
            }

            protected void changed() {
                super.changed();
                dirty = true;
            }

            public boolean type(char c, KeyEvent ev) {
                if (ignoreinit) {
                    ignoreinit = false;
                    return false;
                }
                return super.type(c, ev);
            }

            public boolean keydown(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    final Optional<Integer> idx = list.selindex();
                    if (idx.isPresent()) {
                        list.change(Math.max(idx.get() - 1, 0));
                    } else {
                        list.change(0);
                    }
                    return true;
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    final Optional<Integer> idx = list.selindex();
                    if (idx.isPresent()) {
                        list.change(Math.min(idx.get() + 1, list.listitems() - 1));
                    } else {
                        list.change(0);
                    }
                    return true;
                } else {
                    return super.keydown(e);
                }
            }
        });
        setfocus(entry);
        list = add(new ActList(WIDTH, 10) {
            protected void itemclick(ActItem item, int button) {
                // if(sel == item) {
                act(item.pagina);
                // MenuSearch.this.hide();
                //  } else {
                super.itemclick(item, button);
                // }
            }
        }, 0, entry.sz.y + 5);
        pack();
    }

    public void act(MenuGrid.Pagina act) {
        if (ui.gui != null) {
            ui.gui.menu.use(act.button(), new MenuGrid.Interaction(),false);
        }
    }

    public void show() {
        super.show();
        entry.settext("");
        list.change(0);
        parent.setfocus(this);
    }

    @Override
    protected void added() {
        super.added();
        ui.gui.menu.paginae.addListener(this);
    }

    public void close() {
        hide();
    }

    @Override
    protected void removed() {
        if (ui.gui != null)
            ui.gui.menu.paginae.removeListener(this);
    }

    private boolean dirty = false;

    @Override
    public void tick(final double dt) {
        super.tick(dt);
        if (dirty) {
            dirty = false;
            try {
                refilter();
            } catch (Loading l) {
                dirty = true;
            }
        }
    }

    private void refilter() {
        list.sb.reset();
        list.clear();
        /*for (MenuGrid.Pagina p : all) {
            if (p.res.get().layer(Resource.action).name.toLowerCase().contains(entry.text().toLowerCase()))
                list.add(p);
        }*/

        String filter = entry.text().toLowerCase();
        ItemFilter itemFilter = ItemFilter.create(filter);
        synchronized (all) {
            if (filter.startsWith("new:")) {
                all.stream().filter(p -> p.newp != 0).forEach(list::add);
            }
            if (list.listitems() == 0) {
                List<MenuGrid.Pagina> filtered = all.stream().filter(p -> {
                    String name = p.act().name.toLowerCase();
                    Indir<Resource> parent = p.act().parent;
                    String par = "";
                    if (parent != null) {
                        MenuGrid.Pagina pp = p.scm.paginafor(parent);
                        par = pp.act().name.toLowerCase();
                    }
                    return (par.contains(filter) || name.contains(filter) || itemFilter.matches(p, ui.sess));
                }).collect(Collectors.toList());
                filtered.forEach(list::add);
            }
        }
        list.sort(new ItemComparator());
        if (list.listitems() > 0) {
            final Optional<Integer> idx = list.selindex();
            if (idx.isPresent()) {
                list.change(Math.max(idx.get() - 1, 0));
            } else {
                list.change(0);
            }
        }
    }

    @Override
    public void init(Collection<MenuGrid.Pagina> base) {
        for (final MenuGrid.Pagina pag : base) {
            all.add(pag);
            /*if (isIncluded(pag)) {
                list.add(pag);
            }*/
        }
        dirty = true;
    }

    @Override
    public void added(MenuGrid.Pagina item) {
        all.add(item);
        /*if (isIncluded(item)) {
            list.add(item);
        }*/
    }

    @Override
    public void edited(MenuGrid.Pagina olditem, MenuGrid.Pagina newitem) {
    }

    @Override
    public void remove(MenuGrid.Pagina item) {
        all.remove(item);
        /*if (isIncluded(item)) {
            list.remove(item);
        }*/
    }

    private class ItemComparator implements Comparator<ActList.ActItem> {
        public int compare(ActList.ActItem a, ActList.ActItem b) {
            return a.name.text.compareTo(b.name.text);
        }
    }

    private boolean isIncluded(MenuGrid.Pagina pagina) {
        //ensure it's loaded
        try {
            pagina.res();
        } catch (Loading e) {
            try {
                e.waitfor();
            } catch (InterruptedException ex) {
                //Ignore
            }
        }
        return pagina.res.get().layer(Resource.action).name.toLowerCase().contains(entry.text().toLowerCase());
    }
}
