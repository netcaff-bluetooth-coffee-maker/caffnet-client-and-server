package com.quew8.netcaff.server.machine;

/**
 * @author Quew8
 */
public enum RxReply {
    PERFORMING(RxReplyCodeType.FULL, 0x00), COMPLETE(RxReplyCodeType.FULL, 0x01),
    CUPS_READY(RxReplyCodeType.HALF, 0x40), WATER_LEVEL(RxReplyCodeType.HALF, 0x60),
    GROUNDS_LEVEL(RxReplyCodeType.HALF, 0x20),
    ERR_WATER_LOW(RxReplyCodeType.FULL, 0x81), ERR_GROUNDS_LOW(RxReplyCodeType.FULL, 0x82),
    ERR_NO_MUG(RxReplyCodeType.FULL, 0x83), ERR_WATER_AND_GROUNDS_LOW(RxReplyCodeType.FULL, 0x84),
    ERR_TOO_MUCH_COFFEE(RxReplyCodeType.FULL, 0x85), ERR_NO_COFFEE(RxReplyCodeType.FULL, 0x86),
    ERR_CHECKSUM(RxReplyCodeType.FULL, 0xCC), ERR_UNKNOWN(RxReplyCodeType.FULL, 0xFF);

    RxReply(RxReplyCodeType codeType, int code) {
        this.codeType = codeType;
        this.code = code;
    }

    private final RxReplyCodeType codeType;
    private final int code;

    public int getCode() {
        return code;
    }

    public int getData(int tx) {
        switch(this.codeType) {
            case HALF: return tx & 0x0F;
            case FULL:
            default: return 0;
        }
    }

    public static RxReply fromCode(int code) {
        for(RxReply cmd: RxReply.values()) {
            switch(cmd.codeType) {
                case FULL: {
                    if(cmd.getCode() == code) {
                        return cmd;
                    }
                    break;
                }
                case HALF: {
                    if(cmd.getCode() == (code & 0xF0)) {
                        return cmd;
                    }
                    break;
                }
            }
        }
        throw new IllegalArgumentException("No such rx code");
    }

    private enum RxReplyCodeType {
        FULL, HALF
    }
}
