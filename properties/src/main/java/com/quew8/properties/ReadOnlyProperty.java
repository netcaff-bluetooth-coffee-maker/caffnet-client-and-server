package com.quew8.properties;

/**
 * @author Quew8
 */

public interface ReadOnlyProperty<T> extends Listenable<T> {
    public T get();
}
