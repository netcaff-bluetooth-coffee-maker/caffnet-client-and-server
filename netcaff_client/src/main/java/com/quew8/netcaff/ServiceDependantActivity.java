package com.quew8.netcaff;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;

import com.quew8.netcaff.ble.CoffeeConnector;
import com.quew8.netcaff.ble.CoffeeManager;
import com.quew8.netcaff.ble.CoffeeScanner;
import com.quew8.netcaff.ble.CoffeeServerList;

/**
 * @author Quew8
 */
public abstract class ServiceDependantActivity extends AbstractActivity {
    private CoffeeManager manager;
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            CoffeeManager.ManagerBinder binder = (CoffeeManager.ManagerBinder) service;
            manager = binder.getService(ServiceDependantActivity.this);
            onConnectedToManager();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            manager = null;
            onDisconnectedFromManager();
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, CoffeeManager.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(serviceConnection);
    }

    public void onConnectedToManager() {

    }

    public void onDisconnectedFromManager() {

    }

    public boolean isConnectedToManagerService() {
        return manager != null;
    }

    public CoffeeManager getManager() {
        return manager;
    }

    public CoffeeScanner getScanner() {
        return getManager().getScanner();
    }

    public CoffeeConnector getConnector() {
        return getManager().getConnector();
    }

    public CoffeeServerList getServerList() {
        return getManager().getServerList();
    }


}
