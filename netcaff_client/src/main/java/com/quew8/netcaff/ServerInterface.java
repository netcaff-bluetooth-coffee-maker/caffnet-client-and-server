package com.quew8.netcaff;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;

import com.quew8.netcaff.ble.CoffeeConnector;
import com.quew8.netcaff.ble.CoffeeManager;
import com.quew8.netcaff.lib.ble.util.BLEUtil;
import com.quew8.netcaff.lib.server.CharacteristicStruct;
import com.quew8.netcaff.lib.server.Structs;
import com.quew8.netcaff.lib.server.StructureFormatException;
import com.quew8.properties.deferred.Deferred;
import com.quew8.properties.deferred.Promise;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

/**
 * @author Quew8
 */
class ServerInterface {
    private static final String TAG = ServerInterface.class.getSimpleName();

    private ClientCoffeeServer server;
    private CoffeeManager manager;
    private final ArrayList<UUID> queuedUpdates = new ArrayList<>();
    private final ArrayList<Deferred<Void>> readDeferreds = new ArrayList<>();
    private final ArrayList<Deferred<Void>> writeDeferreds = new ArrayList<>();
    private final ArrayList<Deferred<Void>> setUpdateDeferreds = new ArrayList<>();
    private final HashMap<UUID, Deferred<Void>> updateDeferreds = new HashMap<>();

    void init(ClientCoffeeServer server, CoffeeManager manager) {
        this.server = server;
        this.manager = manager;
    }

    void deinit() {

    }

    Promise<Void> run(ServerAction action) {
        return new Connection(action).run();
    }

    ServerAction readCharacteristic(UUID uuid) {
        return (connection) -> {
            Deferred<Void> d = new Deferred<>();
            readDeferreds.add(d);
            manager.getConnector().readCharacteristic(uuid, readCallback);
            return d.promise();
        };
    }

    ServerAction writeCharacteristic(UUID uuid) {
        return (connection) -> {
            Deferred<Void> d = new Deferred<>();
            writeDeferreds.add(d);
            CharacteristicStruct struct = server.getStructForUUID(uuid);
            manager.getConnector().writeCharacteristic(uuid, Structs.writeOut(struct), writeCallback);
            return d.promise();
        };
    }

    ServerAction writeDescriptor(UUID uuid, boolean active) {
        return (connection) -> {
            Deferred<Void> d = new Deferred<>();
            setUpdateDeferreds.add(d);
            manager.getConnector().setNotify(uuid, active, updateNotifyCallback);
            return d.promise();
        };
    }

    ServerAction waitForUpdate(UUID uuid) {
        return (connection) -> {
            Log.d(TAG, "Waiting for update");
            Deferred<Void> d = new Deferred<>();
            if(queuedUpdates.contains(uuid)) {
                d.resolve(null);
                queuedUpdates.remove(uuid);
            } else {
                updateDeferreds.put(uuid, d);
            }
            return d.promise();
        };
    }

    private class Connection implements CoffeeConnector.ConnectCallback {
        private Deferred<Void> overall = null;
        private boolean success = false;
        private boolean isConnected = false;
        private ServerAction action;

        private Connection(ServerAction action) {
            this.action = action;
        }

        private Promise<Void> run() {
            if(overall != null) {
                throw new IllegalStateException("Connection has already been started");
            }
            overall = new Deferred<>();
            manager.getConnector().scanForAndConnectTo(server, this);
            return overall.promise();
        }

        private boolean isConnected() {
            return isConnected;
        }

        private void failOverall() {
            if(overall.isRunning()) {
                overall.fail();
            }
        }

        @Override
        public void onConnect() {
            isConnected = true;
            queuedUpdates.clear();
            action.create(this)
                    .done((s) -> {
                        success = true;
                        isConnected = false;
                        manager.getConnector().disconnect();
                    })
                    .fail(() -> {
                        success = false;
                        isConnected = false;
                        manager.getConnector().disconnect();
                    });
        }

        @Override
        public void onDisconnect() {
            manager.getConnector().destroy();
            if(success) {
                overall.resolve(null);
            } else {
                failOverall();
            }
        }

        @Override
        public void onUnexpectedDisconnect() {
            isConnected = false;
            manager.getConnector().destroy();
            failOverall();
        }

        @Override
        public void onConnectFailure(int statusCode) {
            failOverall();
        }
    }

    private static class ServerActionLink implements ServerAction {
        private final ServerAction first;
        private final ServerAction second;

        private ServerActionLink(ServerAction first, ServerAction second) {
            this.first = first;
            this.second = second;
        }

        public Promise<Void> create(Connection connection) {
            Deferred<Void> deferred = new Deferred<>();
            first.create(connection).done((s) -> {
                if(connection.isConnected()) {
                    second.create(connection)
                            .done(deferred::resolve)
                            .fail(deferred::fail);
                } else {
                    deferred.fail();
                }
            }).fail(deferred::fail);
            return deferred.promise();
        }
    }

    static class ServerActionConditionalBuilder {
        private final ServerAction from;
        private final ArrayList<ServerActionCondition> conditions;
        private final ArrayList<ServerAction> branches;

        private ServerActionConditionalBuilder(ServerAction from, ServerActionCondition condition, ServerAction action) {
            this.from = from;
            this.conditions = new ArrayList<>();
            this.branches = new ArrayList<>();
            elseIfThen(condition, action);
        }

        public ServerActionConditionalBuilder elseIfThen(ServerActionCondition condition, ServerAction action) {
            conditions.add(condition);
            branches.add(action);
            return this;
        }

        public ServerAction elseThen() {
            return elseThen(new ServerActionNone());
        }

        public ServerAction elseThen(ServerAction action) {
            return from.then(new ServerActionConditional(conditions, branches, action));
        }
    }

    public interface ServerActionCondition {
        boolean test();
    }

    private static class ServerActionConditional implements ServerAction {
        private final ArrayList<ServerActionCondition> conditions;
        private final ArrayList<ServerAction> branches;
        private final ServerAction orElse;

        private ServerActionConditional(ArrayList<ServerActionCondition> conditions,
                                        ArrayList<ServerAction> branches,
                                        ServerAction orElse) {

            this.conditions = conditions;
            this.branches = branches;
            this.orElse = orElse;
        }

        public Promise<Void> create(Connection connection) {
            for(int i = 0; i < conditions.size(); i++) {
                if(conditions.get(i).test()) {
                    return branches.get(i).create(connection);
                }
            }
            return orElse.create(connection);
        }
    }

    private static class ServerActionNone implements ServerAction {

        public Promise<Void> create(Connection connection) {
            Deferred<Void> d = new Deferred<>();
            d.resolve(null);
            return d.promise();
        }
    }

    interface ServerAction {
        Promise<Void> create(Connection connection);

        default ServerAction then(ServerAction then) {
            return new ServerActionLink(this, then);
        }

        default ServerActionConditionalBuilder ifThen(ServerActionCondition condition, ServerAction action) {
            return new ServerActionConditionalBuilder(this, condition, action);
        }
    }

    private static <T> ArrayList<T> copyAndClear(Iterable<T> list) {
        ArrayList<T> copy = new ArrayList<>();
        Iterator<T> it = list.iterator();
        while(it.hasNext()) {
            copy.add(it.next());
            it.remove();
        }
        return copy;
    }

    private static void resolveDeferreds(Iterable<Deferred<Void>> list) {
        ArrayList<Deferred<Void>> copy = copyAndClear(list);
        for(Deferred<Void> d: copy) {
            d.resolve(null);
        }
    }

    private static void failDeferreds(Iterable<Deferred<Void>> list) {
        ArrayList<Deferred<Void>> copy = copyAndClear(list);
        for(Deferred<Void> d: copy) {
            d.fail();
        }
    }

    private final CoffeeConnector.ReadCallback readCallback = new CoffeeConnector.ReadCallback() {

        @Override
        public void onRead(BluetoothGattCharacteristic characteristic, byte[] data) {
            Log.d(TAG, "onRead(" + BLEUtil.byteArrayToString(data) + ")");
            try {
                CharacteristicStruct struct = server.getStructForUUID(characteristic.getUuid());
                Structs.readIn(struct, data);
                resolveDeferreds(readDeferreds);
            } catch(StructureFormatException ex) {
                Log.e(TAG, "StructureFormatException: " + ex.getMessage());
                failDeferreds(readDeferreds);
            }
        }

        @Override
        public void onReadFailure(BluetoothGattCharacteristic characteristic, int statusCode) {
            failDeferreds(readDeferreds);
        }
    };

    private final CoffeeConnector.WriteCallback writeCallback = new CoffeeConnector.WriteCallback() {
        @Override
        public void onWrite(BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onWrite()");
            resolveDeferreds(writeDeferreds);
        }

        @Override
        public void onWriteFailure(BluetoothGattCharacteristic characteristic, int statusCode) {
            failDeferreds(writeDeferreds);
        }
    };

    private final CoffeeConnector.ReadDescriptorCallback readDescriptorCallback = new CoffeeConnector.ReadDescriptorCallback() {
        @Override
        public void onDescriptorRead(BluetoothGattDescriptor descriptor, boolean value) {
            String log = "onReadDesc(" + Boolean.toString(value) + ")";
            Log.d(TAG, log);
        }

        @Override
        public void onDescriptorReadFailure(BluetoothGattDescriptor descriptor, int statusCode) throws CoffeeConnector.ConnectorException {
            throw new CoffeeConnector.ConnectorException();
        }
    };


    private final CoffeeConnector.UpdateNotifyCallback updateNotifyCallback = new CoffeeConnector.UpdateNotifyCallback() {
        @Override
        public void onEnabled(BluetoothGattDescriptor descriptor) {
            Log.d(TAG, "Updates Enabled");
            resolveDeferreds(setUpdateDeferreds);
        }

        @Override
        public void onUpdate(BluetoothGattCharacteristic characteristic, byte[] data) {
            Deferred<Void> deferred = null;
            if(updateDeferreds.containsKey(characteristic.getUuid())) {
                deferred = updateDeferreds.remove(characteristic.getUuid());
            }
            try {
                CharacteristicStruct struct = server.getStructForUUID(characteristic.getUuid());
                Structs.readIn(struct, data);
                if(deferred != null) {
                    deferred.resolve(null);
                } else {
                    if(!queuedUpdates.contains(characteristic.getUuid())) {
                        queuedUpdates.add(characteristic.getUuid());
                    }
                }
                Log.d(TAG, "onUpdate => " + struct.toString());
            } catch(StructureFormatException e) {
                if(deferred != null) {
                    deferred.fail();
                }
            }
        }

        @Override
        public void onDisabled(BluetoothGattDescriptor descriptor) {
            Log.d(TAG, "Updates Disabled");
            resolveDeferreds(setUpdateDeferreds);
        }

        @Override
        public void onDescriptorWriteFailure(BluetoothGattDescriptor descriptor, int statusCode) {
            failDeferreds(setUpdateDeferreds);
        }
    };
}
