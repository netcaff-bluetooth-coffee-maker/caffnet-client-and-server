package com.quew8.netcaff;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.quew8.netcaff.ble.CoffeeConnector;
import com.quew8.netcaff.ble.CoffeeManager;
import com.quew8.netcaff.ble.CoffeeScanner;
import com.quew8.netcaff.ble.CoffeeServerList;
import com.quew8.properties.deferred.Deferred;
import com.quew8.properties.deferred.Promise;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author Quew8
 */
public abstract class AbstractActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private final ArrayList<CodeCallback<Intent>> resultCallbacks = new ArrayList<>();
    private final ArrayList<CodeCallback<String[]>> permissionCallbacks = new ArrayList<>();
    private CoffeeManager manager;
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            CoffeeManager.ManagerBinder binder = (CoffeeManager.ManagerBinder) service;
            manager = binder.getService(AbstractActivity.this);
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

    public Promise<Intent> runActivity(Intent intent) {
        int code = createCounter();
        CodeCallback<Intent> d = new CodeCallback<>(code);
        resultCallbacks.add(d);
        startActivityForResult(intent, code);
        return d.promise();
    }

    public Promise<String[]> requestPermission(String permission) {
        return requestPermissions(new String[]{permission});
    }

    public Promise<String[]> requestPermissions(String[] permissions) {
        int code = createCounter();
        CodeCallback<String[]> d = new CodeCallback<>(code);
        permissionCallbacks.add(d);
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                code
        );
        return d.promise();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Iterator<CodeCallback<Intent>> it = resultCallbacks.iterator();
        while(it.hasNext()) {
            CodeCallback<Intent> d = it.next();
            if(d.code == requestCode) {
                if(resultCode != Activity.RESULT_CANCELED) {
                    d.resolve(data);
                } else {
                    d.fail();
                }
                it.remove();
                break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        ArrayList<String> grantedPermissions = new ArrayList<>();
        for(int i = 0; i < permissions.length; i++) {
            if(grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                grantedPermissions.add(permissions[i]);
            }
        }
        Iterator<CodeCallback<String[]>> it = permissionCallbacks.iterator();
        while(it.hasNext()) {
            CodeCallback<String[]> d = it.next();
            if(d.code == requestCode) {
                if(grantedPermissions.size() > 0) {
                    d.resolve(grantedPermissions.toArray(new String[grantedPermissions.size()]));
                } else {
                    d.fail();
                }
                it.remove();
                break;
            }
        }
    }



    private static int COUNTER = 0;
    private static int createCounter() {
        return COUNTER++;
    }

    private static class CodeCallback<T> extends Deferred<T> {
        final int code;

        CodeCallback(int code) {
            this.code = code;
        }
    }
}
