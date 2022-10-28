/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import haven.sloth.gob.Type;
import modification.configuration;

public class ResDrawable extends Drawable {
    public final Indir<Resource> res;
    public Sprite spr = null;
    public MessageBuf sdt;
    private int delay = 0;
    protected boolean inited;
    protected Loading error;

    public ResDrawable(Gob gob, Indir<Resource> res, Message sdt) {
        super(gob);
        this.res = res;
        this.sdt = new MessageBuf(sdt);
        waitforinit();
    }

    private void waitforinit() {
        if (inited) return;
        try {
            init();
            inited = true;
        } catch (Loading l) {
            error = l;
            l.waitfor(this::waitforinit, waiting -> {});
        }
    }

    public ResDrawable(Gob gob, Resource res) {
        this(gob, res.indir(), MessageBuf.nil);
    }

    public void init() {
        if (spr != null)
            return;
        Resource res = this.res.get();
        if (gob.type == null)
            gob.type = Type.getType(res.name);

        MessageBuf stdCopy = sdt.clone();
        byte[] args = new byte[2];
        /*if(Config.largetree || Config.largetreeleaves || Config.bonsai){
            if(res.name.contains("tree") && !stdCopy.eom()){

                if(Config.largetree){
                    args[0] = -100;
                    args[1] = -5;
                    stdCopy = new MessageBuf(args);
                } else if(Config.largetreeleaves){
                    args[0] = (byte)stdCopy.uint8();
                    args[1] = -5;
                    stdCopy = new MessageBuf(args);
                } else if (Config.bonsai) {
                    args[0] = (byte)stdCopy.uint8();
                    System.out.println("args0: " + args[0]);
                    int fscale = 25;
                    if (!stdCopy.eom()) {
                        fscale = stdCopy.uint8();
                        if (fscale > 25)
                            fscale = 25;

                    }
                    System.out.println("fscale: " + fscale);
                    System.out.println("args1: " + args[1]);
                    args[1] = (byte)fscale;
                    stdCopy = new MessageBuf(args);
                    System.out.println(stdCopy);
                    System.out.println("--------");
                }
            }
        }*/
        if (configuration.scaletree && (this.gob.type == Type.TREE || this.gob.type == Type.BUSH) && !stdCopy.eom()) {
            args[0] = (byte) stdCopy.uint8();
            int fscale = configuration.scaletreeint;
            if (!stdCopy.eom() && (fscale = stdCopy.uint8()) > configuration.scaletreeint) {
                fscale = configuration.scaletreeint;
            }
            args[1] = (byte) fscale;
            stdCopy = new MessageBuf(args);
        }
        //Dump Name/Type of non-gob
        //System.out.println(this.res.get().name);
        //System.out.println(gob.type);

//        for (String hat : configuration.hatslist) {
//            if (res.name.equals(hat)) {
//                try {
//                    Resource r = Resource.remote().loadwait(configuration.hatreplace);
//                    spr = Sprite.create(gob, r, sdt);
//                    return;
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    Resource r = Resource.remote().loadwait(configuration.defaultbrokenhat);
//                    spr = Sprite.create(gob, r, sdt);
//                    return;
//                }
//            }
//        }
        if (res.name.equals("gfx/terobjs/trees/yulestar-fir") || res.name.equals("gfx/terobjs/trees/yulestar-spruce") || res.name.equals("gfx/terobjs/trees/yulestar-silverfir")) {
            spr = Sprite.create(gob, Resource.remote().loadwait("gfx/terobjs/items/yulestar"), sdt);
            return;
        }
        spr = Sprite.create(gob, res, stdCopy);
    }

    public void setup(RenderList rl) {
        if (!inited) return;
        try {
            String name = getres().name;
            if (name.equals("gfx/terobjs/trees/yulestar-fir") || name.equals("gfx/terobjs/trees/yulestar-spruce") || name.equals("gfx/terobjs/trees/yulestar-silverfir")) {
                if (name.equals("gfx/terobjs/trees/yulestar-fir"))
                    rl.prepc(Location.xlate(new Coord3f(0, 0, 45)));
                else if (name.equals("gfx/terobjs/trees/yulestar-spruce"))
                    rl.prepc(Location.xlate(new Coord3f(0, 0, 60)));
                else
                    rl.prepc(Location.xlate(new Coord3f(0, 0, 60)));
                rl.prepc(Location.rot(new Coord3f(0, 1, 0), (float) Math.PI / 2));
            }
        } catch (Loading e) {
            return;
        }
        rl.add(spr, null);
    }

    public int sdtnum() {
        if (sdt != null) {
            Message csdt = sdt.clone();
            return csdt.eom() ? 0xffff000 : Sprite.decnum(csdt);
        }
        return 0;
    }

    public void ctick(int dt) {
        if (spr == null) {
            delay += dt;
        } else {
            spr.tick(delay + dt);
            delay = 0;
        }
    }

    public void dispose() {
        if (spr != null)
            spr.dispose();
    }

    public Resource getres() {
        return (res.get());
    }

    public Skeleton.Pose getpose() {
        if (!inited && error != null) throw (error);
        return (Skeleton.getpose(spr));
    }

    public Object staticp() {
        return ((spr != null) ? spr.staticp() : null);
    }
}
