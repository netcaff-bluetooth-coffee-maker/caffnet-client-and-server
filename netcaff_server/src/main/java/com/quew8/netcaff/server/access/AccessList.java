package com.quew8.netcaff.server.access;

import com.quew8.netcaff.lib.access.AccessException;
import com.quew8.netcaff.lib.access.AccessFailure;
import com.quew8.netcaff.lib.access.ServedAccessCode;
import com.quew8.netcaff.lib.server.UserAccessCode;
import com.quew8.properties.BaseProperty;
import com.quew8.properties.ReadOnlyListProperty;
import com.quew8.properties.ReadOnlyMapProperty;
import com.quew8.properties.ValueMapProperty;

import java.security.SecureRandom;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Quew8
 */
public class AccessList extends BaseProperty<AccessList> {
    private final ValueMapProperty<String, ServedAccessCode> accessCodes;
    private final UserList users;
    private final SecureRandom random;

    public AccessList(UserList users) {
        this.accessCodes = new ValueMapProperty<>();
        this.users = users;
        this.random = new SecureRandom();
        dependsOn(accessCodes);
        dependsOn(users);
    }

    public ReadOnlyMapProperty<String, ServedAccessCode> getAccessCodes() {
        return accessCodes;
    }

    public ReadOnlyListProperty<User> getUsers() {
        return users.getUsers();
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
        return uac;
    }

    @Override
    public AccessList getValue() {
        return this;
    }
}
