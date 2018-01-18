package com.quew8.netcaff.server;

/**
 * @author Quew8
 */
public class UnsupportedSystemServiceException extends Exception {

    public UnsupportedSystemServiceException(String message) {
        super(message);
    }

    public UnsupportedSystemServiceException(Class<?> clazz) {
        this(clazz.getSimpleName() + " is an unsupported system service");
    }
}
