package com.quew8.netcaff.server.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import com.quew8.netcaff.lib.ble.CoffeeServerProfile;
import com.quew8.netcaff.lib.ble.util.BLEUtil;
import com.quew8.netcaff.server.ServerCoffeeServer;
import com.quew8.properties.ListenerSet.ListenerHandle;
import com.quew8.properties.Property;
import com.quew8.properties.PropertyChangeListener;
import com.quew8.properties.ReadOnlyProperty;

/**
 * @author Quew8
 */

public class Advertiser extends AdvertiseCallback {
    private static final String TAG = Advertiser.class.getSimpleName();

    private final Property<AdvertiserStatus> status;
    private final BluetoothLeAdvertiser leAdvertiser;
    private final AdvertiseSettings settings;

    private final ServerCoffeeServer coffeeServer;
    private final Property<byte[]> buffer;

    private ListenerHandle<PropertyChangeListener<byte[]>> bufferAdvertisementDataListenerHandle = null;
    private ListenerHandle<PropertyChangeListener<byte[]>> replaceAdvertisementDataListenerHandle = null;

    Advertiser(Context ctx, BluetoothAdapter adapter, ServerCoffeeServer coffeeServer) {
        this.status = new Property<>(AdvertiserStatus.INACTIVE);
        Log.d(TAG, "Ad Packet Size: " + CoffeeServerProfile.MAX_ADVERTISING_SIZE_BYTES);
        Log.d(TAG, "Max Ad Packet Size: " + adapter.getLeMaximumAdvertisingDataLength());
        this.leAdvertiser = adapter.getBluetoothLeAdvertiser();
        if(leAdvertiser == null) {
            Log.e(TAG, "Failed to create Advertiser");
            status.set(AdvertiserStatus.ERROR);
        }
        this.settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build();

        this.coffeeServer = coffeeServer;
        this.buffer = new Property<>(null);
    }

    void start() {
        if(status.get() == AdvertiserStatus.INACTIVE) {
            Log.i(TAG, "STARTING ADVERTISEMENTS");
            updateAdvertisementData(coffeeServer.getOrderData().get());
        }
    }

    void stop() {
        if(status.get() == AdvertiserStatus.ACTIVE) {
            Log.i(TAG, "STOPPING ADVERTISEMENTS");
            coffeeServer.getOrderData().removeListener(replaceAdvertisementDataListenerHandle);
            replaceAdvertisementDataListenerHandle = null;
            leAdvertiser.stopAdvertising(this);
            status.set(AdvertiserStatus.INACTIVE);
        }
    }

    public ReadOnlyProperty<AdvertiserStatus> getStatus() {
        return status;
    }

    private void bufferAdvertisementData(byte[] newVal, byte[] oldVal) {
        buffer.set(newVal);
    }

    private void replaceAdvertisementData(byte[] newVal, byte[] oldVal) {
        if(replaceAdvertisementDataListenerHandle != null) {
            coffeeServer.getOrderData().removeListener(replaceAdvertisementDataListenerHandle);
            replaceAdvertisementDataListenerHandle = null;
        }
        buffer.set(null);
        leAdvertiser.stopAdvertising(this);
        updateAdvertisementData(newVal);
    }

    private void updateAdvertisementData(byte[] newVal) {
        status.set(AdvertiserStatus.STARTING);
        buffer.set(null);
        bufferAdvertisementDataListenerHandle = coffeeServer.getOrderData().addListener(this::bufferAdvertisementData);
        coffeeServer.getAdData().log();
        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                //.addServiceUuid(new ParcelUuid(CoffeeServerProfile.COFFEE_SERVER_SERVICE))
                /*.addManufacturerData(
                        1,
                        coffeeServer.getOrderData()
                )*/
                .addServiceData(
                        new ParcelUuid(CoffeeServerProfile.COFFEE_REQUEST_SERVICE),
                        newVal
                )
                .build();

        leAdvertiser.startAdvertising(settings, data, this);
    }

    @Override
    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
        coffeeServer.getOrderData().removeListener(bufferAdvertisementDataListenerHandle);
        bufferAdvertisementDataListenerHandle = null;
        if(buffer.get() != null) {
            Log.i(TAG, "LE Advertise Started but new data buffered");
            replaceAdvertisementData(buffer.get(), null);
        } else {
            Log.i(TAG, "LE Advertise Started.");
            replaceAdvertisementDataListenerHandle = coffeeServer.getOrderData().addListener(this::replaceAdvertisementData);
            status.set(AdvertiserStatus.ACTIVE);
        }
    }

    @Override
    public void onStartFailure(int errorCode) {
        coffeeServer.getOrderData().removeListener(bufferAdvertisementDataListenerHandle);
        Log.e(TAG, "LE Advertise Failed: " + BLEUtil.getAdvertisingErrorString(errorCode));
        status.set(AdvertiserStatus.ERROR);
    }

    public enum AdvertiserStatus {
        INACTIVE, STARTING, ACTIVE, ERROR
    }
}
