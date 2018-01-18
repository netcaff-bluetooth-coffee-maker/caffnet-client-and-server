package com.quew8.properties.collections;

import android.support.annotation.NonNull;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Quew8
 */
public class ReadOnlySet<T> extends AbstractSet<T> {
    private final Set<T> set;

    public ReadOnlySet(Set<T> set) {
        this.set = set;
    }

    protected Set<T> getBacking() {
        return set;
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return set.contains(o);
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
        return set.containsAll(c);
    }

    @Override
    @NonNull
    public Iterator<T> iterator() {
        return new ReadOnlyIterator<>(set.iterator());
    }

    @Override
    public int size() {
        return set.size();
    }
}
