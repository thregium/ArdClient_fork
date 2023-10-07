package haven;

import modification.configuration;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StatusWdg extends Widget {
    private static final Tex hearthlingsplayingdef = Text.renderstroked(String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "Players: %s"), "?")).tex();
    private static final Tex pingtimedef = Text.renderstroked(String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "Ping: %s ms"), "?")).tex();
    private Tex players = hearthlingsplayingdef;
    private Tex pingtime = pingtimedef;
    private long lastPingUpdate = System.currentTimeMillis();
    // Windows IPv4:    Reply from 213.239.201.139: bytes=32 time=71ms TTL=127
    // Windows IPv6:    Reply from 2a01:4f8:130:7393::2: time=71ms
    // GNU ping IPv4:   64 bytes from ansgar.seatribe.se (213.239.201.139): icmp_seq=1 ttl=50 time=72.5 ms
    // GNU ping IPv6:   64 bytes from ansgar.seatribe.se: icmp_seq=1 ttl=53 time=15.3 ms
    private static final Pattern pattern = Pattern.compile(Config.iswindows ? ".+?=(\\d+)[^ \\d\\s]" : ".+?time=(\\d+\\.?\\d*) ms");
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public StatusWdg() {
        executor.scheduleWithFixedDelay(this::startUpdater, 0, 5, TimeUnit.SECONDS);
    }

    private void updatepingtime() {
        String ping = "?";

        final List<String> command = new ArrayList<>();
        command.add("ping");
        command.add(Config.iswindows ? "-n" : "-c");
        command.add("1");
        command.add("game.havenandhearth.com");

        final List<String> lines = new ArrayList<>();
        try (BufferedReader standardOutput = new BufferedReader(new InputStreamReader(new ProcessBuilder(command).start().getInputStream()))) {
            lines.addAll(standardOutput.lines().collect(Collectors.toList()));
        } catch (IOException ignore) {
        }

        StringBuilder output = new StringBuilder();
        lines.forEach(output::append);

        Matcher matcher = pattern.matcher(output.toString());
        if (matcher.find()) {
            ping = matcher.group(1);
        }

        if (ping.isEmpty())
            ping = "?";

        synchronized (this) {
            pingtime = Text.renderstroked(String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "Ping: %s ms"), ping)).tex();
        }
    }

    private void startUpdater() {
        try {
            updatepingtime();
            HttpURLConnection conn = (HttpURLConnection) new URL("http://www.havenandhearth.com/portal/index/status").openConnection();
            final List<String> lines = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                lines.addAll(br.lines().collect(Collectors.toList()));
            } finally {
                conn.disconnect();
            }
            StringBuilder brt = new StringBuilder();
            lines.forEach(line -> {
                if (line.contains("There are")) {
                    String p = line.substring("<p>There are  ".length(), line.length() - " hearthlings playing.</p>".length()); //need testing
                    players = Text.renderstroked(String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "Players: %s"), p)).tex();
                }
                if (configuration.statustooltip) {
                    if (!line.contains("<div") && !line.contains("</div>")) {
                        line = line.replace("<p>", "");
                        line = line.replace("</p>", "");
                        line = line.replace("<h2>", "");
                        line = line.replace("</h2>", "");
                        while (line.startsWith(" ")) {
                            line = line.substring(" ".length());
                        }
                        brt.append(line).append("\n");
                    }
                }
            });
            if (configuration.statustooltip) tooltip = RichText.render(RichText.Parser.quote(String.format("%s", brt)), UI.scale(250));
        } catch (SocketException se) {
            // don't print socket exceptions when network is unreachable to prevent console spamming on bad connections
            if (!se.getMessage().equals("Network is unreachable"))
                se.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Exception ignored) {}
    }

    @Override
    public void draw(GOut g) {
        g.image(players, Coord.z);
        g.image(pingtime, new Coord(0, players.sz().y));

        int w = players.sz().x;
        if (pingtime.sz().x > w)
            w = pingtime.sz().x;
        this.sz = new Coord(w, players.sz().y + pingtime.sz().y);
    }

    @Override
    public void reqdestroy() {
        executor.shutdown();
        super.reqdestroy();
    }
}
