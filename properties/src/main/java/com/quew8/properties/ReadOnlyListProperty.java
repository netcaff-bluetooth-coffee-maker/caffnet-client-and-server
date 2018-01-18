package com.quew8.properties;

import java.util.List;

/**
 * @author Quew8
 */
public interface ReadOnlyListProperty<T> extends Listenable<List<T>> {
    T get(int index);
    int size();

    default boolean isEmpty() {
        return size() == 0;
    }
}
