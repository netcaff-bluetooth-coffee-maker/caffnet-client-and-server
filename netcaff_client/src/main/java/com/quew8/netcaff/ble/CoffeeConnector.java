package com.quew8.netcaff.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.quew8.netcaff.ClientCoffeeServer;
import com.quew8.netcaff.lib.ble.util.BLEUtil;
import com.quew8.properties.Property;
import com.quew8.properties.ReadOnlyProperty;

import java.util.Arrays;
import java.util.UUID;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;

/**
 * @author Quew8
 */
public class CoffeeConnector extends BluetoothGattCallback {
    private static final String TAG = CoffeeConnector.class.getSimpleName();

    private final Context ctx;
    private final CoffeeScanner scanner;
    private final Property<ConnectorStatus> externalStatus;
    private final Property<InternalStatus> status;
    private final Handler handler;
    private BluetoothGatt connection;
    private ClientCoffeeServer targetServer;
    private ConnectCallback connectCallback;
    private WriteCallback writeCallback;
    private ReadCallback readCallback;
    private BluetoothGattCharacteristic characteristic;
    private UpdateNotifyCallback updateNotifyCallback;
    private ReadDescriptorCallback readDescriptorCallback;
    private BluetoothGattDescriptor descriptor;

    CoffeeConnector(Context ctx, CoffeeScanner scanner) {
        this.ctx = ctx;
        this.scanner = scanner;
        this.externalStatus = new Property<>(ConnectorStatus.INACTIVE);
        this.status = new Property<>(InternalStatus.DESTROYED);
        this.status.addListener((n,o) -> {
            Log.d(TAG, "New connector status: " + n.toString());
            if(n == InternalStatus.ERROR) {
                externalStatus.set(ConnectorStatus.ERROR);
            }
        });
        this.handler = new Handler();
        this.connection = null;
        this.targetServer = null;
    }

    public void scanForAndConnectTo(ClientCoffeeServer server, ConnectCallback connectCallback) {
        externalStatus.set(ConnectorStatus.CONNECTING);
        scanner.scanForDevice(new CoffeeScanner.FindScanCallback(server) {
            @Override
            public void done(ClientCoffeeServer server) {
                if(server != null) {
                    connectTo(server, connectCallback);
                } else {
                    status.set(InternalStatus.ERROR);
                }
            }
        });
    }

    private void connectTo(ClientCoffeeServer server, ConnectCallback connectCallback) {
        /*if(status.get() == InternalStatus.DISCONNECTED) {
            if(!targetDevice.getAddress().equals(server.getDevice().getAddress())) {
                throw new IllegalStateException("Cannot connect to a different a device before previous connection has been destroyed");
            }
        } else*/ if(status.get() != InternalStatus.DESTROYED) {
            throw new IllegalStateException("Cannot connect to device whilst in " + status.get().toString() + " state");
        }
        externalStatus.set(ConnectorStatus.CONNECTING);
        this.targetServer = server;
        this.connectCallback = connectCallback;
        handler.post(this::doConnectTo);
    }

    public void disconnect() {
        if(status.get() != InternalStatus.DISCOVERED) {
            throw new IllegalStateException("Cannot disconnect to device whilst in " + status.get().toString() + " state");
        }
        externalStatus.set(ConnectorStatus.DISCONNECTING);
        handler.post(this::doDisconnect);
    }

    public void destroy() {
        if(status.get() != InternalStatus.DISCONNECTED) {
            throw new IllegalStateException("Cannot destroy connection whilst in " + status.get().toString() + " state");
        }
        handler.post(this::doDestroy);
    }

    public void writeCharacteristic(UUID uuid, byte[] data, WriteCallback writeCallback) {
        if(status.get() != InternalStatus.DISCOVERED) {
            throw new IllegalStateException("Cannot write a characteristic whilst in " + status.get().toString() + " state");
        }
        externalStatus.set(ConnectorStatus.COMMS);
        this.characteristic = BLEUtil.findCharacteristic(connection.getServices(), uuid);
        this.characteristic.setValue(data);
        this.writeCallback = writeCallback;
        handler.post(this::doWrite);
    }

    public void readCharacteristic(UUID uuid, ReadCallback readCallback) {
        if(status.get() != InternalStatus.DISCOVERED) {
            throw new IllegalStateException("Cannot read a characteristic whilst in " + status.get().toString() + " state");
        }
        externalStatus.set(ConnectorStatus.COMMS);
        this.characteristic = BLEUtil.findCharacteristic(connection.getServices(), uuid);
        this.readCallback = readCallback;
        handler.post(this::doRead);
    }

    public void setNotify(UUID uuid, boolean enabled, UpdateNotifyCallback updateNotifyCallback) {
        if(status.get() != InternalStatus.DISCOVERED) {
            throw new IllegalStateException("Cannot write a characteristic whilst in " + status.get().toString() + " state");
        }
        externalStatus.set(ConnectorStatus.COMMS);
        this.descriptor = BLEUtil.findDescriptor(connection.getServices(), uuid);
        this.descriptor.setValue(enabled ?
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE :
                BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        );
        this.updateNotifyCallback = updateNotifyCallback;
        handler.post(this::doDescWrite);
    }

    public void readDescriptor(UUID uuid, ReadDescriptorCallback readCallback) {
        if(status.get() != InternalStatus.DISCOVERED) {
            throw new IllegalStateException("Cannot read a characteristic whilst in " + status.get().toString() + " state");
        }
        externalStatus.set(ConnectorStatus.COMMS);
        this.descriptor = BLEUtil.findDescriptor(connection.getServices(), uuid);
        this.readDescriptorCallback = readCallback;
        handler.post(this::doDescRead);
    }

    public ReadOnlyProperty<ConnectorStatus> getStatus() {
        return externalStatus;
    }

    private void doConnectTo() {
        if(status.get() == InternalStatus.DISCONNECTED) {
            doDestroy();
        }
        if(status.get() != InternalStatus.DESTROYED) {
            status.set(InternalStatus.ERROR);
            throw new IllegalStateException("doConnectTo whilst not destroyed");
        }
        status.set(InternalStatus.CONNECTING);
        connection = targetServer.getDevice().connectGatt(ctx, false, this);
    }

    private void doDisconnect() {
        if(status.get() != InternalStatus.DISCOVERED) {
            status.set(InternalStatus.ERROR);
            throw new IllegalStateException("doDisconnect whilst not connected");
        }
        status.set(InternalStatus.DISCONNECTING);
        connection.disconnect();
    }

    private void doDestroy() {
        if(status.get() != InternalStatus.DISCONNECTED) {
            status.set(InternalStatus.ERROR);
            throw new IllegalStateException("doDestroy whilst not disconnected");
        }
        connection.close();
        connection = null;
        status.set(InternalStatus.DESTROYED);
    }

    private void doDiscover() {
        if(status.get() != InternalStatus.CONNECTED) {
            status.set(InternalStatus.ERROR);
            throw new IllegalStateException("doDiscover whilst not connected");
        }
        status.set(InternalStatus.DISCOVERING);
        if(!connection.discoverServices()) {
            Log.e(TAG, "Failed to initiate discover");
        }
    }

    private void doWrite() {
        if(status.get() != InternalStatus.DISCOVERED) {
            status.set(InternalStatus.ERROR);
            throw new IllegalStateException("doWrite whilst not connected");
        }
        status.set(InternalStatus.WRITING);
        if(!connection.writeCharacteristic(this.characteristic)) {
            Log.e(TAG, "Failed to initiate write");
        }
        this.characteristic = null;
    }

    private void doRead() {
        if(status.get() != InternalStatus.DISCOVERED) {
            status.set(InternalStatus.ERROR);
            throw new IllegalStateException("doRead whilst not connected");
        }
        status.set(InternalStatus.READING);
        if(!connection.readCharacteristic(this.characteristic)) {
            Log.e(TAG, "Failed to initiate read");
        }
        this.characteristic = null;
    }

    private void doDescWrite() {
        if(status.get() != InternalStatus.DISCOVERED) {
            status.set(InternalStatus.ERROR);
            throw new IllegalStateException("doDescWrite whilst not connected");
        }
        status.set(InternalStatus.WRITING_DESC);
        if(!connection.writeDescriptor(this.descriptor)) {
            Log.e(TAG, "Failed to initiate write");
        }
        this.descriptor = null;
    }

    private void doDescRead() {
        if(status.get() != InternalStatus.DISCOVERED) {
            status.set(InternalStatus.ERROR);
            throw new IllegalStateException("doDescRead whilst not connected");
        }
        status.set(InternalStatus.READING_DESC);
        if(!connection.readDescriptor(this.descriptor)) {
            Log.e(TAG, "Failed to initiate read");
        }
        this.descriptor = null;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int statusCode, int newState) {
        Log.d(TAG, "onConnectionStateChange(" + BLEUtil.getStatusString(statusCode) +
                ", " + BLEUtil.getConnectionStateString(newState) + ")");

        if(statusCode == GATT_SUCCESS) {
            if(newState == STATE_CONNECTED) {
                if(status.get() != InternalStatus.CONNECTING) {
                    throw new IllegalStateException("Unexpected connection");
                }
                status.set(InternalStatus.CONNECTED);
                handler.post(this::doDiscover);
            } else if(newState == STATE_DISCONNECTED) {
                boolean unexpected = status.get() != InternalStatus.DISCONNECTING;
                if(unexpected) {
                    Log.w(TAG, "Unexpected disconnection from BLE server");
                }
                status.set(InternalStatus.DISCONNECTED);
                if(connectCallback != null) {
                    if(unexpected) {
                        connectCallback.onUnexpectedDisconnect();
                    } else {
                        connectCallback.onDisconnect();
                    }
                    connectCallback = null;
                }
                externalStatus.set(ConnectorStatus.INACTIVE);
            }
        } else {
            try {
                status.set(InternalStatus.DISCONNECTED);
                if(connectCallback != null) {
                    connectCallback.onConnectFailure(statusCode);
                    connectCallback = null;
                } else {
                    throw new ConnectorException();
                }
                externalStatus.set(ConnectorStatus.INACTIVE);
            } catch(ConnectorException ex) {
                status.set(InternalStatus.ERROR);
            }
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int statusCode) {
        Log.d(TAG, "onServicesDiscovered(" + BLEUtil.getStatusString(statusCode) + ")");

        if(statusCode == BluetoothGatt.GATT_SUCCESS) {
            status.set(InternalStatus.DISCOVERED);
            if(connectCallback != null) {
                connectCallback.onConnect();
            }
            externalStatus.set(ConnectorStatus.CONNECTED);
        } else {
            try {
                if(connectCallback != null) {
                    connectCallback.onConnectFailure(statusCode);
                    connectCallback = null;
                } else {
                    throw new ConnectorException();
                }
            } catch(ConnectorException ex) {
                status.set(InternalStatus.ERROR);
            }
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                     int statusCode) {

        Log.d(TAG, "onCharacteristicRead(" + characteristic.getUuid().toString() +
                ", " + BLEUtil.getStatusString(statusCode) + ")");

        ReadCallback thisReadCallback = readCallback;
        readCallback = null;

        if(statusCode == BluetoothGatt.GATT_SUCCESS) {
            status.set(InternalStatus.DISCOVERED);
            if(thisReadCallback != null) {
                thisReadCallback.onRead(characteristic, characteristic.getValue());
            }
            externalStatus.set(ConnectorStatus.CONNECTED);
        } else {
            try {
                status.set(InternalStatus.DISCOVERED);
                if(thisReadCallback != null) {
                    thisReadCallback.onReadFailure(characteristic, statusCode);
                } else {
                    throw new ConnectorException();
                }
                externalStatus.set(ConnectorStatus.CONNECTED);
            } catch(ConnectorException ex) {
                status.set(InternalStatus.ERROR);
            }
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                      int statusCode) {

        Log.d(TAG, "onCharacteristicWrite(" + characteristic.getUuid().toString() +
                ", " + BLEUtil.getStatusString(statusCode) + ")");

        WriteCallback thisWriteCallback = writeCallback;
        writeCallback = null;

        if(statusCode == BluetoothGatt.GATT_SUCCESS) {
            status.set(InternalStatus.DISCOVERED);
            if(thisWriteCallback != null) {
                thisWriteCallback.onWrite(characteristic);
            }
            externalStatus.set(ConnectorStatus.CONNECTED);
        } else {
            try {
                status.set(InternalStatus.DISCOVERED);
                if(thisWriteCallback != null) {
                    thisWriteCallback.onWriteFailure(characteristic, statusCode);
                } else {
                    throw new ConnectorException();
                }
                externalStatus.set(ConnectorStatus.CONNECTED);
            } catch(ConnectorException ex) {
                status.set(InternalStatus.ERROR);
            }
        }
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                 int statusCode) {

        Log.d(TAG, "onDescriptorRead(" + descriptor.getUuid().toString() +
                ", " + BLEUtil.getStatusString(statusCode) + ")");

        ReadDescriptorCallback thisReadCallback = readDescriptorCallback;
        readDescriptorCallback = null;

        boolean value = false;
        boolean valid = false;
        if(Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, descriptor.getValue())) {
            value = true;
            valid = true;
        } else if(Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, descriptor.getValue())) {
            value = false;
            valid = true;
        }
        if(statusCode == BluetoothGatt.GATT_SUCCESS && valid) {
            status.set(InternalStatus.DISCOVERED);
            if(thisReadCallback != null) {
                thisReadCallback.onDescriptorRead(descriptor, value);
            }
            externalStatus.set(ConnectorStatus.CONNECTED);
        } else {
            try {
                status.set(InternalStatus.DISCOVERED);
                if(thisReadCallback != null) {
                    thisReadCallback.onDescriptorReadFailure(descriptor, statusCode);
                } else {
                    throw new ConnectorException();
                }
                externalStatus.set(ConnectorStatus.CONNECTED);
            } catch(ConnectorException ex) {
                status.set(InternalStatus.ERROR);
            }
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                  int statusCode) {

        Log.d(TAG, "onDescriptorWrite(" + descriptor.getUuid().toString() +
                ", " + BLEUtil.getStatusString(statusCode) + ")");

        UpdateNotifyCallback thisNotifyCallback = updateNotifyCallback;

        boolean value = false;
        boolean valid = false;
        if(Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, descriptor.getValue())) {
            value = true;
            valid = true;
        } else if(Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, descriptor.getValue())) {
            value = false;
            valid = true;
        }
        if(statusCode == BluetoothGatt.GATT_SUCCESS && valid) {
            status.set(InternalStatus.DISCOVERED);
            if(updateNotifyCallback != null) {
                if(value) {
                    thisNotifyCallback.onEnabled(descriptor);
                } else {
                    updateNotifyCallback = null;
                    thisNotifyCallback.onDisabled(descriptor);
                }
            }
            connection.setCharacteristicNotification(descriptor.getCharacteristic(), value);
            externalStatus.set(ConnectorStatus.CONNECTED);
        } else {
            try {
                status.set(InternalStatus.DISCOVERED);
                if(thisNotifyCallback != null) {
                    updateNotifyCallback = null;
                    thisNotifyCallback.onDescriptorWriteFailure(descriptor, statusCode);
                } else {
                    throw new ConnectorException();
                }
                externalStatus.set(ConnectorStatus.CONNECTED);
            } catch(ConnectorException ex) {
                status.set(InternalStatus.ERROR);
            }
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        Log.d(TAG, "onCharacteristicChanged(" + characteristic.getUuid().toString() + ")");

        if(updateNotifyCallback != null) {
            updateNotifyCallback.onUpdate(characteristic, characteristic.getValue());
        }
    }

    private enum InternalStatus {
        DESTROYED, CONNECTING, CONNECTED, DISCOVERING, DISCOVERED,
        READING, WRITING, READING_DESC, WRITING_DESC,
        DISCONNECTING, DISCONNECTED, ERROR
    }

    public enum ConnectorStatus {
        INACTIVE, CONNECTING, CONNECTED, COMMS, DISCONNECTING, ERROR
    }

    public interface ConnectCallback {
        void onConnect();
        void onDisconnect();
        void onUnexpectedDisconnect();
        default void onConnectFailure(int statusCode) throws ConnectorException {
            throw new ConnectorException("onConnectFailure(" + Integer.toString(statusCode) + ")");
        }
    }

    public interface WriteCallback {
        void onWrite(BluetoothGattCharacteristic characteristic);
        default void onWriteFailure(BluetoothGattCharacteristic characteristic, int statusCode) throws ConnectorException {
            throw new ConnectorException("onWriteFailure(" +
                    characteristic.getUuid().toString() + ", " +
                    Integer.toString(statusCode) +
                    ")");
        }
    }

    public interface ReadCallback {
        void onRead(BluetoothGattCharacteristic characteristic, byte[] data);
        default void onReadFailure(BluetoothGattCharacteristic characteristic, int statusCode) throws ConnectorException {
            throw new ConnectorException("onReadFailure(" +
                    characteristic.getUuid().toString() + ", " +
                    Integer.toString(statusCode) +
                    ")");
        }
    }

    public interface UpdateNotifyCallback {
        void onEnabled(BluetoothGattDescriptor descriptor);
        void onUpdate(BluetoothGattCharacteristic characteristic, byte[] data);
        void onDisabled(BluetoothGattDescriptor descriptor);
        default void onDescriptorWriteFailure(BluetoothGattDescriptor descriptor, int statusCode) throws ConnectorException {
            throw new ConnectorException("onDescriptorWriteFailure(" +
                    descriptor.getUuid().toString() + ", " +
                    Integer.toString(statusCode)
                    + ")");
        }
    }

    public interface ReadDescriptorCallback {
        void onDescriptorRead(BluetoothGattDescriptor descriptor, boolean enabled);
        default void onDescriptorReadFailure(BluetoothGattDescriptor descriptor, int statusCode) throws ConnectorException {
            throw new ConnectorException("onDescriptorReadFailure(" +
                    descriptor.getUuid().toString() + ", " +
                    Integer.toString(statusCode) +
                    ")");
        }
    }

    public static class ConnectorException extends Exception {
        ConnectorException(String message) {
            super(message);
        }

        public ConnectorException() {
            this("Generic error");
        }
    }
}
