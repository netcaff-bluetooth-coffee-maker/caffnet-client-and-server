package com.quew8.netcaff;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.quew8.netcaff.lib.TimeUtil;
import com.quew8.netcaff.lib.access.ServedAccessCode;
import com.quew8.netcaff.lib.machine.MachineConstants;
import com.quew8.netcaff.lib.server.CoffeeServer;
import com.quew8.netcaff.lib.server.CoffeeServerId;
import com.quew8.netcaff.lib.server.Order;
import com.quew8.netcaff.lib.server.OrderId;
import com.quew8.netcaff.lib.server.OrderStatus;
import com.quew8.netcaff.lib.server.ReplyType;
import com.quew8.netcaff.lib.server.UserAccessCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Quew8
 */
public class ClientCoffeeServer extends CoffeeServer {
    private static final String TAG = ClientCoffeeServer.class.getSimpleName();

    private BluetoothDevice device;
    private HashMap<OrderId, Long> orderReadyEstimates;
    private ArrayList<ServedAccessCode> servedCodes;

    public ClientCoffeeServer(CoffeeServerId serverId) {
        super(serverId);
        this.orderReadyEstimates = new HashMap<>();
        this.servedCodes = new ArrayList<>();
        getAdData().addWrittenCallback(this::onAdDataWritten);
        getReply().addWrittenCallback(this::onReplyWritten);
        getResponseUserAccessCode().addWrittenCallback(this::onAccessCodeWritten);
    }

    UserAccessCode getAccessCode() {
        Iterator<ServedAccessCode> it = servedCodes.iterator();
        while(it.hasNext()) {
            ServedAccessCode sac = it.next();
            if(sac.expired()) {
                it.remove();
            } else {
                return sac.getAccessCode();
            }
        }
        return null;
    }

    boolean hasValidUserAccessCode() {
        return getAccessCode() != null;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    long getTimeUntilOrderReady(OrderId id) {
        if(!orderReadyEstimates.containsKey(id)) {
            throw new IllegalArgumentException("No such order \"" + id + "\"");
        }
        return orderReadyEstimates.get(id);
    }

    private void onAdDataWritten() {
        for(int i = 0; i < getAdData().getNActiveOrders(); i++) {
            Order o = getAdData().getOrder(i);
            if(!orderReadyEstimates.containsKey(o.getId())) {
                long time = TimeUtil.currentTimeMillis();
                if(o.getStatus() == OrderStatus.BEING_MADE) {
                    time += MachineConstants.COFFEE_MAKING_TIME_MS;
                }
                if(o.getStatus() == OrderStatus.QUEUED) {
                    time += 10000;
                }
                orderReadyEstimates.put(o.getId(), time);
            }
        }
        Iterator<Map.Entry<OrderId, Long>> it = orderReadyEstimates.entrySet().iterator();
        while(it.hasNext()) {
            int index = getAdData().getOrderIndexForIdNoThrow(it.next().getKey());
            if(index < 0) {
                it.remove();
            }
        }
    }

    private void onReplyWritten() {
        if(getReply().getReply() == ReplyType.OK) {
            switch(getRequest().getRequestType()) {
                case ORDER: {
                    OrderId orderId = getReply().getOrderId();
                    getAdData().orderPlaced(orderId);
                    orderReadyEstimates.put(orderId, getReply().getDuration().getAbsoluteTime());
                    break;
                }
                case POUR:
                case CANCEL: {
                    OrderId orderId = getReply().getOrderId();
                    getAdData().orderRemoved(orderId);
                    orderReadyEstimates.remove(orderId);
                    break;
                }
                default: {
                    Log.w(TAG, "Unrecognized request code: \"" + getRequest().getRequestType() + "\"");
                }
            }
        }
    }

    private void onAccessCodeWritten() {
        if(getResponseUserAccessCode().isValid()) {
            UserAccessCode uac = getResponseUserAccessCode().getAccessCode();
            servedCodes.add(ServedAccessCode.createNow(uac));
        }
    }
}
