package haven.sloth.io;

import com.google.common.flogger.FluentLogger;
import org.sqlite.Function;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConnection;
import org.sqlite.SQLiteDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Just a helper class to access our sqlite storage for dynamic settings that are
 * user defined. Unlike static.sqlite this should never be touched in updates
 * <p>
 * Only for writing back, for reading since it's only at startup can be done on their
 * own connections
 */
public class Storage {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private final ExecutorService writerHandler = Executors.newWorkStealingPool();
    public static final Storage dynamic, overlays;

    static {
        final Optional<Storage> cls = create("jdbc:sqlite:dynamic.sqlite");
        if (cls.isPresent()) {
            dynamic = cls.get();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    dynamic.close();
                }
            });
        } else {
            dynamic = null;
        }
        final Optional<Storage> clos = create("jdbc:sqlite:overlays.sqlite");
        if (clos.isPresent()) {
            overlays = clos.get();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    overlays.close();
                }
            });
        } else {
            overlays = null;
        }
    }

    /**
     * This will exit if it fails since its assumed these are important to load in at the start of the client
     */
    public static Optional<Storage> create(final String jdbc) {
        try {
            return Optional.of(new Storage(jdbc));
        } catch (SQLException se) {
            logger.atSevere().withCause(se).log("failed to create dynamic storage %s", jdbc);
            System.exit(0);
            return Optional.empty();
        }
    }

    private final Connection conn;
    private final Map<String, PreparedStatement> stmts = Collections.synchronizedMap(new HashMap<>());

    private Storage(final String jdbc) throws SQLException {
        this.conn = mkcon(jdbc);
    }

    public PreparedStatement prepare(final String sql) throws SQLException {
        if (stmts.containsKey(sql))
            return stmts.get(sql);
        else {
            final PreparedStatement stmt;
            synchronized (conn) {
                stmts.put(sql, (stmt = conn.prepareStatement(sql)));
            }
            return stmt;
        }
    }

    public PreparedStatement ensurePrepare(final String sql) {
        try {
            return prepare(sql);
        } catch (SQLException se) {
            logger.atSevere().withCause(se).log("Failed to prepare statement needed");
            System.exit(0);
        }
        return null;
    }

    public void close() {
        try {
            stmts.forEach((k, v) -> {
                try {
                    v.close();
                } catch (SQLException se) {
                    logger.atSevere().withCause(se).log("Failed to close %s", v);
                }
            });
            conn.close();
        } catch (SQLException se) {
            logger.atSevere().withCause(se).log("Failed to close %s", conn);
        }
    }

    private Connection mkcon(final String jdbc) throws SQLException {
        final SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl(jdbc);
        ds.setPageSize(4096);
        ds.setCacheSize(2000);
        ds.setEnforceForeignKeys(true);
        ds.setJournalMode(SQLiteConfig.JournalMode.WAL.name());
        final SQLiteConnection scon = ds.getConnection(null, null);
        Function.create(scon, "REGEXP", new Function() {
            @Override
            protected void xFunc() throws SQLException {
                String expression = value_text(0);
                String value = value_text(1);
                if (value == null)
                    value = "";

                Pattern pattern = Pattern.compile(expression);
                result(pattern.matcher(value).find() ? 1 : 0);
            }
        });
        scon.setAutoCommit(false);
        scon.setBusyTimeout(15000);
        return scon;
    }

    @FunctionalInterface
    public interface SQLCallback {
        void run(final Connection sql) throws SQLException;
    }

    public void ensure(final SQLCallback callback) {
        try {
            synchronized (conn) {
                callback.run(conn);
                conn.commit();
            }
        } catch (SQLException se) {
            try {
                synchronized (conn) {
                    conn.rollback();
                }
            } catch (SQLException se2) {
                //Eat it.
            }
            se.printStackTrace();
            logger.atSevere().withCause(se).log("Failed to commit transaction");
            System.exit(0);
        }
    }

    /**
     * These are done async
     */
    public void write(final SQLCallback callback) {
        final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        writerHandler.submit(() -> {
            try {
                synchronized (conn) {
                    callback.run(conn);
                    conn.commit();
                }
            } catch (SQLException se) {
                try {
                    synchronized (conn) {
                        conn.rollback();
                    }
                } catch (SQLException se2) {
                    //Eat it.
                }
                for (final StackTraceElement ele : stack) {
                    logger.atSevere().log(ele.toString());
                }
                logger.atSevere().withCause(se).log("Failed to commit transaction");
            }
        });
    }

    /**
     * These are not done async
     */
    public void writeAndWait(final SQLCallback callback) {
        final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        try {
            synchronized (conn) {
                callback.run(conn);
                conn.commit();
            }
        } catch (SQLException se) {
            try {
                synchronized (conn) {
                    conn.rollback();
                }
            } catch (SQLException se2) {
                //Eat it.
            }
            for (final StackTraceElement ele : stack) {
                logger.atSevere().log(ele.toString());
            }
            logger.atSevere().withCause(se).log("Failed to commit transaction");
        }
    }
}
