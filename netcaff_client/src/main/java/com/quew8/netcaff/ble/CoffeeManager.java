package com.quew8.netcaff.ble;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.quew8.netcaff.ServiceDependantActivity;
import com.quew8.netcaff.RequestPermissionActivity;
import com.quew8.properties.BooleanProperty;
import com.quew8.properties.ReadOnlyBooleanProperty;
import com.quew8.properties.deferred.Deferred;
import com.quew8.properties.deferred.Promise;

/**
 * @author Quew8
 */
public class CoffeeManager extends Service {
    private final IBinder binder = new ManagerBinder();
    private ServiceDependantActivity activity = null;
    private final BooleanProperty registered;
    private final BooleanProperty enabled;

    private CoffeeScanner scanner;
    private CoffeeConnector connector;
    private CoffeeServerList serverList;

    private Deferred<Void> enableBluetooth;

    public CoffeeManager() {
        this.registered = new BooleanProperty(false);
        this.enabled = new BooleanProperty(false);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if(manager == null) {
            throw new RuntimeException("Bluetooth is not supported (Null BluetoothManager)");
        }
        BluetoothAdapter adapter = manager.getAdapter();
        if(adapter == null) {
            throw new RuntimeException("Bluetooth is not supported (Null BluetoothAdapter)");
        }
        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            throw new RuntimeException("Bluetooth LE is not supported");
        }
        enabled.set(adapter.isEnabled());

        this.scanner = new CoffeeScanner(this, this, adapter);
        this.connector = new CoffeeConnector(this, scanner);
        this.serverList = new CoffeeServerList(this);

        register();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregister();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void register() {
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(broadcastReceiver, filter);
        registered.set(true);
    }

    private void unregister() {
        unregisterReceiver(broadcastReceiver);
        registered.set(false);
    }

    public Promise<Void> ensureBluetooth() {
        Promise<Void> thisPromise = enableBluetooth;
        if(enableBluetooth == null) {
            enableBluetooth = new Deferred<>();
            thisPromise = enableBluetooth;
            checkBluetooth();
        }
        return thisPromise;
    }

    private void checkBluetooth() {
        if(enabled.get()) {
            checkBluetoothPermission();
        } else {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.runActivity(enableBtIntent)
                    .done((intent) -> checkBluetoothPermission())
                    .fail(() -> {
                        enableBluetooth.fail();
                        enableBluetooth = null;
                    });
        }
    }

    private void checkBluetoothPermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if(permissionCheck == PackageManager.PERMISSION_GRANTED) {
            enableBluetooth.resolve(null);
            enableBluetooth = null;
        } else {
            Promise<?> d;
            if(ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {

                d = activity.runActivity(new Intent(this, RequestPermissionActivity.class));
            } else {
                d = activity.requestPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            d.done((granted) -> {
                enableBluetooth.resolve(null);
                enableBluetooth = null;
            }).fail(() -> {
                enableBluetooth.fail();
                enableBluetooth = null;
            });
        }
    }

    public ReadOnlyBooleanProperty isBluetoothEnabled() {
        return enabled;
    }

    public CoffeeScanner getScanner() {
        return scanner;
    }

    public CoffeeConnector getConnector() {
        return connector;
    }

    public CoffeeServerList getServerList() {
        return serverList;
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
            switch(state) {
                case BluetoothAdapter.STATE_ON: {
                    enabled.set(true);
                    break;
                }
                case BluetoothAdapter.STATE_OFF: {
                    enabled.set(false);
                    break;
                }
            }
        }
    };

    public class ManagerBinder extends Binder {
        public CoffeeManager getService(ServiceDependantActivity activity) {
            CoffeeManager.this.activity = activity;
            return CoffeeManager.this;
        }
    }
}
