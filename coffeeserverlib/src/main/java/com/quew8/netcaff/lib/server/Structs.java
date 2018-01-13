package com.quew8.netcaff.lib.server;

import java.nio.ByteBuffer;

/**
 * @author Quew8
 */

public class Structs {
    private Structs() {}

    public static byte[] writeOut(AlignedStruct s, int offset) {
        int len = s.getRequiredBytes();
        if(offset > len) {
            throw new IllegalArgumentException("Cannot have an offset greater than the length of the structure");
        }
        ByteBuffer out = ByteBuffer.allocate(len);
        s.putDataInBuffer(out);
        byte[] data = new byte[len - offset];
        out.position(offset);
        out.get(data);
        return data;
    }

    public static byte[] writeOut(AlignedStruct s) {
        return writeOut(s, 0);
    }

    public static void readIn(AlignedStruct s, byte[] data) throws StructureFormatException {
        ByteBuffer in = ByteBuffer.wrap(data);
        s.readFromBuffer(in);
    }
}
