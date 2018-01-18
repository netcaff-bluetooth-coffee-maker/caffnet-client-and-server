package com.quew8.netcaff;

import android.os.Bundle;

import com.quew8.netcaff.ble.CoffeeConnector;
import com.quew8.netcaff.lib.server.CoffeeServerId;
import com.quew8.properties.BooleanProperty;
import com.quew8.properties.ListenerSet;
import com.quew8.properties.PropertyChangeListener;
import com.quew8.properties.ReadOnlyBooleanProperty;

/**
 * @author Quew8
 */
public abstract class ServerCommunicationActivity extends ServiceDependantActivity {
    public static final String EXTRA_SERVER_ID = "extra_server_id";

    private ServerInterface serverInterface;
    private BooleanProperty working;
    private CoffeeServerId coffeeServerId;
    private ClientCoffeeServer coffeeServer;
    private ListenerSet.ListenerHandle<PropertyChangeListener<CoffeeConnector.ConnectorStatus>> connectorStatusListenerHandle;
    private ListenerSet.ListenerHandle<PropertyChangeListener<Boolean>> isScanningListenerHandle;

    public ReadOnlyBooleanProperty isWorking() {
        return working;
    }

    public ServerInterface getServerInterface() {
        return serverInterface;
    }

    public ClientCoffeeServer getCoffeeServer() {
        return coffeeServer;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        serverInterface = new ServerInterface();
        working = new BooleanProperty(false);
        if(!getIntent().hasExtra(EXTRA_SERVER_ID)) {
            throw new RuntimeException("No coffee server id passed in " + this.getClass().getSimpleName() + " intent");
        }
        coffeeServerId = getIntent().getParcelableExtra(EXTRA_SERVER_ID);
    }

    @Override
    public void onConnectedToManager() {
        super.onConnectedToManager();
        coffeeServer = getServerList().getServer(coffeeServerId);
        serverInterface.init(coffeeServer, getManager());
        connectorStatusListenerHandle = getConnector().getStatus().addListener(this::onBLEStatusChange);
        isScanningListenerHandle = getScanner().isScanning().addListener(this::onBLEStatusChange, true);
    }

    @Override
    public void onDisconnectedFromManager() {
        super.onDisconnectedFromManager();
        coffeeServer = null;
        serverInterface.deinit();
        getConnector().getStatus().removeListener(connectorStatusListenerHandle);
        getScanner().isScanning().removeListener(isScanningListenerHandle);
    }

    private void onBLEStatusChange(Object state) {
        runOnUiThread(() -> working.set(
                getConnector().getStatus().get() != CoffeeConnector.ConnectorStatus.INACTIVE ||
                        getScanner().isScanning().get()
        ));
    }
}
