package com.quew8.netcaff;

import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.widget.ImageView;

/**
 * @author Quew8
 */
public class SplashActivity extends ServiceDependantActivity {
    private static final int SPLASH_DURATION_MS = 4000;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
    }

    @Override
    public void onResume() {
        super.onResume();
        ImageView imageView = findViewById(R.id.splash_machine_image);
        Drawable drawable = imageView.getDrawable();
        if(drawable instanceof Animatable) {
            ((Animatable) drawable).start();
        }
        new Handler().postDelayed(this::startApp, SPLASH_DURATION_MS);
    }

    private void startApp() {
        startActivity(new Intent(this, ScanActivity.class));
        finish();
    }
}
