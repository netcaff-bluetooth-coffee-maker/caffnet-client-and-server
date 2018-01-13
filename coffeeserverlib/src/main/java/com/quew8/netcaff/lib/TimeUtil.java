package com.quew8.netcaff.lib;

import java.util.Calendar;

/**
 * @author Quew8
 */
public class TimeUtil {
    private static final String TAG = TimeUtil.class.getSimpleName();
    public static final int MS_IN_S = 1000;
    public static final int S_IN_M = 60;
    public static final int MS_IN_M = MS_IN_S * S_IN_M;

    private TimeUtil() {}

    public static long asMillis(int minutes, int seconds) {
        return (minutes * MS_IN_M) + (seconds * MS_IN_S);
    }

    public static long currentTimeMillis() {
        return Calendar.getInstance().getTimeInMillis();
    }

    public static long diffTimeMillis(long since) {
        return currentTimeMillis() - since;
    }

    public static long diffTimeUntilMillis(long until) {
        return -diffTimeMillis(until);
    }

    public static DiffTime diffTime(long since) {
        return createDiffTime(diffTimeMillis(since));
    }

    public static DiffTime createDiffTime(long diff) {
        return new DiffTime(Math.max(0, diff));
    }

    public static class DiffTime {
        public final int millis;
        public final int seconds;
        public final int minutes;

        private DiffTime(long millis) {
            this.millis = (int) (millis % MS_IN_M);
            this.seconds = ((int) Math.floor((float) millis / MS_IN_S)) % S_IN_M;
            this.minutes = (int) Math.floor((float) millis / MS_IN_M);
        }
    }
}
