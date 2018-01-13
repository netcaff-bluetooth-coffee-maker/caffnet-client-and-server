package com.quew8.netcaff.lib.server;

import java.nio.ByteBuffer;

/**
 * @author Quew8
 */

public class Levels extends CharacteristicStruct {
    public static final int SIZE_BYTES = Level.SIZE_BYTES + Level.SIZE_BYTES;

    private Level groundsLevel;
    private Level waterLevel;

    private Levels(Level groundsLevel, Level waterLevel) {
        this.groundsLevel = groundsLevel;
        this.waterLevel = waterLevel;
    }

    Levels() {
        this(new Level(), new Level());
    }

    public int getGroundsLevel() {
        return groundsLevel.getValue();
    }

    public int getWaterLevel() {
        return waterLevel.getValue();
    }

    public void set(Level groundsLevel, Level waterLevel) {
        this.groundsLevel = groundsLevel;
        groundsLevel.check();
        this.waterLevel = waterLevel;
        waterLevel.check();
        doCheck();
        set();
    }

    @Override
    protected String check() {
        return null;
    }

    @Override
    public void putDataInBuffer(ByteBuffer out) {
        if(out.remaining() < SIZE_BYTES) {
            throw new IllegalArgumentException("At least " + SIZE_BYTES + " bytes are required");
        }
        groundsLevel.putDataInBuffer(out);
        waterLevel.putDataInBuffer(out);
    }

    @Override
    public void getDataFromBuffer(ByteBuffer in) throws StructureFormatException {
        if(in.remaining() < SIZE_BYTES) {
            throw new StructureFormatException("At least " + SIZE_BYTES + " bytes are required");
        }
        groundsLevel.readFromBuffer(in);
        waterLevel.readFromBuffer(in);
    }

    @Override
    public int getRequiredBytes() {
        return SIZE_BYTES;
    }

    @Override
    public String getPrettyString() {
        return groundsLevel.getPrettyString() + " | " + waterLevel.getPrettyString();
    }

    @Override
    public String toString() {
        return "Levels{" +
                "groundsLevel=" + groundsLevel +
                ", waterLevel=" + waterLevel +
                '}';
    }
}
