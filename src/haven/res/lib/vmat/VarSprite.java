package haven.res.lib.vmat;

import haven.FastMesh;
import haven.Gob;
import haven.Loading;
import haven.Message;
import haven.RenderLink;
import haven.Rendered;
import haven.Resource;
import haven.res.lib.uspr.UnivSprite;

import java.util.Collection;
import java.util.LinkedList;

public class VarSprite extends UnivSprite {
    private Gob.ResAttr.Cell<Mapping> aptr;
    private Mapping cmats;

    public VarSprite(Owner owner, Resource res, Message sdt) {
        super(owner, res, sdt);
        aptr = Gob.getrattr(owner, Mapping.class);
    }

    public Mapping mats() {
        return (((aptr != null) && (aptr.attr != null)) ? aptr.attr : Mapping.empty);
    }

    public Collection<Rendered> iparts(int mask) {
        Collection<Rendered> rl = new LinkedList<>();
        Mapping mats = (this.cmats == null) ? Mapping.empty : cmats;
        for (FastMesh.MeshRes mr : res.layers(FastMesh.MeshRes.class)) {
            String sid = mr.rdat.get("vm");
            int mid = (sid == null) ? -1 : Integer.parseInt(sid);
            if (((mr.mat != null) || (mid >= 0)) && ((mr.id < 0) || (((1 << mr.id) & mask) != 0)))
                rl.add(new Wrapping(animmesh(mr.m), mats.mergemat(mr.mat.get(), mid), mid));
        }
        Owner rec = null;
        for (RenderLink.Res lr : res.layers(RenderLink.Res.class)) {
            if ((lr.id < 0) || (((1 << lr.id) & mask) != 0)) {
                if (rec == null)
                    rec = new RecOwner();
                Rendered r = lr.l.make(rec);
                if (r instanceof Wrapping)
                    r = animwrap((Wrapping) r);
                rl.add(r);
            }
        }
        cmats = mats;
        return (rl);
    }

    public boolean tick(int idt) {
        Mapping mats = mats(), pmats = this.cmats;
        if (mats != pmats) {
            try {
                this.cmats = mats;
                update();
            } catch (Loading l) {
                this.cmats = pmats;
            }
        }
        return (super.tick(idt));
    }
}
