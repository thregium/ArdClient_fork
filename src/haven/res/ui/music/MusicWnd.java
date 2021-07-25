/* Preprocessed source code */
package haven.res.ui.music;

import haven.Button;
import haven.CheckBox;
import haven.CheckListbox;
import haven.CheckListboxItem;
import haven.Coord;
import haven.GOut;
import haven.HSlider;
import haven.Listbox;
import haven.Resource;
import haven.Tex;
import haven.Text;
import haven.UI;
import haven.Widget;
import haven.Window;
import haven.purus.pbot.PBotUtils;
import org.json.JSONArray;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* >wdg: MusicWnd */
public class MusicWnd extends Window {
    public static final Tex[] tips;
    public static final Map<Integer, Integer> keys;
    public static final int[] nti = {0, 2, 4, 5, 7, 9, 11}, shi = {1, 3, 6, 8, 10};
    public static final int[] ntp = {0, 1, 2, 3, 4, 5, 6}, shp = {0, 1, 3, 4, 5};
    public static final Tex[] ikeys;
    public final boolean[] cur = new boolean[12 * 3];
    public final int[] act;
    public final double start;
    public double latcomp = 0.15;
    public int actn;

    public Runnable runnable;
    public Thread thread;
    public Button refresh, parsing, play, stop, pause;
    public HSlider multi;
    public List<String> midList = new ArrayList<>();
    public List<midiTrack> trackList = new ArrayList<>();
    //	public ArrayList<String> channelList = new ArrayList<>();
    public Listbox<String> midLst, trackLst, channelLst;
    public CheckListbox trackCheckList;
    public CheckBox allTracks;
    public midiTrack allT, currentT;
    public static JSONArray jsonArray = new JSONArray();
    public static int currentTrack = 0, currentChannel = 0;
    public List<Integer> tracksList = new ArrayList<>();
    public double multiplier = 1;
    public int[] diapasone = new int[2];
    public int[] octaves = new int[10];
    public boolean paused;

    public CheckBox disableKeys;

    static {
        Map<Integer, Integer> km = new HashMap<Integer, Integer>();
        km.put(KeyEvent.VK_Z, 0);
        km.put(KeyEvent.VK_S, 1);
        km.put(KeyEvent.VK_X, 2);
        km.put(KeyEvent.VK_D, 3);
        km.put(KeyEvent.VK_C, 4);
        km.put(KeyEvent.VK_V, 5);
        km.put(KeyEvent.VK_G, 6);
        km.put(KeyEvent.VK_B, 7);
        km.put(KeyEvent.VK_H, 8);
        km.put(KeyEvent.VK_N, 9);
        km.put(KeyEvent.VK_J, 10);
        km.put(KeyEvent.VK_M, 11);
        Tex[] il = new Tex[4];
        for (int i = 0; i < 4; i++) {
            il[i] = Resource.loadtex("ui/music", i);
        }
        String tc = "ZSXDCVGBHNJM";
        Text.Foundry fnd = new Text.Foundry(Text.fraktur.deriveFont(java.awt.Font.BOLD, 16)).aa(true);
        Tex[] tl = new Tex[tc.length()];
        for (int i = 0; i < nti.length; i++) {
            int ki = nti[i];
            tl[ki] = fnd.render(tc.substring(ki, ki + 1), new Color(0, 0, 0)).tex();
        }
        for (int i = 0; i < shi.length; i++) {
            int ki = shi[i];
            tl[ki] = fnd.render(tc.substring(ki, ki + 1), new Color(255, 255, 255)).tex();
        }
        keys = km;
        ikeys = il;
        tips = tl;
    }

    public MusicWnd(String name, int maxpoly) {
        super(ikeys[0].sz().mul(nti.length, 1), name, true);
        this.act = new int[maxpoly];
        this.start = System.currentTimeMillis() / 1000.0;

        runnable = () -> {
            System.out.println("run");
            playMusic();
        };
        thread = new Thread(runnable, "music");

        refresh = new Button(60, "Refresh", this::initBot);
        add(refresh, new Coord(90, -15));
        add(disableKeys = new CheckBox("Disable Keys"), new Coord(-25, -15));
    }

    public void playMusic() {
        try {
            if (allTracks.a) {
                for (int i = 1; i < allT.normalNotes.size(); i++) {
                    if (!ui.rwidgets.containsKey(this)) {
                        break;
                    }
                    while (paused)
                        Thread.sleep(10);
                    int octave = (allT.normalNotes.get(i - 1).getKey() / 12);
                    int note = allT.normalNotes.get(i - 1).getKey() % 12;

                    int mod;
                    if (octave >= diapasone[1]) mod = 2;
                    else if (octave == diapasone[1] - 1) mod = 1;
                    else if (octave <= diapasone[0]) mod = 0;
                    else mod = -1;

                    if (mod != -1)
                        if (allT.normalNotes.get(i - 1).getMsg() == MusicBot.NOTE_ON) {
                            keydown(note, mod);
//					    	Thread.sleep(allT.normalNotes.get(i - 1).getVelocity());
                        } else if (allT.normalNotes.get(i - 1).getMsg() == MusicBot.NOTE_OFF)
                            keyup(note, mod);

//			    	System.out.println((trackList.get(currentTrack).normalNotes.get(i - 1).getKey()) + " key " + note + " note " + MusicBot.NOTE_NAMES[note] + " note_name " + octave + " octave");
                    long ti = allT.normalNotes.get(i).getTick() - allT.normalNotes.get(i - 1).getTick();
                    Thread.sleep((long) (ti / multiplier));
//					Thread.sleep(allT.normalNotes.get(i - 1).getVelocity());
                }

                int octave = (allT.normalNotes.get(allT.normalNotes.size() - 1).getKey() / 12);
                int note = allT.normalNotes.get(allT.normalNotes.size() - 1).getKey() % 12;

                int mod;
                if (octave >= diapasone[1]) mod = 2;
                else if (octave == diapasone[1] - 1) mod = 1;
                else if (octave <= diapasone[0]) mod = 0;
                else mod = -1;

                if (mod != -1)
                    if (allT.normalNotes.get(allT.normalNotes.size() - 1).getMsg() == MusicBot.NOTE_ON)
                        keydown(note, mod);
                    else if (allT.normalNotes.get(allT.normalNotes.size() - 1).getMsg() == MusicBot.NOTE_OFF)
                        keyup(note, mod);

            } else {
                for (int i = 1; i < currentT.normalNotes.size(); i++) {
                    if (!ui.rwidgets.containsKey(this)) {
                        break;
                    }
                    while (paused)
                        Thread.sleep(10);
                    int octave = (currentT.normalNotes.get(i - 1).getKey() / 12);
                    int note = currentT.normalNotes.get(i - 1).getKey() % 12;

                    int mod;
                    if (octave >= diapasone[1]) mod = 2;
                    else if (octave == diapasone[1] - 1) mod = 1;
                    else if (octave <= diapasone[0]) mod = 0;
                    else mod = -1;

                    if (mod != -1)
                        if (currentT.normalNotes.get(i - 1).getMsg() == MusicBot.NOTE_ON) {
                            keydown(note, mod);
//						    Thread.sleep(allT.normalNotes.get(i - 1).getVelocity());
                        } else if (currentT.normalNotes.get(i - 1).getMsg() == MusicBot.NOTE_OFF)
                            keyup(note, mod);

//			    	System.out.println((trackList.get(currentTrack).normalNotes.get(i - 1).getKey()) + " key " + note + " note " + MusicBot.NOTE_NAMES[note] + " note_name " + octave + " octave");
                    long ti = currentT.normalNotes.get(i).getTick() - currentT.normalNotes.get(i - 1).getTick();
                    Thread.sleep((long) (ti / multiplier));
//					Thread.sleep(allT.normalNotes.get(i - 1).getVelocity());
                }

                int octave = (currentT.normalNotes.get(currentT.normalNotes.size() - 1).getKey() / 12);
                int note = currentT.normalNotes.get(currentT.normalNotes.size() - 1).getKey() % 12;

                int mod;
                if (octave >= diapasone[1]) mod = 2;
                else if (octave == diapasone[1] - 1) mod = 1;
                else if (octave <= diapasone[0]) mod = 0;
                else mod = -1;

                if (mod != -1)
                    if (currentT.normalNotes.get(currentT.normalNotes.size() - 1).getMsg() == MusicBot.NOTE_ON)
                        keydown(note, mod);
                    else if (currentT.normalNotes.get(currentT.normalNotes.size() - 1).getMsg() == MusicBot.NOTE_OFF)
                        keyup(note, mod);

					/*for (int i = 1; i < trackList.get(currentTrack).normalNotes.size(); i++) {
						int octave = (trackList.get(currentTrack).normalNotes.get(i - 1).getKey() / 12) - 1;
						int note = trackList.get(currentTrack).normalNotes.get(i - 1).getKey() % 12;
						if (trackList.get(currentTrack).normalNotes.get(i - 1).getChannel() == currentChannel) {
							if (trackList.get(currentTrack).normalNotes.get(i - 1).getMsg() == MusicBot.NOTE_ON) {
								keydown(note, octave);
//							Thread.sleep(trackList.get(currentTrack).normalNotes.get(i - 1).getVelocity());
							} else if (trackList.get(currentTrack).normalNotes.get(i - 1).getMsg() == MusicBot.NOTE_OFF)
								keyup(note, octave);
						}
//			    	System.out.println((trackList.get(currentTrack).normalNotes.get(i - 1).getKey()) + " key " + note + " note " + MusicBot.NOTE_NAMES[note] + " note_name " + octave + " octave");
						long ti = trackList.get(currentTrack).normalNotes.get(i).getTick() - trackList.get(currentTrack).normalNotes.get(i - 1).getTick();
//					Thread.sleep(trackList.get(currentTrack).normalNotes.get(i - 1).getVelocity());
						Thread.sleep((long) (ti / multiplier));
					}
					if (trackList.get(currentTrack).normalNotes.get(trackList.get(currentTrack).normalNotes.size() - 1).getChannel() == currentChannel) {
						int octave = (trackList.get(currentTrack).normalNotes.get(trackList.get(currentTrack).normalNotes.size() - 1).getKey() / 12) - 1;
						int note = trackList.get(currentTrack).normalNotes.get(trackList.get(currentTrack).normalNotes.size() - 1).getKey() % 12;
						if (trackList.get(currentTrack).normalNotes.get(trackList.get(currentTrack).normalNotes.size() - 1).getMsg() == MusicBot.NOTE_ON)
							keydown(note, octave);
						else if (trackList.get(currentTrack).normalNotes.get(trackList.get(currentTrack).normalNotes.size() - 1).getMsg() == MusicBot.NOTE_OFF)
							keyup(note, octave);

					}*/
            }
//			System.out.println((trackList.get(currentTrack).normalNotes.get(trackList.get(currentTrack).normalNotes.size() - 1).getKey()) + " key " + note + " note " + MusicBot.NOTE_NAMES[note] + " note_name " + octave + " octave");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            thread.interrupt();
        }
    }

    public void initBot() {
        midList = MusicBot.findAllFiles(MusicBot.pathName, MusicBot.midExp);
        if (midList == null) {
            PBotUtils.sysMsg(ui, "Not found mid files in " + MusicBot.pathName);
        } else {
			/*channelLst = new Listbox<String>(100, 16, 20) {
				@Override
				protected String listitem(int i) {
					return channelList.get(i);
				}

				@Override
				protected int listitems() {
					return channelList.size();
				}

				@Override
				protected void drawitem(GOut g, String item, int i) {
					g.text(item, new Coord(5, 1));
				}

				@Override
				protected void itemclick(String item, int button) {
					super.itemclick(item, button);
					for (int i = 0; i < channelList.size(); i++) {
						if (channelList.get(i).equals(channelLst.sel)) {
							currentChannel = i;
							PBotUtils.sysMsg("Selected " + currentTrack + " track");
						}
					}
					PBotUtils.sysMsg("Selected " + currentTrack + " track");
				}

			};
			add(channelLst, new Coord(310, 150));*/

            if (trackCheckList != null) trackCheckList.reqdestroy();
            trackCheckList = new CheckListbox(160, 16);
            add(trackCheckList, new Coord(205, 150));
			/*trackLst = new Listbox<String>(100, 16, 20) {
				@Override
				protected String listitem(int i) {
					return trackList.get(i).size + "";
				}

				@Override
				protected int listitems() {
					return trackList.size();
				}

				@Override
				protected void drawitem(GOut g, String item, int i) {
					g.text(item, new Coord(5, 1));
				}

				@Override
				protected void itemclick(String item, int button) {
					super.itemclick(item, button);
					setChannelLst();
				}

			};
			add(trackLst, new Coord(205, 150));*/

            if (allTracks != null) allTracks.reqdestroy();
            allTracks = new CheckBox("Play all tracks") {{
                set(true);
            }};

            midLst = new Listbox<String>(200, 16, 18) {
                @Override
                protected String listitem(int i) {
                    return midList.get(i);
                }

                @Override
                protected int listitems() {
                    return midList.size();
                }

                @Override
                protected void drawitem(GOut g, String item, int i) {
                    g.text(item, new Coord(5, 1));
                }

                @Override
                protected void itemclick(String item, int button) {
                    super.itemclick(item, button);
                    stop();
                    parsing();
                }
            };
            add(midLst, new Coord(0, 150));

            //parsing = new Button(30, "Parsing Mid", this::parsing);

            if (multi != null) multi.reqdestroy();
            multi = new HSlider(200, 1, 5 * 100, (int) (multiplier * 100)) {
                public boolean mousedown(Coord c, int button) {
                    if (button == 3) {
                        val = 100;
                        multiplier = Math.ceil(val / 100f * 100) / 100;
                        return (true);
                    } else
                        return (super.mousedown(c, button));
                }

                public void changed() {
                    multiplier = Math.ceil(val / 100f * 100) / 100;
                }

                @Override
                public Object tooltip(Coord c0, Widget prev) {
                    return Text.render("Music speed : " + multiplier).tex();
                }
            };

            add(multi, new Coord(0, 130));

            if (play != null) play.reqdestroy();
            if (stop != null) stop.reqdestroy();
            if (pause != null) pause.reqdestroy();
            play = new Button(30, "Play", this::play);
            stop = new Button(30, "Stop", this::stop);
            pause = new Button(30, "Pause", this::pause);

            add(allTracks, new Coord(205, 130));
            //add(parsing, new Coord(0, 130));
            add(play, new Coord(300, 0));
            add(stop, new Coord(300, 30));
            add(pause, new Coord(300, 60));

            pack();
//			resize(500, 450);
        }
    }

    public void parsing() {
        if (midLst.sel != null) {
//            MusicBot.createJSONmusicfile(MusicBot.pathName, midLst.sel, MusicBot.midExp);
//            jsonArray = MusicBot.readJSONmusicfile(MusicBot.pathName, midLst.sel, MusicBot.jsonExp);
            jsonArray = MusicBot.readJSON(MusicBot.pathName, midLst.sel, MusicBot.midExp);
            if (jsonArray != null) {
                trackList.clear();
                trackCheckList.items.clear();
                trackList = MusicBot.getTracksFromJSON();
                setDiapasone();

                for (int i = 0; i < trackList.size(); i++) {
                    trackCheckList.items.add(new CheckListboxItem("Track " + trackList.get(i).number + ": " + trackList.get(i).size + "(" + trackList.get(i).normalSize + ")"));
                }

                ArrayList<midiNote> almn = new ArrayList<>();
                for (int i = 0; i < trackList.size(); i++) {
                    if (trackList.get(i).normalNotes.size() != 0)
                        for (int j = 0; j < trackList.get(i).normalNotes.size(); j++) {
                            if (almn.size() == 0) {
                                almn.add(trackList.get(i).normalNotes.get(j));
                            } else {
                                for (int k = 0; k < almn.size(); k++) {
                                    if (trackList.get(i).normalNotes.get(j).getTick() <= almn.get(k).getTick()) {
                                        almn.add(k, trackList.get(i).normalNotes.get(j));
                                        break;
                                    } else {
                                        if (k == almn.size() - 1) {
                                            almn.add(trackList.get(i).normalNotes.get(j));
                                        }
                                    }
                                }
                            }
                        }
                }

                allT = new midiTrack(almn, 0, almn.size());
//                for (int i = 0; i < allT.normalNotes.size(); i++) {
//                    System.out.println("@" + allT.normalNotes.get(i).getTick() + " channel:" + allT.normalNotes.get(i).getChannel() + " note:" + allT.normalNotes.get(i).getMsg() + " key:" + allT.normalNotes.get(i).getKey() + " velocity:" + allT.normalNotes.get(i).getVelocity());
//                }
            } else {
                PBotUtils.sysMsg(ui, "Parsing null");
            }
        } else {
            PBotUtils.sysMsg(ui, "Select a file");
        }
    }

    public void play() {
        if (!allTracks.a) {
            tracksList.clear();
            for (int i = 0; i < trackCheckList.items.size(); i++) {
                if (trackCheckList.items.get(i).selected) {
                    Integer n = Integer.parseInt(trackCheckList.items.get(i).name.substring(trackCheckList.items.get(i).name.indexOf(" ") + 1, trackCheckList.items.get(i).name.indexOf(":")));
                    tracksList.add(n);
                }
            }

            if (tracksList.size() > 0) {
                ArrayList<midiNote> cumn = new ArrayList<>();
                for (int x = 0; x < tracksList.size(); x++) {
                    for (int i = 0; i < trackList.size(); i++) {
                        if (trackList.get(i).number == tracksList.get(x)) {
                            if (trackList.get(i).normalNotes.size() > 0)
                                for (int j = 0; j < trackList.get(i).normalNotes.size(); j++) {
                                    if (cumn.size() == 0) {
                                        cumn.add(trackList.get(i).normalNotes.get(j));
                                    } else {
                                        for (int k = 0; k < cumn.size(); k++) {
                                            if (trackList.get(i).normalNotes.get(j).getTick() <= cumn.get(k).getTick()) {
                                                cumn.add(k, trackList.get(i).normalNotes.get(j));
                                                break;
                                            } else {
                                                if (k == cumn.size() - 1) {
                                                    cumn.add(trackList.get(i).normalNotes.get(j));
                                                }
                                            }
                                        }
                                    }
                                }
                        }
                    }
                }
                if (cumn.size() > 0) {
                    currentT = new midiTrack(cumn, 0, cumn.size());
                } else {
                    PBotUtils.sysMsg(ui, "Channels is empty (it's haven't sounds)");
                }
            }
        }

        if (tracksList.size() > 0 || allTracks.a) {
            if (trackList.size() > 0) {
                if (thread.isAlive()) thread.interrupt();
                thread = new Thread(runnable, "music");
                thread.start();
            } else PBotUtils.sysMsg(ui, "trackList 0");
        } else {
            PBotUtils.sysMsg(ui, "Channels not selected");
        }
    }

    public void stop() {
        thread.interrupt();
        for (int i = 0; i < 12; i++)
            for (int mod = 0; mod < 3; mod++)
                keyup(i, mod);
    }

    public void pause() {
        try {
            paused = !paused;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int[] getDiapasone() {
        return diapasone;
    }

    public void setDiapasone() {
        diapasone = new int[]{0, 0};
        for (midiTrack mt : trackList) {
            for (midiNote mn : mt.normalNotes) {
                int octave = (mn.getKey() / 12);
                octaves[octave]++;
				/*if (diapasone[0] == 0 || diapasone[0] > octave) diapasone[0] = octave;
				if (diapasone[1] == 0 || diapasone[1] < octave) diapasone[1] = octave;*/
            }
        }

        int maxnotes = 0;
        int maxocvate = 0;
        for (int i = 0; i < octaves.length; i++) {
            if (octaves[i] > maxnotes) {
                maxnotes = octaves[i];
                maxocvate = i;
            }
        }

        diapasone[0] = maxocvate - 1;
        diapasone[1] = maxocvate + 1;
//		while (diapasone[1] - diapasone[0] > 2) diapasone[0]++;

		/*System.out.print("diapasone");
		for (int i = 0; i < diapasone.length; i++) {
			System.out.print(" " + diapasone[i] + " ");
		}
		System.out.print("; ");
		System.out.print("octaves");
		for (int i = 0; i < octaves.length; i++) {
			System.out.print(" " + octaves[i] + " ");
		}
		System.out.println(";");*/
    }

    public void keydown(int keyr, int mod) {
        double now = (System.currentTimeMillis() / 1000.0) + latcomp;
        int key = keyr + 12 * mod;
        if (!cur[key]) {
            if (actn >= act.length) {
                wdgmsg("stop", act[0], (float) (now - start));
                for (int i = 1; i < actn; i++)
                    act[i - 1] = act[i];
                actn--;
            }
            wdgmsg("play", key, (float) (now - start));
            cur[key] = true;
            act[actn++] = key;
        }
    }

    public void keyup(int keyr, int mod) {
        double now = (System.currentTimeMillis() / 1000.0) + latcomp;
        int key = keyr + 12 * mod;
        stopnote(now, key);
    }


    public static Widget mkwidget(UI ui, Object[] args) {
        String nm = (String) args[0];
        int maxpoly = (Integer) args[1];
        return (new MusicWnd(nm, maxpoly));
    }

    protected void added() {
        super.added();
        ui.grabkeys(this);
    }

    public void cdraw(GOut g) {
        boolean[] cact = new boolean[cur.length];
        for (int i = 0; i < actn; i++)
            cact[act[i]] = true;
        int base = 12;
        if (ui.modshift) base += 12;
        if (ui.modctrl) base -= 12;
        for (int i = 0; i < nti.length; i++) {
            Coord c = new Coord(ikeys[0].sz().x * ntp[i], 0);
            boolean a = cact[nti[i] + base];
            g.image(ikeys[a ? 1 : 0], c);
            g.image(tips[nti[i]], c.add((ikeys[0].sz().x - tips[nti[i]].sz().x) / 2, ikeys[0].sz().y - tips[nti[i]].sz().y - (a ? 9 : 12)));
        }
        int sho = ikeys[0].sz().x - (ikeys[2].sz().x / 2);
        for (int i = 0; i < shi.length; i++) {
            Coord c = new Coord(ikeys[0].sz().x * shp[i] + sho, 0);
            boolean a = cact[shi[i] + base];
            g.image(ikeys[a ? 3 : 2], c);
            g.image(tips[shi[i]], c.add((ikeys[2].sz().x - tips[shi[i]].sz().x) / 2, ikeys[2].sz().y - tips[shi[i]].sz().y - (a ? 9 : 12)));
        }
    }

    public boolean keydown(KeyEvent ev) {
        double now = (ev.getWhen() / 1000.0) + latcomp;
        Integer keyp = keys.get(ev.getKeyCode());
        if (!disableKeys.a && keyp != null) {
            int key = keyp + 12; //0 12 24 C //11 23 35 B
            if ((ev.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) != 0) key += 12;
            if ((ev.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) key -= 12;
            if (!cur[key]) {
                if (actn >= act.length) {
                    wdgmsg("stop", act[0], (float) (now - start));
                    for (int i = 1; i < actn; i++)
                        act[i - 1] = act[i];
                    actn--;
                }
                wdgmsg("play", key, (float) (now - start));
                cur[key] = true;
                act[actn++] = key;
            }
            return (true);
        }
        return (super.keydown(ev));
    }

    private void stopnote(double now, int key) {
        if (cur[key]) {
            outer:
            for (int i = 0; i < actn; i++) {
                if (act[i] == key) {
                    wdgmsg("stop", key, (float) (now - start));
                    for (actn--; i < actn; i++)
                        act[i] = act[i + 1];
                    break outer;
                }
            }
            cur[key] = false;
        }
    }

    public boolean keyup(KeyEvent ev) {
        double now = (ev.getWhen() / 1000.0) + latcomp;
        Integer keyp = keys.get(ev.getKeyCode());
        if (!disableKeys.a && keyp != null) {
            int key = keyp;
            stopnote(now, key);
            stopnote(now, key + 12);
            stopnote(now, key + 24);
            return (true);
        }
        return (super.keydown(ev));
    }
}

