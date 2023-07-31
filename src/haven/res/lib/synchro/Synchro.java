package haven.res.lib.synchro;

import haven.Skeleton;
import haven.Skeleton.ModOwner;
import haven.Skeleton.ResPose;

public class Synchro extends ResPose.ResMod {
    public final Follow flw;

    public Synchro(ResPose pose, ModOwner owner, Skeleton skel, Follow flw) {
        pose.super(owner, skel);
        this.flw = flw;
    }

    public boolean tick(float dt) {
        aupdate(flw.ctime());
        return (true);
    }
}

