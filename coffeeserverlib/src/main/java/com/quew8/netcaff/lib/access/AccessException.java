package com.quew8.netcaff.lib.access;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Quew8
 */
public class AccessException extends Exception {
    private static final Pattern messageRegex = Pattern.compile("^([\\d]+)[\\s]*-[\\s]*(.)+$");
    private final String rawMessage;
    private final AccessFailure failure;

    public AccessException(AccessFailure failure, String message) {
        super(Integer.toString(failure.getCode()) + " - " + message);
        this.rawMessage = message;
        this.failure = failure;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public AccessFailure getFailure() {
        return failure;
    }

    public static AccessException fromMessage(String message) {
        Matcher m = messageRegex.matcher(message);
        if(m.find()) {
            AccessFailure af = AccessFailure.fromCodeNoThrow(Integer.parseInt(m.group(1)));
            String rawMessage = m.group(2);
            if(af != null) {
                return new AccessException(af, rawMessage);
            }
        }
        return null;
    }
}
