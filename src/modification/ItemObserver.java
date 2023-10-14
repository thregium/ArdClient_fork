package modification;

import java.util.List;

public interface ItemObserver {
    List<InventoryListener> observers();
    void addListeners(final List<InventoryListener> listeners);
    void removeListeners(final List<InventoryListener> listeners);
}
