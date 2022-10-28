package haven.res.lib.vmat;

import haven.FastMesh;
import haven.Gob;
import haven.Message;
import haven.Rendered;
import haven.Resource;
import haven.res.lib.uspr.UnivSprite;
import modification.dev;

import java.util.Collection;
import java.util.LinkedList;

public class VarSprite extends UnivSprite {
    static {
        dev.checkFileVersion("lib/vmat", 36);
    }

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
        Mapping mats = mats();
        for (FastMesh.MeshRes mr : res.layers(FastMesh.MeshRes.class)) {
            String sid = mr.rdat.get("vm");
            int mid = (sid == null) ? -1 : Integer.parseInt(sid);
            if (((mr.mat != null) || (mid >= 0)) && ((mr.id < 0) || (((1 << mr.id) & mask) != 0)))
                rl.add(new Wrapping(animmesh(mr.m), mats.mergemat(mr.mat.get(), mid), mid));
        }
        cmats = mats;
        return (rl);
    }

    public boolean tick(int idt) {
        if (mats() != cmats)
            update();
        return (super.tick(idt));
    }
}
