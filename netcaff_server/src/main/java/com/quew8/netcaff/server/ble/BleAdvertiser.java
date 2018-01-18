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
import com.quew8.properties.BooleanProperty;
import com.quew8.properties.ListenerSet.ListenerHandle;
import com.quew8.properties.Property;
import com.quew8.properties.PropertyChangeListener;
import com.quew8.properties.ReadOnlyProperty;

/**
 * @author Quew8
 */
public class BleAdvertiser extends AdvertiseCallback {
    private static final String TAG = BleAdvertiser.class.getSimpleName();

    private final BooleanProperty turnedOn;
    private final Property<AdvertiserStatus> status;
    private final BluetoothLeAdvertiser leAdvertiser;
    private final AdvertiseSettings settings;

    private final ServerCoffeeServer coffeeServer;
    private final Property<byte[]> buffer;

    private ListenerHandle<PropertyChangeListener<byte[]>> bufferAdvertisementDataListenerHandle = null;
    private ListenerHandle<PropertyChangeListener<byte[]>> replaceAdvertisementDataListenerHandle = null;

    BleAdvertiser(Context ctx, BluetoothAdapter adapter, ServerCoffeeServer coffeeServer) {
        this.turnedOn = new BooleanProperty(false);
        this.status = new Property<>(AdvertiserStatus.INACTIVE);
        this.leAdvertiser = adapter.getBluetoothLeAdvertiser();
        if(leAdvertiser == null) {
            Log.e(TAG, "Failed to create BleAdvertiser");
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
        turnedOn.set(true);
        if(status.get() == AdvertiserStatus.INACTIVE) {
            Log.i(TAG, "STARTING ADVERTISEMENTS");
            updateAdvertisementData(coffeeServer.getOrderData().get());
        }
    }

    void stop() {
        turnedOn.set(false);
        if(status.get() == AdvertiserStatus.ACTIVE) {
            Log.i(TAG, "STOPPING ADVERTISEMENTS");
            if(replaceAdvertisementDataListenerHandle != null) {
                coffeeServer.getOrderData().removeListener(replaceAdvertisementDataListenerHandle);
                replaceAdvertisementDataListenerHandle = null;
            }
            leAdvertiser.stopAdvertising(this);
            status.set(AdvertiserStatus.INACTIVE);
        }
    }

    public ReadOnlyProperty<AdvertiserStatus> getStatus() {
        return status;
    }

    private void bufferAdvertisementData(byte[] newVal) {
        buffer.set(newVal);
    }

    private void replaceAdvertisementData(byte[] newVal) {
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
        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceData(
                        new ParcelUuid(CoffeeServerProfile.COFFEE_REQUEST_SERVICE),
                        newVal
                )
                .build();

        leAdvertiser.startAdvertising(settings, data, this);
    }

    @Override
    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
        status.set(AdvertiserStatus.ACTIVE);
        coffeeServer.getOrderData().removeListener(bufferAdvertisementDataListenerHandle);
        bufferAdvertisementDataListenerHandle = null;
        if(turnedOn.get()) {
            if(buffer.get() != null) {
                Log.i(TAG, "LE Advertise Started but new data buffered");
                replaceAdvertisementData(buffer.get());
            } else {
                Log.i(TAG, "LE Advertise Started.");
                replaceAdvertisementDataListenerHandle =
                        coffeeServer.getOrderData().addListener(this::replaceAdvertisementData);
            }
        } else {
            stop();
        }
    }

    @Override
    public void onStartFailure(int errorCode) {
        Log.e(TAG, "LE Advertise Failed: " + BLEUtil.getAdvertisingErrorString(errorCode));
        coffeeServer.getOrderData().removeListener(bufferAdvertisementDataListenerHandle);
        status.set(AdvertiserStatus.ERROR);
    }

    public enum AdvertiserStatus {
        INACTIVE, STARTING, ACTIVE, ERROR
    }
}
