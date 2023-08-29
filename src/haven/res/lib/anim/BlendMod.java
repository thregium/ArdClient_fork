package haven.res.lib.anim;

import haven.Skeleton;
import haven.Skeleton.ModOwner;
import haven.Skeleton.PoseMod;
import haven.Skeleton.ResPose;
import haven.Skeleton.TrackMod;
import haven.Utils;

public abstract class BlendMod extends PoseMod {
    public final TrackMod a, b;

    public BlendMod(ModOwner owner, Skeleton skel, TrackMod a, TrackMod b) {
        skel.super(owner);
        this.a = a;
        this.b = b;
        blend(factor());
    }

    public BlendMod(ModOwner owner, Skeleton skel, ResPose a, ResPose b) {
        this(owner, skel, a.forskel(owner, skel, a.defmode), b.forskel(owner, skel, b.defmode));
    }

    protected abstract float factor();

    private static float[] qset(float[] d, float[] s) {
        d[0] = s[0];
        d[1] = s[1];
        d[2] = s[2];
        d[3] = s[3];
        return (d);
    }

    /*
     * XXX: Should not be duplicated, but apparently Skeleton.qqslerp was private. :(
     *
     * Arguably though, the whole blending thing should have some utility function directly in Skeleton.
     */
    private static float[] qqslerp(float[] d, float[] a, float[] b, float t) {
        float aw = a[0], ax = a[1], ay = a[2], az = a[3];
        float bw = b[0], bx = b[1], by = b[2], bz = b[3];
        if ((aw == bw) && (ax == bx) && (ay == by) && (az == bz))
            return (qset(d, a));
        float cos = (aw * bw) + (ax * bx) + (ay * by) + (az * bz);
        if (cos < 0) {
            bw = -bw; bx = -bx; by = -by; bz = -bz;
            cos = -cos;
        }
        float d0, d1;
        if (cos > 0.9999f) {
            /* Reasonable threshold? Is this function even critical
             * for performance? */
            d0 = 1.0f - t; d1 = t;
        } else {
            float da = (float) Math.acos(Utils.clip(cos, 0.0, 1.0));
            float nf = 1.0f / (float) Math.sin(da);
            d0 = (float) Math.sin((1.0f - t) * da) * nf;
            d1 = (float) Math.sin(t * da) * nf;
        }
        d[0] = (d0 * aw) + (d1 * bw);
        d[1] = (d0 * ax) + (d1 * bx);
        d[2] = (d0 * ay) + (d1 * by);
        d[3] = (d0 * az) + (d1 * bz);
        return (d);
    }

    public void blend(float f) {
        float F = 1.0f - f;
        for (int i = 0; i < skel().blist.length; i++) {
            qqslerp(lrot[i], a.lrot[i], b.lrot[i], f);
            lpos[i][0] = (a.lpos[i][0] * F) + (b.lpos[i][0] * f);
            lpos[i][1] = (a.lpos[i][1] * F) + (b.lpos[i][1] * f);
            lpos[i][2] = (a.lpos[i][2] * F) + (b.lpos[i][2] * f);
        }
    }

    public boolean tick(float dt) {
        a.tick(dt);
        b.tick(dt);
        blend(factor());
        return (false);
    }

    public boolean stat() {return (false);}

    public boolean done() {return (false);}
}