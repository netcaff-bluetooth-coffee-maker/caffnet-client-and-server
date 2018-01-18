package com.quew8.netcaff.server.machine;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

import com.quew8.properties.deferred.Deferred;
import com.quew8.properties.deferred.Promise;

/**
 * @author Quew8
 */
class MachineHandlerThread extends HandlerThread {
    private static final String BASE_NAME = "MachineThread-";

    private final Handler handler;
    private Machine machine;

    MachineHandlerThread(String deviceName) {
        super(BASE_NAME + deviceName, Process.THREAD_PRIORITY_FOREGROUND);
        start();
        this.handler = new Handler(getLooper());
    }

    Promise<Machine> init(UsbManager usbManager, UsbDevice device) {
        Handler h = new Handler();
        Deferred<Machine> d = new Deferred<>();
        handler.post(() -> {
            machine = Machine.fromUSB(this, usbManager, device);
            h.post(() -> d.resolve(machine));
        });
        return d.promise();
    }

    Promise<Machine> disconnect() {
        Handler h = new Handler();
        Deferred<Machine> d = new Deferred<>();
        handler.post(() -> {
            machine.disconnected();
            h.post(() -> d.resolve(machine));
            quitSafely();
        });
        return d.promise();
    }

    void runOnThread(Runnable r) {
        handler.post(r);
    }
}
