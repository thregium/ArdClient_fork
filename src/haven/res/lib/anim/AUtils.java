package haven.res.lib.anim;

import haven.Skeleton;
import haven.Skeleton.PoseMod;

import java.util.Collection;

public class AUtils {
    public static PoseMod combine(Skeleton skel, PoseMod... mods) {
        int n = 0;
        PoseMod last = null;
        for (PoseMod mod : mods) {
            if (mod != null) {
                last = mod;
                n++;
            }
        }
        if (n == 0)
            return (skel.nilmod());
        if (n == 1)
            return (last);
        if (n != mods.length) {
            PoseMod[] buf = new PoseMod[n];
            for (int i = 0, o = 0; i < mods.length; i++) {
                if (mods[i] != null)
                    buf[o++] = mods[i];
            }
            mods = buf;
        }
        return (Skeleton.combine(mods));
    }

    public static PoseMod combine(Skeleton skel, Collection<PoseMod> mods) {
        return (combine(skel, mods.toArray(new PoseMod[0])));
    }
}