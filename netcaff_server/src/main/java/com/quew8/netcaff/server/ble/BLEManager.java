package com.quew8.netcaff.server.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import com.quew8.netcaff.server.ServerCoffeeServer;
import com.quew8.netcaff.server.ble.Advertiser.AdvertiserStatus;
import com.quew8.netcaff.server.ble.Server.ServerStatus;
import com.quew8.properties.BooleanProperty;
import com.quew8.properties.ListenerSet.ListenerHandle;
import com.quew8.properties.PropertyChangeListener;
import com.quew8.properties.ReadOnlyBooleanProperty;
import com.quew8.netcaff.lib.server.CoffeeServer;

import static android.content.Context.BLUETOOTH_SERVICE;

/**
 * @author Quew8
 */
public class BLEManager extends BroadcastReceiver {
    private final Context ctx;
    private final BooleanProperty registered;
    private final BooleanProperty enabled;
    private final BluetoothManager manager;
    private final BluetoothAdapter adapter;

    private final CoffeeServer coffeeServer;
    private final Server server;
    private final Advertiser advertiser;

    private ListenerHandle<PropertyChangeListener<Boolean>> adapterStartupCallbackHandle = null;
    private ListenerHandle<PropertyChangeListener<ServerStatus>> serverStartupCallbackHandle = null;
    private ListenerHandle<PropertyChangeListener<AdvertiserStatus>> advertiserStartupCallbackHandle = null;

    public BLEManager(Context ctx, ServerCoffeeServer coffeeServer) {
        this.ctx = ctx;
        this.registered = new BooleanProperty(false);
        this.enabled = new BooleanProperty(false);
        this.manager = (BluetoothManager) ctx.getSystemService(BLUETOOTH_SERVICE);
        this.adapter = manager.getAdapter();
        if(adapter == null) {
            throw new RuntimeException("Bluetooth is not supported");
        }
        if(!ctx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            throw new RuntimeException("Bluetooth LE is not supported");
        }
        enabled.set(adapter.isEnabled());

        this.coffeeServer = coffeeServer;
        this.advertiser = new Advertiser(ctx, adapter, coffeeServer);
        this.server = new Server(ctx, manager, advertiser, coffeeServer);
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
            throw new IllegalStateException("BLEManager is not registered");
        }
        if(!enabled.get()) {
            adapterStartupCallbackHandle = enabled.addListener(this::adapterStartupCallback);
            turnOnBluetooth();
        } else if(server.getStatus().get() == ServerStatus.INACTIVE) {
            serverStartupCallbackHandle = server.getStatus().addListener(this::serverStartupCallback);
            server.start();
        } else if(advertiser.getStatus().get() == Advertiser.AdvertiserStatus.INACTIVE) {
            advertiserStartupCallbackHandle = advertiser.getStatus().addListener(this::advertiserStartupCallback);
            advertiser.start();
        }
    }

    public void shutdown() {
        advertiser.stop();
        server.stop();
    }

    public ReadOnlyBooleanProperty isBluetoothEnabled() {
        return enabled;
    }

    public Server getServer() {
        return server;
    }

    public Advertiser getAdvertiser() {
        return advertiser;
    }

    private void adapterStartupCallback(Boolean newState, Boolean oldState) {
        if(newState) {
            enabled.removeListener(adapterStartupCallbackHandle);
            adapterStartupCallbackHandle = null;
            startup();
        }
    }

    private void serverStartupCallback(ServerStatus newState, ServerStatus oldState) {
        if(newState == ServerStatus.ACTIVE) {
            server.getStatus().removeListener(serverStartupCallbackHandle);
            serverStartupCallbackHandle = null;
            startup();
        }
    }

    private void advertiserStartupCallback(AdvertiserStatus newState, AdvertiserStatus oldState) {
        if(newState == AdvertiserStatus.ACTIVE) {
            advertiser.getStatus().removeListener(advertiserStartupCallbackHandle);
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
                advertiser.stop();
                server.stop();
                enabled.set(false);
                break;
            }
        }
    }
}
