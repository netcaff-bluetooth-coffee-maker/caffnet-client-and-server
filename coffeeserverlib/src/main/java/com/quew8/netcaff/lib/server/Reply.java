package com.quew8.netcaff.lib.server;

import com.quew8.netcaff.lib.ble.util.BLEUtil;

import java.nio.ByteBuffer;

/**
 * @author Quew8
 */

public class Reply extends CharacteristicStruct {
    private static final int SHORT_SIZE_BYTES = 1, FULL_SIZE_BYTES = 1 + Duration.SIZE_BYTES;
    private static final int
            ID_OFF = 0, ID_LEN = 4,
            CODE_OFF = ID_LEN, CODE_LEN = 2;

    private OrderId orderId;
    private ReplyType reply;
    private Duration duration;
    private boolean longForm = false;

    private Reply(OrderId orderId, ReplyType reply, Duration duration) {
        this.orderId = orderId;
        this.reply = reply;
        this.duration = duration;
    }

    Reply() {
        this(new OrderId(), ReplyType.ERROR, new Duration());
    }

    public void set(OrderId orderId, ReplyType reply, Duration duration) {
        if(orderId != null) {
            this.orderId = orderId;
            orderId.doCheck();
        }
        this.reply = reply;
        if(duration != null) {
            this.duration = duration;
            this.duration.doCheck();
            this.longForm = true;
        } else {
            this.longForm = false;
        }
        doCheck();
        set();
    }

    public void set(OrderId orderId, ReplyType reply) {
        this.set(orderId, reply, null);
    }

    public void setError(ReplyType type) {
        this.set(null, type, null);
    }

    protected String check() {
        if(reply == ReplyType.OK && orderId.isEmpty()) {
            return "No order id for a positive response";
        }
        return null;
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public ReplyType getReply() {
        return reply;
    }

    public Duration getDuration() {
        return duration;
    }

    public boolean isLongForm() {
        return longForm;
    }

    @Override
    public void putDataInBuffer(ByteBuffer out) {
        if(out.remaining() < getRequiredBytes()) {
            throw new IllegalArgumentException("At least " + getRequiredBytes() + " are required");
        }
        byte b = 0;
        b = BLEUtil.writeBits(b, (byte) orderId.getId(), ID_OFF, ID_LEN);
        b = BLEUtil.writeBits(b, (byte) reply.getCode(), CODE_OFF, CODE_LEN);
        out.put(b);
        if(longForm) {
            duration.putDataInBuffer(out);
        }
    }

    @Override
    public void getDataFromBuffer(ByteBuffer in) throws StructureFormatException {
        if(in.remaining() < SHORT_SIZE_BYTES) {
            throw new StructureFormatException("At least " + SHORT_SIZE_BYTES + " are required");
        }
        byte b = in.get();
        orderId = new OrderId(BLEUtil.readBits(b, ID_OFF, ID_LEN));
        reply = ReplyType.fromCode(BLEUtil.readBits(b, CODE_OFF, CODE_LEN));
        if(in.remaining() < Duration.SIZE_BYTES) {
            longForm = false;
        } else {
            longForm = true;
            duration.readFromBuffer(in);
        }
    }

    @Override
    public int getRequiredBytes() {
        if(longForm) {
            return FULL_SIZE_BYTES;
        } else {
            return SHORT_SIZE_BYTES;
        }
    }

    @Override
    public String getPrettyString() {
        String s = "[" + getOrderId().getPrettyString() + " | " + getReply().getPrettyString() + "]";
        if(isLongForm()) {
            s += " | " + getDuration().getPrettyString();
        }
        return s;
    }

    @Override
    public String toString() {
        return "Reply{" +
                "orderId=" + orderId +
                ", reply=" + reply +
                ", duration=" + duration +
                ", longForm=" + longForm +
                '}';
    }
}
