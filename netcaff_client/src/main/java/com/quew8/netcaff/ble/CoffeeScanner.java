package com.quew8.netcaff.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.quew8.netcaff.ClientCoffeeServer;
import com.quew8.netcaff.lib.ble.util.DecodedScanData;
import com.quew8.netcaff.lib.server.CoffeeServerID;
import com.quew8.netcaff.lib.server.Structs;
import com.quew8.netcaff.lib.server.StructureFormatException;
import com.quew8.properties.BooleanProperty;
import com.quew8.properties.ReadOnlyBooleanProperty;

import java.util.ArrayList;

import static com.quew8.netcaff.lib.ble.CoffeeServerProfile.COFFEE_REQUEST_SERVICE;

/**
 * @author Quew8
 */
public class CoffeeScanner implements BluetoothAdapter.LeScanCallback {
    private static final String TAG = CoffeeScanner.class.getSimpleName();

    private static final long SCAN_PERIOD = 5000;

    private final Context ctx;
    private final CoffeeManager manager;
    private final BluetoothAdapter adapter;
    private final ArrayList<String> scannedAddresses;
    private final BooleanProperty scanning;
    private final Handler handler;
    private ScanCallback callback;

    CoffeeScanner(Context ctx, CoffeeManager manager, BluetoothAdapter adapter) {
        this.ctx = ctx;
        this.manager = manager;
        this.adapter = adapter;
        this.scannedAddresses = new ArrayList<>();
        this.scanning = new BooleanProperty(false);
        this.handler = new Handler();
    }

    public void scan(ScanCallback callback) {
        if(scanning.get()) {
            throw new IllegalStateException("Cannot start a new scan whilst another is underway");
        }
        this.callback = callback;
        manager.ensureBluetooth().done(this::doScan);
    }

    public void scanForDevice(FindScanCallback callback) {
        scan(callback);
    }

    public void stopScan() {
        if(scanning.get()) {
            scanning.set(false);
            adapter.stopLeScan(this);
            if(callback != null) {
                callback.onScanEnded(this);
                callback = null;
            }
        }
    }

    private void doScan() {
        scannedAddresses.clear();
        scanning.set(true);
        if(callback != null) {
            callback.onScanStarted(this);
        }
        manager.getServerList().startingScan();
        handler.postDelayed(this::stopScan, SCAN_PERIOD);
        adapter.startLeScan(this);
    }

    public ReadOnlyBooleanProperty isScanning() {
        return scanning;
    }

    @Override
    public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
        if(!scannedAddresses.contains(device.getAddress())) {
            scannedAddresses.add(device.getAddress());
            DecodedScanData decoded = DecodedScanData.decode(scanRecord);
            if(decoded.hasUUID(COFFEE_REQUEST_SERVICE)) {
                try {
                    CoffeeServerID serverId = new CoffeeServerID();
                    byte[] serviceData = decoded.getServiceData(COFFEE_REQUEST_SERVICE);
                    Structs.readIn(serverId, serviceData);
                    if(callback == null || callback.verify(serverId)) {
                        boolean discovered = false;
                        ClientCoffeeServer server = manager.getServerList().getServerOrNull(serverId);
                        if(server == null) {
                            discovered = true;
                            server = manager.getServerList().addServer(serverId);
                        }
                        Structs.readIn(server.getAdData(), serviceData);
                        server.setDevice(device);
                        if(callback != null) {
                            if(discovered) {
                                callback.onDiscovered(this, server);
                            }
                            callback.onScanned(this, server);
                        }
                    }
                } catch(StructureFormatException ex) {
                    Log.w(TAG, "Malformed ad data: " + ex.getMessage());
                }
            }
        }
    }

    public interface ScanCallback {
        default boolean verify(CoffeeServerID id) {
            return true;
        }
        default void onScanStarted(CoffeeScanner scanner) {}
        default void onDiscovered(CoffeeScanner scanner, ClientCoffeeServer server) {}
        default void onScanned(CoffeeScanner scanner, ClientCoffeeServer server) {}
        default void onScanEnded(CoffeeScanner scanner) {}
    }

    public static class FindScanCallback implements ScanCallback {
        private final ClientCoffeeServer lookingFor;
        private boolean found = false;

        protected FindScanCallback(ClientCoffeeServer lookingFor) {
            this.lookingFor = lookingFor;
        }

        @Override
        public boolean verify(CoffeeServerID id) {
            return lookingFor.getAdData().getServerId().equals(id);
        }

        @Override
        public void onScanned(CoffeeScanner scanner, ClientCoffeeServer server) {
            if(!found) {
                found = true;
                scanner.stopScan();
                done(server);
            }
        }

        @Override
        public void onScanEnded(CoffeeScanner scanner) {
            if(!found) {
                done(null);
            }
        }

        public void done(ClientCoffeeServer server) {}
    }
}
