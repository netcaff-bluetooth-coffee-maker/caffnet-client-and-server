package com.quew8.netcaff.lib.ble.util;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.UUID;

import static android.bluetooth.BluetoothProfile.*;
import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED;
import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE;
import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED;
import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR;
import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS;

/**
 * @author Quew8
 */
public class BLEUtil {
    private BLEUtil() {}

    public static int getSizeOfBLEAdPacket(int nContentBytes) {
        return nContentBytes + 2;
    }

    public static int getSizeOfBLEServiceDataPacket(int nContentBytes) {
        return getSizeOfBLEAdPacket(nContentBytes + 16);
    }

    public static BluetoothGattCharacteristic findCharacteristic(Iterable<BluetoothGattService> services, UUID characteristicUUID) {
        BluetoothGattCharacteristic characteristic;
        for(BluetoothGattService service: services) {
            characteristic = service.getCharacteristic(characteristicUUID);
            if(characteristic != null) {
                return characteristic;
            }
        }
        throw new IllegalArgumentException("No characteristic {" + characteristicUUID.toString() + "} found in these services");
    }

    public static BluetoothGattDescriptor findDescriptor(Iterable<BluetoothGattService> services, UUID descriptorUUID) {
        BluetoothGattDescriptor descriptor;
        for(BluetoothGattService service: services) {
            for(BluetoothGattCharacteristic characteristic: service.getCharacteristics()) {
                descriptor = characteristic.getDescriptor(descriptorUUID);
                if(descriptor != null) {
                    return descriptor;
                }
            }
        }
        throw new IllegalArgumentException("No descriptor {" + descriptorUUID.toString() + "} found in these services");
    }

    public static byte readBits(byte data, int off, int n) {
        int mask = 0;
        for(int i = 0; i < n; i++) {
            mask = mask | (1 << (off + i));
        }
        return (byte) ((data & mask) >> off);
    }

    public static byte writeBits(byte in, byte data, int off, int n) {
        if((data & 0xFF) >= (2 << n)) {
            throw new IllegalArgumentException("Data cannot be less than zero");
        }
        if((data & 0xFF) >= (2 << n)) {
            throw new IllegalArgumentException("Data cannot be greater than " + Integer.toString(2 << n));
        }
        int mask = 0;
        for(int i = 0; i < n; i++) {
            mask = mask | (1 << (off + i));
        }
        return (byte) (in | ((data << off) & mask));
    }

    public static String getConnectionStateString(int code) {
        switch(code) {
            case STATE_DISCONNECTED: return "STATE_DISCONNECTED";
            case STATE_CONNECTING: return "STATE_CONNECTED";
            case STATE_CONNECTED: return "STATE_CONNECTED";
            case STATE_DISCONNECTING: return "STATE_DISCONNECTING";
            default: return "Unknown State (" + Integer.toString(code) + ")";
        }
    }

    public static String getStatusString(int code) {
        switch(code) {
            case 0x0000: return "GATT_SUCCESS (GATT_ENCRYPED_MITM)";
            case 0x0001: return "GATT_INVALID_HANDLE";
            case 0x0002: return "GATT_READ_NOT_PERMIT";
            case 0x0003: return "GATT_WRITE_NOT_PERMIT";
            case 0x0004: return "GATT_INVALID_PDU";
            case 0x0005: return "GATT_INSUF_AUTHENTICATION";
            case 0x0006: return "GATT_REQ_NOT_SUPPORTED";
            case 0x0007: return "GATT_INVALID_OFFSET";
            case 0x0008: return "GATT_INSUF_AUTHORIZATION";
            case 0x0009: return "GATT_PREPARE_Q_FULL";
            case 0x000a: return "GATT_NOT_FOUND";
            case 0x000b: return "GATT_NOT_LONG";
            case 0x000c: return "GATT_INSUF_KEY_SIZE";
            case 0x000d: return "GATT_INVALID_ATTR_LEN";
            case 0x000e: return "GATT_ERR_UNLIKELY";
            case 0x000f: return "GATT_INSUF_ENCRYPTION";
            case 0x0010: return "GATT_UNSUPPORT_GRP_TYPE";
            case 0x0011: return "GATT_INSUF_RESOURCE";
            case 0x0087: return "GATT_ILLEGAL_PARAMETER";
            case 0x0080: return "GATT_NO_RESOURCES";
            case 0x0081: return "GATT_INTERNAL_ERROR";
            case 0x0082: return "GATT_WRONG_STATE";
            case 0x0083: return "GATT_DB_FULL";
            case 0x0084: return "GATT_BUSY";
            case 0x0085: return "GATT_ERROR";
            case 0x0086: return "GATT_CMD_STARTED";
            case 0x0088: return "GATT_PENDING";
            case 0x0089: return "GATT_AUTH_FAIL";
            case 0x008a: return "GATT_MORE";
            case 0x008b: return "GATT_INVALID_CFG";
            case 0x008c: return "GATT_SERVICE_STARTED";
            case 0x008d: return "GATT_ENCRYPED_NO_MITM";
            case 0x008e: return "GATT_NOT_ENCRYPTED";
            default: return "Unknown Status (" + Integer.toString(code) + ")";
        }
    }

    public static String getAdvertisingErrorString(int code) {
        switch(code) {
            case ADVERTISE_FAILED_DATA_TOO_LARGE:
                return "ADVERTISE_FAILED_DATA_TOO_LARGE";
            case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                return "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS";
            case ADVERTISE_FAILED_ALREADY_STARTED:
                return "ADVERTISE_FAILED_ALREADY_STARTED";
            case ADVERTISE_FAILED_INTERNAL_ERROR:
                return "ADVERTISE_FAILED_INTERNAL_ERROR";
            case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                return "ADVERTISE_FAILED_FEATURE_UNSUPPORTED";
            default:
                return "Unknown advertising error code (" + Integer.toString(code) + ")";
        }
    }

    public static String byteToString(byte b) {
        String s = "0b";
        for(int i = 0; i < 8; i++) {
            s += (b & (1 << i)) > 0 ? "1" : "0";
        }
        return s;
    }

    public static String byteArrayToString(byte[] ba) {
        StringBuilder hex = new StringBuilder(ba.length * 10);
        for(byte b : ba) {
            hex.append(" " + byteToString(b));
        }
        return hex.toString();
    }
}
