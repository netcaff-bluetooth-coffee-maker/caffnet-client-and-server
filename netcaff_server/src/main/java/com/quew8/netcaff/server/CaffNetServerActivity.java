package com.quew8.netcaff.server;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.quew8.netcaff.server.ble.BleAdvertiser;
import com.quew8.netcaff.server.ble.BleServer;
import com.quew8.properties.ListenerSet;
import com.quew8.properties.PropertyChangeListener;

public class CaffNetServerActivity extends AppCompatActivity {
    private TextView statusView;
    private ProgressBar loadingBar;
    private View connectedDeviceView;
    private TextView connectedBdAddr;

    private OrderAdapter orderAdapter;
    private MachineAdapter machineAdapter;
    private UserAdapter userAdapter;

    private CharacteristicView requestView;
    private CharacteristicView replyView;
    private CharacteristicView errorView;
    private CharacteristicView levelsView;
    private CharacteristicView usernameView;
    private CharacteristicView passwordView;
    private CharacteristicView accessCodeView;
    private CharacteristicView loginErrorView;

    private CaffNetServerService service;
    private ListenerSet.ListenerHandle<PropertyChangeListener<Boolean>> bluetoothEnabledHandle;
    private ListenerSet.ListenerHandle<PropertyChangeListener<BleServer.ServerStatus>> serverStatusHandle;
    private ListenerSet.ListenerHandle<PropertyChangeListener<BleAdvertiser.AdvertiserStatus>> advertiserStatusHandle;

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            CaffNetServerService service = ((CaffNetServerService.ServiceBinder) binder).getService();
            CaffNetServerActivity.this.onServiceConnected(service);
            CaffNetServerActivity.this.service = service;
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            CaffNetServerActivity.this.onServiceDisconnected(service);
            service = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caff_net_server);

        statusView = findViewById(R.id.status_text_view);
        loadingBar = findViewById(R.id.loading_bar);
        connectedDeviceView = findViewById(R.id.connected_device_view);
        connectedBdAddr = findViewById(R.id.connected_bd_addr);

        ListView ordersList = findViewById(R.id.orders_list);
        ordersList.setAdapter(orderAdapter = new OrderAdapter(this));
        ListView machinesList = findViewById(R.id.machines_list);
        machinesList.setAdapter(machineAdapter = new MachineAdapter(this));
        ListView usersList = findViewById(R.id.users_list);
        usersList.setAdapter(userAdapter = new UserAdapter(this));

        requestView = new CharacteristicView(this, findViewById(R.id.request_char), "Request");
        replyView = new CharacteristicView(this, findViewById(R.id.reply_char), "Reply");
        errorView = new CharacteristicView(this, findViewById(R.id.error_char), "Error");
        levelsView = new CharacteristicView(this, findViewById(R.id.levels_char), "Levels");

        usernameView = new CharacteristicView(this, findViewById(R.id.username_char), "Username");
        passwordView = new CharacteristicView(this, findViewById(R.id.password_char), "Password");
        accessCodeView = new CharacteristicView(this, findViewById(R.id.access_code_char), "Access Code");
        loginErrorView = new CharacteristicView(this, findViewById(R.id.login_error_char), "Login Error");

        updateUIState();

        Intent serviceIntent = new Intent(this, CaffNetServerService.class);
        startService(serviceIntent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, CaffNetServerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(serviceConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        /*bleManager.shutdown();
        bleManager.unregister();*/
    }

    private void onServiceConnected(CaffNetServerService service) {
        bluetoothEnabledHandle =
                service.getBleManager().isBluetoothEnabled().addListener((n)-> updateUIState());
        serverStatusHandle =
                service.getBleServer().getStatus().addListener((n)-> updateUIState());
        advertiserStatusHandle =
                service.getBleAdvertiser().getStatus().addListener((n)-> updateUIState());

        orderAdapter.setCoffeeServer(service.getCoffeeServer());
        machineAdapter.setCoffeeServer(service.getCoffeeServer());
        userAdapter.setCoffeeServer(service.getCoffeeServer());

        requestView.setStruct(service.getCoffeeServer().getRequest());
        replyView.setStruct(service.getCoffeeServer().getReply());
        errorView.setStruct(service.getCoffeeServer().getError());
        levelsView.setStruct(service.getCoffeeServer().getLevels());
        usernameView.setStruct(service.getCoffeeServer().getUserName());
        passwordView.setStruct(service.getCoffeeServer().getPassword());
        accessCodeView.setStruct(service.getCoffeeServer().getResponseUserAccessCode());
        loginErrorView.setStruct(service.getCoffeeServer().getLoginError());

        updateUIState();
    }

    private void onServiceDisconnected(CaffNetServerService service) {
        service.getBleManager().isBluetoothEnabled().removeListener(bluetoothEnabledHandle);
        service.getBleServer().getStatus().removeListener(serverStatusHandle);
        service.getBleAdvertiser().getStatus().removeListener(advertiserStatusHandle);

        orderAdapter.setCoffeeServer(null);
        machineAdapter.setCoffeeServer(null);
        userAdapter.setCoffeeServer(null);

        requestView.setStruct(null);
        replyView.setStruct(null);
        errorView.setStruct(null);
        levelsView.setStruct(null);
        usernameView.setStruct(null);
        passwordView.setStruct(null);
        accessCodeView.setStruct(null);
        loginErrorView.setStruct(null);

        updateUIState();
    }

    private CaffNetServerService getService() {
        return this.service;
    }

    private boolean isServiceBound() {
        return this.service != null;
    }

    private void updateUIState() {
        ServerState state;
        if(isServiceBound()) {
            if(!getService().getBleManager().isBluetoothEnabled().get()) {
                state = ServerState.BLE_OFF;
            } else {
                if(getService().getBleServer().getStatus().get() == BleServer.ServerStatus.INACTIVE) {
                    state = ServerState.INACTIVE;
                } else if(getService().getBleServer().getStatus().get() == BleServer.ServerStatus.STARTING) {
                    state = ServerState.SERVER_STARTING;
                } else if(getService().getBleServer().getStatus().get() == BleServer.ServerStatus.ERROR) {
                    state = ServerState.SERVER_ERR;
                } else {
                    if(getService().getBleServer().getConnectedDevice().get() != null) {
                        state = ServerState.DEVICE_CONNECTED;
                    } else {
                        if(getService().getBleAdvertiser().getStatus().get() == BleAdvertiser.AdvertiserStatus.INACTIVE) {
                            state = ServerState.SERVER_STARTED;
                        } else if(getService().getBleAdvertiser().getStatus().get() == BleAdvertiser.AdvertiserStatus.STARTING) {
                            state = ServerState.ADVERTS_STARTING;
                        } else if(getService().getBleAdvertiser().getStatus().get() == BleAdvertiser.AdvertiserStatus.ERROR) {
                            state = ServerState.ADVERTS_ERR;
                        } else {
                            state = ServerState.ADVERTS_STARTED;
                        }
                    }
                }
            }
        } else {
            state = ServerState.SERVICE_NOT_BOUND;
        }
        updateUIState(state);
    }

    private void updateUIState(ServerState currentState) {
        runOnUiThread(() -> {
            statusView.setText(currentState.stateId);
            loadingBar.setVisibility(currentState.activeState ? View.VISIBLE : View.INVISIBLE);
            if(currentState == ServerState.DEVICE_CONNECTED) {
                connectedDeviceView.setVisibility(View.VISIBLE);
                connectedBdAddr.setText(getService().getBleServer().getConnectedDevice().get().toString());
            } else {
                connectedDeviceView.setVisibility(View.INVISIBLE);
            }
        });
    }

    public enum ServerState {
        SERVICE_NOT_BOUND(R.string.service_not_bound, false),
        BLE_OFF(R.string.ble_off_status, false),
        INACTIVE(R.string.inactive_status, false),
        SERVER_STARTING(R.string.server_starting_status, true),
        SERVER_ERR(R.string.server_err_status, false),
        SERVER_STARTED(R.string.server_started_status, false),
        ADVERTS_STARTING(R.string.advert_starting_status, true),
        ADVERTS_ERR(R.string.advert_err_status, false),
        ADVERTS_STARTED(R.string.advert_started_status, false),
        DEVICE_CONNECTED(R.string.device_connected_status, false);

        ServerState(int stateId,
                    boolean activeState) {

            this.stateId = stateId;
            this.activeState = activeState;
        }

        public final int stateId;
        public final boolean activeState;
    }

}
