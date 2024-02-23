package haven.sloth.gfx;

import haven.Buff;
import haven.Camera;
import haven.Coord;
import haven.Coord3f;
import haven.FightWnd;
import haven.Fightview;
import haven.GLState;
import haven.GOut;
import haven.Gob;
import haven.Location;
import haven.Matrix4f;
import haven.PUtils;
import haven.PView;
import haven.Projection;
import haven.RenderList;
import haven.Resource;
import haven.RichText;
import haven.Sprite;
import haven.Tex;
import haven.TexI;
import haven.Text;
import haven.Utils;
import haven.Widget;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
                Double ameter = (buf.ameter >= 0) ? Double.valueOf(buf.ameter / 100.0) : buf.ameteri.get();
                if (ameter != null && buf.isOpening()) {
                    count++;
                }
            }
            bc.x -= (int) (((Buff.scframe.sz().x * scale) + 2) * count / 2);
            for (Widget wdg = rel.buffs.child; wdg != null; wdg = wdg.next) {
                if (!(wdg instanceof Buff))
                    continue;
                final Buff buf = (Buff) wdg;
                Double ameter = (buf.ameter >= 0) ? Double.valueOf(buf.ameter / 100.0) : buf.ameteri.get();
                if (ameter != null && buf.isOpening()) {
                    buf.fightdraw(g.reclip(bc.copy(), Buff.scframe.sz()), scale);
                    bc.x += (int) (Buff.scframe.sz().x * scale) + 2;
                }
            }

            for (Widget wdg = rel.buffs.child; wdg != null; wdg = wdg.next) {
                if (!(wdg instanceof Buff))
                    continue;
                final Buff buf = (Buff) wdg;
                if (!buf.isOpening()) {
                    buf.stancedraw(g.reclip(sc.copy().add(-(int) (Buff.scframe.sz().x * scale / 2.0), (int) (Buff.scframe.sz().y * scale) + 20), Buff.scframe.sz()), scale);
                }
            }

            if (rel.lastact != null) {
                Coord lc = c.add(0, Buff.scframe.sz().y * 3);
                double max = 10;
                try {
                    Resource lastres = rel.lastact.get();
                    if (lastres != null) {
                        double lastuse = rel.lastuse;
                        Double savedt = cooldowns.get(lastres.name);
                        if (savedt != null) {
                            int delay = 0;
                            if (lastres.name.contains("takeaim"))
                                delay = 1;
                            if (lastres.name.contains("think"))
                                delay = 2;
                            max = delay == 0 ? savedt : savedt * (1 + 0.2 * (rel.oip - delay));
                        }
                        double time = Utils.rtime() - lastuse;
                        boolean verylong = time > max;
                        Tex lasttex = texMap.computeIfAbsent(lastres.name + (verylong ? "_grey" : ""), t -> {
                            Resource.Image lastimg = lastres.layer(Resource.imgc);
                            BufferedImage bimg = verylong ? PUtils.monochromize(lastimg.img, Color.WHITE) : PUtils.copy(lastimg.img);
                            if (lastres.name.contains("cleave") && !verylong)
                                bimg = PUtils.rasterimg(PUtils.blurmask2(bimg.getRaster(), 2, 2, Color.RED));
                            else
                                bimg = PUtils.rasterimg(PUtils.blurmask2(bimg.getRaster(), 1, 1, Color.BLACK));
                            bimg = PUtils.convolvedown(bimg, Coord.of((int) (bimg.getWidth() * scale), (int) (bimg.getHeight() * scale)), new PUtils.Hanning(1));
                            return (new TexI(bimg));
                        });
                        g.aimage(lasttex, lc, 0.5, 0.5);
                        if (!verylong) {
                            Tex timetex = texMap.computeIfAbsent(String.format("%.1f", max - time), t -> Text.renderstroked(t, new Color(30, 30, 30), Color.WHITE, Text.num14boldFnd).tex());
                            g.aimage(timetex, lc, 0.5, 0.5);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Tex render = texMap.computeIfAbsent(String.format("$bg[35,35,35,192]{$size[14]{$b{$col[0,255,0]{%d} : $col[255,0,0]{%d}}}}", rel.ip, rel.oip), t -> RichText.render(t, -1).tex());
            g.aimage(render, c, 0.5, 1);
            g.chcolor();
        }
    }

    private String lastn;
    private double lastt;

    public final Map<String, Tex> texMap = new HashMap<>();
    public final Map<String, Double> cooldowns = new ConcurrentHashMap<>();

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
        if (rel != null && rel.lastact != null) {
            try {
                Resource lastres = rel.lastact.get();
                if (lastres != null) {
                    double max = 10;
                    double lastuse = rel.lastuse;
                    if (!lastres.name.equals(lastn) || lastuse != lastt) {
                        double timem = Utils.rtime() - lastt;
                        if (lastn != null && timem < max) {
                            int delay = 0;
                            if (lastres.name.contains("takeaim"))
                                delay = 1;
                            if (lastres.name.contains("think"))
                                delay = 2;
                            timem = delay == 0 ? timem : timem / (1 + 0.2 * (rel.oip - delay));
                            final double finalTimem = timem;
                            cooldowns.compute(lastn, (s, d) -> d == null ? finalTimem : Math.min(d, finalTimem));
                        }
                        if (!lastres.name.equals(lastn))
                            lastn = lastres.name;
                        if (lastuse != lastt)
                            lastt = lastuse;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return rel == null;
    }

    public Object staticp() {
        return Gob.STATIC;
    }
}
