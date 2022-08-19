package haven.sloth.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ObservableCollection<T> implements Iterable<T> {
    private final Collection<T> base;
    private final Set<ObservableListener<T>> listeners = new HashSet<>();

    public ObservableCollection(Collection<T> base) {
        this.base = base;
    }

    public boolean add(T item) {
        boolean added;
        synchronized (base) {
            added = base.add(item);
        }
        if (added) {
            synchronized (listeners) {
                if (item != null)
                    listeners.forEach((lst) -> lst.added(item));
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean edit(T olditem, T newitem) {
        if (replaceItem(olditem, newitem)) {
            synchronized (listeners) {
                listeners.forEach((lst) -> lst.edited(olditem, newitem));
            }
            return true;
        }
        return false;
    }

    public boolean remove(T item) {
        boolean removed;
        synchronized (base) {
            removed = base.remove(item);
        }
        if (removed) {
            synchronized (listeners) {
                listeners.forEach((lst) -> lst.remove(item));
            }
            return true;
        } else {
            return false;
        }
    }

    public int size() {
        synchronized (base) {
            return base.size();
        }
    }


    public boolean contains(T other) {
        synchronized (base) {
            return base.contains(other);
        }
    }

    public void addListener(final ObservableListener<T> listener) {
        synchronized (listeners) {
            if (listener != null) {
                listeners.add(listener);
                synchronized (base) {
                    listener.init(base);
                }
            }
        }
    }

    public void removeListener(final ObservableListener<T> listener) {
        synchronized (listeners) {
            if (listener != null)
                listeners.remove(listener);
        }
    }

    public Iterator<T> iterator() {
        synchronized (base) {
            return base.iterator();
        }
    }

    public boolean replaceItem(T olditem, T newitem) {
        int n = 0;
        boolean s = false;
        synchronized (base) {
            for (T item : base) {
                if (item.equals(olditem)) {
                    base.remove(item);
                    s = true;
                    break;
                }
                n++;
            }
        }
        if (!s) return false;

        List<T> newbase;
        synchronized (base) {
            newbase = new ArrayList<>(base);
        }
        newbase.add(n, newitem);

        synchronized (base) {
            base.clear();
            if (!base.addAll(newbase)) return false;
        }
        return true;
    }
}
