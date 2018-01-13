package com.quew8.properties;

/**
 * @author Quew8
 */

public class Property<T> extends BaseProperty<T> implements ReadOnlyProperty<T> {
    private T value;

    public Property(T value) {
        this.value = value;
    }

    public void set(T b) {
        if(this.value != b) {
            T oldVal = this.value;
            this.value = b;
            notifyChange(b, oldVal);
        }
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public void setValue(T value) {
        set(value);
    }

    @Override
    public T getValue() {
        return get();
    }
}
