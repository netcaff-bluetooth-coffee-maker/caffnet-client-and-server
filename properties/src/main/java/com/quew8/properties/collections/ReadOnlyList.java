package com.quew8.properties.collections;

import android.support.annotation.NonNull;

import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author Quew8
 */
public class ReadOnlyList<T> extends AbstractList<T> {
    private final List<T> list;

    public ReadOnlyList(List<T> list) {
        this.list = list;
    }

    protected List<T> getBacking() {
        return list;
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return list.contains(o);
    }

    @Override
    @NonNull
    public Iterator<T> iterator() {
        return new ReadOnlyIterator<>(list.iterator());
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
        return list.containsAll(c);
    }

    @Override
    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    @Override
    @NonNull
    public List<T> subList(int fromIndex, int toIndex) {
        return new ReadOnlyList<>(list.subList(fromIndex, toIndex));
    }

    @Override
    public T get(int index) {
        return list.get(index);
    }

    @Override
    public int size() {
        return list.size();
    }
}
