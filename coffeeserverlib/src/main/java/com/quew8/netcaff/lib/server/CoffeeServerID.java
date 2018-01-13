package com.quew8.netcaff.lib.server;


import android.os.Parcel;
import android.os.Parcelable;

import com.quew8.properties.IntegerProperty;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * @author Quew8
 */
public class CoffeeServerID extends CharacteristicStruct implements Parcelable {
    public static final int SIZE_BYTES = 4;

    private int id;

    CoffeeServerID(int id) {
        this.id = id;
    }

    public CoffeeServerID() {
        this(0);
    }

    @Override
    protected String check() {
        if(id <= 0) {
            return "Invalid server id";
        }
        return null;
    }

    @Override
    public void putDataInBuffer(ByteBuffer out) {
        if(out.remaining() < SIZE_BYTES) {
            throw new IllegalArgumentException("At least " + SIZE_BYTES + " are required");
        }
        out.putInt(id);
    }

    @Override
    public void getDataFromBuffer(ByteBuffer in) throws StructureFormatException {
        if(in.remaining() < SIZE_BYTES) {
            throw new StructureFormatException("At least " + SIZE_BYTES + " are required");
        }
        id= in.getInt();
    }

    @Override
    public int getRequiredBytes() {
        return SIZE_BYTES;
    }

    @Override
    public String toString() {
        return "ServerID{" + Integer.toString(id) + "}";
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        CoffeeServerID that = (CoffeeServerID) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(id);
    }

    public static final Parcelable.Creator<CoffeeServerID> CREATOR = new Parcelable.Creator<CoffeeServerID>() {
        public CoffeeServerID createFromParcel(Parcel in) {
            return new CoffeeServerID(in.readInt());
        }

        public CoffeeServerID[] newArray(int size) {
            return new CoffeeServerID[size];
        }
    };

    @Override
    public String getPrettyString() {
        return Integer.toString(id);
    }

    public static String toPrefString(CoffeeServerID id) {
        return Integer.toString(id.id);
    }

    public static CoffeeServerID fromPrefString(String s) {
        try {
            int i = Integer.parseInt(s);
            CoffeeServerID id = new CoffeeServerID(i);
            String err = id.check();
            return err == null ? id : null;
        } catch(NumberFormatException ex) {
            return null;
        }
    }
}
