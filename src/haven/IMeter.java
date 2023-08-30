/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import haven.sloth.gui.MovableWidget;
import java.awt.Color;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IMeter extends MovableWidget {
    private static final Resource ponysfx = Resource.local().loadwait("sfx/alarmpony");
    private static final Pattern hppat = Pattern.compile("Health: (\\d+)/(\\d+)/(\\d+)/?(\\d+)?");
    private static final Pattern stampat = Pattern.compile("Stamina: (\\d+)");
    private static final Pattern energypat = Pattern.compile("Energy: (\\d+)");
    static Coord off = new Coord(22, 7);
    static Coord fsz = new Coord(101, 24);
    static Coord msz = new Coord(75, 10);
    Indir<Resource> bg;
    List<Meter> meters;
    private boolean ponyalarm = true;
    Text meterinfo = null;

    @RName("im")
    public static class $_ implements Factory {
        public Widget create(UI ui, Object[] args) {
            Indir<Resource> bg = ui.sess.getres((Integer) args[0]);
            List<Meter> meters = new LinkedList<>();
            for (int i = 1; i < args.length; i += 2) {
                meters.add(new Meter((Color) args[i], (Integer) args[i + 1]));
            }
            String name = null;
            while (name == null) {
                if (bg != null)
                    try {
                        name = bg.get().basename();
                    } catch (Exception ignore) {
                    }
            }
            return (new IMeter(bg, meters, "meter-" + name));
        }
    }

    protected float scale = Utils.getpreff("scale-" + this.key, 1f);

    private IMeter(Indir<Resource> bg, List<Meter> meters, final String name) {
        super(fsz.mul(Utils.getpreff("scale-" + name, 1f)), name);
        this.bg = bg;
        this.meters = meters;
    }

    public IMeter(Indir<Resource> bg, final String name) {
        this(bg, Collections.emptyList(), name);
    }

    public static class Meter {
        Color c;
        public int a;

        public Meter(Color c, int a) {
            this.c = c;
            this.a = a;
        }
    }

    protected Tex tex() {
        return (this.bg.get().layer(Resource.imgc).tex(this.scale));
    }

    protected void drawBg(GOut g) {
        g.chcolor(0, 0, 0, 255);
        g.frect(off.mul(this.scale), msz.mul(this.scale));
    }

    protected void drawMeters(GOut g) {
        for (Meter m : meters) {
            int w = msz.x;
            w = (w * m.a) / 100;
            g.chcolor(m.c);
            g.frect(off.mul(this.scale), Coord.of(w, msz.y).mul(this.scale));
        }
    }

    public void draw(GOut g) {
        drawBg(g);
        drawMeters(g);
        g.chcolor();
        if (Config.showmetertext) {
            Text meterinfo = this.meterinfo;
            if (meterinfo != null)
                g.aimage(meterinfo.tex(), sz.div(2).add(10 * this.scale, -1 * this.scale), 0.5, 0.5);
        }
        try {
            g.image(tex(), Coord.z);
        } catch (Loading l) {
            //Ignore
        }
        super.draw(g);
    }

    public void uimsg(String msg, Object... args) {
        if (msg == "set") {
            List<Meter> meters = new LinkedList<>();
            for (int i = 0; i < args.length; i += 2)
                meters.add(new Meter((Color) args[i], (Integer) args[i + 1]));
            this.meters = meters;

            if (ponyalarm) {
                try {
                    Resource res = bg.get();
                    if (res != null && res.name.equals("gfx/hud/meter/häst")) {
                        if (meters.get(0).a <= 10) {
                            Audio.play(ponysfx, 1.0);
                            ponyalarm = false;
                        }
                    }
                } catch (Loading e) {
                }
            }
        } else {
            super.uimsg(msg, args);
            if (msg.equals("tip")) {
                final String tt = (String) args[0];
                Matcher matcher = hppat.matcher(tt);
                String meterinfo = null;
                if (matcher.find()) {
                    if (matcher.group(4) != null) {
                        meterinfo = matcher.group(1);
                    } else {
                        ui.sess.details.shp = Integer.parseInt(matcher.group(1));
                        ui.sess.details.hhp = Integer.parseInt(matcher.group(2));
                        ui.sess.details.mhp = Integer.parseInt(matcher.group(3));
                        if (ui.sess.details.shp < ui.sess.details.hhp && ui.sess.details.hhp < ui.sess.details.mhp)
                            meterinfo = ui.sess.details.shp + "/" + ui.sess.details.hhp + "/" + ui.sess.details.mhp;
                        else if ((ui.sess.details.shp < ui.sess.details.hhp && ui.sess.details.hhp == ui.sess.details.mhp) || (ui.sess.details.shp == ui.sess.details.hhp && ui.sess.details.hhp < ui.sess.details.mhp))
                            meterinfo = ui.sess.details.shp + "/" + ui.sess.details.mhp;
                        else if (ui.sess.details.shp == ui.sess.details.hhp && ui.sess.details.hhp == ui.sess.details.mhp)
                            meterinfo = ui.sess.details.mhp + "";
                    }
                } else {
                    matcher = stampat.matcher(tt);
                    if (matcher.find()) {
                        ui.sess.details.stam = Integer.parseInt(matcher.group(1));
                        meterinfo = ui.sess.details.stam + "%";
                    } else {
                        matcher = energypat.matcher(tt);
                        if (matcher.find()) {
                            ui.sess.details.energy = Integer.parseInt(matcher.group(1));
                            meterinfo = ui.sess.details.energy + "%";
                        } else {
                            meterinfo = tt;
                            if (meterinfo.contains("ow")) {
                                meterinfo = tt.split(" ")[2];
                            } else {
                                meterinfo = tt.split(" ")[1];
                            }
                            if (meterinfo.contains("/")) {
                                String[] hps = meterinfo.split("/");
                                meterinfo = hps[0] + "/" + hps[hps.length - 1];
                            }
                        }
                    }
                }
                if (meterinfo == null)
                    meterinfo = tt;
                updatemeterinfo(meterinfo);
            }
        }
    }

    private static final Text.Foundry fnd = new Text.Foundry(Text.latin);

    public boolean mousewheel(Coord coord, int amount) {
        if (ui.modflags() == (UI.MOD_CTRL | UI.MOD_META)) {
            float scale = Math.max(Math.min(this.scale - (0.1f * amount), 5f), 1f);
            float lastscale = this.scale;
            Utils.setpreff("scale-" + this.key, this.scale = scale);

            resize(fsz.mul(this.scale));
            move(c.sub(sz.sub(fsz.mul(lastscale)).div(2)));
            Text meterinfo = this.meterinfo;
            if (meterinfo != null)
                updatemeterinfo(meterinfo.text);
            return (true);
        }
        return (super.mousewheel(c, amount));
    }

    protected void updatemeterinfo(String str) {
        if (str == null || str.isEmpty()) return;
        Text meterinfo = this.meterinfo;
        if (meterinfo == null || !meterinfo.text.equals(str)) {
            this.meterinfo = Text.create(str, PUtils.strokeImg(new Text.Foundry(Text.latin, (int) (10 * this.scale)).render(str)));
        }
    }
}
