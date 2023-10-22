package haven;

public class HungerMeter extends IMeter {
    private final CharWnd.GlutMeter glut;

    public HungerMeter(CharWnd.GlutMeter glut, final String name) {
        super(Resource.local().load("hud/meter/hungermeter"), name);
        this.glut = glut;
    }

    @Override
    protected void drawBg(GOut g) {
        if (glut.bg == null) return;
        g.chcolor(glut.bg);
        g.frect(off.mul(this.scale), msz.mul(this.scale));
    }

    @Override
    protected void drawMeters(GOut g) {
        if (glut.fg == null) return;
        g.chcolor(glut.fg);
        g.frect(off.mul(this.scale), Coord.of((int) Math.round(msz.x * (glut.glut - Math.floor(glut.glut))), msz.y).mul(this.scale));
    }

    @Override
    public void tick(double dt) {
        super.tick(dt);
        updatemeterinfo(String.format("%s‰:%s%%", Utils.odformat2(glut.lglut * 1000, 1), Math.round(glut.gmod * 100)));
    }

    @Override
    public Object tooltip(Coord c, Widget prev) {
        return (glut.tooltip(c, prev));
    }
}