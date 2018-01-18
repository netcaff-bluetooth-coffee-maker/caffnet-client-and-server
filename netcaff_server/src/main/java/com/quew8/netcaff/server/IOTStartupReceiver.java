package com.quew8.netcaff.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * @author Quew8
 */
public class IOTStartupReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, CaffNetServerService.class);
        context.startService(serviceIntent);
    }
}
