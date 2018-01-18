package com.quew8.netcaff.server.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.quew8.netcaff.lib.ble.CoffeeServerProfile;
import com.quew8.netcaff.lib.ble.util.BLEUtil;
import com.quew8.netcaff.lib.server.CharacteristicStruct;
import com.quew8.netcaff.lib.server.Structs;
import com.quew8.netcaff.lib.server.StructureFormatException;
import com.quew8.netcaff.server.ServerCoffeeServer;
import com.quew8.properties.BooleanProperty;
import com.quew8.properties.Property;
import com.quew8.properties.ReadOnlyProperty;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

/**
 * @author Quew8
 */
public class BleServer extends BluetoothGattServerCallback {
    private static final String TAG = BleServer.class.getSimpleName();

    private static final int AD_STOP_DELAY_MS = 500;

    private final Context ctx;
    private final Handler handler;
    private final Property<ServerStatus> status;
    private final Property<BluetoothDevice> connectedDevice;
    private final BooleanProperty requestServiceNotify;
    private final BooleanProperty loginServiceNotify;
    private final BluetoothManager manager;
    private final BleAdvertiser bleAdvertiser;
    private final ServerCoffeeServer coffeeServer;

    private BluetoothGattServer gattServer;

    BleServer(Context ctx, BluetoothManager manager, BleAdvertiser bleAdvertiser, ServerCoffeeServer coffeeServer) {
        this.ctx = ctx;
        this.handler = new Handler();
        this.status = new Property<>(ServerStatus.INACTIVE);
        this.connectedDevice = new Property<>(null);
        this.requestServiceNotify = new BooleanProperty(false);
        this.loginServiceNotify = new BooleanProperty(false);
        this.manager = manager;
        this.bleAdvertiser = bleAdvertiser;
        this.coffeeServer = coffeeServer;
        coffeeServer.getReply().addModifiedCallback(this::notifyOfReplyChanged);
        coffeeServer.getResponseUserAccessCode().addModifiedCallback(this::notifyOfResponseAccessCodeChanged);
    }

    void start() {
        if(status.get() == ServerStatus.INACTIVE) {
            status.set(ServerStatus.STARTING);
            gattServer = manager.openGattServer(ctx, this);
            if(gattServer == null) {
                Log.e(TAG, "Unable to create GATT server");
                status.set(ServerStatus.ERROR);
            } else {
                for(BluetoothGattService service: CoffeeServerProfile.getAllCoffeeServices()) {
                    boolean requestAdded = gattServer.addService(service);
                    Log.d(TAG, "Service Added: " + requestAdded);
                }
                /*boolean requestAdded = gattServer.addService(CoffeeServerProfile.getCoffeeRequestService());
                Log.d(TAG, "Request Service Added: " + requestAdded);

                boolean loginAdded = gattServer.addService(CoffeeServerProfile.getCoffeeLoginService());
                Log.d(TAG, "Login Service Added: " + loginAdded);*/

                status.set(ServerStatus.ACTIVE);
            }
        }
    }

    void stop() {
        if(gattServer != null) {
            gattServer.close();
            gattServer = null;
            status.set(ServerStatus.INACTIVE);
        }
    }

    public ReadOnlyProperty<ServerStatus> getStatus() {
        return status;
    }

    public ReadOnlyProperty<BluetoothDevice> getConnectedDevice() {
        return connectedDevice;
    }

    private void notifyOfReplyChanged() {
        handler.post(this::doNotifyOfReplyChanged);
    }

    private void notifyOfResponseAccessCodeChanged() {
        handler.post(this::doNotifyOfResponseAccessCodeChanged);
    }

    private void doNotifyOfReplyChanged() {
        if(requestServiceNotify.get()) {
            Log.d(TAG, "doNotifyOfReplyChanged");
            BluetoothGattCharacteristic replyCharacteristic = CoffeeServerProfile.getCoffeeRequestService()
                    .getCharacteristic(CoffeeServerProfile.COFFEE_REQUEST_SERVICE_REPLY);
            replyCharacteristic.setValue(Structs.writeOut(coffeeServer.getReply()));
            gattServer.notifyCharacteristicChanged(connectedDevice.get(), replyCharacteristic, false);
        }
    }

    private void doNotifyOfResponseAccessCodeChanged() {
        if(loginServiceNotify.get()) {
            Log.d(TAG, "doNotifyOfResponseAccessCodeChanged");
            BluetoothGattCharacteristic replyCharacteristic = CoffeeServerProfile.getCoffeeLoginService()
                    .getCharacteristic(CoffeeServerProfile.COFFEE_LOGIN_SERVICE_ACCESS_CODE);
            replyCharacteristic.setValue(Structs.writeOut(coffeeServer.getResponseUserAccessCode()));
            gattServer.notifyCharacteristicChanged(connectedDevice.get(), replyCharacteristic, false);
        }
    }

    @Override
    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
        if(newState == BluetoothProfile.STATE_CONNECTED) {
            Log.i(TAG, "BluetoothDevice CONNECTED: " + device);
            connectedDevice.set(device);
            handler.postDelayed(bleAdvertiser::stop, AD_STOP_DELAY_MS);
            requestServiceNotify.set(false);
            loginServiceNotify.set(false);
            coffeeServer.resetResponses();
        } else if(newState == BluetoothProfile.STATE_DISCONNECTED) {
            Log.i(TAG, "BluetoothDevice DISCONNECTED: " + device);
            connectedDevice.set(null);
            handler.postDelayed(bleAdvertiser::start, AD_STOP_DELAY_MS);
        }
    }

    @Override
    public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                            BluetoothGattCharacteristic characteristic) {

        Log.d(TAG, "onCharacteristicReadRequest(" + device.getAddress() + ", " + requestId + ", " +
                offset + ", " + characteristic.getUuid() + ")");

        if(CoffeeServerProfile.COFFEE_REQUEST_SERVICE_LEVELS.equals(characteristic.getUuid())) {
            coffeeServer.updateMachineLevels().done(() -> {
                CharacteristicStruct s = coffeeServer.getLevels();
                int responseStatus = BluetoothGatt.GATT_FAILURE;
                int responseOffset = 0;
                byte[] responseValue = null;
                if(s != null) {
                    responseValue = Structs.writeOut(s, offset);
                    responseStatus = BluetoothGatt.GATT_SUCCESS;
                }
                gattServer.sendResponse(device, requestId, responseStatus, responseOffset, responseValue);
            });
        } else {
            CharacteristicStruct s = null;
            if(CoffeeServerProfile.COFFEE_REQUEST_SERVICE_REPLY.equals(characteristic.getUuid())) {
                s = coffeeServer.getReply();
            } else if(CoffeeServerProfile.COFFEE_REQUEST_SERVICE_ERROR.equals(characteristic.getUuid())) {
                s = coffeeServer.getError();
            } else if(CoffeeServerProfile.COFFEE_LOGIN_SERVICE_ACCESS_CODE.equals(characteristic.getUuid())) {
                s = coffeeServer.getResponseUserAccessCode();
            } else if(CoffeeServerProfile.COFFEE_LOGIN_SERVICE_ERROR.equals(characteristic.getUuid())) {
                s = coffeeServer.getLoginError();
            }
            int responseStatus = BluetoothGatt.GATT_FAILURE;
            int responseOffset = 0;
            byte[] responseValue = null;
            if(s != null) {
                responseValue = Structs.writeOut(s, offset);
                responseStatus = BluetoothGatt.GATT_SUCCESS;
            }
            gattServer.sendResponse(device, requestId, responseStatus, responseOffset, responseValue);
        }
    }

    private UUID preparedWriteDestUuid = null;
    private CharacteristicStruct preparedWriteDest = null;
    private ByteBuffer preparedWritesBuffer = ByteBuffer.allocate(20);
    @Override
    public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
        int responseStatus = BluetoothGatt.GATT_FAILURE;
        int responseOffset = 0;
        byte[] responseValue = null;
        if(preparedWriteDest != null) {
            if(execute) {
                try {
                    preparedWritesBuffer.flip();
                    byte[] data = new byte[preparedWritesBuffer.limit()];
                    preparedWritesBuffer.get(data);
                    Structs.readIn(preparedWriteDest, data);
                    Log.d(TAG, "Value is now: " + preparedWriteDest.toString());
                } catch(StructureFormatException ex) {
                    Log.d(TAG, "Invalid structure format");
                }
            }
            preparedWriteDestUuid = null;
            preparedWriteDest = null;
            preparedWritesBuffer.rewind();
            preparedWritesBuffer.limit(preparedWritesBuffer.capacity());
            responseStatus = BluetoothGatt.GATT_SUCCESS;
        }
        gattServer.sendResponse(device,
                requestId,
                responseStatus,
                responseOffset,
                responseValue
        );
    }

    @Override
    public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattCharacteristic characteristic,
                                             boolean preparedWrite, boolean responseNeeded,
                                             int offset, byte[] value) {

        super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
        Log.d(TAG, "onCharacteristicWriteRequest(" + device.getAddress() + ", " + requestId + ", " +
                characteristic.getUuid() + ", " + preparedWrite + ", " + responseNeeded + ", " + offset +
                BLEUtil.byteArrayToString(value) + ")");

        CharacteristicStruct s = null;
        if(CoffeeServerProfile.COFFEE_REQUEST_SERVICE_REQUEST.equals(characteristic.getUuid())) {
            s = coffeeServer.getRequest();
        } else if(CoffeeServerProfile.COFFEE_LOGIN_SERVICE_USERNAME.equals(characteristic.getUuid())) {
            s = coffeeServer.getUserName();
        } else if(CoffeeServerProfile.COFFEE_LOGIN_SERVICE_PASSWORD.equals(characteristic.getUuid())) {
            s = coffeeServer.getPassword();
        }
        int responseStatus = BluetoothGatt.GATT_FAILURE;
        int responseOffset = 0;
        byte[] responseValue = null;
        if(s != null) {
            try {
                if(preparedWrite) {
                    if(preparedWriteDestUuid != null && !preparedWriteDestUuid.equals(characteristic.getUuid())) {
                        Log.e(TAG, "Prepared writes to multiple characteristics not supported");
                    } else {
                        if(preparedWritesBuffer.remaining() < value.length) {
                            ByteBuffer newBuff = ByteBuffer.allocate(preparedWritesBuffer.position() + value.length);
                            preparedWritesBuffer.flip();
                            newBuff.put(preparedWritesBuffer);
                            preparedWritesBuffer = newBuff;
                        }
                        preparedWriteDest = s;
                        preparedWritesBuffer.put(value);
                        responseValue = value;
                        responseStatus = BluetoothGatt.GATT_SUCCESS;
                    }
                } else {
                    responseStatus = BluetoothGatt.GATT_SUCCESS;
                    Structs.readIn(s, value);
                    Log.d(TAG, "Value is now: " + s.toString());
                }
            } catch(StructureFormatException ex) {
                Log.d(TAG, "Invalid structure format");
            }
        }
        if(responseNeeded) {
            gattServer.sendResponse(device,
                    requestId,
                    responseStatus,
                    responseOffset,
                    responseValue
            );
        }
    }

    @Override
    public void onNotificationSent(BluetoothDevice device, int status) {
        Log.d(TAG, "onNotificationSent(" + device.getAddress() + ", " + BLEUtil.getStatusString(status) + ")");
    }

    @Override
    public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
                                        BluetoothGattDescriptor descriptor) {

        BooleanProperty b = null;
        if(CoffeeServerProfile.COFFEE_REQUEST_SERVICE_CONFIG.equals(descriptor.getUuid())) {
            b = this.requestServiceNotify;
        } else if(CoffeeServerProfile.COFFEE_LOGIN_SERVICE_CONFIG.equals(descriptor.getUuid())) {
            b = this.loginServiceNotify;
        }
        int responseStatus = BluetoothGatt.GATT_FAILURE;
        int responseOffset = 0;
        byte[] responseValue = null;
        if(b != null) {
            responseValue = b.get() ?
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE :
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
            responseStatus = BluetoothGatt.GATT_SUCCESS;
        }
        gattServer.sendResponse(device,
                requestId,
                responseStatus,
                responseOffset,
                responseValue
        );
    }

    @Override
    public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                         BluetoothGattDescriptor descriptor,
                                         boolean preparedWrite, boolean responseNeeded,
                                         int offset, byte[] value) {

        Log.d(TAG, "onDescriptorWriteRequest(" + device.getAddress() + ", " + requestId + ", " +
                descriptor.getUuid() + ", " + preparedWrite + ", " + responseNeeded + ", " + offset +
                BLEUtil.byteArrayToString(value) + ")");

        BooleanProperty b = null;
        if(CoffeeServerProfile.COFFEE_REQUEST_SERVICE_CONFIG.equals(descriptor.getUuid())) {
            b = this.requestServiceNotify;
        } else if(CoffeeServerProfile.COFFEE_LOGIN_SERVICE_CONFIG.equals(descriptor.getUuid())) {
            b = this.loginServiceNotify;
        }
        int responseStatus = BluetoothGatt.GATT_FAILURE;
        int responseOffset = 0;
        byte[] responseValue = null;
        if(b != null) {
            if(Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                b.set(true);
                responseStatus = BluetoothGatt.GATT_SUCCESS;
            } else if(Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
                b.set(false);
                responseStatus = BluetoothGatt.GATT_SUCCESS;
            }
        }
        if(responseNeeded) {
            gattServer.sendResponse(device,
                    requestId,
                    responseStatus,
                    responseOffset,
                    responseValue
            );
        }


    }

    public enum ServerStatus {
        INACTIVE, STARTING, ACTIVE, ERROR
    }
}
