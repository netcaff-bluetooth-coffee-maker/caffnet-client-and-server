package com.quew8.netcaff.server.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import com.quew8.netcaff.server.ServerCoffeeServer;
import com.quew8.netcaff.server.UnsupportedSystemServiceException;
import com.quew8.netcaff.server.ble.BleAdvertiser.AdvertiserStatus;
import com.quew8.netcaff.server.ble.BleServer.ServerStatus;
import com.quew8.properties.BooleanProperty;
import com.quew8.properties.ListenerSet.ListenerHandle;
import com.quew8.properties.PropertyChangeListener;
import com.quew8.properties.ReadOnlyBooleanProperty;

import static android.content.Context.BLUETOOTH_SERVICE;

/**
 * @author Quew8
 */
public class BleManager extends BroadcastReceiver {
    private final Context ctx;
    private final BooleanProperty registered;
    private final BooleanProperty enabled;
    private final BluetoothAdapter adapter;

    private final BleServer bleServer;
    private final BleAdvertiser bleAdvertiser;

    private ListenerHandle<PropertyChangeListener<Boolean>> adapterStartupCallbackHandle = null;
    private ListenerHandle<PropertyChangeListener<ServerStatus>> serverStartupCallbackHandle = null;
    private ListenerHandle<PropertyChangeListener<AdvertiserStatus>> advertiserStartupCallbackHandle = null;

    public BleManager(Context ctx, ServerCoffeeServer coffeeServer) throws UnsupportedSystemServiceException {
        this.ctx = ctx;
        this.registered = new BooleanProperty(false);
        this.enabled = new BooleanProperty(false);
        BluetoothManager manager = (BluetoothManager) ctx.getSystemService(BLUETOOTH_SERVICE);
        if(manager == null) {
            throw new UnsupportedSystemServiceException(BluetoothManager.class);
        }
        this.adapter = manager.getAdapter();
        if(adapter == null) {
            throw new UnsupportedSystemServiceException("Bluetooth is not supported");
        }
        if(!ctx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            throw new UnsupportedSystemServiceException("Bluetooth LE is not supported");
        }
        enabled.set(adapter.isEnabled());

        this.bleAdvertiser = new BleAdvertiser(ctx, adapter, coffeeServer);
        this.bleServer = new BleServer(ctx, manager, bleAdvertiser, coffeeServer);
    }

    public void register() {
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        ctx.registerReceiver(this, filter);
        registered.set(true);
    }

    public void unregister() {
        ctx.unregisterReceiver(this);
        registered.set(false);
    }

    public void startup() {
        if(!registered.get()) {
            throw new IllegalStateException("BleManager is not registered");
        }
        if(!enabled.get()) {
            adapterStartupCallbackHandle = enabled.addListener(this::adapterStartupCallback);
            turnOnBluetooth();
        } else if(bleServer.getStatus().get() == ServerStatus.INACTIVE) {
            serverStartupCallbackHandle = bleServer.getStatus().addListener(this::serverStartupCallback);
            bleServer.start();
        } else if(bleAdvertiser.getStatus().get() == BleAdvertiser.AdvertiserStatus.INACTIVE) {
            advertiserStartupCallbackHandle = bleAdvertiser.getStatus().addListener(this::advertiserStartupCallback);
            bleAdvertiser.start();
        }
    }

    public void shutdown() {
        bleAdvertiser.stop();
        bleServer.stop();
    }

    public ReadOnlyBooleanProperty isBluetoothEnabled() {
        return enabled;
    }

    public BleServer getBleServer() {
        return bleServer;
    }

    public BleAdvertiser getBleAdvertiser() {
        return bleAdvertiser;
    }

    private void adapterStartupCallback(Boolean newState) {
        if(newState) {
            enabled.removeListener(adapterStartupCallbackHandle);
            adapterStartupCallbackHandle = null;
            startup();
        }
    }

    private void serverStartupCallback(ServerStatus newState) {
        if(newState == ServerStatus.ACTIVE) {
            bleServer.getStatus().removeListener(serverStartupCallbackHandle);
            serverStartupCallbackHandle = null;
            startup();
        }
    }

    private void advertiserStartupCallback(AdvertiserStatus newState) {
        if(newState == AdvertiserStatus.ACTIVE) {
            bleAdvertiser.getStatus().removeListener(advertiserStartupCallbackHandle);
            advertiserStartupCallbackHandle = null;
            startup();
        }
    }

    public void turnOnBluetooth() {
        if(!enabled.get()) {
            adapter.enable();
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
        switch(state) {
            case BluetoothAdapter.STATE_ON: {
                enabled.set(true);
                break;
            }
            case BluetoothAdapter.STATE_OFF: {
                bleAdvertiser.stop();
                bleServer.stop();
                enabled.set(false);
                break;
            }
        }
    }
}
