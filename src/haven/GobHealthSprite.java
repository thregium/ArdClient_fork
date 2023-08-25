package haven;

import java.awt.Color;


public class GobHealthSprite extends Sprite {
    private static final Tex hlt0 = PUtils.strokeTex(Text.num12Fnd.render("1/4", new Color(255, 0, 0)));
    private static final Tex hlt1 = PUtils.strokeTex(Text.num12Fnd.render("2/4", new Color(255, 128, 0)));
    private static final Tex hlt2 = PUtils.strokeTex(Text.num12Fnd.render("3/4", new Color(255, 255, 0)));
    public int val;
    private Tex tex;
    private static Matrix4f mv = new Matrix4f();
    private Projection proj;
    private Coord wndsz;
    private Location.Chain loc;
    private Camera camp;

    public GobHealthSprite(int val) {
        super(null, null);
        update(val);
    }

    public void draw(GOut g) {
        float[] c = mv.load(camp.fin(Matrix4f.id)).mul1(loc.fin(Matrix4f.id)).homoc();
        Coord sc = proj.get2dCoord(c, wndsz);
//        sc.x -= 15;
//        sc.y -= 20;
        g.aimage(tex, sc, 0.5, 0.5);
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

    public void update(int val) {
        this.val = val;
        switch (val - 1) {
            case 0:
                tex = hlt0;
                break;
            case 1:
                tex = hlt1;
                break;
            case 2:
                tex = hlt2;
                break;
        }
    }
}
