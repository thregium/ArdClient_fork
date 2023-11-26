package haven.sloth.gfx;

import haven.DefSettings;
import haven.FastMesh;
import haven.Gob;
import haven.MapMesh;
import haven.Material;
import haven.RenderList;
import haven.States;
import haven.Utils;
import haven.VertexBuf;
import haven.sloth.script.pathfinding.Hitbox;
import modification.configuration;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//TODO:: The four corners should match the heightmap
public class HitboxMesh extends FastMesh {
    private static States.ColState hiddencolor = new States.ColState(DefSettings.HIDDENCOLOR.get());
    //Lots of hidden sprites will be identical, rather than each gob have there own we'll share
    //sprites that have the same sizes
    private static ConcurrentHashMap<String[], HitboxMesh[]> hbs = new ConcurrentHashMap<>();

    private HitboxMesh(VertexBuf buf, ShortBuffer sa) {
        super(buf, sa);
    }

    public boolean setup(RenderList rl) {
        rl.prepo(Material.nofacecull);
        rl.prepo(MapMesh.postmap);
        rl.prepo(States.vertexcolor);
        return super.setup(rl);
    }

    public synchronized static void updateColor(final States.ColState col) {
        hiddencolor = col;
        updateColor();
    }

    public synchronized static void updateColor() {
//        hbs.forEach((name, mesh) -> mesh.dispose());
        for (Map.Entry<String[], HitboxMesh[]> entry : hbs.entrySet()) {
            for (HitboxMesh mesh : entry.getValue()) {
                mesh.dispose();
            }
        }
        hbs.clear();
    }

    public synchronized static HitboxMesh[] makehb(Gob gob, Hitbox[] hitboxes) {
//        Coord rec, Coord off
//        size and 0
        String[] keys = new String[hitboxes.length];

        for (int h = 0; h < hitboxes.length; h++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < hitboxes[h].points.length; i++) {
                if (i != 0) sb.append(",");
                sb.append(hitboxes[h].points[i]);
            }
            keys[h] = sb.toString();
        }

//        final String[] key = rec + "," + off;

        HitboxMesh[] hb = hbs.get(keys);
        if (hb == null) {
//            rec = rec.add(off).sub(1, 1);
            float h = 2f;
//            float
//                    rx = off.x, ry = off.y,
//                    lx = rec.x, ly = rec.y;
            hb = new HitboxMesh[hitboxes.length];

            Color col = configuration.canCustomColor(gob);
            Color[] colors = new Color[]{col != null ? col : hiddencolor.c, new Color(0, 255, 0, 150), new Color(255, 255, 0, 150)};

            for (int i = 0; i < hitboxes.length; i++) {
                FloatBuffer pa = Utils.mkfbuf(hitboxes[i].points.length * 2 * 3);
                FloatBuffer na = Utils.mkfbuf(hitboxes[i].points.length * 2 * 3);
                FloatBuffer cl = Utils.mkfbuf(hitboxes[i].points.length * 2 * 4);
                ShortBuffer sa = Utils.mksbuf(3 * (hitboxes[i].points.length - 2) + (hitboxes[i].points.length * 2 * 3));

                Color c = colors[i % 3];
                for (int j = 0; j < hitboxes[i].points.length; j++) {
                    pa.put((float) hitboxes[i].points[j].x).put((float) hitboxes[i].points[j].y).put(h + hitboxes[i].zplus);
                    na.put((float) hitboxes[i].points[j].x).put((float) hitboxes[i].points[j].y).put(0f + hitboxes[i].zplus);    //0

                    cl.put(c.getRed() / 255f).put(c.getGreen() / 255f).put(c.getBlue() / 255f).put(c.getAlpha() / 255f);
                }

                for (int j = 0; j < hitboxes[i].points.length; j++) {
                    pa.put((float) hitboxes[i].points[j].x).put((float) hitboxes[i].points[j].y).put(0f + hitboxes[i].zplus);
                    na.put((float) hitboxes[i].points[j].x).put((float) hitboxes[i].points[j].y).put(0f + hitboxes[i].zplus);    //0

                    cl.put(c.getRed() / 255f).put(c.getGreen() / 255f).put(c.getBlue() / 255f).put(c.getAlpha() / 255f);
                }

                for (int j = 0; j < hitboxes[i].points.length - 2; j++) {
                    sa.put((short) 0).put((short) (j + 1)).put((short) (j + 2));
                }

                for (int j = 0; j < hitboxes[i].points.length; j++) {
                    sa.put((short) j).put((short) (j + hitboxes[i].points.length)).put((short) ((j + 1) % hitboxes[i].points.length));
                    sa.put((short) ((j + 1) % hitboxes[i].points.length)).put((short) (hitboxes[i].points.length + (j + 1) % hitboxes[i].points.length)).put((short) (j + hitboxes[i].points.length));
                }
                hb[i] = new HitboxMesh(new VertexBuf(new VertexBuf.VertexArray(pa),
                        new VertexBuf.NormalArray(na),
                        new VertexBuf.ColorArray(cl)),
                        sa);
//                    //Top verts
//                    pa.put(lx).put(ly).put(h);
//                    na.put(lx).put(ly).put(0f);    //0
//                    pa.put(lx).put(ry).put(h);
//                    na.put(lx).put(ry).put(0f);    //1
//                    pa.put(rx).put(ry).put(h);
//                    na.put(rx).put(ry).put(0f);    //2
//                    pa.put(rx).put(ly).put(h);
//                    na.put(rx).put(ly).put(0f);    //3
//                    //bottom verts
//                    pa.put(lx).put(ly).put(0f);
//                    na.put(lx).put(ly).put(0f);    //4 under 0
//                    pa.put(lx).put(ry).put(0f);
//                    na.put(lx).put(ry).put(0f);    //5 under 1
//                    pa.put(rx).put(ry).put(0f);
//                    na.put(rx).put(ry).put(0f);    //6 under 2
//                    pa.put(rx).put(ly).put(0f);
//                    na.put(rx).put(ly).put(0f);    //7 under 3

//                //Each vert is given the same color
//                cl.put(hiddencolor.ca[0]).put(hiddencolor.ca[1]).put(hiddencolor.ca[2]).put(hiddencolor.ca[3]);
//                cl.put(hiddencolor.ca[0]).put(hiddencolor.ca[1]).put(hiddencolor.ca[2]).put(hiddencolor.ca[3]);
//                cl.put(hiddencolor.ca[0]).put(hiddencolor.ca[1]).put(hiddencolor.ca[2]).put(hiddencolor.ca[3]);
//                cl.put(hiddencolor.ca[0]).put(hiddencolor.ca[1]).put(hiddencolor.ca[2]).put(hiddencolor.ca[3]);
//                cl.put(hiddencolor.ca[0]).put(hiddencolor.ca[1]).put(hiddencolor.ca[2]).put(hiddencolor.ca[3]);
//                cl.put(hiddencolor.ca[0]).put(hiddencolor.ca[1]).put(hiddencolor.ca[2]).put(hiddencolor.ca[3]);
//                cl.put(hiddencolor.ca[0]).put(hiddencolor.ca[1]).put(hiddencolor.ca[2]).put(hiddencolor.ca[3]);
//                cl.put(hiddencolor.ca[0]).put(hiddencolor.ca[1]).put(hiddencolor.ca[2]).put(hiddencolor.ca[3]);

//                //Top
//                sa.put((short) 0).put((short) 1).put((short) 2);
//                sa.put((short) 0).put((short) 2).put((short) 3);
//                //left 0-1 4-5
//                sa.put((short) 0).put((short) 4).put((short) 1);
//                sa.put((short) 1).put((short) 5).put((short) 4);
//                //right 2-3 6-7
//                sa.put((short) 2).put((short) 6).put((short) 3);
//                sa.put((short) 3).put((short) 7).put((short) 6);
//                //front 1-2 5-6
//                sa.put((short) 1).put((short) 5).put((short) 2);
//                sa.put((short) 2).put((short) 6).put((short) 5);
//                //back 0-3 4-7
//                sa.put((short) 0).put((short) 4).put((short) 3);
//                sa.put((short) 3).put((short) 7).put((short) 4);
            }
            hbs.put(keys, hb);
        }
        return hb;
    }
}
