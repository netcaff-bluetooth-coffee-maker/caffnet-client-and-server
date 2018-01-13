package com.quew8.netcaff;

import android.os.Handler;
import android.util.Log;

import com.quew8.netcaff.lib.TimeUtil;

import java.util.Observable;

/**
 * @author Quew8
 */
class UntilTime extends Observable {
    private static final String TAG = UntilTime.class.getSimpleName();

    private static final int UPDATE_PERIOD = 1000;
    private long until = -1;
    private Handler h;

    UntilTime() {
        this.h = new Handler();
        loop();
    }

    void setUntil(long until) {
        this.until = until;
        update();
    }

    private String getUntilString() {
        if(until < 0) {
            return "Never";
        } else {
            long diffMs = TimeUtil.diffTimeUntilMillis(until);
            if(diffMs > 0) {
                TimeUtil.DiffTime diff = TimeUtil.createDiffTime(diffMs);
                String s = "In ";
                if(diff.minutes > 0) {
                    s = s + Integer.toString(diff.minutes) + " minutes, ";
                }
                s = s + Integer.toString(diff.seconds) + " seconds";
                return s;
            } else {
                return "Now";
            }
        }
    }

    private void loop() {
        update();
        this.h.postDelayed(this::loop, UPDATE_PERIOD);
    }

    private void update() {
        String s = getUntilString();
        setChanged();
        notifyObservers(s);
    }
}
