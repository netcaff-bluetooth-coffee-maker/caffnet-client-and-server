package com.quew8.netcaff;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import com.quew8.properties.deferred.Deferred;
import com.quew8.properties.deferred.Promise;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author Quew8
 */
public abstract class AbstractActivity extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static int COUNTER = 0;
    private final ArrayList<CodeCallback<Intent>> resultCallbacks = new ArrayList<>();
    private final ArrayList<CodeCallback<String[]>> permissionCallbacks = new ArrayList<>();

    private static int createCounter() {
        return COUNTER++;
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

    private static class CodeCallback<T> extends Deferred<T> {
        final int code;

        CodeCallback(int code) {
            this.code = code;
        }
    }
}
