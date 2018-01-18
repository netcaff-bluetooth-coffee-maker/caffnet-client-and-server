package com.quew8.netcaff.server;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.quew8.netcaff.server.access.UserList;
import com.quew8.netcaff.server.ble.BleAdvertiser;
import com.quew8.netcaff.server.ble.BleManager;
import com.quew8.netcaff.server.ble.BleServer;
import com.quew8.netcaff.server.machine.MachineManager;
import com.quew8.properties.Property;
import com.quew8.properties.ReadOnlyProperty;

/**
 * @author Quew8
 */
public class CaffNetServerService extends Service {
    private static final String NOTIFICATION_CHANNEL_ID =
            CaffNetServerActivity.class.getSimpleName() + "_channel";
    private static final int FOREGROUND_NOTIFICATION_ID = 1;

    private final IBinder binder = new ServiceBinder();
    private final Property<String> startupError = new Property<>(null);

    private ServerCoffeeServer coffeeServer;
    private UserList userList;
    private BleManager bleManager;
    private MachineManager machineManager;

    @Override
    public void onCreate() {
        super.onCreate();

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(notificationManager == null) {
            throw new RuntimeException("Null notification manager");
        }

        NotificationChannel notificationChannel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
        );
        notificationChannel.setDescription(getString(R.string.notification_channel_desc));
        notificationChannel.enableLights(false);
        notificationChannel.enableVibration(false);
        notificationManager.createNotificationChannel(notificationChannel);

        Intent notificationIntent = new Intent(this, CaffNetServerActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification =
                new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                        .setContentTitle(getText(R.string.notification_title))
                        .setContentText(getText(R.string.notification_content))
                        .setSmallIcon(R.drawable.ic_notification_small)
                        .setContentIntent(pendingIntent)
                        .build();

        try {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification);

            userList = new UserList();
            userList.addUser("quew8", "hello");
            userList.addUser("Alan@Alan", "Fine sea salt, store in a cool, dry place.");
            coffeeServer = new ServerCoffeeServer(12, userList);

            bleManager = new BleManager(this, coffeeServer);
            machineManager = new MachineManager(this, coffeeServer);
            bleManager.register();
            bleManager.startup();
            coffeeServer.startCheckLevelsLoop();
        } catch(UnsupportedSystemServiceException ex) {
            Log.e("CaffNetServerService", "Startup error - " + ex.getMessage());
            startupError.set(ex.getMessage());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        machineManager.shutdown();
        bleManager.shutdown();
        bleManager.unregister();
    }

    public ReadOnlyProperty<String> getStartupError() {
        return startupError;
    }

    public boolean hasStartupError() {
        return getStartupError().get() != null;
    }

    public ServerCoffeeServer getCoffeeServer() {
        return coffeeServer;
    }

    public UserList getUserList() {
        return userList;
    }

    public BleManager getBleManager() {
        return bleManager;
    }

    public BleServer getBleServer() {
        return getBleManager().getBleServer();
    }

    public BleAdvertiser getBleAdvertiser() {
        return getBleManager().getBleAdvertiser();
    }

    public MachineManager getMachineManager() {
        return machineManager;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    class ServiceBinder extends Binder {
        CaffNetServerService getService() {
            return CaffNetServerService.this;
        }
    }
}
