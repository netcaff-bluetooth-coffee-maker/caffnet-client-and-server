package com.quew8.netcaff.lib.server;

import com.quew8.netcaff.lib.ble.util.BLEUtil;

import java.nio.ByteBuffer;

/**
 * @author Quew8
 */

public class Request extends CharacteristicStruct {
    public static final int SIZE_BYTES = 1 + UserAccessCode.SIZE_BYTES;
    private static final int
            ID_OFF = 0, ID_LEN = 4,
            CODE_OFF = ID_LEN, CODE_LEN = 2;

    private OrderID orderId;
    private RequestType request;
    private UserAccessCode accessCode;

    private Request(OrderID orderId, RequestType request, UserAccessCode accessCode) {
        this.orderId = orderId;
        this.request = request;
        this.accessCode = accessCode;
    }

    Request() {
        this(new OrderID(), RequestType.CANCEL, new UserAccessCode());
    }

    public void set(OrderID orderId, RequestType request, UserAccessCode accessCode) {
        if(orderId != null) {
            this.orderId = orderId;
            orderId.doCheck();
        } else {
            this.orderId = new OrderID();
        }
        this.request = request;
        this.accessCode = accessCode;
        accessCode.doCheck();
        doCheck();
        set();
    }

    public void setOrder(UserAccessCode accessCode) {
        set(null, RequestType.ORDER, accessCode);
    }

    public void setPour(OrderID orderID, UserAccessCode accessCode) {
        set(orderID, RequestType.POUR, accessCode);
    }

    public void setCancel(OrderID orderID, UserAccessCode accessCode) {
        set(orderID, RequestType.CANCEL, accessCode);
    }

    public OrderID getOrderId() {
        return orderId;
    }

    public RequestType getRequestType() {
        return request;
    }

    public UserAccessCode getAccessCode() {
        return accessCode;
    }

    @Override
    protected String check() {
        if(request == RequestType.RESERVED) {
            return "Reserved request code (" + RequestType.RESERVED.getCode() + ")";
        }
        if(orderId.isEmpty() && request != RequestType.ORDER) {
            return "An order id is required for a " + request.toString() + " order (" + this.toString() + ")";
        }
        if(!orderId.isEmpty() && request == RequestType.ORDER) {
            return "An order id cannot be specified for a " + RequestType.ORDER.toString() + " order (" + this.toString() + ")";
        }
        return null;
    }

    @Override
    public void putDataInBuffer(ByteBuffer out) {
        if(out.remaining() < SIZE_BYTES) {
            throw new IllegalArgumentException("At least " + SIZE_BYTES + " are required");
        }
        byte b = 0;
        b = BLEUtil.writeBits(b, (byte) orderId.getId(), ID_OFF, ID_LEN);
        b = BLEUtil.writeBits(b, (byte) request.getCode(), CODE_OFF, CODE_LEN);
        out.put(b);
        accessCode.putDataInBuffer(out);
    }

    @Override
    public void getDataFromBuffer(ByteBuffer in) throws StructureFormatException {
        if(in.remaining() < SIZE_BYTES) {
            throw new StructureFormatException("At least " + SIZE_BYTES + " are required");
        }
        byte b = in.get();
        orderId = new OrderID(BLEUtil.readBits(b, ID_OFF, ID_LEN));
        request = RequestType.fromCode(BLEUtil.readBits(b, CODE_OFF, CODE_LEN));
        accessCode.readFromBuffer(in);
    }

    @Override
    public int getRequiredBytes() {
        return SIZE_BYTES;
    }

    @Override
    public String getPrettyString() {
        return "[" + getOrderId().getPrettyString() + " | " + Integer.toString(getRequestType().getCode()) + "] | "
                + accessCode.toHexString();
    }

    @Override
    public String toString() {
        return "Request{" +
                "orderId=" + orderId +
                ", request=" + request +
                ", accessCode=" + accessCode +
                '}';
    }
}
