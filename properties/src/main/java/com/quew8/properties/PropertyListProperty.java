package com.quew8.properties;

import android.support.annotation.NonNull;

import com.quew8.properties.collections.ReadOnlyIterator;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Quew8
 */
public class PropertyListProperty<T extends Listenable<T>> extends BaseProperty<List<T>>
        implements ReadOnlyListProperty<T>, Iterable<T> {

    private final ArrayList<PropertyListElement> array;
    private final PropertyListView view;

    public PropertyListProperty() {
        this.array = new ArrayList<>();
        this.view = new PropertyListView();
    }

    public void add(T elem) {
        array.add(new PropertyListElement(elem));
        notifyChange(getValue());
    }

    public T removeIndex(int index) {
        PropertyListElement elem = array.remove(index);
        elem.remove();
        notifyChange(getValue());
        return elem.value;
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

    private class PropertyListElement {
        private T value;
        private ListenerSet.ListenerHandle<PropertyChangeListener<T>> listenerHandle;

        private PropertyListElement(T value) {
            this.value = value;
            this.listenerHandle= PropertyListProperty.this.dependsOn(value);
        }

        private void remove() {
            value.removeListener(listenerHandle);
        }
    }

    private class PropertyListView extends AbstractList<T> {
        @Override
        public T get(int index) {
            return array.get(index).value;
        }

        @Override
        public int size() {
            return array.size();
        }
    }
}
