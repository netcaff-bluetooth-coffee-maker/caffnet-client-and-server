package com.quew8.netcaff.lib.server;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * @author Quew8
 */

public class CString extends CharacteristicStruct {
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final CharsetEncoder ENCODER = CHARSET.newEncoder()
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .onMalformedInput(CodingErrorAction.REPORT);
    private static final CharsetDecoder DECODER = CHARSET.newDecoder()
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .onMalformedInput(CodingErrorAction.REPORT);

    private String content;
    private ByteBuffer encodedContent;
    private String errString;

    private CString(String content) {
        this.content = content;
        this.encodedContent = null;
        encodeContent();
    }

    CString() {
        this("");
    }

    public void set(String content) {
        this.content = content;
        encodeContent();
        doCheck();
    }

    public String get() {
        return content;
    }

    @Override
    protected String check() {
        return errString;
    }

    private void encodeContent() {
        errString = null;
        try {
            encodedContent = ENCODER.encode(CharBuffer.wrap(content));
        } catch(CharacterCodingException | BufferOverflowException ex) {
            errString = ex.getMessage();
        }
    }

    private void decodeContent() {
        errString = null;
        try {
            content = DECODER.decode(encodedContent).toString();
        } catch(CharacterCodingException | BufferOverflowException ex) {
            errString = ex.getMessage();
        }
    }

    @Override
    public void putDataInBuffer(ByteBuffer out) {
        if(out.remaining() < encodedContent.remaining()) {
            throw new IllegalArgumentException("At least " + encodedContent.remaining() + " bytes are required");
        }
        out.put(encodedContent);
        encodedContent.flip();
    }

    @Override
    public void getDataFromBuffer(ByteBuffer in) throws StructureFormatException {
        if(encodedContent.remaining() < in.remaining()) {
            encodedContent = ByteBuffer.allocate(in.remaining());
        }
        encodedContent.put(in);
        encodedContent.flip();
        decodeContent();
    }

    @Override
    public int getRequiredBytes() {
        return encodedContent.remaining();
    }

    @Override
    public String getPrettyString() {
        return "\"" + content + "\" (" + content.length() + ")";
    }

    @Override
    public String toString() {
        return "CString{" +
                "content='" + content + '\'' +
                '}';
    }
}
