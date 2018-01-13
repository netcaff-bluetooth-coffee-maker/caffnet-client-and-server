package com.quew8.properties;

/**
 * @author Quew8
 */

public interface PropertyChangeListener<T> {
    void onChange(T newVal, T oldVal);
}
