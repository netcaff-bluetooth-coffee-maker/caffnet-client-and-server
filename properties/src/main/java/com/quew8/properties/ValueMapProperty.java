package com.quew8.properties;

import android.support.annotation.NonNull;

import com.quew8.properties.collections.ReadOnlyIterator;
import com.quew8.properties.collections.ReadOnlyMap;
import com.quew8.properties.collections.ReadOnlySet;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author Quew8
 */
public class ValueMapProperty<T, S> extends BaseProperty<Map<T, S>> implements ReadOnlyMapProperty<T, S> {
    private final HashMap<T, S> map;
    private final ReadOnlyMap<T, S> view;

    public ValueMapProperty() {
        this.map = new HashMap<>();
        this.view = new ReadOnlyMap<>(this.map);
    }

    @Override
    public S get(T key) {
        return map.get(key);
    }

    @Override
    public int size() {
        return map.size();
    }

    public S put(T key, S value) {
        S val = map.put(key, value);
        notifyChange(getValue());
        return val;
    }

    public Set<Map.Entry<T, S>> entrySet() {
        return new ValueMapPropertyEntrySet(map.entrySet());
    }

    @Override
    public boolean containsKey(T key) {
        return map.containsKey(key);
    }

    @Override
    public Map<T, S> getValue() {
        return view;
    }

    private class ValueMapPropertyEntrySet extends ReadOnlySet<Map.Entry<T, S>> {

        private ValueMapPropertyEntrySet(Set<Map.Entry<T, S>> set) {
            super(set);
        }

        @NonNull
        @Override
        public Iterator<Map.Entry<T, S>> iterator() {
            return new ValueMapPropertyEntrySetIterator(getBacking().iterator());
        }
    }

    private class ValueMapPropertyEntrySetIterator extends ReadOnlyIterator<Map.Entry<T, S>> {

        private ValueMapPropertyEntrySetIterator(Iterator<Map.Entry<T, S>> iterator) {
            super(iterator);
        }

        @Override
        public void remove() {
            getBacking().remove();
            notifyChange(getValue());
        }
    }
}
