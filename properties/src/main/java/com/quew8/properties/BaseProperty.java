package com.quew8.properties;

import com.quew8.properties.ListenerSet.ListenerHandle;

/**
 * @author Quew8
 */

public abstract class BaseProperty<T> implements Listenable<T> {
    private ListenerSet<PropertyChangeListener<T>> listeners = new ListenerSet<>();

    @Override
    public ListenerHandle<PropertyChangeListener<T>> addListener(PropertyChangeListener<T> listener, boolean notify) {
        ListenerHandle<PropertyChangeListener<T>> lh = listeners.addListener(listener);
        if(notify) {
            T val = getValue();
            listener.onChange(val);
        }
        return lh;
    }

    @Override
    public void removeListener(ListenerHandle<PropertyChangeListener<T>> handle) {
        listeners.removeListener(handle);
    }

    protected void notifyChange(T newVal) {
        listeners.notify((listener) -> listener.onChange(newVal));
    }

    protected <S> ListenerHandle<PropertyChangeListener<S>> dependsOn(Listenable<S> listenable) {
        return listenable.addListener((a) -> notifyChange(getValue()));
    }
}
