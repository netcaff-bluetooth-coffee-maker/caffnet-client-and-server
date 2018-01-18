package com.quew8.properties;

import java.util.Map;

/**
 * @author Quew8
 */
public interface ReadOnlyMapProperty<T, S> extends Listenable<Map<T, S>> {
    S get(T key);
    int size();
    boolean containsKey(T key);
    default S getOrDefault(T key, S defaultValue) {
        if(containsKey(key)) {
            return get(key);
        } else {
            return defaultValue;
        }
    }
    default boolean isEmpty() {
        return size() == 0;
    }
}
