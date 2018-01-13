package com.quew8.properties;

/**
 * @author Quew8
 */

public interface ReadOnlyBooleanProperty extends Listenable<Boolean> {
    public boolean get();
}
