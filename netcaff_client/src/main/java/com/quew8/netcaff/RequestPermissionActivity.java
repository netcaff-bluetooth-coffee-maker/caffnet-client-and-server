package com.quew8.netcaff;

import android.Manifest;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * @author Quew8
 */
public class RequestPermissionActivity extends AbstractActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_permission);

        Button requestPermissionBtn = findViewById(R.id.request_permissions_btn);
        requestPermissionBtn.setOnClickListener(this::onRequestPermissionBtnClick);
    }

    private void onRequestPermissionBtnClick(View v) {
        requestPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                .done((granted) -> {
                    setResult(RESULT_OK);
                    finish();
                });
    }
}
