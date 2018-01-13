package com.quew8.netcaff.lib.server;

import com.quew8.netcaff.lib.TimeUtil;
import com.quew8.netcaff.lib.ble.util.BLEUtil;

import java.nio.ByteBuffer;

/**
 * @author Quew8
 */

public class Order extends AlignedStruct {
    public static final int SIZE_BYTES = 1;
    private static final int
            ID_OFF = 0, ID_LEN = 4,
            STATUS_OFF = ID_LEN, STATUS_LEN = 2;

    private OrderID id;
    private OrderStatus status;
    private long timeReady;

    private Order(OrderID id, OrderStatus status) {
        this.id = id;
        this.status = status;
        this.timeReady = status == OrderStatus.READ_TO_POUR
                ? TimeUtil.currentTimeMillis()
                : -1;
    }

    Order() {
        this(new OrderID(), OrderStatus.QUEUED);
    }

    OrderID queue(OrderID orderId) {
        this.id = orderId;
        this.status = OrderStatus.QUEUED;
        this.timeReady = -1;
        doCheck();
        return this.id;
    }

    void making() {
        this.status = OrderStatus.BEING_MADE;
        this.timeReady = -1;
        doCheck();
    }

    void readyToPour() {
        this.status = OrderStatus.READ_TO_POUR;
        this.timeReady = TimeUtil.currentTimeMillis();
        doCheck();
    }

    void pullFrom(Order below) {
        if(below == null) {
            this.id = new OrderID();
            this.status = OrderStatus.QUEUED;
            this.timeReady = -1;
        } else {
            this.id = below.id;
            this.status = below.status;
            this.timeReady = below.timeReady;
            doCheck();
        }
    }

    void reset() {
        this.status = OrderStatus.QUEUED;
        this.timeReady = -1;
        doCheck();
    }

    public OrderID getId() {
        return id;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public long getTimeReady() {
        return timeReady;
    }

    public boolean isTimedOut() {
        return getTimeReady() >= 0 && TimeUtil.diffTimeMillis(getTimeReady()) >= CoffeeServer.READY_COFFEE_TIMEOUT_MS;
    }

    boolean isActive() {
        return !id.isEmpty();
    }

    @Override
    protected String check() {
        if(!isActive()) {
            return "Inactive order";
        }
        if(status == OrderStatus.RESERVED) {
            return "Reserved status(" + status.toString() + ")";
        }
        if(status == OrderStatus.READ_TO_POUR && timeReady < 0) {
            return "Ready to pour order without ready time";
        }
        if(status != OrderStatus.READ_TO_POUR && timeReady >= 0) {
            return "Not ready to pour order with ready time";
        }
        return null;
    }

    @Override
    public void putDataInBuffer(ByteBuffer out) {
        if(out.remaining() < SIZE_BYTES) {
            throw new IllegalArgumentException("At least " + SIZE_BYTES + " are required");
        }
        byte b = 0;
        b = BLEUtil.writeBits(b, (byte) id.getId(), ID_OFF, ID_LEN);
        b = BLEUtil.writeBits(b, (byte) status.code, STATUS_OFF, STATUS_LEN);
        out.put(b);
    }

    @Override
    public void getDataFromBuffer(ByteBuffer in) throws StructureFormatException {
        if(in.remaining() < SIZE_BYTES) {
            throw new StructureFormatException("At least " + SIZE_BYTES + " are required");
        }
        byte b = in.get();
        id = new OrderID(BLEUtil.readBits(b, ID_OFF, ID_LEN));
        status = OrderStatus.fromCode(BLEUtil.readBits(b, STATUS_OFF, STATUS_LEN));
        timeReady = status == OrderStatus.READ_TO_POUR ? 0 : -1;
    }

    @Override
    public int getRequiredBytes() {
        return SIZE_BYTES;
    }

    public String getPrettyString() {
        return "[" + getId().getPrettyString()  + " | " + getStatus().code + "]";
    }

    @Override
    public String toString() {
        return "Order{" + id + ", " + status + "}";
    }
}
