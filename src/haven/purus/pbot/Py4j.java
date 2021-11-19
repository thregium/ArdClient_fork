package haven.purus.pbot;

import py4j.GatewayServer;
import py4j.GatewayServerListener;
import py4j.Py4JServerConnection;

import java.io.File;
import java.util.Locale;

public class Py4j {
    public static GatewayServer server;

    public static class GSL implements GatewayServerListener {
        @Override
        public void connectionError(Exception e) {
            e.printStackTrace();
        }

        @Override
        public void connectionStarted(Py4JServerConnection py4JServerConnection) {
//            System.out.println(py4JServerConnection);
        }

        @Override
        public void connectionStopped(Py4JServerConnection py4JServerConnection) {
//            System.out.println(py4JServerConnection);
        }

        @Override
        public void serverError(Exception e) {
            e.printStackTrace();
        }

        @Override
        public void serverPostShutdown() {
            System.out.println("Py4j serverPostShutdown");
        }

        @Override
        public void serverPreShutdown() {
            System.out.println("Py4j serverPreShutdown");
        }

        @Override
        public void serverStarted() {
            System.out.println("Py4j serverStarted");
        }

        @Override
        public void serverStopped() {
            System.out.println("Py4j serverStopped");
        }
    }

    public interface PBotScriptLoader {
        void start(String name, String methodname, String id);

        void stop(String id);
    }

    public static void start() {
        try {
            ProcessBuilder pb;
            if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
                new File("./bin/python/python.exe").setExecutable(true);
                pb = new ProcessBuilder(".\\bin\\python\\python.exe", ".\\scripts\\py\\loader.py");
            } else {
                pb = new ProcessBuilder("python", "scripts/py/loader.py");
            }
            pb.redirectErrorStream(true);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            pb.inheritIO();
            Process p = pb.start();
            Runtime.getRuntime().addShutdownHook(new Thread(p::destroyForcibly));
        } catch (Exception e) {
            e.printStackTrace();
        }
        new Thread(() -> {
            server = new GatewayServer();
            server.addListener(new GSL());
            server.start();
        }, "PBot Runner").start();
    }
}
