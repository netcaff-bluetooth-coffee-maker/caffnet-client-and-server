package com.quew8.properties.collections;

import java.util.Iterator;

/**
 * @author Quew8
 */
public class ReadOnlyIterator<T> implements Iterator<T> {
    private final Iterator<T> iterator;

    public ReadOnlyIterator(Iterator<T> iterator) {
        this.iterator = iterator;
    }

    protected Iterator<T> getBacking() {
        return iterator;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public T next() {
        return iterator.next();
    }
}
