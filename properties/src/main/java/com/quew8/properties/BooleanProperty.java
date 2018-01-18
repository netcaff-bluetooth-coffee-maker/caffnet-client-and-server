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
            this.value = b;
            notifyChange(b);
        }
    }

    @Override
    public boolean get() {
        return value;
    }

    @Override
    public Boolean getValue() {
        return get();
    }
}
