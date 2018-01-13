package com.quew8.netcaff.lib.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * @author Quew8
 */

public class Level extends AlignedStruct {
    public static final int SIZE_BYTES = 1;

    private int level;

    private Level(int level) {
        this.level = level;
    }

    Level() {
        this(0);
    }

    public int getValue() {
        return level;
    }

    @Override
    protected String check() {
        if(level < 0) {
            return "Negative level";
        }
        if(level > Byte.MAX_VALUE) {
            return "Level greater than representable value";
        }
        return null;
    }

    @Override
    public void putDataInBuffer(ByteBuffer out) {
        if(out.remaining() < SIZE_BYTES) {
            throw new IllegalArgumentException("At least " + SIZE_BYTES + " bytes are required");
        }
        out.put((byte) level);
    }

    @Override
    public void getDataFromBuffer(ByteBuffer in) throws StructureFormatException {
        if(in.remaining() < SIZE_BYTES) {
            throw new StructureFormatException("At least " + SIZE_BYTES + " bytes are required");
        }
        level = in.get();
    }

    @Override
    public int getRequiredBytes() {
        return SIZE_BYTES;
    }

    public String getPrettyString() {
        return Integer.toString(level);
    }

    @Override
    public String toString() {
        return "Level{" +
                "level=" + level +
                '}';
    }

    public static Level fromN(int n) {
        return new Level(n);
    }
}
