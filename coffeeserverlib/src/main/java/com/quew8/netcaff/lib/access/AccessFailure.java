package com.quew8.netcaff.lib.access;

/**
 * @author Quew8
 */
public enum AccessFailure {
    USERNAME_PASSWORD_INCORRECT(1), TOKEN_EXPIRED(2), TOKEN_INVALID(3);

    AccessFailure(int code) {
        this.code = code;
    }

    private final int code;

    public int getCode() {
        return code;
    }

    public static AccessFailure fromCodeNoThrow(int code) {
        for(AccessFailure af: AccessFailure.values()) {
            if(af.getCode() == code) {
                return af;
            }
        }
        return null;
    }
}
