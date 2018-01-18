package com.quew8.netcaff.server.machine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.quew8.netcaff.server.ServerCoffeeServer;
import com.quew8.netcaff.server.UnsupportedSystemServiceException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Quew8
 */
public class MachineManager extends BroadcastReceiver {
    private static final String TAG = MachineManager.class.getSimpleName();

    public static final UsbDeviceType[] DEVICE_WHITE_LIST = {
            new UsbDeviceType(67, 9025),
            new UsbDeviceType(579, 9025),
            new UsbDeviceType(67, 10755)
    };
    public static final UsbDeviceType[] DEVICE_BLACK_LIST = {
            new UsbDeviceType(60416, 1060)
    };

    private final Context context;
    private final ServerCoffeeServer coffeeServer;
    private final UsbManager usbManager;
    private final HashMap<String, MachineHandlerThread> connectedMachines;

    public MachineManager(Context context, ServerCoffeeServer coffeeServer) throws UnsupportedSystemServiceException {
        this.context = context;
        this.coffeeServer = coffeeServer;
        this.usbManager = context.getSystemService(UsbManager.class);
        if(usbManager == null) {
            throw new UnsupportedSystemServiceException(UsbManager.class);
        }
        this.connectedMachines = new HashMap<>();

        Map<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        for(UsbDevice device: usbDevices.values()) {
            this.onNewDevice(device);
        }

        IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        context.registerReceiver(this, filter);
    }

    public void shutdown() {
        for(String machineName: connectedMachines.keySet()) {
            removeDevice(connectedMachines.get(machineName));
        }
        connectedMachines.clear();
        context.unregisterReceiver(this);
    }

    private void onNewDevice(UsbDevice device) {
        Log.i(TAG, "Device found: \"" + device.getDeviceName() + "\", " +
                device.getVendorId() + ", \"" + device.getManufacturerName() + "\", " +
                device.getProductId() + ", \"" + device.getProductName() + "\""
        );
        if(inList(DEVICE_WHITE_LIST, device)) {
            String deviceName = device.getDeviceName();
            MachineHandlerThread thread = new MachineHandlerThread(deviceName);
            thread.init(usbManager, device).done(coffeeServer::addMachine);
            connectedMachines.put(deviceName, thread);
        }
    }

    private void onDeviceDisconnect(UsbDevice device) {
        String deviceName = device.getDeviceName();
        if(connectedMachines.containsKey(deviceName)) {
            removeDevice(connectedMachines.remove(deviceName));
        }
    }

    private void removeDevice(MachineHandlerThread thread) {
        thread.disconnect().done(coffeeServer::removeMachine);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if(UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if(device != null) {
                onNewDevice(device);
            }
        } else if(UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if(device != null) {
                onDeviceDisconnect(device);
            }
        }
    }

    private static boolean inList(UsbDeviceType[] list, UsbDevice device) {
        return Arrays.stream(list).anyMatch((d) -> d.match(device));
    }

    private static class UsbDeviceType {
        private final int productId;
        private final int vendorId;

        private UsbDeviceType(int productId, int vendorId) {
            this.productId = productId;
            this.vendorId = vendorId;
        }

        private boolean match(UsbDevice device) {
            return device.getProductId() == productId &&
                    device.getVendorId() == vendorId;
        }
    }
}
