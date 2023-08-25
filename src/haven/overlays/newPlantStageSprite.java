package haven.overlays;

import haven.Camera;
import haven.Config;
import haven.Coord;
import haven.GLState;
import haven.GOut;
import haven.Location;
import haven.Matrix4f;
import haven.PUtils;
import haven.PView;
import haven.Projection;
import haven.RenderList;
import haven.Sprite;
import haven.Tex;
import haven.Text;
import org.json.JSONObject;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class newPlantStageSprite extends Sprite {
    public int stg;
    private Tex tex;
    private static Matrix4f mv = new Matrix4f();
    private Projection proj;
    private Coord wndsz;
    private Location.Chain loc;
    private Camera camp;
    private final boolean multistg, offsetmultisg;

    public static Map<String, Tex> cachedTex = new HashMap<>();

    public static Tex getCachedTex(String text, Color color) {
        JSONObject jo = new JSONObject();
        jo.put("name", text);
        jo.put("col", color.hashCode());
//        jo.put("border", bgcol.hashCode());

        Tex tex = cachedTex.get(jo.toString());
        if (tex == null) {
            tex = PUtils.strokeTex(Text.num11Fnd.render(text, color));
            cachedTex.put(jo.toString(), tex);
        }
        return (tex);
    }

    Tex stg(int stg, int stgmax, Color clr) {
        return getCachedTex(String.format("%d/%d", stg, stgmax), clr);
    }

//    Tex stg(int stg, int stgmax, Color clr, Color border) {
//        return getCachedTex(String.format("%d/%d", stg, stgmax), clr, border);
//    }

    public newPlantStageSprite(int stg, int stgmax, boolean multistg, boolean offsetmultisg) {
        super(null, null);
        this.multistg = multistg;
        this.offsetmultisg = offsetmultisg;
        update(stg, stgmax);
    }

    public void draw(GOut g) {
        float[] c = mv.load(camp.fin(Matrix4f.id)).mul1(loc.fin(Matrix4f.id)).homoc();
        Coord sc = proj.get2dCoord(c, wndsz);
        sc.x -= tex.sz().x / 2;
        sc.y -= 10;
        g.image(tex, sc);
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

    public void update(int stg, int stgmax) {
        this.stg = stg;
        int truestg = stg + 1;
        int truestgmax = stgmax + 1;

        if (stgmax == 0)
            tex = stg(stg, stgmax, Color.gray);
        else if (stg > stgmax)
            tex = stg(truestg, truestgmax, Color.blue);
        else if ((multistg || offsetmultisg) && stg == stgmax - 1)
            tex = stg(truestg, truestgmax, Color.yellow);
        else if (offsetmultisg && stg == stgmax - 2)
            tex = stg(truestg, truestgmax, new Color(255, 100, 50));
        else if (stg == stgmax)
            tex = stg(truestg, truestgmax, Color.green);
        else {
            if (Config.showfreshcropstage)
                tex = stg(truestg, truestgmax, Color.red);
            else
                tex = stg(truestg, truestgmax, Color.red);
        }
        tex.dispose();
    }

    public Object staticp() {
        return CONSTANS;
    }
}
