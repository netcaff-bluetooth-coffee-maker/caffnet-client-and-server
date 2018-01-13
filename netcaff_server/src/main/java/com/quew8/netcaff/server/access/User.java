package com.quew8.netcaff.server.access;

/**
 * @author Quew8
 */
public class User {
    private final String username;
    private final byte[] salt;
    private final byte[] hash;

    public User(String username, byte[] salt, byte[] hash) {
        this.username = username;
        this.salt = salt;
        this.hash = hash;
    }

    public String getUsername() {
        return username;
    }

    public byte[] getSalt() {
        return salt;
    }

    public byte[] getHash() {
        return hash;
    }
}
