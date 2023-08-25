package haven;

import java.awt.Color;

public class GobQualitySprite extends Sprite {
    private static Tex qimg = Resource.remote().loadwait("ui/tt/q/quality").layer(Resource.imgc, 0).tex();
    public int val;
    private Tex tex;
    private static Matrix4f mv = new Matrix4f();
    private Projection proj;
    private Coord wndsz;
    private Location.Chain loc;
    private Camera camp;

    public GobQualitySprite(int val) {
        super(null, null);
        update(val);
    }

    public void draw(GOut g) {
        float[] c = mv.load(camp.fin(Matrix4f.id)).mul1(loc.fin(Matrix4f.id)).homoc();
        Coord sc = proj.get2dCoord(c, wndsz);
        if (tex != null) {
            sc.x -= (qimg.sz().x + tex.sz().x) / 2;
            sc.y -= 40;
            g.image(qimg, sc.add(0, Math.max(qimg.sz().y, tex.sz().y) / 2 - qimg.sz().y / 2));
            g.image(tex, sc.add(qimg.sz().x, Math.max(qimg.sz().y, tex.sz().y) / 2 - tex.sz().y / 2));
        }
    }

    public boolean setup(RenderList rl) {
        rl.prepo(last);
        GLState.Buffer buf = rl.state();
        proj = buf.get(PView.proj);
        wndsz = buf.get(PView.wnd).sz();
        loc = buf.get(PView.loc);
        camp = buf.get(PView.cam);
        //hlt0 = PUtils.strokeTex(Text.render(val + "", new Color(255, 227, 168)));
        return true;
    }

    public void update(int val) {
        this.val = val;
        tex = PUtils.strokeTex(Text.num12boldFnd.render(val + "", new Color(255, 227, 168)));
    }
}
