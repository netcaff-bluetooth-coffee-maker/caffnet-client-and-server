package com.quew8.properties;

import android.support.annotation.NonNull;

import com.quew8.properties.collections.ReadOnlyIterator;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author Quew8
 */
public class ValueListProperty<T> extends BaseProperty<List<T>> implements ReadOnlyListProperty<T>, Iterable<T> {
    private final ArrayList<T> array;
    private final ValueListView view;

    public ValueListProperty() {
        this.array = new ArrayList<>();
        this.view = new ValueListView();
    }

    public void add(T elem) {
        array.add(elem);
        notifyChange(getValue());
    }

    public void addAll(Collection<? extends T> add) {
        array.addAll(add);
        notifyChange(getValue());
    }

    public int indexOf(T elem) {
        for(int i = 0; i < size(); i++) {
            T t = get(i);
            if((t == null && elem == null) || (t != null && t.equals(elem))) {
                return i;
            }
        }
        return -1;
    }

    public T remove(T value) {
        int index = indexOf(value);
        if(index < 0) {
            throw new IllegalArgumentException("This array doesn't contain this value (" + value + ")");
        }
        return removeIndex(index);
    }

    public T removeIndex(int index) {
        T val = array.remove(index);
        notifyChange(getValue());
        return val;
    }

    public void clear() {
        array.clear();
        notifyChange(getValue());
    }

    @Override
    public T get(int index) {
        return getValue().get(index);
    }

    @Override
    public int size() {
        return getValue().size();
    }

    @Override
    public List<T> getValue() {
        return view;
    }

    @NonNull
    @Override
    public Iterator<T> iterator() {
        return new ReadOnlyIterator<>(view.iterator());
    }

    private class ValueListView extends AbstractList<T> {
        @Override
        public T get(int index) {
            return array.get(index);
        }

        @Override
        public int size() {
            return array.size();
        }
    }
}
