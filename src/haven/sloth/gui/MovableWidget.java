package haven.sloth.gui;

import haven.Coord;
import haven.Coord2d;
import haven.GOut;
import haven.UI;
import haven.Widget;
import haven.sloth.io.Storage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Based class to handle everything to do with moving widgets around.
 */
public abstract class MovableWidget extends Widget {
    public static final Map<String, Coord2d> knownPositions = new HashMap<>();
    private static final Map<String, Boolean> knownLocks = new HashMap<>();

    static {
        //These settings are stored in dynamic.sqlite under `widget_position`
        Storage.dynamic.ensure(sql -> {
            try (final Statement stmt = sql.createStatement()) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS widget_position ( name TEXT PRIMARY KEY, x REAL, y REAL, locked BOOLEAN)");
                //For older client versions widget_position didn't have the `locked` column and it needs added in
                try (final ResultSet res = stmt.executeQuery("PRAGMA table_info(widget_position)")) {
                    boolean has3 = false;
                    while (res.next()) {
                        if (res.getInt(1) == 3) {
                            has3 = true;
                            break;
                        }
                    }

                    if (!has3) {
                        //Older client, make the column
                        stmt.executeUpdate("ALTER TABLE widget_position ADD COLUMN locked BOOLEAN");
                    }
                }
            }
        });
        Storage.dynamic.ensure(sql -> {
            try (final Statement stmt = sql.createStatement()) {
                try (final ResultSet res = stmt.executeQuery("SELECT name, x, y, locked FROM widget_position")) {
                    while (res.next()) {
                        final String name = res.getString(1);
                        final double x = res.getDouble(2);
                        final double y = res.getDouble(3);
                        final boolean locked = res.getBoolean(4);
                        knownPositions.put(name, new Coord2d(x, y));
                        knownLocks.put(name, locked);
                    }
                }
            }
        });
    }

    public static final double VISIBLE_PER = 0.9;

    //Database key
    private final String key;
    //Whether we want to lock the current position or not
    private boolean lock;

    private UI.Grab dm = null;
    private Coord doff;

    private boolean movableBg;
    protected boolean loadPosition = true;

    public MovableWidget(final Coord sz, final String name) {
        super(sz);
        this.key = name;
    }

    public MovableWidget(final String name) {
        super();
        this.key = name;
    }

    public MovableWidget() {
        super();
        this.key = null;
    }

    public MovableWidget(final UI ui, final Coord c, final Coord sz, final String name) {
        super(ui, c, sz);
        this.key = name;
    }

    public void toggleLock() {
        lock(!lock);
        savePosition();
    }

    public void lock(boolean l) {
        lock = l;
    }

    public boolean locked() {
        return lock;
    }

    private void loadPosition() {
        if (key != null && knownPositions.containsKey(key)) {
            setPosRel(knownPositions.get(key));
        }
    }

    public boolean moving() {
        return dm != null;
    }

    private void savePosition() {
        if (key != null) {
            final Coord2d rel = relpos();
            knownPositions.put(key, rel);
            Storage.dynamic.write(sql -> {
                final PreparedStatement stmt = Storage.dynamic.prepare("INSERT OR REPLACE INTO widget_position VALUES (?, ?, ?, ?)");
                stmt.setString(1, key);
                stmt.setDouble(2, rel.x);
                stmt.setDouble(3, rel.y);
                stmt.setBoolean(4, lock);
                stmt.executeUpdate();
            });
        }
    }

    @Override
    protected void added() {
        if (loadPosition)
            loadPosition();
        lock = knownLocks.getOrDefault(key, false);
        super.added();
    }

    protected boolean moveHit(final Coord c, final int btn) {
        return ui.modflags() == UI.MOD_CTRL && btn == 3;
    }

    @Override
    public boolean mousedown(final Coord mc, final int button) {
        if (super.mousedown(mc, button)) {
            //Give preference to the Widget using this
            return (true);
        } else if (moveHit(mc, button)) {
            if (!lock) {
                movableBg = true;
                dm = ui.grabmouse(this);
                doff = mc;
                parent.setfocus(this);
                raise();
            }
            return (true);
        } else {
            return (false);
        }
    }

    @Override
    public boolean mouseup(final Coord mc, final int button) {
        if (dm != null) {
            //Preference to this if we're in the middle of moving the widget
            movableBg = false;
            dm.remove();
            dm = null;
            //Ensure user didn't throw the window right off the visible screen...
            if ((c.x + (sz.x * (1 - VISIBLE_PER))) > parent.sz.x) {
                c.x = (int) Math.round(parent.sz.x - sz.x * (1 - VISIBLE_PER));
            } else if ((c.x + (sz.x * VISIBLE_PER)) < 0) {
                c.x = -(int) Math.round(sz.x * VISIBLE_PER);
            }
            if ((c.y + (sz.y * (1 - VISIBLE_PER))) > parent.sz.y) {
                c.y = (int) Math.round(parent.sz.y - sz.y * (1 - VISIBLE_PER));
            } else if ((c.y + (sz.y * VISIBLE_PER)) < 0) {
                c.y = -(int) Math.round(sz.y * VISIBLE_PER);
            }
            savePosition();
            return true;
        } else {
            return super.mouseup(mc, button);
        }
    }

    @Override
    public void move(Coord c) {
        super.move(c);
        if ((c.x + (sz.x * (1 - VISIBLE_PER))) > parent.sz.x) {
            c.x = (int) Math.round(parent.sz.x - sz.x * (1 - VISIBLE_PER));
        } else if ((c.x + (sz.x * VISIBLE_PER)) < 0) {
            c.x = -(int) Math.round(sz.x * VISIBLE_PER);
        }
        if ((c.y + (sz.y * (1 - VISIBLE_PER))) > parent.sz.y) {
            c.y = (int) Math.round(parent.sz.y - sz.y * (1 - VISIBLE_PER));
        } else if ((c.y + (sz.y * VISIBLE_PER)) < 0) {
            c.y = -(int) Math.round(sz.y * VISIBLE_PER);
        }
        savePosition();
    }

    @Override
    public void mousemove(final Coord mc) {
        if (dm != null) {
            //Preference to this if we're in the middle of moving the widget
            c = c.add(mc.add(doff.inv()));
        } else {
            super.mousemove(mc);
        }
    }

    @Override
    public void draw(GOut g) {
        super.draw(g);
        if (movableBg) {
            g.chcolor(60, 60, 60, 120);
            g.frect(Coord.z, sz);
            g.chcolor();
        }
    }

    public void drag(Coord off) {
        dm = ui.grabmouse(this);
        doff = off;
    }
}
