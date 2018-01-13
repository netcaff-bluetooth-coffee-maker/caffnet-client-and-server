package com.quew8.netcaff.lib.server;

/**
 * @author Quew8
 */

public abstract class Struct {
    public void doCheck() {
        String msg = check();
        if(msg != null) {
            throw new IllegalArgumentException(msg);
        }
    }

    protected abstract String check();
}
