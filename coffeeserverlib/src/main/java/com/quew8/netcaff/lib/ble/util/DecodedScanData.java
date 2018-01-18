package com.quew8.netcaff.lib.ble.util;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Quew8
 */

public class DecodedScanData {
    private static final String TAG = DecodedScanData.class.getSimpleName();
    public static final int UUID16_N_BYTES = 2,
            UUID128_N_BYTES = 16,
            MANUFACTURER_ID_N_BYTES = 2;

    private int flags = 0;
    private final List<UUID> uuids = new ArrayList<>();
    private final Map<UUID, byte[]> serviceData = new HashMap<>();
    private final Map<Integer, byte[]> manufacturerData = new HashMap<>();

    private DecodedScanData() {}

    public int getFlags() {
        return flags;
    }

    public List<UUID> getUUIDs() {
        return uuids;
    }

    public boolean hasUUID(UUID uuid) {
        return uuids.contains(uuid);
    }

    public byte[] getServiceData(UUID service) {
        return serviceData.get(service);
    }

    public byte[] getManufacturerData(int manufacturerId) {
        return manufacturerData.get(manufacturerId);
    }

    public static String byteToString(byte b) {
        String s = "0b";
        for(int i = 0; i < 8; i++) {
            s += (b & (1 << i)) > 0 ? "1" : "0";
        }
        return s;
    }

    public static DecodedScanData decode(byte[] scanData) {
        DecodedScanData decoded = new DecodedScanData();

        byte[] advertisedData = Arrays.copyOf(scanData, scanData.length);
        int offset = 0;
        while(offset < (advertisedData.length - 2)) {
            int len = advertisedData[offset++];
            if(len == 0) {
                break;
            }
            byte typeByte = advertisedData[offset];
            int type = typeByte & 0xFF;
            offset++;
            len--;
            switch(type) {
                case AdvType.FLAGS: {
                    Log.d(TAG, "Flags: " + advertisedData[offset]);
                    decoded.flags = advertisedData[offset];
                    offset++;
                    len--;
                    break;
                }
                case AdvType.UUID16_PARTIAL_LIST:
                case AdvType.UUID16_FULL_LIST: {
                    while(len >= UUID16_N_BYTES) {
                        decoded.uuids.add(readUUID16(advertisedData, offset));
                        offset += UUID16_N_BYTES;
                        len -= UUID16_N_BYTES;
                    }
                    break;
                }
                case AdvType.UUID128_PARTIAL_LIST:
                case AdvType.UUID128_FULL_LIST:
                    while(len >= UUID128_N_BYTES) {
                        decoded.uuids.add(readUUID128(advertisedData, offset));
                        offset += UUID128_N_BYTES;
                        len -= UUID128_N_BYTES;
                    }
                    break;
                case AdvType.UUID16_SERVICE_DATA: {
                    if(len >= UUID16_N_BYTES) {
                        UUID serviceUuid = readUUID16(advertisedData, offset);
                        offset += UUID16_N_BYTES;
                        len -= UUID16_N_BYTES;
                        decoded.uuids.add(serviceUuid);
                        decoded.serviceData.put(
                                serviceUuid,
                                Arrays.copyOfRange(advertisedData, offset, offset + len)
                        );
                        offset += len;
                        len = 0;
                    }
                    break;
                }
                case AdvType.UUID128_SERVICE_DATA: {
                    if(len >= UUID128_N_BYTES) {
                        UUID serviceUuid = readUUID128(advertisedData, offset);
                        offset += UUID128_N_BYTES;
                        len -= UUID128_N_BYTES;
                        decoded.uuids.add(serviceUuid);
                        decoded.serviceData.put(
                                serviceUuid,
                                Arrays.copyOfRange(advertisedData, offset, offset + len)
                        );
                        offset += len;
                        len = 0;
                    }
                    break;
                }
                case AdvType.MANUFACTURER_SPECIFIC_DATA:
                    if(len >= MANUFACTURER_ID_N_BYTES) {
                        int manufacturerId = readManufacturerId(advertisedData, offset);
                        offset += MANUFACTURER_ID_N_BYTES;
                        len -= MANUFACTURER_ID_N_BYTES;
                        decoded.manufacturerData.put(
                                manufacturerId,
                                Arrays.copyOfRange(advertisedData, offset, offset + len)
                        );
                        offset += len;
                        len = 0;
                    }
                    break;
                case AdvType.UUID32_PARTIAL_LIST:
                case AdvType.UUID32_FULL_LIST:
                case AdvType.UUID32_SERVICE_DATA: {
                    Log.w(TAG, "32 bit service uuids are not supported");
                }
                case AdvType.SHORT_LOCAL_NAME:
                case AdvType.FULL_LOCAL_NAME:
                case AdvType.TX_POWER_LEVEL:
                case AdvType.CLASS_OF_DEVICE:
                case AdvType.SIMPLE_PAIRING_HASH_C_192:
                case AdvType.SIMPLE_PAIRING_RANDOMIZER_R_192:
                case AdvType.SECURITY_MANAGER_TK_VALUE:
                case AdvType.SECURITY_MANAGER_OUT_OF_BOUNDS_FLAGS:
                case AdvType.SLAVE_CONNECTION_INTERVAL_RANGE:
                case AdvType.UUID16_SERVICE_SOLICITATION_LIST:
                case AdvType.UUID128_SERVICE_SOLICITATION_LIST:
                case AdvType.PUBLIC_TARGET_ADDRESS:
                case AdvType.RANDOM_TARGET_ADDRESS:
                case AdvType.APPEARANCE:
                case AdvType.ADVERTISING_INTERVAL:
                case AdvType.LE_BLUETOOTH_DEVICE_ADDRESS:
                case AdvType.LE_ROLE:
                case AdvType.SIMPLE_PAIRING_HASH_C_256:
                case AdvType.SIMPLE_PAIRING_RANDOMIZER_R_256:
                case AdvType.UUID32_SERVICE_SOLICITATION_LIST:
                case AdvType.LE_SECURE_CONNECTIONS_CONFIRMATION_VALUE:
                case AdvType.LE_SECURE_CONNECTIONS_RANDOM_VALUE:
                case AdvType.URI:
                case AdvType.INDOOR_POSITIONING:
                case AdvType.TRANSPORT_DISCOVERY_DATA:
                case AdvType.LE_SUPPORTED_FEATURES:
                case AdvType.CHANNEL_MAP_UPDATE_INDICATION:
                case AdvType.PB_ADV:
                case AdvType.MESH_MESSAGE:
                case AdvType.MESH_BEACON:
                case AdvType.INFORMATION_DATA_3D: {
                    offset += len;
                    len = 0;
                    break;
                }
                default: {
                    Log.w(TAG, "Unknown adv type: " + (type) + " (" + byteToString(typeByte) + ")");
                    offset += len;
                    len = 0;
                    break;
                }
            }
            if(len > 0) {
                Log.w(TAG, "Payload is larger than expected, type: " + type + " (" + byteToString(typeByte) + ")");
                offset += len;
            }
        }

        return decoded;
    }

    public static UUID readUUID16(byte[] data, int offset) {
        int uuid16 = data[offset++] & 0xFF;
        uuid16 |= (data[offset] << 8);
        return UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", uuid16));
    }

    public static UUID readUUID128(byte[] data, int offset) {
        ByteBuffer buffer = ByteBuffer.wrap(data, offset, UUID128_N_BYTES).order(ByteOrder.LITTLE_ENDIAN);
        long msb = buffer.getLong();
        long lsb = buffer.getLong();
        return new UUID(lsb, msb);
    }

    public static int readManufacturerId(byte[] data, int offset) {
        ByteBuffer buffer = ByteBuffer.wrap(data, offset, MANUFACTURER_ID_N_BYTES).order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getShort();
    }
    
    public static final class AdvType {
        public static final int
                FLAGS = 0x01,
                UUID16_PARTIAL_LIST = 0x02,
                UUID16_FULL_LIST = 0x03,
                UUID32_PARTIAL_LIST = 0x04,
                UUID32_FULL_LIST = 0x05,
                UUID128_PARTIAL_LIST = 0x06,
                UUID128_FULL_LIST = 0x07,
                SHORT_LOCAL_NAME = 0x08,
                FULL_LOCAL_NAME = 0x09,
                TX_POWER_LEVEL = 0x0A,
                CLASS_OF_DEVICE = 0x0D,
                SIMPLE_PAIRING_HASH_C_192 = 0x0E,
                SIMPLE_PAIRING_RANDOMIZER_R_192 = 0x0F,
                SECURITY_MANAGER_TK_VALUE = 0x10,
                SECURITY_MANAGER_OUT_OF_BOUNDS_FLAGS = 0x11,
                SLAVE_CONNECTION_INTERVAL_RANGE = 0x12,
                UUID16_SERVICE_SOLICITATION_LIST = 0x14,
                UUID128_SERVICE_SOLICITATION_LIST = 0x15,
                UUID16_SERVICE_DATA = 0x16,
                PUBLIC_TARGET_ADDRESS = 0x17,
                RANDOM_TARGET_ADDRESS = 0x18,
                APPEARANCE = 0x19,
                ADVERTISING_INTERVAL = 0x1A,
                LE_BLUETOOTH_DEVICE_ADDRESS = 0x1B,
                LE_ROLE = 0x1C,
                SIMPLE_PAIRING_HASH_C_256 = 0x1D,
                SIMPLE_PAIRING_RANDOMIZER_R_256 = 0x1E,
                UUID32_SERVICE_SOLICITATION_LIST = 0x1F,
                UUID32_SERVICE_DATA = 0x20,
                UUID128_SERVICE_DATA = 0x21,
                LE_SECURE_CONNECTIONS_CONFIRMATION_VALUE = 0x22,
                LE_SECURE_CONNECTIONS_RANDOM_VALUE = 0x23,
                URI = 0x24,
                INDOOR_POSITIONING = 0x25,
                TRANSPORT_DISCOVERY_DATA = 0x26,
                LE_SUPPORTED_FEATURES = 0x27,
                CHANNEL_MAP_UPDATE_INDICATION = 0x28,
                PB_ADV = 0x29,
                MESH_MESSAGE = 0x2A,
                MESH_BEACON = 0x2B,
                INFORMATION_DATA_3D = 0x3D,
                MANUFACTURER_SPECIFIC_DATA = 0xFF;
    }

    public static final class AdvFlags {
        public static final int
                LE_LIMITED_DISCOVERABLE_MODE_BIT = 0x01,
                LE_GENERAL_DISCOVERABLE_MODE_BIT = 0x02,
                BR_EDR_NOT_SUPPROTED_BIT = 0x04,
                LE_BR_EDR_CONTROLLER_BIT = 0x08,
                LE_BR_EDR_HOST_BIT = 0x10;
    }
}
