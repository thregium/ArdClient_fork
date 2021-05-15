package haven.sloth.gfx;

import haven.Buff;
import haven.Camera;
import haven.Coord;
import haven.Coord3f;
import haven.Fightview;
import haven.GLState;
import haven.GOut;
import haven.Gob;
import haven.Location;
import haven.Matrix4f;
import haven.PView;
import haven.Projection;
import haven.RenderList;
import haven.RichText;
import haven.Sprite;
import haven.Widget;

public class GobCombatSprite extends Sprite {
    public static final int id = -244942;
    private final Matrix4f mv = new Matrix4f();
    private Fightview.Relation rel;
    private Projection proj;
    private Coord wndsz;
    private Location.Chain loc;
    private Camera camp;
    private Coord3f sc, sczu;

    public GobCombatSprite(final Gob g, final Fightview.Relation relation) {
        super(g, null);
        this.rel = relation;
    }


    public void draw(GOut g) {
        if (rel != null) {
            mv.load(camp.fin(Matrix4f.id)).mul1(loc.fin(Matrix4f.id));
            sc = proj.toscreen(mv.mul4(Coord3f.o), wndsz);
            sczu = proj.toscreen(mv.mul4(Coord3f.zu), wndsz).sub(sc);
            final Coord c = new Coord(sc.add(sczu.mul(16))).sub(0, 40);
            final Coord bc = c.copy();
            final Coord sc = c.copy();
            float scale = 0.8f;

            //Draw Buffs
            int count = 0;
            for (Widget wdg = rel.buffs.child; wdg != null; wdg = wdg.next) {
                if (!(wdg instanceof Buff))
                    continue;
                final Buff buf = (Buff) wdg;
                if (buf.ameter >= 0 && buf.isOpening()) {
                    count++;
                }
            }
            bc.x -= (int) (((Buff.scframe.sz().x * scale) + 2) * count / 2);
            for (Widget wdg = rel.buffs.child; wdg != null; wdg = wdg.next) {
                if (!(wdg instanceof Buff))
                    continue;
                final Buff buf = (Buff) wdg;
                if (buf.ameter >= 0 && buf.isOpening()) {
                    buf.fightdraw(g.reclip(bc.copy(), Buff.scframe.sz()), scale);
                    bc.x += (int) (Buff.scframe.sz().x * scale) + 2;
                }
            }

            for (Widget wdg = rel.buffs.child; wdg != null; wdg = wdg.next) {
                if (!(wdg instanceof Buff))
                    continue;
                final Buff buf = (Buff) wdg;
                if (!buf.isOpening()) {
                    buf.stancedraw(g.reclip(sc.copy().add(-(int) (Buff.scframe.sz().x * scale / 2), (int) (Buff.scframe.sz().y * scale) + 20), Buff.scframe.sz()), scale);
                }
            }

            //Draw IP
//            g.chcolor(new Color(60, 60, 60, 168));
//            g.frect(c.sub(40, 0), FastText.sizes("IP " + rel.ip));
//            g.frect(c.sub(40, -15), FastText.sizes("IP " + rel.oip));
//            g.chcolor(Color.GREEN);
//            FastText.printsf(g, c.sub(40, 0), "IP %d", rel.ip);
//            g.chcolor(Color.RED);
//            FastText.printsf(g, c.sub(40, -15), "IP %d", rel.oip);
            g.aimage(RichText.render(String.format("$bg[35,35,35,192]{$size[14]{$b{$col[0,255,0]{%d} : $col[255,0,0]{%d}}}}", rel.ip, rel.oip), -1).tex(), c, 0.5, 1);
            g.chcolor();
        }
    }

    public void update(final Fightview.Relation rel) {
        this.rel = rel;
    }

    public boolean setup(RenderList rl) {
        rl.prepo(last);
        GLState.Buffer buf = rl.state();
        proj = buf.get(PView.proj);
        wndsz = buf.get(PView.wnd).sz();
        loc = buf.get(PView.loc);
        camp = buf.get(PView.cam);
        return true;
    }

    @Override
    public boolean tick(int dt) {
        return rel == null;
    }

    public Object staticp() {
        return Gob.STATIC;
    }
}
