package modification;

import haven.ResCache;
import haven.sloth.io.Storage;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class SQLiteCache implements ResCache {
    private static final Map<String, SQLiteCache> caches = Collections.synchronizedMap(new HashMap<>());
    private final Storage storage;

    public SQLiteCache(String name) {
        final Optional<Storage> cls = Storage.create("jdbc:sqlite:" + name + ".sqlite");
        if (cls.isPresent()) {
            storage = cls.get();
            Runtime.getRuntime().addShutdownHook(new Thread(storage::close));
        } else {
            throw new RuntimeException("SQLITE STORAGE FAILED");
        }
        storage.ensure(sql -> {
            try (final Statement stmt = sql.createStatement()) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS database ( name TEXT PRIMARY KEY , data BLOB )");
            }
        });
    }

    public static SQLiteCache get(String name) {
        synchronized (caches) {
            return (caches.computeIfAbsent(name, nm -> new SQLiteCache(name)));
        }
    }

    @Override
    public OutputStream store(String name) throws IOException {
        return (new ByteArrayOutputStream() {
            @Override
            public void close() {
//                final AtomicBoolean sqlExist = new AtomicBoolean();
//                storage.ensure(sql -> {
//                    final PreparedStatement stmt = storage.prepare("SELECT * FROM database WHERE name = ?");
//                    stmt.setString(1, name);
//                    try (final ResultSet res = stmt.executeQuery()) {
//                        while (res.next()) {
//                            sqlExist.set(true);
//                            return;
//                        }
//                    }
//                });
//                if (sqlExist.get()) {
//                    storage.writeAndWait(sql -> {
//                        final PreparedStatement stmt = storage.prepare("UPDATE database SET name = ?, data = ? WHERE name = ?");
//                        stmt.setString(1, name);
//                        stmt.setBytes(2, toByteArray());
//                        stmt.executeUpdate();
//                    });
//                } else {
//                    storage.writeAndWait(sql -> {
//                        final PreparedStatement stmt = storage.prepare("INSERT INTO database VALUES (? , ?)");
//                        stmt.setString(1, name);
//                        stmt.setBytes(2, toByteArray());
//                        stmt.executeUpdate();
//                    });
//                }
                storage.write(sql -> {
                    final PreparedStatement stmt = storage.prepare("INSERT OR REPLACE INTO database VALUES (? , ?)");
                    stmt.setString(1, name);
                    stmt.setBytes(2, toByteArray());
                    stmt.executeUpdate();
                });
            }
        });
    }

    @Override
    public InputStream fetch(String name) throws IOException {
        final AtomicReference<InputStream> ret = new AtomicReference<>();
        storage.ensure(sql -> {
            final PreparedStatement stmt = storage.prepare("SELECT data FROM database WHERE name = ?");
            stmt.setString(1, name);
            try (final ResultSet res = stmt.executeQuery()) {
                if (res.next()) {
                    ret.set(res.getBinaryStream(1));
                }
            }
        });
        InputStream in = ret.get();
        if (in == null) throw (new FileNotFoundException(name));
        else return (in);
    }

    @Override
    public void remove(String name) {
        storage.write(sql -> {
            final PreparedStatement stmt = storage.prepare("DELETE FROM database WHERE name = ?");
            stmt.setString(1, name);
            stmt.executeUpdate();
        });
    }
}
