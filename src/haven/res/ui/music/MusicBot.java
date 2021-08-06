package haven.res.ui.music;

import haven.Button;
import haven.Coord;
import haven.Dropbox;
import haven.GOut;
import haven.GameUI;
import haven.HSlider;
import haven.IButton;
import haven.Label;
import haven.Tex;
import haven.Text;
import haven.TextEntry;
import haven.UI;
import haven.Utils;
import haven.Widget;
import haven.WidgetList;
import haven.purus.pbot.PBotAPI;
import modification.configuration;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;

public class MusicBot extends Widget {
    public static final String PAUSE_TEXT = "⏸";
    public static final String PLAY_TEXT = "⏵";
    public static final String STOP_TEXT = "⏹";
    public static final BufferedImage REFRESH_IMG = configuration.scaleImage(Utils.outline(Text.render("🔄").img, Color.RED), 5);
    public static final Path defPath = Paths.get(configuration.modificationPath + File.separator + "music" + File.separator);
    public static final String midExp = ".mid";

    public final MusicWnd musicWnd;
    public final IButton loadingIndicator;
    public final FileList fileList;
    public final TextEntry search;
    public Player player;
    public SoundSettings settings;
    public final Sound sound; //добавить текущую музыку


    public MusicBot(MusicWnd musicWnd) {
        this.musicWnd = musicWnd;
        add(loadingIndicator = new IButton(REFRESH_IMG, REFRESH_IMG, REFRESH_IMG));
        loadingIndicator.z = 1;
        loadingIndicator.hide();
        add(fileList = new FileList(reloadList(defPath), 150, 16));
        add(search = new TextEntry(150, "", null, t -> execute(() -> {
            musicWnd.setfocus(fileList);
            fileList.reload(reloadList(defPath, t));
        })) {
            @Override
            public boolean mousedown(Coord mc, int btn) {
                if (btn == 3) {
                    settext("");
                    execute(() -> fileList.reload(reloadList(defPath)));
                    return (true);
                } else {
                    return (super.mousedown(mc, btn));
                }
            }
        }, Coord.of(0, fileList.c.y + fileList.sz.y + 5));
        sound = new Sound();
        pack();
    }


    @Override
    public void tick(double dt) {
        super.tick(dt);
        if (loadingIndicator != null) {
            if (loadingIndicator.visible() && !loadingIndicator.c.equals(sz.div(2).sub(loadingIndicator.sz.div(2))))
                loadingIndicator.move(sz.div(2), 0.5, 0.5);
        }
    }

    public void repack() {
        pack();
        musicWnd.resize(true);
    }


    public class FileWidget extends Widget implements Comparable<FileWidget> {
        public final Path path;
        public final boolean isParent;
        public final boolean isFolder;
        public String name;
        public Tex tex;

        public FileWidget(Path path, boolean parent) {
            this.path = path;
            this.isParent = parent;
            this.isFolder = Files.isDirectory(path);

            if (Files.exists(path))
                if (parent)
                    setName("../");
                else
                    setName(path.getFileName().toString() + (isFolder() ? "/" : ""));
            else
                setName("...");

            resize(tex.sz());
        }

        public FileWidget(Path path) {
            this(path, false);
        }

        public boolean isFolder() {
            return (isFolder);
        }

        public void setName(String name) {
            this.name = name;
            Color clr = Color.WHITE;
            if (isParent)
                clr = Color.ORANGE;
            else if (isFolder)
                clr = Color.YELLOW;
            this.tex = Text.render(name, clr).tex();
        }

        public void action() {
            execute(() -> {
                if (isFolder()) {
                    fileList.reload(reloadList(path));
                } else {
                    try {
                        sound.stop();
                        sound.set(path);
                        if (player == null)
                            player = MusicBot.this.add(new Player(), Coord.of(fileList.c.x + fileList.sz.x + 5, 0));
                        player.setCurrenttime(0);
                        player.setFulltime(sound.getLength());
                        if (settings != null)
                            settings.reqdestroy();
                        settings = MusicBot.this.add(new SoundSettings(), Coord.of(fileList.c.x + fileList.sz.x + 5, player.c.y + player.sz.y + 5));
                        repack();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void draw(GOut g) {
            super.draw(g);
            g.aimage(tex, Coord.of(0, sz.y / 2), 0, 0.5);
        }

        @Override
        public int compareTo(FileWidget o) {
            int c;
            c = Boolean.compare(o.isParent, this.isParent);
            if (c == 0)
                c = Boolean.compare(o.isFolder(), this.isFolder());
            if (c == 0)
                c = this.name.toLowerCase().compareTo(o.name.toLowerCase());
            return (c);
        }
    }

    public class FileList extends WidgetList<FileWidget> {
        public final List<FileWidget> list = new ArrayList<>();

        public FileList(List<FileWidget> initList, int w, int items) {
            super(Coord.of(w, maxFileSize(initList).y), items);
            list.addAll(initList);
            pack();
        }

        @Override
        protected FileWidget listitem(int i) {
            return (list.get(i));
        }

        @Override
        protected int listitems() {
            return (list.size());
        }

        @Override
        protected void drawitem(GOut g, FileWidget item, int i) {
            item.draw(g);
        }

        @Override
        protected void itemclick(FileWidget item, int button) {
            super.itemclick(item, button);
            item.action();
        }

        public void reload(List<FileWidget> list) {
            this.list.clear();
            this.list.addAll(list);
            sb.ch(sb.min - sb.val);
        }

        @Override
        public Object tooltip(Coord c, Widget prev) {
            final FileWidget item = itemat(c);
            if (item != null) {
                return (itemtooltip(new Coord(c.x, c.y % itemh), item));
            } else {
                return (super.tooltip(c, prev));
            }
        }

        @Override
        protected Object itemtooltip(Coord c, FileWidget item) {
            return (item.tex);
        }
    }

    public class Sound {
        public Sequence sequence;
        public Channel[] trackList = new Channel[16];

        public Sequencer sequencer;
        public KeyReceiver receiver;

        public Play playing;
        public long savedposition = 0;
        public boolean isPlaying = false;
        public float temp = 1.0f;
        public final Queue<Runnable> tasks = new LinkedList<>();


        public void set(Path path) throws Exception {
            sequence = MidiSystem.getSequence(path.toFile());
            Synthesizer synth = MidiSystem.getSynthesizer();
            Instrument[] orchestra = synth.getAvailableInstruments();
            Arrays.fill(trackList, null);
            Arrays.asList(sequence.getTracks()).forEach(t -> {
                for (int i = 0; i < t.size(); i++) {
                    if (t.get(i).getMessage() instanceof ShortMessage) {
                        ShortMessage smsg = (ShortMessage) t.get(i).getMessage();
                        int channel = smsg.getChannel();
                        if (smsg.getCommand() == ShortMessage.NOTE_ON) {
                            of(trackList, channel).addNote(smsg.getData1());
                        }
                        if (smsg.getCommand() == ShortMessage.PROGRAM_CHANGE) {
                            Instrument inst = findFromProgram(orchestra, smsg.getData1());
                            if (inst != null)
                                of(trackList, channel).addInstrument(inst.getName().trim());
                        }
                    }
                }
            });
            Arrays.asList(trackList).forEach(c -> {
                if (c != null) {
                    c.calcOctave();
                    c.setUser(ui.sess.username);
                    c.updateLabel();
                }
            });
        }

        public Instrument findFromProgram(Instrument[] orchestra, int program) {
            for (Instrument instrument : orchestra)
                if (instrument.getPatch().getProgram() == program)
                    return (instrument);
            return (null);
        }


        public class Play extends Thread implements Runnable {
            @Override
            public void run() {
                KeyReceiver receiver = Sound.this.receiver;
                Sequencer sequencer = Sound.this.sequencer;
                try {
                    sequencer.start();
                    sequencer.setTempoFactor(temp);
                    sequencer.setTickPosition(savedposition);
                    boolean stoptrigger = false;
                    while (isPlaying) {
                        if (!stoptrigger && getTickPosition() == getLength()) {
                            stoptrigger = true;
                            MusicBot.this.stop();
                        }
                        synchronized (tasks) {
                            while (!tasks.isEmpty())
                                tasks.poll().run();
                        }
                        player.setCurrenttime(getTickPosition());
                        Thread.sleep(50);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    deleteSequencer(receiver, sequencer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public class KeyReceiver implements Receiver {
            public boolean isClosed = false;

            @Override
            public void send(MidiMessage msg, long timeStamp) {
                if (!isClosed) {
                    if (msg instanceof ShortMessage) {
                        ShortMessage smsg = (ShortMessage) msg;
                        int channel = smsg.getChannel();
                        int command = smsg.getCommand();
                        int data1 = smsg.getData1();
                        int data2 = smsg.getData2();

                        Function<ShortMessage, Integer> getKey = m -> {//TODO fix for better diapasone (diff tools or troika octave)
                            int avg = trackList[channel].avgoct;
                            int octave = data1 / 12 - 1;
                            int note = data1 % 12;
                            if (octave < avg - 1)
                                octave = avg - 1;
                            if (octave > avg + 1)
                                octave = avg + 1;
                            return (note + (octave - (avg - 1)) * 12);
                        };

                        if (command == ShortMessage.NOTE_OFF || data2 < 0) {
                            sound.key_off(channel, getKey.apply(smsg));
                        } else if (command == ShortMessage.NOTE_ON) {
                            sound.key_on(channel, getKey.apply(smsg));
                        }
                    }
                    if (!isActive(musicWnd))
                        sound.stop();
                }
            }

            @Override
            public void close() {
                player.setCurrenttime(savedposition);
                final List<String> names = new ArrayList<>();
                Arrays.asList(trackList).forEach(t -> {
                    if (t != null) {
                        MusicWnd wnd = t.getMusicWnd();
                        if (wnd != null && !names.contains(t.user.sel)) {
                            names.add(t.user.sel);
                            stopallnote(wnd);
                        }
                    }
                });
            }
        }


        public void createSequencer() throws Exception {
            sequencer = MidiSystem.getSequencer(false);
            receiver = new KeyReceiver();
            sequencer.getTransmitter().setReceiver(receiver);
            sequencer.open();
            sequencer.setSequence(sequence);
        }

        public void deleteSequencer(KeyReceiver receiver, Sequencer sequencer) {
            receiver.isClosed = true;
            receiver.close();
            sequencer.close();
        }

        public long getLength() {
            return (sequence.getTickLength());
        }

        public long getTickPosition() {
            return (sequencer.getTickPosition());
        }

        public void setTickPosition(long tick) {
            if (isPlaying) {
                synchronized (tasks) {
                    tasks.add(() -> {
                        sequencer.setTickPosition(tick);
                    });
                }
            } else {
                savedposition = tick;
                player.setCurrenttime(tick);
            }
        }

        public void setTemp(float temp) {
            this.temp = temp;
            if (isPlaying) {
                synchronized (tasks) {
                    tasks.add(() -> sequencer.setTempoFactor(temp));
                }
            }
        }

        public void play() {
            if (!isPlaying) {
                try {
                    createSequencer();
                    isPlaying = true;
                    playing = new Play();
                    playing.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void pause() {
            if (isPlaying) {
                synchronized (tasks) {
                    tasks.add(() -> {
                        isPlaying = false;
                        savedposition = getTickPosition();
                    });
                }
            }
        }

        public void stop() {
            if (isPlaying) {
                synchronized (tasks) {
                    tasks.add(() -> {
                        isPlaying = false;
                        savedposition = 0;
                    });
                }
            }
        }

        public Channel getChannel(int id) {
            return (trackList[id]);
        }

        public void key_on(int id, int key) {
            Channel channel = getChannel(id);
            if (channel != null && !channel.user.sel.equals(""))
                channel.key_on(key);
        }

        public void key_off(int id, int key) {
            Channel channel = getChannel(id);
            if (channel != null && !channel.user.sel.equals(""))
                channel.key_off(key);
        }
    }

    public Channel of(Channel[] list, int channel) {
        if (list[channel] == null)
            list[channel] = new Channel(channel);
        return (list[channel]);
    }

    public class Channel extends Widget {
        public final List<Integer> notes = new ArrayList<>();
        public final List<Integer> octavcounter = new ArrayList<>();
        public int avgoct = 0;

        public void calcOctave() {
            int[] octaves = new int[11];
            int max = -1;
            int keys = 0;
            notes.forEach(n -> octaves[n / 12 - 1]++);
            for (int i = 0; i < octaves.length; i++) {
                octavcounter.add(octaves[i]);
                if (octaves[i] > keys) {
                    keys = octaves[i];
                    max = i;
                }
            }
            if (max == -1)
                max = 0;
            avgoct = max;
        }

        public final List<String> availableInstruments = new ArrayList<>();

        public final int id;

        public void setUser(String user) {
            this.user.change(user);
        }

        public void updateLabel() {
            label.settext(info());
            pack();
        }

        public final Dropbox<String> user;
        public final Label label;

        public Channel(int id) {
            this.id = id;
            user = createDropbox();
            add(user);
            adda(label = new Label(""), Coord.of(user.c.x + user.sz.x + 2, user.sz.y / 2), 0, 0.5);
            pack();
        }

        public void addNote(int key) {
            notes.add(key);
        }

        public void addInstrument(String name) {
            if (!availableInstruments.contains(name))
                availableInstruments.add(name);
        }

        public MusicWnd musicWnd;

        public MusicWnd getMusicWnd() {
            if (musicWnd != null) {
                UI ui = musicWnd.ui;
                if (ui != null && ui.sess != null) {
                    if (user.sel.equals(ui.sess.username)) {
                        return (musicWnd);
                    }
                }
                musicWnd = null;
            }
            UI ui = PBotAPI.getUIByName(user.sel);
            if (ui != null) {
                GameUI gui = ui.gui;
                if (gui != null) {
                    MusicWnd wnd = gui.getchild(MusicWnd.class);
                    if (wnd != null) {
                        return (musicWnd = wnd);
                    }
                }
            }
            return (musicWnd);
        }

        public void key_on(int key) {
            MusicWnd wnd = getMusicWnd();
            if (wnd != null)
                MusicBot.keydown(wnd, key);
        }

        public void key_off(int key) {
            MusicWnd wnd = getMusicWnd();
            if (wnd != null)
                MusicBot.keyup(wnd, key);
        }

        public Dropbox<String> createDropbox() {
            final List<String> namelist = new ArrayList<>();
            Runnable search = () -> {
                namelist.clear();
                namelist.add("");
                PBotAPI.uis().stream().filter(ui -> {
                    if (ui.sess != null && ui.gui != null) {
                        MusicWnd wnd = ui.gui.getchild(MusicWnd.class);
                        return (wnd != null);
                    }
                    return (false);
                }).forEach(ui -> namelist.add(ui.sess.username));
            };
            search.run();

            return (new Dropbox<String>(namelist.size(), namelist) {
                @Override
                protected String listitem(int i) {
                    return (namelist.get(i));
                }

                @Override
                protected int listitems() {
                    return (namelist.size());
                }

                @Override
                protected void drawitem(GOut g, String item, int i) {
                    g.text(item, Coord.z);
                }

                @Override
                public void change(String item) {
                    MusicWnd wnd = getMusicWnd();
                    if (wnd != null)
                        stopallnote(wnd);
                    super.change(item);
                }

                @Override
                public boolean mousedown(Coord c, int btn) {
                    update();
                    return (super.mousedown(c, btn));
                }

                @Override
                public boolean mousewheel(Coord c, int amount) {
                    update();
                    int index = selindex + amount;
                    if (index >= 0 && index < listitems()) {
                        change(listitem(index));
                    }
                    return (true);
                }

                public void update() {
                    search.run();
                    resizedl(namelist);
                }
            });
        }

        public String info() {
            StringBuilder sb = new StringBuilder();
            sb.append("Channel:").append(id).append(" ");
            sb.append("Notes:").append(notes.size()).append(" ");
            availableInstruments.forEach(i -> sb.append("[").append(i).append("]"));
            return (sb.toString());
        }
    }

    public class Player extends Widget {
        public final Label currenttime;
        public final Label fulltime;
        public final HSlider progress;
        public final HSlider temp;
        public final Button play;
        public final Button pause;
        public final Button stop;

        public Player() {
            play = add(new Button(PLAY_TEXT, MusicBot.this::play));
            stop = add(new Button(STOP_TEXT, MusicBot.this::stop));
            pause = add(new Button(PAUSE_TEXT, MusicBot.this::pause));

            temp = add(new HSlider(100, 0, 300, (int) (sound.temp * 100f)) {
                public boolean mousedown(Coord c, int button) {
                    if (button == 3) {
                        val = 100;
                        setTemp(val / 100f);
                        return (true);
                    } else
                        return (super.mousedown(c, button));
                }

                @Override
                public void changed() {
                    setTemp(val / 100f);
                }

                @Override
                public Object tooltip(Coord c, Widget prev) {
                    return (Text.render("Temp: " + val / 100f).tex());
                }
            });

            currenttime = add(new Label("0"));
            progress = add(new HSlider(200, 0, 1, 0) {
                @Override
                public void changed() {
                    sound.setTickPosition(val);
                }

                @Override
                public Object tooltip(Coord c, Widget prev) {
                    return (Text.render(String.format("Current tick: %d of %d - (%.1f%%)", val, max, (val * 100f / max))).tex());
                }
            });
            fulltime = add(new Label("0"));

            pack();
            defpos();
            pack();
        }

        public void defpos() {
            Coord pc = Coord.of(sz.x / 2, 0);

            play.move(pc, 0.5, 0);
            stop.move(Coord.of(play.c.x - stop.sz.x, 0));
            pause.move(Coord.of(play.c.x + play.sz.x, 0));

            temp.move(Coord.of(pc.x, play.c.y + play.sz.y + 5), 0.5, 0);
            progress.move(Coord.of(pc.x, temp.c.y + temp.sz.y + 5), 0.5, 0);

            currenttime.move(Coord.of(0, progress.c.y + progress.sz.y + 2));
            fulltime.move(Coord.of(sz.x, progress.c.y + progress.sz.y + 2), 1, 0);
        }

        public void setTemp(float temp) {
            if (sound != null)
                sound.setTemp(temp);
        }

        public void setCurrenttime(long time) {
            progress.val = (int) time;
            currenttime.settext(time + "");
        }

        public void setFulltime(long time) {
            progress.min = 0;
            progress.val = 0;
            progress.max = (int) time;
            fulltime.settext(time + "");
            fulltime.move(Coord.of(sz.x, progress.c.y + progress.sz.y + 2), 1, 0);
        }
    }

    public class SoundSettings extends Widget {
        public SoundSettings() {
            int i = 0;
            for (Channel ch : sound.trackList) {
                if (ch != null && !ch.notes.isEmpty()) {
                    int height = ch.sz.y;
                    add(ch, Coord.of(0, (height + 2) * i));
                    i++;
                }
            }
            pack();
        }
    }


    @Override
    public void reqdestroy() {
        super.reqdestroy();
        stop();
    }

    public void execute(Runnable run) {
        loadingIndicator.show();
        run.run();
        loadingIndicator.hide();
    }


    public void play() {
        try {
            sound.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void pause() {
        try {
            sound.pause();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            sound.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public List<FileWidget> reloadList(Path path) {
        final List<FileWidget> list = new ArrayList<>();
        try {
            if (!path.equals(defPath))
                list.add(new FileWidget(path.getParent(), true));
            Files.list(path).filter(p -> Files.isDirectory(p) || p.toString().toLowerCase().endsWith(midExp)).forEach(p -> list.add(new FileWidget(p)));
            list.sort(FileWidget::compareTo);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return (list);
    }

    public List<FileWidget> reloadList(Path path, String text) {
        if (text.equals(""))
            return (reloadList(path));
        final List<FileWidget> list = new ArrayList<>();
        try {
            if (!path.equals(defPath))
                list.add(new FileWidget(path.getParent(), true));
            Files.find(path, 100, (p, bfa) -> {
                String name = p.toString().toLowerCase();
                return (!Files.isDirectory(p) && name.endsWith(midExp) && name.contains(text.toLowerCase().trim()));
            }).forEach(p -> list.add(new FileWidget(p)));
            list.sort(FileWidget::compareTo);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return (list);
    }

    public Coord maxFileSize(final List<FileWidget> list) {
        Coord size = new Coord();
        for (FileWidget fw : list) {
            size.x = Math.max(size.x, fw.sz.x);
            size.y = Math.max(size.y, fw.sz.y);
        }
        return (size);
    }


    public static void keydown(MusicWnd musicWnd, int key) {
        if (musicWnd == null)
            return;
        if (!musicWnd.cur[key]) {
            if (musicWnd.actn >= musicWnd.act.length) {
                musicWnd.wdgmsg("stop", musicWnd.act[0], (float) curTime(musicWnd));
                if (musicWnd.actn - 1 >= 0) System.arraycopy(musicWnd.act, 1, musicWnd.act, 0, musicWnd.actn - 1);
                musicWnd.actn--;
            }
            musicWnd.wdgmsg("play", key, (float) curTime(musicWnd));
            musicWnd.cur[key] = true;
            musicWnd.act[musicWnd.actn++] = key;
        }
    }

    public static void keyup(MusicWnd musicWnd, int key) {
        if (musicWnd == null)
            return;
        stopnote(musicWnd, key);
    }

    public static void stopnote(MusicWnd musicWnd, int key) {
        if (musicWnd == null)
            return;
        if (musicWnd.cur[key]) {
            for (int i = 0; i < musicWnd.actn; i++) {
                if (musicWnd.act[i] == key) {
                    musicWnd.wdgmsg("stop", key, (float) curTime(musicWnd));
                    for (musicWnd.actn--; i < musicWnd.actn; i++)
                        musicWnd.act[i] = musicWnd.act[i + 1];
                    break;
                }
            }
            musicWnd.cur[key] = false;
        }
    }

    public static void stopallnote(MusicWnd musicWnd) {
        if (musicWnd == null)
            return;
        for (int i = 0; i < musicWnd.actn; i++) {
            stopnote(musicWnd, musicWnd.act[i]);
        }
    }

    public static double curTime(MusicWnd musicWnd) {
        return (Utils.ntime() + musicWnd.latcomp - musicWnd.start);
    }


    public static boolean isActive(MusicWnd musicWnd) {
        UI ui = musicWnd.ui;
        if (ui != null && ui.sess != null) {
            GameUI gui = ui.gui;
            if (gui != null) {
                return (musicWnd.equals(gui.getchild(MusicWnd.class)));
            }
        }
        return (false);
    }
}

