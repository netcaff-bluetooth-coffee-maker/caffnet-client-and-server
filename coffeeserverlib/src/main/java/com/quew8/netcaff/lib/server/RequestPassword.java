package com.quew8.netcaff.lib.server;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @author Quew8
 */

public class RequestPassword extends CharacteristicStruct {
    public static final int SIZE_BYTES = 32;

    private byte[] data;

    private RequestPassword(byte[] data) {
        this.data = data;
    }

    RequestPassword() {
        this(new byte[SIZE_BYTES]);
    }

    public byte[] getValue() {
        return data;
    }

    public void set(byte[] data) {
        this.data = data;
        doCheck();
        set();
    }

    @Override
    protected String check() {
        if(this.data == null) {
            return "Data is null";
        }
        if(this.data.length != SIZE_BYTES) {
            return "Data is not 256 bits";
        }
        return null;
    }

    @Override
    public void putDataInBuffer(ByteBuffer out) {
        if(out.remaining() < SIZE_BYTES) {
            throw new IllegalArgumentException("At least " + SIZE_BYTES + " bytes are required");
        }
        out.put(data);
    }

    @Override
    public void getDataFromBuffer(ByteBuffer in) throws StructureFormatException {
        if(in.remaining() < SIZE_BYTES) {
            throw new StructureFormatException("At least " + SIZE_BYTES + " bytes are required");
        }
        in.get(data);
    }

    @Override
    public int getRequiredBytes() {
        return SIZE_BYTES;
    }

    private static String toHexString(byte[] data) {
        StringBuilder hexString = new StringBuilder();
        for(byte aData : data) {
            String hex = Integer.toHexString(0xff & aData);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    @Override
    public String getPrettyString() {
        return toHexString(data);
    }

    @Override
    public String toString() {
        return "RequestPassword{" +
                "data=" + toHexString(data) +
                '}';
    }
}
