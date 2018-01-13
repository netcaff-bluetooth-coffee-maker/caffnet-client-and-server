package com.quew8.netcaff;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.quew8.properties.ListenerSet;
import com.quew8.properties.PropertyChangeListener;

/**
 * @author Quew8
 */
public class ScanActivity extends AbstractActivity {
    private Button scanBtn;
    private ProgressBar scanningProgress;
    private ListenerSet.ListenerHandle<PropertyChangeListener<Boolean>> scanStateListener;
    private ServerItemAdapter serverItemsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        ListView serversView = findViewById(R.id.server_items_list);
        this.scanBtn = findViewById(R.id.scan_btn);
        this.scanningProgress = findViewById(R.id.scanning_progress);
        this.serverItemsAdapter = new ServerItemAdapter(this);
        serversView.setAdapter(serverItemsAdapter);

        scanBtn.setOnClickListener(this::onScanBtnClick);
    }

    @Override
    public void onConnectedToManager() {
        super.onConnectedToManager();
        scanStateListener = getScanner().isScanning().addListener(ScanActivity.this::onScanningStateChange, true);
        serverItemsAdapter.bind(getManager());
    }

    @Override
    public void onDisconnectedFromManager() {
        if(getScanner().isScanning().get()) {
            getScanner().stopScan();
        }
        serverItemsAdapter.bind(null);
        getScanner().isScanning().removeListener(scanStateListener);
        super.onDisconnectedFromManager();
    }

    public void onScanBtnClick(View btn) {
        getScanner().scan(serverItemsAdapter);
    }

    public void onScanningStateChange(Boolean state, Boolean oldState) {
        if(state) {
            scanningProgress.setVisibility(View.VISIBLE);
            scanBtn.setText(R.string.scanning_text);
            scanBtn.setEnabled(false);
        } else {
            scanningProgress.setVisibility(View.GONE);
            scanBtn.setText(R.string.scan_action);
            scanBtn.setEnabled(true);
        }
    }

}
