package com.quew8.netcaff.lib.server;

import android.os.Parcel;
import android.os.Parcelable;

import java.nio.ByteBuffer;
import java.security.SecureRandom;

/**
 * @author Quew8
 */
public class UserAccessCode extends AlignedStruct implements Parcelable {
    static final int SIZE_BYTES = 16;

    private long ls, ms;

    private UserAccessCode(long ls, long ms) {
        this.ls = ls;
        this.ms = ms;
    }

    UserAccessCode() {
        this(0, 0);
    }

    @Override
    protected String check() {
        if(ls == 0 && ms == 0) {
            return "Invalid user access code";
        }
        return null;
    }

    @Override
    public void putDataInBuffer(ByteBuffer out) {
        if(out.remaining() < SIZE_BYTES) {
            throw new IllegalArgumentException("At least " + SIZE_BYTES + " are required");
        }
        out.putLong(ls);
        out.putLong(ms);
    }

    @Override
    public void getDataFromBuffer(ByteBuffer in) throws StructureFormatException {
        if(in.remaining() < SIZE_BYTES) {
            throw new StructureFormatException("At least " + SIZE_BYTES + " are required");
        }
        ls = in.getLong();
        ms = in.getLong();
        String msg = check();
        if(msg != null) {
            throw new StructureFormatException(msg);
        }
    }

    @Override
    public int getRequiredBytes() {
        return SIZE_BYTES;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        UserAccessCode that = (UserAccessCode) o;

        return ls == that.ls && ms == that.ms;
    }

    @Override
    public int hashCode() {
        int result = (int) (ls ^ (ls >>> 32));
        result = 31 * result + (int) (ms ^ (ms >>> 32));
        return result;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(ls);
        out.writeLong(ms);
    }

    public static final Parcelable.Creator<UserAccessCode> CREATOR = new Parcelable.Creator<UserAccessCode>() {
        public UserAccessCode createFromParcel(Parcel in) {
            return new UserAccessCode(in.readLong(), in.readLong());
        }

        public UserAccessCode[] newArray(int size) {
            return new UserAccessCode[size];
        }
    };

    public static UserAccessCode generate(SecureRandom random) {
        return new UserAccessCode(random.nextLong(), random.nextLong());
    }

    public String toHexString() {
        return Long.toHexString(ms) + Long.toHexString(ls);
    }

    @Override
    public String toString() {
        return "UserAccessCode{" +
                "ls=" + ls +
                ", ms=" + ms +
                '}';
    }
}
