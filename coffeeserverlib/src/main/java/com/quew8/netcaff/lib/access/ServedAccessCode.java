package com.quew8.netcaff.lib.access;

import com.quew8.netcaff.lib.TimeUtil;
import com.quew8.netcaff.lib.ble.CoffeeServerProfile;
import com.quew8.netcaff.lib.server.UserAccessCode;

/**
 * @author Quew8
 */
public class ServedAccessCode {
    private final UserAccessCode accessCode;
    private final long expires;

    private ServedAccessCode(UserAccessCode accessCode, long expires) {
        this.accessCode = accessCode;
        this.expires = expires;
    }

    public long getExpires() {
        return expires;
    }

    public UserAccessCode getAccessCode() {
        return accessCode;
    }

    public boolean expired() {
        return expires <= TimeUtil.currentTimeMillis();
    }

    public static ServedAccessCode createNow(UserAccessCode code) {
        return new ServedAccessCode(code, TimeUtil.currentTimeMillis() + CoffeeServerProfile.TOKEN_LIFETIME_MS);
    }
}
