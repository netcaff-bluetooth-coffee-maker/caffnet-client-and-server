package com.quew8.netcaff.server.access;

import com.quew8.netcaff.lib.access.AccessException;
import com.quew8.netcaff.lib.access.AccessFailure;
import com.quew8.netcaff.lib.access.ServedAccessCode;
import com.quew8.netcaff.lib.server.UserAccessCode;
import com.quew8.properties.IntegerProperty;
import com.quew8.properties.ReadOnlyIntegerProperty;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Quew8
 */
public class AccessList {
    private final HashMap<String, ServedAccessCode> accessCodes;
    private final UserList users;
    private final IntegerProperty nAccessCodes;
    private final SecureRandom random;

    public AccessList(UserList users) {
        this.accessCodes = new HashMap<>();
        this.nAccessCodes = new IntegerProperty(0);
        this.users = users;
        this.random = new SecureRandom();
    }

    public ReadOnlyIntegerProperty getNAccessCodes() {
        return nAccessCodes;
    }

    public UserList getUsers() {
        return users;
    }

    public ServedAccessCode getAccessCodeForUser(String username) {
        return accessCodes.getOrDefault(username, null);
    }

    public String validateAccessCode(UserAccessCode accessCode) throws AccessException {
        if(accessCode == null) {
            throw new IllegalArgumentException("Null access code");
        }
        Iterator<Map.Entry<String, ServedAccessCode>> it = accessCodes.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<String, ServedAccessCode> entry = it.next();
            String username = entry.getKey();
            ServedAccessCode sac = entry.getValue();
            if(sac.getAccessCode().equals(accessCode)) {
                if(sac.expired()) {
                    it.remove();
                    nAccessCodes.set(accessCodes.size());
                    throw new AccessException(AccessFailure.TOKEN_EXPIRED, "Access token has expired");
                }
                return username;
            }
        }
        throw new AccessException(AccessFailure.TOKEN_INVALID, "Access token is invalid");
    }

    public UserAccessCode validateUser(String username, byte[] transferHash) throws AccessException {
        users.validateUser(username, transferHash);
        UserAccessCode uac = UserAccessCode.generate(random);
        ServedAccessCode served = ServedAccessCode.createNow(uac);
        accessCodes.put(username, served);
        nAccessCodes.set(0);
        nAccessCodes.set(accessCodes.size());
        return uac;
    }
}
