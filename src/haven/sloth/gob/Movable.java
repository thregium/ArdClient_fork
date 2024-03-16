package haven.sloth.gob;

import haven.BuddyWnd;
import haven.Coord2d;
import haven.DefSettings;
import haven.GAttrib;
import haven.GOut;
import haven.Gob;
import haven.GobPath;
import haven.KinInfo;
import haven.Loading;
import haven.Moving;
import haven.RenderList;
import haven.Rendered;
import haven.States;
import haven.sloth.gfx.GobPathSprite;
import haven.sloth.io.Storage;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

import static haven.DefSettings.ANIMALPATHCOL;
import static haven.DefSettings.GOBPATHCOL;
import static haven.DefSettings.PLAYERPATHCOL;
import static haven.DefSettings.VEHPATHCOL;

public class Movable extends GAttrib implements Rendered {
    public static States.ColState vehiclepathcol;
    public static States.ColState animalpathcol;
    public static States.ColState unknowngobcol;
    public static States.ColState playercol;
    public static final States.ColState[] buddycol;
    private static Set<String> movable = new HashSet<>();

    static {
        //Setup our colors
        vehiclepathcol = new States.ColState(VEHPATHCOL.get());
        unknowngobcol = new States.ColState(GOBPATHCOL.get());
        animalpathcol = new States.ColState(ANIMALPATHCOL.get()); //Animals
        playercol = new States.ColState(PLAYERPATHCOL.get());
        buddycol = new States.ColState[BuddyWnd.gc.length]; //Humans
        IntStream.range(0, buddycol.length).forEach(i -> buddycol[i] = new States.ColState(BuddyWnd.gc[i]));
    }

    public static void init(final Storage internal) {
        internal.ensure(sql -> {
            try (final Statement stmt = sql.createStatement()) {
                try (final ResultSet res = stmt.executeQuery(
                        "SELECT object.name " +
                                "FROM object JOIN move USING (object_id)")) {
                    while (res.next()) {
                        movable.add(res.getString(1));
                    }
                }
            }
        });
    }

    public static boolean isMovable(final String resname) {
        return movable.contains(resname);
    }

    public Movable(final Gob g) {
        super(g);
    }

    private GobPathSprite pathol = null;
    private GobPath path = null;

    public boolean setup(RenderList rl) {
        if (gob.isMoving()) {
            if ((DefSettings.SHOWPLAYERPATH.get() && gob.isplayer()) || (!gob.isplayer() && (gob.type == Type.HUMAN || gob.type == Type.VEHICLE || gob.type == Type.WATERVEHICLE) && DefSettings.SHOWGOBPATH.get()) || ((gob.type == Type.ANIMAL || gob.type == Type.SMALLANIMAL || gob.type == Type.TAMEDANIMAL || gob.type == Type.DANGANIMAL) && DefSettings.SHOWANIMALPATH.get())) {
                if (path == null)
                    path = new GobPath(gob);
                rl.add(path, null);
            }/* else if (pathol != null) {
                if (((gob.type == Type.HUMAN || gob.type == Type.VEHICLE || gob.type == Type.WATERVEHICLE) && DefSettings.SHOWGOBPATH.get()) ||
                        ((gob.type == Type.ANIMAL || gob.type == Type.SMALLANIMAL || gob.type == Type.TAMEDANIMAL || gob.type == Type.DANGANIMAL) && DefSettings.SHOWANIMALPATH.get())) {
                    rl.add(pathol, null);
                }
            } */
        } else if (path != null) {
            path = null;
        }
        return (true);
    }

    @Override
    public void tick() {
        if (true) {
            dispose();
            return;
        }
        if (((gob.type == Type.HUMAN || gob.type == Type.VEHICLE || gob.type == Type.WATERVEHICLE) && DefSettings.SHOWGOBPATH.get()) ||
                ((gob.type == Type.ANIMAL || gob.type == Type.SMALLANIMAL || gob.type == Type.TAMEDANIMAL || gob.type == Type.DANGANIMAL) && DefSettings.SHOWANIMALPATH.get())) {
            Moving mv = gob.getattr(Moving.class);
            if (mv != null) {
                try {
                    mv.getDest().ifPresent(t -> {
                        final Coord2d grc = new Coord2d(gob.getc());
                        if (pathol == null) {
                            //We need a new path setup
                            final States.ColState col;
                            if (gob.type == Type.VEHICLE || gob.type == Type.WATERVEHICLE) {
                                col = vehiclepathcol;
                            } else if (gob.type == Type.ANIMAL || gob.type == Type.SMALLANIMAL || gob.type == Type.TAMEDANIMAL || gob.type == Type.DANGANIMAL) {
                                col = animalpathcol;
                            } else {
                                //Humans, based off kin
                                final KinInfo kin = gob.getattr(KinInfo.class);
                                if (kin != null) {
                                    col = buddycol[kin.group];
                                } else {
                                    col = unknowngobcol;
                                }
                            }
                            double myz;
                            try {
                                myz = gob.glob.map.getcz(gob.rc);
                            } catch (Loading l) {
                                myz = 0;
                            }
                            double oz;
                            try {
                                oz = gob.glob.map.getcz(t);
                            } catch (Loading l) {
                                oz = myz;
                            }
                            pathol = new GobPathSprite(t, grc, (float) grc.dist(t), (float) (oz - myz), col);
                        } else if (!Objects.equals(pathol.dest, t) || !Objects.equals(pathol.rc, grc)) {
                            double myz;
                            try {
                                myz = gob.glob.map.getcz(gob.rc);
                            } catch (Loading l) {
                                myz = 0;
                            }
                            double oz;
                            try {
                                oz = gob.glob.map.getcz(t);
                            } catch (Loading l) {
                                oz = myz;
                            }
                            pathol.update(t, grc, (float) grc.dist(t), (float) (oz - myz));
                        }
                    });
                    if (!mv.getDest().isPresent()) {
                        dispose();
                    }
                } catch (Loading l) {
                    dispose();
                    //Try again another frame, getc() likely error'd
                }
            } else {
                dispose();
            }
        }
    }

    public void dispose() {
        if (pathol != null)
            pathol.dispose();
        pathol = null;
    }

    @Override
    public void draw(final GOut g) {}
}
