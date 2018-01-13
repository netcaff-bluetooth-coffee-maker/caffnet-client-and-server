package com.quew8.netcaff.server;

import android.os.Handler;

import com.quew8.netcaff.lib.TimeUtil;

import java.util.Observable;

/**
 * @author Quew8
 */
class SinceTime extends Observable {
    private static final int UPDATE_PERIOD = 2000;
    private long since = -1;
    private Handler h;

    SinceTime() {
        this.h = new Handler();
        loop();
    }

    void setSince(long since) {
        this.since = since;
        update();
    }

    private String getSinceString() {
        if(since == 0) {
            return "Forever ago";
        } else if(since > 0) {
            TimeUtil.DiffTime diff = TimeUtil.diffTime(since);
            String s = Integer.toString(diff.seconds) + " seconds ago";
            if(diff.minutes > 0) {
                s = Integer.toString(diff.minutes) + " minutes, " + s;
            }
            return s;
        } else {
            return "Never";
        }
    }

    private void loop() {
        update();
        this.h.postDelayed(this::loop, UPDATE_PERIOD);
    }

    private void update() {
        String s = getSinceString();
        setChanged();
        notifyObservers(s);
    }
}
