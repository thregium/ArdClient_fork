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

import haven.GItem;
import haven.Glob;
import haven.ItemData;
import haven.ItemInfo;
import haven.QualityList;
import haven.Resource;
import haven.RichText;
import haven.Session;
import haven.Utils;

import java.awt.image.BufferedImage;
import java.text.DecimalFormat;

import static haven.QualityList.SingleType.Quality;

public class Curiosity extends ItemInfo.Tip {
    public final int exp, mw, enc;
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

    private String timefmt() {
        double rtime = time / owner.glob().getTimeFac();
        int hours = (int) (time / 60.0);
        int rhours = (int) (rtime / 60.0);
        int minutes = (int) (time % 60);
        int rminutes = (int) (rtime % 60);

        String fmt = Resource.getLocString(Resource.BUNDLE_LABEL, "Study time: %s%s%s (%s%s%s)"); // / owner.glob().getTimeFac()
        String hstr = hours > 0 ? String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "$col[192,255,192]{%d}h"), hours) : "";
        String rhstr = rhours > 0 ? String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "$col[192,255,192]{%d}h"), rhours) : "";
        String mstr = minutes > 0 ? String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "$col[192,255,192]{%d}m"), minutes) : "";
        String rmstr = rminutes > 0 ? String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "$col[192,255,192]{%d}m"), rminutes) : "";

        return (String.format(fmt, hstr, mstr.equals("") ? "" : " ", mstr, rhstr, rmstr.equals("") ? "" : " ", rmstr));
    }

    public BufferedImage tipimg() {
        StringBuilder buf = new StringBuilder();
        if (exp > 0)
            buf.append(String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "Learning points: $col[192,192,255]{%s}\n"), Utils.thformat(exp)));
        if (mw > 0)
            buf.append(String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "Mental weight: $col[255,192,255]{%d}\n"), mw));
        if (enc > 0)
            buf.append(String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "Experience cost: $col[255,255,192]{%d}\n"), enc));
        if (time > 0) {
            double rtime = time / owner.glob().getTimeFac();
            double lph = 1d * exp / (time / 60.0);
            double rlph = 1d * exp / (rtime / 60.0);
            double lphw = lph / mw;
            double rlphw = rlph / mw;
            buf.append(timefmt()).append("\n");
            buf.append(String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "LP/H: $col[192,192,255]{%s (%s)}"), f.format(lph), f.format(rlph))).append("\n");
            if (exp > 0 && mw > 0)
                buf.append(String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "LP/H/Weight: $col[255,255,192]{%s (%s)}\n"), f.format(lphw), f.format(rlphw)));
        }
        return (RichText.render(buf.toString(), 0).img);
    }

    public static class Data implements ItemData.ITipData {
        public final int lp, weight, xp, time;

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
