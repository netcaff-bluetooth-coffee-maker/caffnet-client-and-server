package com.quew8.netcaff.lib.server;

/**
 * @author Quew8
 */
public enum OrderStatus {
    QUEUED(0),
    BEING_MADE(1),
    READ_TO_POUR(2),
    RESERVED(3);

    OrderStatus(int code) {
        this.code = code;
    }

    public final int code;

    public static OrderStatus fromCode(int code) {
        for(OrderStatus or: OrderStatus.values()) {
            if(or.code == code) {
                return or;
            }
        }
        throw new IllegalArgumentException(Integer.toString(code) + " is not a valid status");
    }
}