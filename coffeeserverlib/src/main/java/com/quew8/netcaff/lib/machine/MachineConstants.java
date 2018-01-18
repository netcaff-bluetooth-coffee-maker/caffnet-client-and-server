package com.quew8.netcaff.lib.machine;

import com.quew8.netcaff.lib.TimeUtil;
import com.quew8.netcaff.lib.server.CoffeeServer;

/**
 * @author Quew8
 */
public class MachineConstants {
    public static final long MAX_COFFEE_AGE_MS = TimeUtil.asMillis(15, 0);
    public static final int COFFEE_MAKING_TIME_MS = (int) TimeUtil.asMillis(2, 40);
    public static final int COFFEE_POURING_TIME_MS = (int) TimeUtil.asMillis(2, 30);
    public static final int AVG_COFFEE_POURING_WAIT_TIME_MS = (int) TimeUtil.asMillis(1, 0);
    public static final int MAX_BATCH_SIZE  = 3;

    static {
        if(MAX_COFFEE_AGE_MS < CoffeeServer.READY_COFFEE_TIMEOUT_MS) {
            throw new IllegalStateException("Max coffee age (" + MAX_COFFEE_AGE_MS + ") cannot be less than the ready timeout period (" + CoffeeServer.READY_COFFEE_TIMEOUT_MS + ")");
        }
        if(MAX_BATCH_SIZE <= 0) {
            throw new IllegalStateException("Max match size (" + MAX_BATCH_SIZE + ") must be greater than 0");
        }
    }
}
