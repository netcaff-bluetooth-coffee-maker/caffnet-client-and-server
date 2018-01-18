package com.quew8.properties.collections;

import android.support.annotation.NonNull;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author Quew8
 */
public class ReadOnlyMap<T, S> extends AbstractMap<T, S> {
    private final Map<T, S> map;

    public ReadOnlyMap(Map<T, S> map) {
        this.map = map;
    }

    protected Map<T, S> getBacking() {
        return map;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public S get(Object key) {
        return map.get(key);
    }

    @Override
    @NonNull
    public Set<T> keySet() {
        return map.keySet();
    }

    @Override
    @NonNull
    public Collection<S> values() {
        return map.values();
    }

    @Override
    @NonNull
    public Set<Entry<T, S>> entrySet() {
        return new ReadOnlySet<>(map.entrySet());
    }
}
