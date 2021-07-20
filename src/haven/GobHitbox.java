package haven;

import haven.sloth.gob.Type;
import modification.configuration;

import javax.media.opengl.GL2;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class GobHitbox extends Sprite {
    public static States.ColState fillclrstate = new States.ColState(DefSettings.HIDDENCOLOR.get());
    public static States.ColState bbclrstate = new States.ColState(DefSettings.HITBOXCOLOR.get());
    private BBox[] b;
    private int mode;
    private States.ColState clrstate;
    private boolean wall = false;
    private final FloatBuffer[] buffers;

    public GobHitbox(Gob gob, BBox[] b, boolean fill) {
        super(gob, null);

        if (fill) {
            mode = GL2.GL_QUADS;
            clrstate = fillclrstate;
        } else if (gob.type != Type.WALLSEG) {
            mode = GL2.GL_LINE_LOOP;
            clrstate = bbclrstate;
        } else {
            if (Config.flatwalls)
                wall = true;
            mode = GL2.GL_LINE_LOOP;
            clrstate = bbclrstate;
        }

//        a = new Coordf(ac.x, ac.y);
//        b = new Coordf(ac.x, bc.y);
//        c = new Coordf(bc.x, bc.y);
//        d = new Coordf(bc.x, ac.y);
        this.b = b;

        int type = getType();
        buffers = new FloatBuffer[b.length];
        for (int i = 0; i < b.length; i++) {
            buffers[i] = Utils.mkfbuf(b[i].points.length * 3);
            for (int p = 0; p < b[i].points.length; p++) {
                Coord2d point = b[i].points[p];
                buffers[i].put((float) point.x).put((float) ((type == 2 ? -1 : 1) * point.y)).put((float) (type == 3 ? 11 : 1));
            }
            buffers[i].rewind();
        }
    }

    public int getType() {
        if (mode == GL2.GL_LINE_LOOP && !wall) {
            return (1);
        } else if (!wall) {
            return (2);
        } else {
            return (3);
        }
    }

    public boolean setup(RenderList rl) {
        rl.prepo(clrstate);
        if (mode == GL2.GL_LINE_LOOP && !configuration.showaccboundingboxes)
            rl.prepo(States.xray);
        return true;
    }

    public void draw(GOut g) {
        g.apply();
        BGL gl = g.gl;
        if (getType() == 1)
            gl.glLineWidth(2.0F);
        for (int i = 0; i < b.length; i++) {
            gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
            gl.glVertexPointer(3, GL2.GL_FLOAT, 0, buffers[i]);
            gl.glDrawArrays(mode, 0, b[i].points.length);
            gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        }
    }

    public static class BBox {
        public final Coord2d[] points;

        public BBox(Coord2d[] points) {
            this.points = new Coord2d[points.length];
            for (int i = 0; i < points.length; i++) {
                this.points[i] = new Coord2d(points[i].x, -points[i].y);
            }
        }

        public BBox(Coord ac, Coord bc) {
            this.points = new Coord2d[]{
                    new Coord2d(ac.x, -ac.y), new Coord2d(bc.x, -ac.y), new Coord2d(bc.x, -bc.y), new Coord2d(ac.x, -bc.y)
            };
        }
    }

    private static final BBox[] bboxCalf = new BBox[]{new BBox(new Coord(-9, -3), new Coord(9, 3))};
    private static final BBox[] bboxLamb = new BBox[]{new BBox(new Coord(-4, -2), new Coord(5, 2))};
    private static final BBox[] bboxGoat = new BBox[]{new BBox(new Coord(-3, -2), new Coord(4, 2))};
    private static final BBox[] bboxPig = new BBox[]{new BBox(new Coord(-6, -3), new Coord(6, 3))};
    private static final BBox[] bboxBoost = new BBox[]{new BBox(new Coord(-4, -4), new Coord(4, 4))};
    //    private static final BBox[] bboxCattle = new BBox[]{new BBox(new Coord(-12, -4), new Coord(12, 4))};
    private static final BBox[] bboxHorse = new BBox[]{new BBox(new Coord(-8, -4), new Coord(8, 4))};
//    private static final BBox[] bboxSmelter = new BBox[]{new BBox(new Coord(-12, -12), new Coord(12, 20))};
//    private static final BBox[] bboxWallseg = new BBox[]{new BBox(new Coord(-5, -6), new Coord(6, 5))};
//    private static final BBox[] bboxHwall = new BBox[]{new BBox(new Coord(-1, 0), new Coord(0, 11))};
//    private static final BBox[] bboxCupboard = new BBox[]{new BBox(new Coord(-5, -5), new Coord(5, 5))};

    public static BBox[] getBBox(Gob gob) {
        Resource res = null;
        try {
            res = gob.getres();
        } catch (Loading l) {
        }
        if (res == null)
            return null;

        String name = res.name;

        // calves, lambs, cattle, goat
        if (name.equals("gfx/kritter/cattle/calf"))
            return bboxCalf;
        else if (name.equals("gfx/kritter/sheep/lamb"))
            return bboxLamb;
//        else if (name.equals("gfx/kritter/cattle/cattle"))
//            return bboxCattle;
        else if (name.startsWith("gfx/kritter/horse/"))
            return bboxHorse;
        else if (name.startsWith("gfx/kritter/goat/"))
            return bboxGoat;
        else if (name.startsWith("gfx/kritter/pig/"))
            return bboxPig;
        else if (name.startsWith("gfx/terobjs/boostspeed"))
            return bboxBoost;

        // dual state gobs
//        if (name.endsWith("gate") && name.startsWith("gfx/terobjs/arch")) {
//            GAttrib rd = gob.getattr(ResDrawable.class);
//            if (rd == null)     // shouldn't happen
//                return null;
//            int state = ((ResDrawable) rd).sdt.peekrbuf(0);
//            if (state == 1)     // open gate
//                return null;
//        } else if (name.endsWith("/pow")) {
//            GAttrib rd = gob.getattr(ResDrawable.class);
//            if (rd == null)     // shouldn't happen
//                return null;
//            int state = ((ResDrawable) rd).sdt.peekrbuf(0);
//            if (state == 17 || state == 33) // hf
//                return null;
//        }


//        if (name.endsWith("/smelter"))
//            return bboxSmelter;
//        else if (name.endsWith("brickwallseg") || name.endsWith("brickwallcp") ||
//                name.endsWith("palisadeseg") || name.endsWith("palisadecp") ||
//                name.endsWith("poleseg") || name.endsWith("polecp") ||
//                name.endsWith("drystonewallseg") || name.endsWith("drystonewallcp"))
//            return bboxWallseg;
//        else if (name.endsWith("/hwall"))
//            return bboxHwall;

        if (name.endsWith("/consobj")) {
            ResDrawable rd = gob.getattr(ResDrawable.class);
            if (rd != null && rd.sdt.rbuf.length >= 4) {
                MessageBuf buf = rd.sdt.clone();
                return new BBox[]{new BBox(new Coord(buf.rbuf[0], buf.rbuf[1]), new Coord(buf.rbuf[2], buf.rbuf[3]))};
            }
        }

        Resource.Neg neg = res.layer(Resource.Neg.class);
        if (neg == null) {
            for (RenderLink.Res link : res.layers(RenderLink.Res.class)) {
//                if (link.mesh().isPresent()) {
//                    neg = link.mesh().get().layer(Resource.Neg.class);
//                    break;
//                }
                try {
                    RenderLink l = link.l;
                    if (l instanceof RenderLink.MeshMat) {
                        RenderLink.MeshMat mm = (RenderLink.MeshMat) l;
                        if (mm.srcres != null) {
                            Resource.Neg ng = mm.srcres.layer(Resource.Neg.class);
                            if (ng != null) {
                                neg = ng;
                                break;
                            }
                        }
                        if (mm.mesh != null) {
                            Resource.Neg ng = mm.mesh.get().layer(Resource.Neg.class);
                            if (ng != null) {
                                neg = ng;
                                break;
                            }
                        }
//                        if (mm.mat != null) {
//                            Resource.Neg ng = mm.mat.get().layer(Resource.Neg.class);
//                            if (ng != null) {
//                                neg = ng;
//                                break;
//                            }
//                        }
                    }
//                    if (l instanceof RenderLink.AmbientLink) {
//                        RenderLink.AmbientLink al = (RenderLink.AmbientLink) l;
//                        if (al.res != null) {
//                            Resource.Neg ng = al.res.get().layer(Resource.Neg.class);
//                            if (ng != null) {
//                                neg = ng;
//                                break;
//                            }
//                        }
//                    }
//                    if (l instanceof RenderLink.Collect) {
//                        RenderLink.Collect cl = (RenderLink.Collect) l;
//                        if (cl.from != null) {
//                            Resource.Neg ng = cl.from.get().layer(Resource.Neg.class);
//                            if (ng != null) {
//                                neg = ng;
//                                break;
//                            }
//                        }
//                    }
//                    if (l instanceof RenderLink.Parameters) {
//                        RenderLink.Parameters pl = (RenderLink.Parameters) l;
//                        if (pl.res != null) {
//                            Resource.Neg ng = pl.res.get().layer(Resource.Neg.class);
//                            if (ng != null) {
//                                neg = ng;
//                                break;
//                            }
//                        }
//                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        List<Resource.Obst> obsts = new ArrayList<>(res.layers(Resource.Obst.class));
        if (obsts.size() == 0) {
            for (RenderLink.Res link : res.layers(RenderLink.Res.class)) {
//                if (link.mesh().isPresent()) {
//                    obst = link.mesh().get().layer(Resource.Obst.class);
//                    break;
//                }
                try {
                    RenderLink l = link.l;
                    if (l instanceof RenderLink.MeshMat) {
                        RenderLink.MeshMat mm = (RenderLink.MeshMat) l;
                        if (mm.srcres != null) {
                            Resource.Obst ng = mm.srcres.layer(Resource.Obst.class);
                            if (ng != null) {
                                obsts.add(ng);
                                break;
                            }
                        } else if (mm.mesh != null && mm.mesh.get() != null) {
                            Resource.Obst ng = mm.mesh.get().layer(Resource.Obst.class);
                            if (ng != null) {
                                obsts.add(ng);
                                break;
                            }
                        } //else if (mm.mat != null && mm.mat.get() != null) {
//                            Resource.Obst ng = mm.mat.get().layer(Resource.Obst.class);
//                            if (ng != null) {
//                                obsts.add(ng);
//                                break;
//                            }
//                        }
                    } //else if (l instanceof RenderLink.AmbientLink) {
//                        RenderLink.AmbientLink al = (RenderLink.AmbientLink) l;
//                        if (al.res != null && al.res.get() != null) {
//                            Resource.Obst ng = al.res.get().layer(Resource.Obst.class);
//                            if (ng != null) {
//                                obsts.add(ng);
//                                break;
//                            }
//                        }
//                    } else if (l instanceof RenderLink.Collect) {
//                        RenderLink.Collect cl = (RenderLink.Collect) l;
//                        if (cl.from != null && cl.from.get() != null) {
//                            Resource.Obst ng = cl.from.get().layer(Resource.Obst.class);
//                            if (ng != null) {
//                                obsts.add(ng);
//                                break;
//                            }
//                        }
//                    } else if (l instanceof RenderLink.Parameters) {
//                        RenderLink.Parameters pl = (RenderLink.Parameters) l;
//                        if (pl.res != null && pl.res.get() != null) {
//                            Resource.Obst ng = pl.res.get().layer(Resource.Obst.class);
//                            if (ng != null) {
//                                obsts.add(ng);
//                                break;
//                            }
//                        }
//
//                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        List<BBox> bBoxes = new ArrayList<>();
        if (obsts.size() > 0) {
            for (int o = 0; o < obsts.size(); o++)
                for (int i = 0; i < obsts.get(o).ep.length; i++)
                    bBoxes.add(new BBox(obsts.get(o).ep[i]));
        }
        if (neg != null) {
            bBoxes.add(new BBox(neg.bs, neg.bc));
        }

        if (bBoxes.isEmpty()) {
            return (null);
        } else {
            BBox[] boxes = new BBox[bBoxes.size()];
            for (int i = 0; i < bBoxes.size(); i++)
                boxes[i] = bBoxes.get(i);
            return (boxes);
        }
    }
}
