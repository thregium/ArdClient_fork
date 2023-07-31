package haven.res.lib.synchro;

import haven.Composite;
import haven.Drawable;
import haven.Following;
import haven.Glob;
import haven.Gob;
import haven.Message;
import haven.ResDrawable;
import haven.Resource;
import haven.SkelSprite;
import haven.Skeleton;
import haven.Skeleton.ModFactory;
import haven.Skeleton.ModOwner;
import haven.Skeleton.PoseMod;
import haven.Skeleton.ResPose;
import haven.Skeleton.TrackMod;
import haven.Sprite;
import haven.res.lib.uspr.UnivSprite;

public class Synched implements ModFactory {
    public PoseMod create(Skeleton skel, final ModOwner owner, Resource res, Message sdt) {
        final long tgtid = sdt.eom() ? -1 : sdt.uint32();
        final int olid = sdt.eom() ? 0 : sdt.int32();
        Follow flw = new Follow() {
            Gob lg;
            TrackMod mod;

            Gob getgob() {
                if (tgtid < 0 && owner instanceof Gob) {
                    Following f = ((Gob) owner).getattr(Following.class);
                    if (f == null)
                        return (null);
                    return (f.tgt());
                } else {
                    return (owner.context(Glob.class).oc.getgob(tgtid));
                }
            }

            TrackMod ftm(PoseMod[] mods) {
                for (PoseMod mod : mods) {
                    if (mod instanceof TrackMod)
                        return ((TrackMod) mod);
                }
                return (null);
            }

            TrackMod sprmod(Sprite spr) {
                if (spr instanceof SkelSprite)
                    return (ftm(((SkelSprite) spr).mods));
                if (spr instanceof UnivSprite)
                    return (ftm(((UnivSprite) spr).mods));
                return (null);
            }

            TrackMod mod(Gob gob) {
                if (gob == null)
                    return (null);
                if (olid != 0) {
                    Gob.Overlay ol = gob.findol(olid);
                    if (ol == null)
                        return (null);
                    Sprite spr = ol.spr;
                    if (spr == null)
                        return (null);
                    return (sprmod(spr));
                } else {
                    Drawable d = gob.getattr(Drawable.class);
                    if (d instanceof ResDrawable) {
                        Sprite spr = ((ResDrawable) d).spr;
                        if (spr == null)
                            return (null);
                        return (sprmod(spr));
                    } else if (d instanceof Composite) {
                        Composite cd = (Composite) d;
                        if (cd.comp == null)
                            return (null);
                        return (ftm(cd.comp.poses.mods));
                    } else {
                        return (null);
                    }
                }
            }

            public float ctime() {
                Gob gob = getgob();
                if (gob != lg) {
                    lg = null;
                    mod = null;
                }
                if (mod == null) {
                    if ((mod = mod(gob)) == null)
                        return (0);
                    lg = gob;
                }
                if (mod == null)
                    return (0);
                return (mod.time);
            }
        };
        return (new Synchro(res.layer(ResPose.class), owner, skel, flw));
    }
}
