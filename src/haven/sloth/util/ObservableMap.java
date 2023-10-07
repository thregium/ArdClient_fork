package haven.sloth.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ObservableMap<K, V> {
    public final Map<K, V> base;
    private final Set<ObservableMapListener<K, V>> listeners = new HashSet<>();

    public ObservableMap(Map<K, V> base) {
        this.base = base;
    }

    public synchronized void put(K key, V val) {
        base.put(key, val);
        listeners.forEach((lst) -> lst.put(key, val));
    }

    public synchronized void putAll(Map<? extends K, ? extends V> m) {
        base.putAll(m);
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            listeners.forEach((lst) -> lst.put(entry.getKey(), entry.getValue()));
        }
    }

    public synchronized void remove(K key) {
        base.remove(key);
        listeners.forEach((lst) -> lst.remove(key));
    }

    public synchronized void clear() {
        Collection<K> keys = keySet();
        base.clear();
        for (K key: keys) {
            listeners.forEach((lst) -> lst.remove(key));
        }
    }

    public Set<Map.Entry<K, V>> entrySet() {
        return base.entrySet();
    }

    public Set<K> keySet() {
        return base.keySet();
    }

    public Collection<V> values() {
        return base.values();
    }

    public V get(K key) {
        return base.get(key);
    }

    public V getOrDefault(K key, V def) {
        return base.getOrDefault(key, def);
    }

    public boolean containsKey(K key) {
        return base.containsKey(key);
    }

    public void addListener(final ObservableMapListener<K, V> listener) {
        listeners.add(listener);
        listener.init(base);
    }

    public int size() {
        return base.size();
    }

    public void removeListener(final ObservableMapListener<K, V> listener) {
        listeners.remove(listener);
    }
}
