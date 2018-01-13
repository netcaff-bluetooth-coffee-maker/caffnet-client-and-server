package com.quew8.netcaff.lib.server;

import android.os.Parcelable;

import java.nio.ByteBuffer;

/**
 * @author Quew8
 */

public class ResponseUserAccessCode extends CharacteristicStruct {
    public static final int SIZE_BYTES = UserAccessCode.SIZE_BYTES;

    private UserAccessCode accessCode;
    private boolean isValid;

    private ResponseUserAccessCode(UserAccessCode accessCode, boolean isValid) {
        this.accessCode = accessCode;
        this.isValid = isValid;
    }

    ResponseUserAccessCode() {
        this(new UserAccessCode(), false);
    }

    public UserAccessCode getAccessCode() {
        return accessCode;
    }

    public boolean isValid() {
        return isValid;
    }

    public void set(UserAccessCode accessCode) {
        this.accessCode = accessCode;
        this.isValid = true;
        accessCode.check();
        doCheck();
        set();
    }

    public void setNone() {
        this.isValid = false;
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
        if(isValid) {
            accessCode.putDataInBuffer(out);
        } else {
            out.putLong(0);
            out.putLong(0);
        }
    }

    @Override
    public void getDataFromBuffer(ByteBuffer in) throws StructureFormatException {
        if(in.remaining() < SIZE_BYTES) {
            throw new StructureFormatException("At least " + SIZE_BYTES + " bytes are required");
        }
        long ls = in.getLong(in.position());
        long ms = in.getLong(in.position() + 8);
        if(ls == 0 && ms == 0) {
            isValid = false;
            in.getLong();
            in.getLong();
        } else {
            isValid = true;
            accessCode.getDataFromBuffer(in);
        }
    }

    @Override
    public String getPrettyString() {
        if(isValid()) {
            return accessCode.toHexString();
        } else {
            StringBuilder s = new StringBuilder(32);
            for(int i = 0; i < 32; i++) {
                s.append("0");
            }
            return s.toString();
        }
    }

    @Override
    public int getRequiredBytes() {
        return SIZE_BYTES;
    }

    @Override
    public String toString() {
        return "ResponseUserAccessCode{" +
                "accessCode=" + accessCode +
                ", isValid=" + isValid +
                '}';
    }
}
