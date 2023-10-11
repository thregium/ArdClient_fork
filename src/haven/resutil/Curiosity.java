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

package haven.resutil;

import haven.CompImage;
import haven.Coord;
import haven.GItem;
import haven.ItemData;
import haven.ItemInfo;
import haven.QualityList;
import static haven.QualityList.SingleType.Quality;
import haven.Resource;
import haven.RichText;
import haven.Session;
import haven.UI;
import haven.Utils;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.function.Function;

public class Curiosity extends ItemInfo.Tip {
    public final int exp;
    public final int mw;
    public final int enc;
    public final double time;

    public Curiosity(Owner owner, int exp, int mw, int enc, int time) {
        super(owner);
        this.exp = exp;
        this.mw = mw;
        this.enc = enc;
        this.time = time / 60.0;
        if (owner instanceof GItem)
            ((GItem) owner).studytime = this.time;
    }

    DecimalFormat f = new DecimalFormat("##.##");

    private String timefmt(boolean extended) {
        double rtime = time / owner.glob().getTimeFac();
        int hours = (int) (time / 60.0);
        int rhours = (int) (rtime / 60.0);
        int minutes = (int) (time % 60);
        int rminutes = (int) (rtime % 60);

        boolean rl = Utils.getprefb("tooltipapproximatert", false);
        String itinfo = "%s%s%s";
        String rtinfo = "~%s%s%s RL";
        Function<Object[], Object[]> par = (args) -> new Object[]{args[rl ? 1 : 0], args[rl ? 3 : 2].equals("") ? "" : " ", args[rl ? 3 : 2], args[rl ? 0 : 1], args[rl ? 2 : 3].equals("") ? "" : " ", args[rl ? 2 : 3]};
        Function<Object[], Object[]> par2 = (args) -> new Object[]{args[rl ? 1 : 0], args[rl ? 3 : 2].equals("") ? "" : " ", args[rl ? 3 : 2]};
        String fmt = Resource.getLocString(Resource.BUNDLE_LABEL, "Study time") + ": $col[128,255,128]{" + (rl ? rtinfo : itinfo) + (extended ? " (" + (rl ? itinfo : rtinfo) + ")" : "") + "}";
        String hstr = hours > 0 ? String.format("%dh", hours) : "";
        String rhstr = rhours > 0 ? String.format("%dh", rhours) : "";
        String mstr = minutes > 0 ? String.format("%dm", minutes) : "";
        String rmstr = rminutes > 0 ? String.format("%dm", rminutes) : "";

        Object[] args = {hstr, rhstr, mstr, rmstr};
        return (extended ? String.format(fmt, par.apply(args)) : String.format(fmt, par2.apply(args)));
    }

    public BufferedImage tipimg() {
        UI ui = owner.glob().ui.get();

        int size = 0;
        if (owner instanceof GItem) {
            Coord cs = ((GItem) owner).size();
            size = cs.x * cs.y;
        }
        boolean extended = ui != null && ui.modflags() == UI.MOD_SHIFT;
        CompImage imgs = new CompImage();
//        StringBuilder buf = new StringBuilder();
        if (exp > 0)
            imgs.add(RichText.render(String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "Learning points") + ": $col[128,128,255]{%s}", Utils.thformat(exp)), 0).img);
        if (mw > 0)
            imgs.add(RichText.render(String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "Mental weight") + ": $col[255,128,255]{%d}", mw), 0).img);
        if (enc > 0)
            imgs.add(RichText.render(String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "Experience cost") + ": $col[255,255,128]{%d}", enc), 0).img);
        if (size > 1)
            imgs.add(RichText.render(String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "Size") + ": $col[128,255,255]{%d}", size), 0).img);
        if (time > 0)
            imgs.add(RichText.render(timefmt(extended), 0).img);

        double rtime = 0;
        double lph = 0;
        double rlph = 0;
        double lpw = 0;
        double lphw = 0;
        double rlphw = 0;
        double lpexp = 0;
        double lphexp = 0;
        double rlphexp = 0;
        double lps = 0;
        double lphs = 0;
        double rlphs = 0;
        double lphws = 0;
        double rlphws = 0;

        if (extended) {
            if (time > 0) {
                rtime = time / owner.glob().getTimeFac();
                if (exp > 0) {
                    lph = 1d * exp / (time / 60.0);
                    rlph = 1d * exp / (rtime / 60.0);
                }
            }
            if (mw > 0) {
                if (exp > 0) lpw = 1d * exp / mw;
                if (lph > 0) {
                    lphw = lph / mw;
                    rlphw = rlph / mw;
                }
            }
            if (enc > 0) {
                if (exp > 0) lpexp = 1d * exp / enc;
                if (lph > 0) {
                    lphexp = lph / enc;
                    rlphexp = rlph / enc;
                }
            }
            if (size > 0) {
                if (size > 1) {
                    if (exp > 0) lps = 1d * exp / size;
                    if (lph > 0) {
                        lphs = lph / size;
                        rlphs = rlph / size;
                    }
                }
                if (lphw > 0) {
                    lphws = lphw / size;
                    rlphws = rlphw / size;
                }
            }
            boolean rl = Utils.getprefb("tooltipapproximatert", false);
            String itinfo = "%s";
            String rtinfo = "~%s RL";
            String info = "{" + (rl ? rtinfo : itinfo) + " (" + (!rl ? rtinfo : itinfo) + ")}";
            Function<Object[], Object[]> par = (args) -> new Object[]{args[rl ? 1 : 0], args[rl ? 0 : 1]};
            if (lph > 0 || lpw > 0 || lphw > 0 || lpexp > 0 || lphexp > 0 || lps > 0 || lphs > 0 || lphws > 0) {
                imgs.sz.y += 3;
                if (lph > 0)
                    imgs.add(RichText.render(String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "LP/Time") + ": $col[128,128,255]" + info, par.apply(new Object[]{f.format(lph), f.format(rlph)})), 0).img);
                if (lpw > 0)
                    imgs.add(RichText.render(String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "LP/Weight") + ": $col[255,128,255]{%s}", f.format(lpw)), 0).img);
                if (lphw > 0)
                    imgs.add(RichText.render(String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "LP/Time/Weight") + ": $col[255,128,255]" + info, par.apply(new Object[]{f.format(lphw), f.format(rlphw)})), 0).img);
                if (lpexp > 0)
                    imgs.add(RichText.render(String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "LP/Exp") + ": $col[255,255,128]{%s}", f.format(lpexp)), 0).img);
                if (lphexp > 0)
                    imgs.add(RichText.render(String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "LP/Time/Exp") + ": $col[255,255,128]" + info, par.apply(new Object[]{f.format(lphexp), f.format(rlphexp)})), 0).img);
                if (lps > 0)
                    imgs.add(RichText.render(String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "LP/Size") + ": $col[128,255,255]{%s}", f.format(lps)), 0).img);
                if (lphs > 0)
                    imgs.add(RichText.render(String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "LP/Time/Size") + ": $col[128,255,255]" + info, par.apply(new Object[]{f.format(lphs), f.format(rlphs)})), 0).img);
                if (lphws > 0)
                    imgs.add(RichText.render(String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "LP/Time/Weight/Size") + ": $col[255,0,128]" + info, par.apply(new Object[]{f.format(lphws), f.format(rlphws)})), 0).img);
            }
        }
        return (imgs.compose());
    }

    public static class Data implements ItemData.ITipData {
        public final int lp;
        public final int weight;
        public final int xp;
        public final int time;

        public Data(Curiosity ii, QualityList q) {
            QualityList.Quality single = q.single(Quality);
            if (single == null) {
                single = QualityList.DEFAULT;
            }
            lp = (int) Math.round(ii.exp / single.multiplier);
            weight = ii.mw;
            xp = ii.enc;
            time = (int) ii.time;
        }

        @Override
        public ItemInfo create(Session sess) {
            return new Curiosity(null, lp, weight, xp, time);
        }
    }
}
