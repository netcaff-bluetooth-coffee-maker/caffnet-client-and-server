package com.quew8.netcaff;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.quew8.netcaff.ble.CoffeeScanner;
import static com.quew8.netcaff.lib.ble.CoffeeServerProfile.*;

import com.quew8.netcaff.lib.server.OrderID;
import com.quew8.netcaff.lib.server.ReplyType;
import com.quew8.properties.ReadOnlyBooleanProperty;
import com.quew8.properties.deferred.Deferred;
import com.quew8.properties.deferred.Promise;

/**
 * @author Quew8
 */
public class ServerActivity extends AbstractServerActivity {
    private static final String TAG = ServerActivity.class.getSimpleName();

    private Button orderBtn;
    private Button rescanBtn;
    private ProgressBar commsProgress;
    private ServerActivityItemAdapter itemAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        ListView orderItemsList = findViewById(R.id.order_items_list);
        this.orderBtn = findViewById(R.id.order_btn);
        this.rescanBtn = findViewById(R.id.rescan_btn);
        this.commsProgress = findViewById(R.id.comms_progress);

        this.itemAdapter = new ServerActivityItemAdapter(this, adapterItemsCallback);
        orderItemsList.setAdapter(itemAdapter);

        this.orderBtn.setOnClickListener(this::onOrderBtnClick);
        this.rescanBtn.setOnClickListener(this::onRescanBtnClick);
        isWorking().addListener(this::onIsWorkingChange, true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(isConnectedToManagerService()) {
            checkHasAccessCode();
        }
    }

    @Override
    public void onConnectedToManager() {
        super.onConnectedToManager();
        itemAdapter.setServer(getCoffeeServer());
        checkHasAccessCode();
    }

    @Override
    public void onDisconnectedFromManager() {
        super.onDisconnectedFromManager();
        itemAdapter.setServer(null);
    }

    private Promise<Void> checkHasAccessCode() {
        Deferred<Void> d = new Deferred<>();
        if(getCoffeeServer().hasValidUserAccessCode()) {
            d.resolve(null);
        } else {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra(EXTRA_SERVER_ID, getCoffeeServer().getId());
            runActivity(intent)
                    .done(() -> d.resolve(null))
                    .fail(d::fail);
        }
        return d.promise();
    }

    private void onIsWorkingChange(boolean working, boolean old) {
        commsProgress.setVisibility(working ? View.VISIBLE : View.GONE);
        orderBtn.setEnabled(!working);
        rescanBtn.setEnabled(!working);
    }

    private Promise<Void> doRequestReply() {
        Deferred<Void> deferred = new Deferred<>();
        getServerInterface()
                .run(
                        getServerInterface().writeDescriptor(COFFEE_REQUEST_SERVICE_CONFIG, true)
                                .then(getServerInterface().writeCharacteristic(COFFEE_REQUEST_SERVICE_REQUEST))
                                .then(getServerInterface().waitForUpdate(COFFEE_REQUEST_SERVICE_REPLY))
                                .ifThen(
                                        () -> getCoffeeServer().getReply().getReply() != ReplyType.OK,
                                        getServerInterface().readCharacteristic(COFFEE_REQUEST_SERVICE_ERROR)
                                ).elseThen()
                )
                .done((s) -> runOnUiThread(() -> {
                    if(getCoffeeServer().getReply().getReply() != ReplyType.OK) {
                        itemAdapter.setErrorString(getCoffeeServer().getError().get());
                    }
                    deferred.resolve(null);
                    itemAdapter.notifyDataSetChanged();
                }))
                .fail(() -> runOnUiThread(deferred::fail));
        return deferred.promise();
    }

    private final ServerActivityItemAdapter.AdapterItemsCallback adapterItemsCallback = new ServerActivityItemAdapter.AdapterItemsCallback() {

        @Override
        public Promise<Void> refreshLevels() {
            Deferred<Void> deferred = new Deferred<>();
            getServerInterface().run(getServerInterface().readCharacteristic(COFFEE_REQUEST_SERVICE_LEVELS))
                    .done(() -> runOnUiThread(() -> deferred.resolve(null)))
                    .fail(() -> {
                        Log.e(TAG, "Read Levels Failed");
                        runOnUiThread(() -> deferred.fail());
                    });
            return deferred.promise();
        }

        @Override
        public Promise<Void> pour(OrderID orderId) {
            Deferred<Void> d = new Deferred<>();
            checkHasAccessCode()
                    .done(() -> {
                        getCoffeeServer().getRequest().setPour(orderId, getCoffeeServer().getAccessCode());
                        doRequestReply()
                                .done(d::resolve)
                                .fail(d::fail);
                    })
                    .fail(d::fail);
            return d.promise();
        }

        @Override
        public Promise<Void> cancel(OrderID orderId) {
            Deferred<Void> d = new Deferred<>();
            checkHasAccessCode()
                    .done(() -> {
                        getCoffeeServer().getRequest().setCancel(orderId, getCoffeeServer().getAccessCode());
                        doRequestReply()
                                .done(d::resolve)
                                .fail(d::fail);
                    })
                    .fail(d::fail);
            return d.promise();
        }

        @Override
        public ReadOnlyBooleanProperty isWorking() {
            return ServerActivity.this.isWorking();
        }
    };

    private void onOrderBtnClick(View v) {
        checkHasAccessCode().done(() -> {
            getCoffeeServer().getRequest().setOrder(getCoffeeServer().getAccessCode());
            orderBtn.setText(R.string.requesting_label);
            doRequestReply().always(() -> orderBtn.setText(R.string.request_label));
        });
    }

    private void onRescanBtnClick(View v) {
        getScanner().scanForDevice(new CoffeeScanner.FindScanCallback(getCoffeeServer()) {
            @Override
            public void onScanned(CoffeeScanner scanner, ClientCoffeeServer server) {
                super.onScanned(scanner, server);
                runOnUiThread(itemAdapter::notifyDataSetChanged);
            }
        });
    }
}
