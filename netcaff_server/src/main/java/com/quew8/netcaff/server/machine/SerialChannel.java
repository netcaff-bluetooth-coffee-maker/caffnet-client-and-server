package com.quew8.netcaff.server.machine;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Quew8
 */
class SerialChannel {
    private static final String TAG = SerialChannel.class.getSimpleName();

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private static final int BAUD = 9600;

    private static final int
            RX_MESSAGE_LEN = 18,
            TX_MESSAGE_LEN = 15;
    private static final String
            START_DELIM = "$",
            FIELD_DELIM = ",",
            CHECKSUM_DELIM = "*",
            NEWLINE = "\n",
            MSG_START = "BLECOFF",
            CODE_PATTERN = "[\\dA-Fa-f]{2}";
    private static final Pattern rxMessagePattern = Pattern.compile(
            "^" + Pattern.quote(START_DELIM)
                    + Pattern.quote(MSG_START)
                    + Pattern.quote(FIELD_DELIM) + "(" + CODE_PATTERN + ")"
                    + Pattern.quote(FIELD_DELIM) + "(" + CODE_PATTERN + ")"
                    + Pattern.quote(CHECKSUM_DELIM) + "(" + CODE_PATTERN + ")"
                    + Pattern.quote(NEWLINE) + "$"
    );

    private final UsbSerialDevice serial;
    private ByteBuffer rxBuffer;
    private MessageCallback messageCallback;

    private SerialChannel(UsbSerialDevice serial) {
        this.serial = serial;
        this.rxBuffer = ByteBuffer.allocate(RX_MESSAGE_LEN);
        this.messageCallback = null;
        serial.setBaudRate(BAUD);
        serial.setDataBits(UsbSerialInterface.DATA_BITS_8);
        serial.setStopBits(UsbSerialInterface.STOP_BITS_1);
        serial.setParity(UsbSerialInterface.PARITY_NONE);
        serial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
        serial.read(this::onReceivedData);
    }

    void setMessageCallback(MessageCallback messageCallback) {
        this.messageCallback = messageCallback;
    }

    void writeMessage(TxCommand cmd) {
        byte[] data = new byte[TX_MESSAGE_LEN];
        int off = 0;
        ByteBuffer bb;
        bb = CHARSET.encode(START_DELIM);
        bb.get(data, off, bb.limit());
        off += bb.limit();
        bb = CHARSET.encode(MSG_START + FIELD_DELIM + padHexInt(cmd.getCode()));
        bb.get(data, off, bb.limit());
        int checksum = calcChecksum(data, off, bb.limit());
        off += bb.limit();
        bb = CHARSET.encode(CHECKSUM_DELIM + padHexInt(checksum) + NEWLINE);
        bb.get(data, off, bb.limit());
        Log.d(TAG, "Sending: \"" + new String(data, CHARSET) + "\"");
        serial.write(data);
    }

    private void readMessage() {
        rxBuffer.flip();
        CharBuffer cb = CHARSET.decode(rxBuffer);
        Matcher matcher = rxMessagePattern.matcher(cb);
        if(matcher.matches()) {
            try {
                int calcChecksum = calcChecksum(rxBuffer, START_DELIM.length(), (MSG_START + FIELD_DELIM + FIELD_DELIM).length() + 2 + 2);
                int checksum = Integer.parseInt(matcher.group(3), 16);
                if(checksum == calcChecksum) {
                    int codeInt = Integer.parseInt(matcher.group(1), 16);
                    TxCommand code = TxCommand.fromCode(codeInt);
                    int messageReply = Integer.parseInt(matcher.group(2), 16);
                    RxReply reply = RxReply.fromCode(messageReply);
                    int data = reply.getData(messageReply);
                    Log.d(TAG, "Code: \"" + code + "\", \"" + "Reply: \"" + reply + "\", " + "Data: \"" + data + "\"");
                    if(messageCallback != null) {
                        messageCallback.onMessageReceived(code, reply, data);
                    }
                } else {
                    Log.w(TAG, "Bad Checksum: \"" + cb + "\"");
                }
            } catch(NumberFormatException ex) {
                Log.w(TAG, "Bad Match: \"" + cb + "\"");
            } catch(IllegalArgumentException ex) {
                Log.w(TAG, "Bad Reply: \"" + cb + "\"");
            }
        } else {
            Log.w(TAG, "No Match: \"" + cb + "\"");
        }
        rxBuffer.rewind();
        rxBuffer.limit(rxBuffer.capacity());
    }

    private void onReceivedData(byte[] bytes) {
        if(rxBuffer.remaining() < bytes.length) {
            ByteBuffer bb = ByteBuffer.allocate(rxBuffer.position() + bytes.length);
            rxBuffer.flip();
            bb.put(rxBuffer);
            rxBuffer = bb;
        }
        for(byte b : bytes) {
            rxBuffer.put(b);
            if(b == '\n') {
                readMessage();
            }
        }
    }

    void close() {
        serial.close();
    }

    private static String padHexInt(int i) {
        StringBuilder s = new StringBuilder(Integer.toString(i, 16));
        while(s.length() < 2) {
            s.insert(0, "0");
        }
        return s.toString();
    }

    private static int calcChecksum(byte[] stream, int off, int len) {
        return calcChecksum(ByteBuffer.wrap(stream), off, len);
    }

    private static int calcChecksum(ByteBuffer stream, int off, int len) {
        int c = 0;
        for(int i = 0; i < len; i++) {
            c ^= stream.get(off + i);
        }
        return c;
    }

    static SerialChannel fromUSB(UsbManager usbManager, UsbDevice device) {
        UsbDeviceConnection connection = usbManager.openDevice(device);
        UsbSerialDevice serial = UsbSerialDevice.createUsbSerialDevice(device, connection);
        if(serial == null || !serial.open()) {
            throw new RuntimeException("Couldn't open serial channel");
        }
        return new SerialChannel(serial);
    }

    public interface MessageCallback {
        void onMessageReceived(TxCommand code, RxReply reply, int data);
    }
}
