package com.quew8.properties;

/**
 * @author Quew8
 */

public class BooleanProperty extends BaseProperty<Boolean> implements ReadOnlyBooleanProperty {
    private boolean value;

    public BooleanProperty(boolean value) {
        this.value = value;
    }

    public void set(boolean b) {
        if(this.value != b) {
            boolean oldVal = this.value;
            this.value = b;
            notifyChange(b, oldVal);
        }
    }

    @Override
    public boolean get() {
        return value;
    }

    @Override
    public void setValue(Boolean value) {
        set(value);
    }

    @Override
    public Boolean getValue() {
        return get();
    }
}
