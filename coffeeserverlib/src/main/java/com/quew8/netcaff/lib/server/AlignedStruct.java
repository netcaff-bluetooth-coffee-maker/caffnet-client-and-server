package com.quew8.netcaff.lib.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * @author Quew8
 */

public abstract class AlignedStruct extends Struct {
    public void readFromBuffer(ByteBuffer in) throws StructureFormatException {
        getDataFromBuffer(in);
        String msg = check();
        if(msg != null) {
            throw new StructureFormatException(msg);
        }
    }

    public abstract void putDataInBuffer(ByteBuffer out);
    protected abstract void getDataFromBuffer(ByteBuffer in) throws StructureFormatException;
    public abstract int getRequiredBytes();

}
