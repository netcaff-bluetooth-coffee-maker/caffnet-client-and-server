package com.quew8.netcaff.lib.server;

/**
 * @author Quew8
 */

public enum ReplyType {
    OK(0), LEVELS_LOW(1), AUTH_FAILED(2), ERROR(3);

    ReplyType(int code) {
        this.code = code;
    }

    private final int code;

    public int getCode() {
        return code;
    }

    public String getPrettyString() {
        return String.format("%x (%s)", getCode(), name());
    }

    public static ReplyType fromCode(int code) {
        for(ReplyType rt: ReplyType.values()) {
            if(rt.code == code) {
                return rt;
            }
        }
        throw new IllegalArgumentException(Integer.toString(code) + " is not a valid reply type");
    }
}
