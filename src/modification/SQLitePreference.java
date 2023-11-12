package modification;

import haven.sloth.io.Storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;

public class SQLitePreference extends AbstractPreferences {
    private String nodeName = "database";
    private static final SQLitePreference ROOT = new SQLitePreference();
    private final Map<String, SQLitePreference> childNodes = Collections.synchronizedMap(new HashMap<>());
    private Storage storage;

    private SQLitePreference(SQLitePreference parent, String name) {
        super(parent, name);
    }

    private SQLitePreference() {
        super(null, "");
    }

    private void init() {
        final Optional<Storage> cls = Storage.create("jdbc:sqlite:" + name() + ".sqlite");
        if (cls.isPresent()) {
            storage = cls.get();
            Runtime.getRuntime().addShutdownHook(new Thread(storage::close));
        } else {
            throw new RuntimeException("SQLITE STORAGE FAILED");
        }
        storage.ensure(sql -> {
            try (final Statement stmt = sql.createStatement()) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS " + nodeName + " ( name TEXT PRIMARY KEY , data BLOB )");
            }
        });
    }

    public static SQLitePreference get(String name) {
        return (ROOT.childSpi(name));
    }

    public void putWait(final String key, final String value) {
        storage.ensure(sql -> {
            final PreparedStatement stmt = storage.prepare("INSERT OR REPLACE INTO " + nodeName + " VALUES (? , ?)");
            stmt.setString(1, key);
            stmt.setString(2, value);
            stmt.executeUpdate();
        });
    }

    @Override
    protected void putSpi(final String key, final String value) {
        map.put(key, value);
        storage.write(sql -> {
            final PreparedStatement stmt = storage.prepare("INSERT OR REPLACE INTO " + nodeName + " VALUES (? , ?)");
            stmt.setString(1, key);
            stmt.setString(2, value);
            stmt.executeUpdate();
        });
    }

    private final Map<String, String> map = Collections.synchronizedMap(new HashMap<>());
    @Override
    protected String getSpi(final String key) {
        String get = map.get(key);
        if (get != null)
            return (get);
        final AtomicReference<String> ret = new AtomicReference<>();
        storage.ensure(sql -> {
            final PreparedStatement stmt = storage.prepare("SELECT data FROM " + nodeName + " WHERE name = ?");
            stmt.setString(1, key);
            try (final ResultSet res = stmt.executeQuery()) {
                if (res.next()) {
                    String value = res.getString(1);
                    map.put(key, value);
                    ret.set(value);
                }
            }
        });
        return (ret.get());
    }

    @Override
    protected void removeSpi(final String key) {
        String get = map.get(key);
        if (get != null)
            map.remove(key, get);
        storage.write(sql -> {
            final PreparedStatement stmt = storage.prepare("DELETE FROM " + nodeName + " WHERE name = ?");
            stmt.setString(1, key);
            stmt.executeUpdate();
        });
    }

    @Override
    protected void removeNodeSpi() throws BackingStoreException {
        storage.ensure(sql -> {
            try (final Statement stmt = sql.createStatement()) {
                stmt.executeUpdate("DROP TABLE IF EXIST '" + nodeName + "'");
            }
        });
    }

    @Override
    protected String[] keysSpi() throws BackingStoreException {
        final List<String> ret = new ArrayList<>();
        storage.ensure(sql -> {
            final PreparedStatement stmt = storage.prepare("SELECT name FROM " + nodeName);
            try (final ResultSet res = stmt.executeQuery()) {
                while (res.next()) {
                    ret.add(res.getString(1));
                }
            }
        });
        return (ret.toArray(new String[0]));
    }

    @Override
    protected String[] childrenNamesSpi() throws BackingStoreException {
        return (childNodes.keySet().toArray(new String[0]));
    }

    @Override
    protected SQLitePreference childSpi(String s) {
        return (childNodes.computeIfAbsent(s, nm -> {
            SQLitePreference pref = new SQLitePreference(this, s);
            pref.init();
            return (pref);
        }));
    }

    @Override
    protected void syncSpi() throws BackingStoreException {}

    @Override
    protected void flushSpi() throws BackingStoreException {}

    @Deprecated
    public <T> void upsertType(PreparedStatement prepared, int idx, T value) throws SQLException {
        if (value instanceof String) prepared.setString(idx, (String) value);
        else if (value instanceof Integer) prepared.setString(idx, Integer.toString((Integer) value));
        else if (value instanceof Long) prepared.setString(idx, Long.toString((Long) value));
        else if (value instanceof Float) prepared.setString(idx, Float.toString((Float) value));
        else if (value instanceof Double) prepared.setString(idx, Double.toString((Double) value));
        else if (value instanceof byte[]) prepared.setString(idx, new String((byte[]) value));
        else if (value instanceof Boolean) prepared.setString(idx, Boolean.toString((Boolean) value));
        else if (value instanceof Serializable) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(value);
                prepared.setString(idx, bos.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            throw (new RuntimeException("Cannot set unknown type " + value.getClass() + " to config!"));
        }
    }

    @Deprecated
    public <T> T selectType(ResultSet result, int idx, Class<T> cl) throws SQLException {
        String obj = result.getString(idx);
        if (obj == null) return (null);
        Object ret = null;
        if (cl.equals(String.class)) ret = obj;
        else if (cl.equals(Integer.class)) ret = Integer.parseInt(obj);
        else if (cl.equals(Long.class)) ret = Long.parseLong(obj);
        else if (cl.equals(Float.class)) ret = Float.parseFloat(obj);
        else if (cl.equals(Double.class)) ret = Double.parseDouble(obj);
        else if (cl.equals(byte[].class)) ret = obj.getBytes();
        else if (cl.equals(Boolean.class)) ret = Boolean.parseBoolean(obj);
        else if (cl.isAssignableFrom(Serializable.class)) {
            try {
                byte[] arr = obj.getBytes();
                ByteArrayInputStream bis = new ByteArrayInputStream(arr);
                ObjectInputStream ois = new ObjectInputStream(bis);
                ret = ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            throw (new RuntimeException("Cannot get unknown type " + cl + " to config!"));
        }
        return (cl.cast(ret));
    }
}
