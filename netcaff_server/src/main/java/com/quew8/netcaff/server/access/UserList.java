package com.quew8.netcaff.server.access;

import com.quew8.netcaff.lib.access.AccessException;
import com.quew8.netcaff.lib.access.AccessFailure;
import com.quew8.netcaff.lib.access.TransferAccess;
import com.quew8.properties.IntegerProperty;
import com.quew8.properties.ReadOnlyIntegerProperty;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;

/**
 * @author Quew8
 */
public class UserList {
    private static final int SALT_LENGTH = 32;
    private final TransferAccess transfer;
    private final SecureRandom random;
    private final ArrayList<User> users;
    private final IntegerProperty nUsers;

    public UserList() throws NoSuchAlgorithmException {
        this.transfer = new TransferAccess();
        this.random = new SecureRandom();
        this.users = new ArrayList<>();
        this.nUsers = new IntegerProperty(0);
    }

    public User getUser(int index) {
        return users.get(index);
    }

    public ReadOnlyIntegerProperty getNUsers() {
        return nUsers;
    }

    private int getIndexOfUser(String username) {
        for(int i = 0; i < users.size(); i++) {
            if(users.get(i).getUsername().equals(username)) {
                return i;
            }
        }
        return -1;
    }

    void validateUser(String username, byte[] transferHash) throws AccessException {
        int userIndex = getIndexOfUser(username);
        if(userIndex >= 0) {
            User u = users.get(userIndex);
            byte[] salt = u.getSalt();
            byte[] hash = generateHash(transferHash, salt);
            if(MessageDigest.isEqual(hash, u.getHash())) {
                return;
            }
        }
        throw new AccessException(AccessFailure.USERNAME_PASSWORD_INCORRECT, "Invalid username and or password");
    }

    private byte[] generateHash(byte[] transferHash, byte[] salt) {
        transfer.getDigest().reset();
        transfer.getDigest().update(salt);
        transfer.getDigest().update(transferHash);
        return transfer.getDigest().digest();
    }

    public void addUser(String username, String password) {
        byte[] usernameBytes = username.getBytes(TransferAccess.CHARSET);
        byte[] passwordBytes = password.getBytes(TransferAccess.CHARSET);
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        byte[] transferHash = transfer.hashUser(usernameBytes, passwordBytes);
        byte[] hash = generateHash(transferHash, salt);
        addUser(username, salt, hash);
    }

    public void addUser(String username, byte[] salt, byte[] hash) {
        users.add(new User(username, salt, hash));
        nUsers.set(users.size());
    }
}
