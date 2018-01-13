package com.quew8.netcaff.lib.access;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Quew8
 */
public class TransferAccess {
    public static final Charset CHARSET = StandardCharsets.UTF_8;
    private final MessageDigest digest;

    public TransferAccess() throws NoSuchAlgorithmException {
        this.digest = MessageDigest.getInstance("SHA-256");
    }

    public MessageDigest getDigest() {
        return digest;
    }

    public byte[] hashUser(String username, String password) {
        return hashUser(username.getBytes(CHARSET), password.getBytes(CHARSET));
    }

    public byte[] hashUser(byte[] username, byte[] password) {
        digest.update(username);
        digest.update(password);
        return digest.digest();
    }
}
