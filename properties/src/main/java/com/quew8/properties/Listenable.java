package com.quew8.properties;

import com.quew8.properties.ListenerSet.ListenerHandle;

/**
 * @author Quew8
 */
public interface Listenable<T> {
    ListenerHandle<PropertyChangeListener<T>> addListener(PropertyChangeListener<T> listener, boolean notify);
    default ListenerHandle<PropertyChangeListener<T>> addListener(PropertyChangeListener<T> listener) {
        return addListener(listener, false);
    }
    void removeListener(ListenerHandle<PropertyChangeListener<T>> handle);
    T getValue();
}
