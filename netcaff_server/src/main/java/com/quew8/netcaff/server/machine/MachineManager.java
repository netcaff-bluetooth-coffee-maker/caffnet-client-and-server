package com.quew8.netcaff.server.machine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.quew8.netcaff.server.ServerCoffeeServer;

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

    private final Context ctx;
    private final ServerCoffeeServer coffeeServer;
    private final UsbManager usbManager;
    private final HashMap<String, Machine> connectedMachines;

    public MachineManager(Context ctx, ServerCoffeeServer coffeeServer) {
        this.ctx = ctx;
        this.coffeeServer = coffeeServer;
        this.usbManager = ctx.getSystemService(UsbManager.class);
        this.connectedMachines = new HashMap<>();

        Map<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        for(UsbDevice device: usbDevices.values()) {
            this.onNewDevice(device);
        }

        IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        ctx.registerReceiver(this, filter);
    }

    private void onNewDevice(UsbDevice device) {
        Log.i(TAG, "Device found: \"" + device.getDeviceName() + "\", " +
                device.getVendorId() + ", \"" + device.getManufacturerName() + "\", " +
                device.getProductId() + ", \"" + device.getProductName() + "\""
        );
        if(inList(DEVICE_WHITE_LIST, device)) {
        //if(!inList(DEVICE_BLACK_LIST, device)) {
        /*if((device.getVendorId() != 1060 && device.getProductId() != 60416) ||
                (device.getVendorId() == 10755 && device.getProductId() == 67)) {*/

            String deviceName = device.getDeviceName();
            Machine m = Machine.fromUSB(usbManager, device);
            connectedMachines.put(deviceName, m);
            coffeeServer.addMachine(m);
        }
    }

    private void onDeviceDisconnect(UsbDevice device) {
        String deviceName = device.getDeviceName();
        if(connectedMachines.containsKey(deviceName)) {
            Machine m = connectedMachines.remove(deviceName);
            coffeeServer.removeMachine(m);
            m.disconnected();
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if(UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if(device != null) {
                Log.d(TAG, "Device attached");
                onNewDevice(device);
            }
        } else if(UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if(device != null) {
                Log.d(TAG, "Device detached");
                onDeviceDisconnect(device);
                // call your method that cleans up and closes communication with the device
            }
        }
    }

    private static boolean inList(UsbDeviceType[] list, UsbDevice device) {
        return Arrays.stream(list).anyMatch((d) -> d.match(device));
    }

    private static class UsbDeviceType {
        public final int productId;
        public final int vendorId;

        public UsbDeviceType(int productId, int vendorId) {
            this.productId = productId;
            this.vendorId = vendorId;
        }

        public boolean match(UsbDevice device) {
            return device.getProductId() == productId &&
                    device.getVendorId() == vendorId;
        }
    }
}
