package haven;

import java.io.Serializable;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public class DeadlockWatchdog extends HackThread {
    private boolean running = true;

    public DeadlockWatchdog(ThreadGroup tg) {
        super(tg, null, "Deadlock watchdog");
        setDaemon(true);
    }

    public DeadlockWatchdog() {
        this(null);
    }

    public static class ThreadState implements Serializable {
        public final String name;
        public final StackTraceElement[] trace;
        public final String[] locks;
        public final int[] lockdepth;

        public ThreadState(ThreadInfo mi) {
            this.name = mi.getThreadName();
            this.trace = mi.getStackTrace();
            MonitorInfo[] mons = mi.getLockedMonitors();
            LockInfo[] syncs = mi.getLockedSynchronizers();
            this.locks = new String[mons.length + syncs.length];
            this.lockdepth = new int[mons.length];
            for (int i = 0; i < mons.length; i++) {
                locks[i] = String.valueOf(mons[i]);
                lockdepth[i] = mons[i].getLockedStackDepth();
            }
            for (int i = 0; i < syncs.length; i++) {
                locks[i + mons.length] = String.valueOf(syncs[i]);
            }
        }
    }

    public static class DeadlockException extends RuntimeException {
        public final ThreadState[] threads;

        public DeadlockException(ThreadState[] threads) {
            super("Deadlock detected");
            this.threads = threads;
        }
    }

    protected void report(ThreadInfo[] threads) {
        ThreadState[] states = new ThreadState[threads.length];
        for (int i = 0; i < threads.length; i++)
            states[i] = new ThreadState(threads[i]);
        throw (new DeadlockException(states));
    }

    @Override
    public void run() {
        ThreadMXBean tm = ManagementFactory.getThreadMXBean();
        while (running) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                continue;
            }
            long[] locked = tm.findDeadlockedThreads();
            if (locked != null) {
                ThreadInfo[] threads = tm.getThreadInfo(locked, true, true);
                report(threads);
            }
        }
    }

    public void quit() {
        running = false;
        interrupt();
    }
}
