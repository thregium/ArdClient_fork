package haven.res.ui.music;

import modification.configuration;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

public class MusicBot {
    public static final int NOTE_ON = 0x90;
    public static final int NOTE_OFF = 0x80;
    public static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    public static String pathName = configuration.modificationPath + File.separator + "music" + File.separator;
    public static String justName = "Allwood_Richard_-_Claro_Pascali_Gaudio";
    public static String midExp = ".mid";
    public static String jsonExp = ".json";
    public static String musicName = pathName + justName + midExp;
    public static String jsonName = pathName + justName + jsonExp;
    public static boolean log = false;
    public static JSONArray jsonArray = new JSONArray();
    public static ArrayList<JSONArray> tracksList = new ArrayList<>();

    static void loggingln(Object string) {
        if (log) System.out.println(string);
    }

    static void loggingln() {
        if (log) System.out.println();
    }

    static void logging(Object string) {
        if (log) System.out.print(string);
    }

    public static List<String> findAllFiles(String pathName, String expansionwithdot) {
        try {
            File folder = new File(pathName);
            List<String> list = new ArrayList<>();
            String[] files = folder.list((folder1, name) -> name.endsWith(expansionwithdot));

            for (String fileName : files) {
                fileName = fileName.substring(0, fileName.length() - expansionwithdot.length());
                System.out.println("File: " + fileName + " : " + expansionwithdot);
                list.add(fileName);
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String readFile(String pathName, String name, String expansion) {
        String result = "";
        try (BufferedReader br = new BufferedReader(new FileReader(pathName + name + expansion))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                line = br.readLine();
            }
            result = sb.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void createJSONmusicfile(String pathName, String name, String expansion) {

        FileWriter jsonWriter = null;
        try {
            jsonWriter = new FileWriter(pathName + name + jsonExp);

            jsonWriter.write(createJSON(pathName, name, expansion).toString());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                jsonWriter.flush();
                jsonWriter.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static JSONArray readJSONmusicfile(String pathName, String name, String expansion) {
        try {
            JSONObject jsonFile = new JSONObject(readFile(pathName, name, expansion));
            jsonArray = new JSONArray(jsonFile.getJSONArray("tracks").toString());
            return jsonArray;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static JSONArray readJSON(String pathName, String name, String expansion) {
        try {
            JSONObject jsonFile = createJSON(pathName, name, expansion);
            jsonArray = new JSONArray(jsonFile.getJSONArray("tracks").toString());
            return jsonArray;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static JSONObject createJSON(String pathName, String name, String expansion) {
        Sequence sequence = null;
        System.out.println("createJSONmusicfile " + name);
        try {
            sequence = MidiSystem.getSequence(new File(pathName + name + expansion));
        } catch (Exception e) {
            e.printStackTrace();
        }

        int trackNumber = 0;

        JSONObject jtracks = new JSONObject();
        JSONArray jatrack = new JSONArray();

        for (Track track : sequence.getTracks()) {
            trackNumber++;
            loggingln("Track " + trackNumber + ": size = " + track.size());
            loggingln();

            JSONArray jtrack = new JSONArray();
            JSONObject jnumber = new JSONObject();
            JSONObject jsize = new JSONObject();

            jnumber.put("number", trackNumber);
            jsize.put("size", track.size());

            jtrack.put(jnumber);
            jtrack.put(jsize);

            JSONArray jnotes = new JSONArray();
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                logging("@" + event.getTick() + " ");

                JSONArray jnote = new JSONArray();

                JSONObject jmsg = new JSONObject();
                JSONObject jtick = new JSONObject();
                JSONObject jchannel = new JSONObject();
                JSONObject jkey = new JSONObject();
                JSONObject jvelocity = new JSONObject();

                MidiMessage message = event.getMessage();
                if (message instanceof ShortMessage) {
                    ShortMessage sm = (ShortMessage) message;
                    logging("Channel: " + sm.getChannel() + " ");
                    if (sm.getCommand() == NOTE_ON) {
                        int key = sm.getData1();
                        int octave = key / 12;
                        int note = key % 12;
                        String noteName = NOTE_NAMES[note];
                        int velocity = sm.getData2();
                        loggingln("Note on, " + noteName + octave + " key=" + key + " velocity: " + velocity);

                        jmsg.put("msg", sm.getCommand());
                        jtick.put("tick", event.getTick());
                        jchannel.put("channel", sm.getChannel());
                        jkey.put("key", sm.getData1());
                        jvelocity.put("velocity", sm.getData2());

                        jnote.put(jmsg);
                        jnote.put(jtick);
                        jnote.put(jchannel);
                        jnote.put(jkey);
                        jnote.put(jvelocity);

                        jnotes.put(jnote);
                    } else if (sm.getCommand() == NOTE_OFF) {
                        int key = sm.getData1();
                        int octave = key / 12;
                        int note = key % 12;
                        String noteName = NOTE_NAMES[note];
                        int velocity = sm.getData2();
                        loggingln("Note off, " + noteName + octave + " key=" + key + " velocity: " + velocity);

                        jmsg.put("msg", sm.getCommand());
                        jtick.put("tick", event.getTick());
                        jchannel.put("channel", sm.getChannel());
                        jkey.put("key", sm.getData1());
                        jvelocity.put("velocity", sm.getData2());

                        jnote.put(jmsg);
                        jnote.put(jtick);
                        jnote.put(jchannel);
                        jnote.put(jkey);
                        jnote.put(jvelocity);

                        jnotes.put(jnote);
                    } else {
                        loggingln("Command:" + sm.getCommand());

                        jmsg.put("msg", sm.getCommand());
                        jtick.put("tick", event.getTick());
                        jchannel.put("channel", sm.getChannel());

                        jnote.put(jmsg);
                        jnote.put(jtick);
                        jnote.put(jchannel);

                        jnotes.put(jnote);
                    }
                } else {
                    loggingln("Other message: " + message.getClass());

                    jmsg.put("msg", message.getClass());
                    jtick.put("tick", event.getTick());

                    jnote.put(jmsg);
                    jnote.put(jtick);

                    jnotes.put(jnote);
                }

            }
            jtrack.put(jnotes);

            jatrack.put(jtrack);

        }

        jtracks.put("tracks", jatrack);

        return jtracks;
    }

    public static ArrayList<midiTrack> getTracksFromJSON() {
        ArrayList<JSONArray> ja = new ArrayList<>();
        ArrayList<midiTrack> midiTrackArrayList = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            ja.add(jsonArray.getJSONArray(i));

            try {
                Integer number = (Integer) ja.get(i).getJSONObject(0).get("number");
                Integer size = (Integer) ja.get(i).getJSONObject(1).get("size");
                ArrayList<JSONArray> notes = new ArrayList<>();
                for (int m = 0; m < ja.get(i).getJSONArray(2).length(); m++) {
                    notes.add(ja.get(i).getJSONArray(2).getJSONArray(m));
                }
                midiTrackArrayList.add(new midiTrack(number, size, notes));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        tracksList = ja;
        return midiTrackArrayList;
    }

    public static void main(String[] args) {
        createJSONmusicfile(pathName, justName, midExp);
        readJSONmusicfile(pathName, justName, jsonExp);
        findAllFiles(pathName, midExp);
        findAllFiles(pathName, jsonExp);
    }

}

