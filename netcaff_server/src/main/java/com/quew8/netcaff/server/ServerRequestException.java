package com.quew8.netcaff.server;

import com.quew8.netcaff.lib.server.ReplyType;
import com.quew8.netcaff.lib.access.AccessException;

/**
 * @author Quew8
 */
class ServerRequestException extends Exception {
    private final ReplyType replyType;

    ServerRequestException(String message, ReplyType replyType) {
        super(message);
        this.replyType = replyType;
    }

    ServerRequestException(AccessException accessException) {
        this(accessException.getMessage(), ReplyType.AUTH_FAILED);
    }

    ReplyType getReplyType() {
        return replyType;
    }
}
