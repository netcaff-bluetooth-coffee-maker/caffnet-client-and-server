package com.quew8.netcaff.lib.ble;

import android.bluetooth.BluetoothGattService;

import com.quew8.netcaff.lib.TimeUtil;
import com.quew8.netcaff.lib.ble.util.BLEUtil;
import com.quew8.netcaff.lib.ble.util.ServiceBuilder;
import com.quew8.netcaff.lib.server.AdData;
import java.util.UUID;

/**
 * @author Quew8
 */
public class CoffeeServerProfile {
    public static final long TOKEN_LIFETIME_MS = TimeUtil.asMillis(30, 0);

    private static final String COFFEE_SERVER_PROFILE_UUID_BASE = "1e99XXXX-ba71-4502-846d-060b16187553";
    public static final UUID
            COFFEE_REQUEST_SERVICE = getUUIDFromBase(1),
            COFFEE_REQUEST_SERVICE_REQUEST = getUUIDFromBase(2),
            COFFEE_REQUEST_SERVICE_REPLY = getUUIDFromBase(4),
            COFFEE_REQUEST_SERVICE_CONFIG = getUUIDFromBase(5),
            COFFEE_REQUEST_SERVICE_ERROR = getUUIDFromBase(6),
            COFFEE_REQUEST_SERVICE_LEVELS = getUUIDFromBase(7),
            /*COFFEE_LOGIN_SERVICE = getUUIDFromBase(8),*/
            COFFEE_LOGIN_SERVICE_USERNAME = getUUIDFromBase(9),
            COFFEE_LOGIN_SERVICE_PASSWORD = getUUIDFromBase(10),
            COFFEE_LOGIN_SERVICE_ACCESS_CODE = getUUIDFromBase(11),
            COFFEE_LOGIN_SERVICE_CONFIG = getUUIDFromBase(12),
            COFFEE_LOGIN_SERVICE_ERROR = getUUIDFromBase(13);

    private static int
            AD_FLAGS_CONTENT_LENGTH = 1;
    public static int
            MAX_ADVERTISING_SIZE_BYTES = BLEUtil.getSizeOfBLEAdPacket(AD_FLAGS_CONTENT_LENGTH) +
                    BLEUtil.getSizeOfBLEServiceDataPacket(AdData.MAX_SIZE_BYTES);

    private static BluetoothGattService coffeeRequestService = null;
    /*private static BluetoothGattService coffeeLoginService = null;*/

    public static BluetoothGattService[] getAllCoffeeServices() {
        return new BluetoothGattService[] {
                getCoffeeRequestService()/*,
                getCoffeeLoginService()*/
        };
    }

    public static BluetoothGattService getCoffeeRequestService() {
        if(coffeeRequestService == null) {
            coffeeRequestService = new ServiceBuilder(COFFEE_REQUEST_SERVICE)
                    .newCharacteristic(COFFEE_REQUEST_SERVICE_REQUEST).writeable().add()
                    .newCharacteristic(COFFEE_REQUEST_SERVICE_REPLY).readable().notifiable()
                    .withDescriptor(COFFEE_REQUEST_SERVICE_CONFIG).add()
                    .newCharacteristic(COFFEE_REQUEST_SERVICE_ERROR).readable().add()
                    .newCharacteristic(COFFEE_REQUEST_SERVICE_LEVELS).readable().add()

                    .newCharacteristic(COFFEE_LOGIN_SERVICE_USERNAME).writeable().add()
                    .newCharacteristic(COFFEE_LOGIN_SERVICE_PASSWORD).writeable().add()
                    .newCharacteristic(COFFEE_LOGIN_SERVICE_ACCESS_CODE).readable().notifiable()
                    .withDescriptor(COFFEE_LOGIN_SERVICE_CONFIG).add()
                    .newCharacteristic(COFFEE_LOGIN_SERVICE_ERROR).readable().add()

                    .build();
        }
        return coffeeRequestService;
    }

    /*public static BluetoothGattService getCoffeeLoginService() {
        return getCoffeeRequestService();
        if(coffeeLoginService == null) {
            coffeeLoginService = new ServiceBuilder(COFFEE_LOGIN_SERVICE)
                    .newCharacteristic(COFFEE_LOGIN_SERVICE_USERNAME).writeable().add()
                    .newCharacteristic(COFFEE_LOGIN_SERVICE_PASSWORD).writeable().add()
                    .newCharacteristic(COFFEE_LOGIN_SERVICE_ACCESS_CODE).readable().notifiable()
                    .withDescriptor(COFFEE_LOGIN_SERVICE_CONFIG).add()
                    .newCharacteristic(COFFEE_LOGIN_SERVICE_ERROR).readable().add()
                    .build();
        }
        return coffeeLoginService;
    }*/

    private static UUID getUUIDFromBase(int handle16) {
        return UUID.fromString(COFFEE_SERVER_PROFILE_UUID_BASE.replace("XXXX", intToPaddedString(handle16)));
    }

    private static String intToPaddedString(int i) {
        StringBuilder s = new StringBuilder(4);
        s.insert(0, Integer.toString(i));
        while(s.length() < 4) {
            s.insert(0, "0");
        }
        return s.toString();
    }
}
