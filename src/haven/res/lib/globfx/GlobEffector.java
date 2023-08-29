package haven.res.lib.globfx;

import haven.Coord2d;
import haven.Coord3f;
import haven.Drawable;
import haven.GLState;
import haven.Glob;
import haven.Gob;
import haven.RenderList;
import haven.Resource;
import haven.Sprite;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public class GlobEffector extends Drawable {
    /* Keep weak references to the glob-effectors themselves, or
     * GlobEffector.glob (and GlobEffector.gob.glob) will keep the
     * globs alive through the strong value references forever. */
    static Map<Glob, Reference<GlobEffector>> cur = new WeakHashMap<Glob, Reference<GlobEffector>>();
    public final Glob glob;
    public Collection<Gob> holder = null;
    Map<Effect, Effect> effects = new HashMap<Effect, Effect>();
    Map<Datum, Datum> data = new HashMap<Datum, Datum>();

    public GlobEffector(Gob gob) {
        super(gob);
        this.glob = gob.glob;
    }

    public void setup(RenderList rl) {
        synchronized (effects) {
            for (Effect spr : effects.values())
                rl.add(spr, null);
        }
    }

    public Object staticp() {
        return (null);
    }

    public void ctick(int idt) {
        float dt = idt * 0.001f;
        synchronized (effects) {
            for (Iterator<Effect> i = effects.values().iterator(); i.hasNext(); ) {
                Effect spr = i.next();
                if (spr.tick(dt))
                    i.remove();
            }
            for (Iterator<Datum> i = data.values().iterator(); i.hasNext(); ) {
                Datum d = i.next();
                if (d.tick(dt))
                    i.remove();
            }
        }
        synchronized (cur) {
            if ((effects.size() == 0) && (data.size() == 0)) {
                glob.oc.lrem(holder);
                cur.remove(glob);
            }
        }
    }

    public Resource getres() {
        return (null);
    }

    private <T> T create(Class<T> fx) {
        Resource res = Resource.classres(fx);
        try {
            try {
                Constructor<T> cons = fx.getConstructor(Sprite.Owner.class, Resource.class);
                return (cons.newInstance(gob, res));
            } catch (NoSuchMethodException e) {
            }
            throw (new RuntimeException("No valid constructor found for global effect " + fx));
        } catch (InstantiationException e) {
            throw (new RuntimeException(e));
        } catch (IllegalAccessException e) {
            throw (new RuntimeException(e));
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException)
                throw ((RuntimeException) e.getCause());
            throw (new RuntimeException(e));
        }
    }

    public Object monitor() {
        return (this.gob);
    }

    @SuppressWarnings("unchecked")
    public <T extends Effect> T get(T fx) {
        synchronized (this.gob) {
            T ret = (T) effects.get(fx);
            if (ret == null)
                effects.put(ret = fx, fx);
            return (ret);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Datum> T getdata(T fx) {
        synchronized (this.gob) {
            T ret = (T) data.get(fx);
            if (ret == null)
                data.put(ret = fx, fx);
            return (ret);
        }
    }

    private static GlobEffector get(Glob glob) {
        Collection<Gob> add = null;
        GlobEffector ret;
        synchronized (cur) {
            Reference<GlobEffector> ref = cur.get(glob);
            ret = (ref == null) ? null : ref.get();
            if (ret == null) {
                Gob hgob = new Gob(glob, Coord2d.z) {
                    public Coord3f getc() {
                        return (Coord3f.o);
                    }

                    public GLState getmapstate(Coord3f pc) {
                        return (null);
                    }
                };
                GlobEffector ne = new GlobEffector(hgob);
                hgob.setattr(ne);
                add = ne.holder = Collections.singleton(hgob);
                cur.put(glob, new WeakReference<GlobEffector>(ret = ne));
            }

        }
        if (add != null)
            glob.oc.ladd(add);
        return (ret);
    }

    public static <T extends Effect> T get(Glob glob, T fx) {
        return (get(glob).get(fx));
    }

    public static <T extends Datum> T getdata(Glob glob, T fx) {
        return (get(glob).getdata(fx));
    }
}