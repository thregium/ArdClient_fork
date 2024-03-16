package haven.res.ui.croster;

import haven.BuddyWnd;
import haven.Camera;
import haven.CheckBox;
import haven.Coord;
import haven.Coord3f;
import haven.GAttrib;
import haven.GLState;
import haven.GOut;
import haven.Gob;
import haven.Location;
import haven.MapView;
import haven.Matrix4f;
import haven.Message;
import haven.PUtils;
import haven.PView;
import haven.Projection;
import haven.RenderList;
import haven.Tex;
import haven.Text;

import java.awt.Color;

public class CattleId extends GAttrib implements PView.Render2D{
    public final long id;

    public CattleId(Gob gob, long id) {
        super(gob);
        this.id = id;
    }

    public static void parse(Gob gob, Message dat) {
        long id = dat.int64();
        gob.setattr(new CattleId(gob, id));
    }

    @Override
    public void tick() {
        super.tick();
        update();
    }

    @Override
    public void draw(final GOut g) {}

    private static Matrix4f mv = new Matrix4f();
    private Projection proj;
    private Coord wndsz;
    private Location.Chain loc;
    private Camera camp;
    private Coord3f fsc, sczu;

    private String lnm;
    private int lgrp;
    private Tex rnm;

    private int rmseq = 0, entryseq = 0;
    private RosterWindow wnd = null;
    private CattleRoster<?> roster = null;
    private Entry entry = null;

    public Entry entry() {
        if ((entry == null) || ((roster != null) && (roster.entryseq != entryseq))) {
            if (rmseq != RosterWindow.rmseq) {
                synchronized (RosterWindow.rosters) {
                    RosterWindow wnd = RosterWindow.rosters.get(gob.glob);
                    if (wnd != null) {
                        for (CattleRoster<?> ch : wnd.children(CattleRoster.class)) {
                            if (ch.entries.get(id) != null) {
                                this.wnd = wnd;
                                this.roster = ch;
                                this.rmseq = RosterWindow.rmseq;
                                break;
                            }
                        }
                    }
                }
            }
            if (roster != null)
                this.entry = roster.entries.get(id);
        }
        return (entry);
    }

    @Override
    public void draw2d(final GOut g) {
        mv.load(camp.fin(Matrix4f.id)).mul1(loc.fin(Matrix4f.id));
        fsc = proj.toscreen(mv.mul4(Coord3f.o), wndsz);
        sczu = proj.toscreen(mv.mul4(Coord3f.zu), wndsz).sub(fsc);
        final Coord sc = new Coord(fsc.add(sczu.mul(25)));
        boolean c = MapView.markedGobs.contains(gob.id);
        if (sc.isect(Coord.z, g.sz())) {
            if ((rnm != null) && (wnd != null) && wnd.visible) {
                if (entry != null) {
                    Coord nmc = sc.sub(rnm.sz().x / 2, -rnm.sz().y);
                    g.image(rnm, nmc);
                    if (entry.mark.a) {
                        g.image(CheckBox.smark, nmc.sub(CheckBox.smark.sz().x, 0));
                        if (!c)
                            MapView.markedGobs.add(gob.id);
                    } else {
                        if (c)
                            MapView.markedGobs.remove(gob.id);
                    }
                }
            }
        }
    }

    @Override
    public boolean setup(final RenderList r) {
        r.prepo(last);
        GLState.Buffer buf = r.state();
        proj = buf.get(PView.proj);
        wndsz = buf.get(PView.wnd).sz();
        loc = buf.get(PView.loc);
        camp = buf.get(PView.cam);
        return (true);
    }

    public void update() {
        Entry entry = entry();
        int grp = (entry != null) ? entry.grp : 0;
        String name = (entry != null) ? entry.name : null;
        if ((name != null) && ((rnm == null) || !name.equals(lnm) || (grp != lgrp))) {
            Color col = BuddyWnd.gc[grp];
//            rnm = new TexI(Utils.outline2(Text.render(name, col).img, Utils.contrast(col)));
            rnm = PUtils.strokeTex(Text.render(name, col));
            lnm = name;
            lgrp = grp;
        }
    }
}
