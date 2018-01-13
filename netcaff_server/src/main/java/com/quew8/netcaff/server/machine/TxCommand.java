package com.quew8.netcaff.server.machine;

/**
 * @author Quew8
 */
public enum TxCommand {
    GET(0x00, 0), MAKE_1(0x01, 1), MAKE_2(0x02, 2), MAKE_3(0x03, 3), POUR(0x04, 1), DUMP(0x05, 1);

    TxCommand(int code, int nCups) {
        this.code = code;
        this.nCups = nCups;
    }

    private final int code;
    private final int nCups;

    public int getCode() {
        return code;
    }

    public int getNCups() {
        return nCups;
    }

    public static TxCommand fromCode(int code) {
        for(TxCommand cmd: TxCommand.values()) {
            if(cmd.getCode() == code) {
                return cmd;
            }
        }
        throw new IllegalArgumentException("No such tx code");
    }
}
