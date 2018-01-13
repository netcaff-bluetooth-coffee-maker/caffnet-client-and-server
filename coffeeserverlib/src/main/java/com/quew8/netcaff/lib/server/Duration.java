package com.quew8.netcaff.lib.server;

import com.quew8.netcaff.lib.TimeUtil;

import java.nio.ByteBuffer;

/**
 * @author Quew8
 */

public class Duration extends AlignedStruct {
    public static final int SIZE_BYTES = 2;
    /* REPRESENTED IN 10s of seconds */
    private static final int CONVERSION = 10 * 1000;

    private int duration;

    private Duration(int duration) {
        this.duration = duration;
    }

    Duration() {
        this(0);
    }

    private int getMillis() {
        return duration * CONVERSION;
    }

    public long getAbsoluteTime() {
        return TimeUtil.currentTimeMillis() + getMillis();
    }

    @Override
    protected String check() {
        if(duration < 0) {
            return "Negative duration";
        }
        if(duration > Short.MAX_VALUE) {
            return "Duration greater than representable values";
        }
        return null;
    }

    @Override
    public void putDataInBuffer(ByteBuffer out) {
        if(out.remaining() < SIZE_BYTES) {
            throw new IllegalArgumentException("At least " + SIZE_BYTES + " are required");
        }
        out.putShort((short) duration);
    }

    @Override
    public void getDataFromBuffer(ByteBuffer in) throws StructureFormatException {
        if(in.remaining() < SIZE_BYTES) {
            throw new StructureFormatException("At least " + SIZE_BYTES + " are required");
        }
        duration = in.getShort();
    }

    @Override
    public int getRequiredBytes() {
        return SIZE_BYTES;
    }

    public String getPrettyString() {
        return Integer.toString(duration);
    }

    @Override
    public String toString() {
        return "Duration{" +
                "duration=" + duration +
                '}';
    }

    public static Duration fromMS(int ms) {
        return new Duration(ms / CONVERSION);
    }
}
