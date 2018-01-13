package com.quew8.properties;

/**
 * @author Quew8
 */

public class IntegerProperty extends BaseProperty<Integer> implements ReadOnlyIntegerProperty {
    private int value;

    public IntegerProperty(int value) {
        this.value = value;
    }

    public void set(int b) {
        if(this.value != b) {
            int oldVal = this.value;
            this.value = b;
            notifyChange(b, oldVal);
        }
    }

    @Override
    public int get() {
        return value;
    }

    @Override
    public void setValue(Integer value) {
        set(value);
    }

    @Override
    public Integer getValue() {
        return get();
    }
}
