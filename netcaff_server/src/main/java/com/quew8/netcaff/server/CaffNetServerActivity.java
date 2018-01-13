package com.quew8.netcaff.server;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.quew8.netcaff.lib.server.CharacteristicStruct;
import com.quew8.netcaff.server.access.UserList;
import com.quew8.netcaff.server.ble.Advertiser;
import com.quew8.netcaff.server.ble.BLEManager;
import com.quew8.netcaff.server.ble.Server;
import com.quew8.netcaff.server.machine.MachineManager;

import java.security.NoSuchAlgorithmException;

public class CaffNetServerActivity extends AppCompatActivity {
    /* UI */
    private TextView statusView;
    private ProgressBar loadingBar;
    private View connectedDeviceView;
    private TextView connectedBdAddr;
    private Button startServerBtn;
    private Button startAdvertsBtn;

    private UserList userList;
    private ServerCoffeeServer coffeeServer;
    private BLEManager bleManager;
    private MachineManager machineManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caff_net_server);

        statusView = findViewById(R.id.status_text_view);
        loadingBar = findViewById(R.id.loading_bar);
        connectedDeviceView = findViewById(R.id.connected_device_view);
        connectedBdAddr = findViewById(R.id.connected_bd_addr);
        startServerBtn = findViewById(R.id.start_server_btn);
        startAdvertsBtn = findViewById(R.id.start_adverts_btn);
        ListView ordersList = findViewById(R.id.orders_list);
        ListView machinesList = findViewById(R.id.machines_list);
        ListView usersList = findViewById(R.id.users_list);

        try {
            userList = new UserList();
        } catch(NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        userList.addUser("quew8", "hello");
        userList.addUser("Alan Alan", "Fine sea salt, store in a cool, dry place.");
        coffeeServer = new ServerCoffeeServer(12, userList);

        bleManager = new BLEManager(this, coffeeServer);
        bleManager.isBluetoothEnabled().addListener((n, o)-> updateUIState());
        bleManager.getServer().getStatus().addListener((n, o)-> updateUIState());
        bleManager.getAdvertiser().getStatus().addListener((n, o)-> updateUIState());

        machineManager = new MachineManager(this, coffeeServer);

        ordersList.setAdapter(new OrderAdapter(this, coffeeServer));
        machinesList.setAdapter(new MachineAdapter(this, coffeeServer));
        usersList.setAdapter((new UserAdapter(this, coffeeServer)));
        updateUIState();

        new CharacteristicView(findViewById(R.id.request_char), "Request", coffeeServer.getRequest());
        new CharacteristicView(findViewById(R.id.reply_char), "Reply", coffeeServer.getReply());
        new CharacteristicView(findViewById(R.id.error_char), "Error", coffeeServer.getError());
        new CharacteristicView(findViewById(R.id.levels_char), "Levels", coffeeServer.getLevels());

        new CharacteristicView(findViewById(R.id.username_char), "Username", coffeeServer.getUserName());
        new CharacteristicView(findViewById(R.id.password_char), "Password", coffeeServer.getPassword());
        new CharacteristicView(findViewById(R.id.access_code_char), "Access Code", coffeeServer.getResponseUserAccessCode());
        new CharacteristicView(findViewById(R.id.login_error_char), "Login Error", coffeeServer.getLoginError());

        bleManager.register();
        bleManager.startup();
        coffeeServer.startCheckLevelsLoop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        bleManager.shutdown();
        bleManager.unregister();
    }

    public void onServerStartBtnClick(View v) {
        /*if(statusProp.get() == ServerState.INACTIVE) {
            startServer();
        } else if(statusProp.get() == ServerState.SERVER_STARTED) {
            stopServer();
        }*/
    }

    public void onAdvertsStartBtnClick(View v) {
        /*if(statusProp.get() == ServerState.SERVER_STARTED) {
            startAdvertising();
        } else if(statusProp.get() == ServerState.ADVERTS_STARTED) {
            stopAdvertising();
        }*/
    }

    public void updateUIState() {
        ServerState state;
        if(!bleManager.isBluetoothEnabled().get()) {
            state = ServerState.BLE_OFF;
        } else {
            if(bleManager.getServer().getStatus().get() == Server.ServerStatus.INACTIVE) {
                state = ServerState.INACTIVE;
            } else if(bleManager.getServer().getStatus().get() == Server.ServerStatus.STARTING) {
                state = ServerState.SERVER_STARTING;
            } else if(bleManager.getServer().getStatus().get() == Server.ServerStatus.ERROR) {
                state = ServerState.SERVER_ERR;
            } else {
                if(bleManager.getServer().getConnectedDevice().get() != null) {
                    state = ServerState.DEVICE_CONNECTED;
                } else {
                    if(bleManager.getAdvertiser().getStatus().get() == Advertiser.AdvertiserStatus.INACTIVE) {
                        state = ServerState.SERVER_STARTED;
                    } else if(bleManager.getAdvertiser().getStatus().get() == Advertiser.AdvertiserStatus.STARTING) {
                        state = ServerState.ADVERTS_STARTING;
                    } else if(bleManager.getAdvertiser().getStatus().get() == Advertiser.AdvertiserStatus.ERROR) {
                        state = ServerState.ADVERTS_ERR;
                    } else {
                        state = ServerState.ADVERTS_STARTED;
                    }
                }
            }
        }
        updateUIState(state);
    }

    public void updateUIState(ServerState currentState) {
        runOnUiThread(() -> {
            statusView.setText(currentState.stateId);
            loadingBar.setVisibility(currentState.activeState ? View.VISIBLE : View.INVISIBLE);
            startServerBtn.setEnabled(currentState.serverButtonEnabled);
            startServerBtn.setText(currentState.serverButtonText);
            startAdvertsBtn.setEnabled(currentState.advertsButtonEnabled);
            startAdvertsBtn.setText(currentState.advertsButtonText);
            if(currentState == ServerState.DEVICE_CONNECTED) {
                connectedDeviceView.setVisibility(View.VISIBLE);
                connectedBdAddr.setText(bleManager.getServer().getConnectedDevice().get().toString());
            } else {
                connectedDeviceView.setVisibility(View.GONE);
            }
        });
    }

    public enum ServerState {
        BLE_OFF(R.string.ble_off_status, false, R.string.start_server_action, false, R.string.start_adverts_action, false),
        INACTIVE(R.string.inactive_status, true, R.string.start_server_action, false, R.string.start_adverts_action, false),
        SERVER_STARTING(R.string.server_starting_status, false, R.string.start_server_action, false, R.string.start_adverts_action, true),
        SERVER_ERR(R.string.server_err_status, false, R.string.start_server_action, false, R.string.start_adverts_action, false),
        SERVER_STARTED(R.string.server_started_status, true, R.string.stop_server_action, true, R.string.start_adverts_action, false),
        ADVERTS_STARTING(R.string.advert_starting_status, false, R.string.stop_server_action, false, R.string.start_adverts_action, true),
        ADVERTS_ERR(R.string.advert_err_status, false, R.string.stop_server_action, false, R.string.start_adverts_action, false),
        ADVERTS_STARTED(R.string.advert_started_status, false, R.string.stop_server_action, true, R.string.stop_adverts_action, false),
        DEVICE_CONNECTED(R.string.device_connected_status, false, R.string.stop_server_action, false, R.string.stop_adverts_action, false);

        ServerState(int stateId, boolean serverButtonEnabled, int serverButtonText,
                    boolean advertsButtonEnabled, int advertsButtonText, boolean activeState) {

            this.stateId = stateId;
            this.serverButtonEnabled = serverButtonEnabled;
            this.serverButtonText = serverButtonText;
            this.advertsButtonEnabled = advertsButtonEnabled;
            this.advertsButtonText = advertsButtonText;
            this.activeState = activeState;
        }

        public final int stateId;
        public final boolean serverButtonEnabled;
        public final int serverButtonText;
        public final boolean advertsButtonEnabled;
        public final int advertsButtonText;
        public final boolean activeState;
    }

    private class CharacteristicView {
        private final String title;
        private final CharacteristicStruct struct;
        private final TextView titleField;
        private final TextView contentField;

        private CharacteristicView(View parent, String title, CharacteristicStruct struct) {
            this.title = title;
            this.struct = struct;
            this.titleField = parent.findViewById(R.id.characteristic_title);
            this.contentField = parent.findViewById(R.id.characteristic_content);
            struct.addWrittenCallback(this::onWritten);
            struct.addModifiedCallback(this::onSet);
            update();
        }

        private void onSet() {
            update();
        }

        private void onWritten() {
            update();
        }

        private void update() {
            runOnUiThread(() -> {
                titleField.setText(title);
                contentField.setText(struct.getPrettyString());
            });
        }
    }
}
