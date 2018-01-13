package com.quew8.properties;

/**
 * @author Quew8
 */

public class LongProperty extends BaseProperty<Long> implements ReadOnlyLongProperty {
    private long value;

    public LongProperty(long value) {
        this.value = value;
    }

    public void set(long b) {
        if(this.value != b) {
            long oldVal = this.value;
            this.value = b;
            notifyChange(b, oldVal);
        }
    }

    @Override
    public long get() {
        return value;
    }

    @Override
    public void setValue(Long value) {
        set(value);
    }

    @Override
    public Long getValue() {
        return get();
    }
}
