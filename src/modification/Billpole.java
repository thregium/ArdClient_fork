package modification;

import haven.Coord3f;
import haven.GLState;
import haven.ItemInfo;
import haven.Loading;
import haven.Location;
import haven.MCache;
import haven.Message;
import haven.PView;
import haven.RenderList;
import haven.Rendered;
import haven.Resource;
import haven.Sprite;
import haven.States;
import haven.StaticSprite;
import haven.UI;
import haven.res.ui.tt.carrytag.Tagged;

public class Billpole extends Sprite {
    public static final GLState hilight = new States.ColState(255, 0, 0, 255);
    public static Rendered[] parts = null;
    public final Location loc;
    public final long id;
    private UI ui;

    public Billpole(Owner owner, Resource res, Location loc, long id) {
        super(owner, res);
        if (parts == null)
            parts = StaticSprite.lsparts(Resource.remote().loadwait("gfx/terobjs/items/carrytagpole"), Message.nil);
        this.loc = loc;
        finalState = loc;
        this.id = id;
    }

    public static Sprite mksprite(Owner owner, Resource res, Message sdt) {
        Coord3f pc;
        long id;
        if (sdt.eom()) {
            pc = Coord3f.o;
            id = 0;
        } else {
            pc = new Coord3f((float) (sdt.float16() * MCache.tilesz.x), -(float) (sdt.float16() * MCache.tilesz.y), 0);
            id = sdt.int64();
        }
        return (new Billpole(owner, res, Location.xlate(pc), id));
    }

    private Object lasttip;
    private boolean lasthl;

    @Override
    public boolean tick(int dt) {
        boolean hl = lasthl;
        Object ut = (ui == null) ? null : ui.lasttip;
        if (ut != this.lasttip) {
            hl = false;
            try {
                if (ut instanceof ItemInfo.InfoTip) {
                    ItemInfo.InfoTip tip = (ItemInfo.InfoTip) ut;
                    Tagged tag;
                    try {
                        tag = (tip == null) ? null : ItemInfo.find(Tagged.class, tip.info());
                    } catch (Loading l) {
                        tag = null;
                    }
                    hl = (tag != null) && (tag.id == this.id);
                }
            } catch (NoClassDefFoundError e) {
                /* XXX: Only here waiting for clients to update with
                 * ItemInfo.InfoTip. Remove in due time. */
            }
            this.lasttip = ut;
        }
        if (hl != lasthl) {
            lasthl = hl;
            finalState = GLState.compose(loc, lasthl ? hilight : null);
        }
        return (false);
    }

    private GLState finalState;

    @Override
    public boolean setup(RenderList rls) {
        PView.RenderContext ctx = rls.cstate().get(PView.ctx);
        if (ctx instanceof PView.WidgetContext)
            this.ui = ((PView.WidgetContext) ctx).widget().ui;
        rls.prepc(finalState);
        for (Rendered p : parts) {
            rls.add(p, null);
        }
        return (false);
    }
}
