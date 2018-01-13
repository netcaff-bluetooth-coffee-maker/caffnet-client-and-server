package com.quew8.netcaff.lib.server;

import com.quew8.netcaff.lib.TimeUtil;
import com.quew8.netcaff.lib.ble.CoffeeServerProfile;

import java.util.UUID;

/**
 * @author Quew8
 */
public class CoffeeServer {
    public static final long READY_COFFEE_TIMEOUT_MS = TimeUtil.asMillis(2, 0);

    private final AdData adData;

    private final Request request;
    private final Reply reply;
    private final CString error;
    private final Levels levels;

    private final CString userName;
    private final RequestPassword password;
    private final ResponseUserAccessCode responseUserAccessCode;
    private final CString loginError;

    public CoffeeServer(CoffeeServerID id) {
        this.adData = new AdData(id);
        this.request = new Request();
        this.reply = new Reply();
        this.error = new CString();
        this.levels = new Levels();
        this.userName = new CString();
        this.password = new RequestPassword();
        this.responseUserAccessCode = new ResponseUserAccessCode();
        this.loginError = new CString();
    }

    public CoffeeServer(int id) {
        this(new CoffeeServerID(id));
    }

    public CoffeeServerID getId() {
        return adData.getServerId();
    }

    public AdData getAdData() {
        return adData;
    }

    public Request getRequest() {
        return request;
    }

    public Reply getReply() {
        return reply;
    }

    public CString getError() {
        return error;
    }

    public Levels getLevels() {
        return levels;
    }

    public CString getUserName() {
        return userName;
    }

    public RequestPassword getPassword() {
        return password;
    }

    public ResponseUserAccessCode getResponseUserAccessCode() {
        return responseUserAccessCode;
    }

    public CString getLoginError() {
        return loginError;
    }

    public void resetResponses() {
        getReply().setError(ReplyType.ERROR);
        getError().set("No error");
        getResponseUserAccessCode().setNone();
        getLoginError().set("No error");
    }

    public CharacteristicStruct getStructForUUID(UUID uuid) {
        if(CoffeeServerProfile.COFFEE_REQUEST_SERVICE_REQUEST.equals(uuid)) {
            return getRequest();
        } else if(CoffeeServerProfile.COFFEE_REQUEST_SERVICE_REPLY.equals(uuid)) {
            return getReply();
        } else if(CoffeeServerProfile.COFFEE_REQUEST_SERVICE_ERROR.equals(uuid)) {
            return getError();
        } else if(CoffeeServerProfile.COFFEE_REQUEST_SERVICE_LEVELS.equals(uuid)) {
            return getLevels();
        } else if(CoffeeServerProfile.COFFEE_LOGIN_SERVICE_USERNAME.equals(uuid)) {
            return getUserName();
        } else if(CoffeeServerProfile.COFFEE_LOGIN_SERVICE_PASSWORD.equals(uuid)) {
            return getPassword();
        } else if(CoffeeServerProfile.COFFEE_LOGIN_SERVICE_ACCESS_CODE.equals(uuid)) {
            return getResponseUserAccessCode();
        } else if(CoffeeServerProfile.COFFEE_LOGIN_SERVICE_ERROR.equals(uuid)) {
            return getLoginError();
        } else {
            throw new IllegalArgumentException("No such characteristic on this server");
        }
    }
}
