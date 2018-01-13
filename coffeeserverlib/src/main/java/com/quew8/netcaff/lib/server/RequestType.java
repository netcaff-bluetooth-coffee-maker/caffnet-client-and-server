package com.quew8.netcaff.lib.server;

/**
 * @author Quew8
 */

public enum RequestType {
    ORDER(0), CANCEL(1), POUR(2), RESERVED(3);

    private RequestType(int code) {
        this.code = code;
    }

    private final int code;

    public int getCode() {
        return code;
    }

    public static RequestType fromCode(int code) {
        for(RequestType rt: RequestType.values()) {
            if(rt.code == code) {
                return rt;
            }
        }
        throw new IllegalArgumentException(Integer.toString(code) + " is not a valid request type");
    }
}
