package com.quew8.netcaff.lib.server;

import android.util.Log;

import java.nio.ByteBuffer;
import java.util.Collections;

/**
 * @author Quew8
 */
public class AdData extends CharacteristicStruct {
    private static final String TAG = AdData.class.getSimpleName();
    public static final int N_ORDERS = 5;
    private static final int MIN_SIZE_BYTES = getSizeBytes(0);
    public static final int MAX_SIZE_BYTES = getSizeBytes(N_ORDERS);

    private CoffeeServerID serverId;
    private final Order[] orders;
    private int nActiveOrders;

    AdData(CoffeeServerID serverId, Order... orders) {
        this.serverId = serverId;
        this.orders = new Order[N_ORDERS];
        this.nActiveOrders = 0;
        for(int i = 0; i < N_ORDERS; i++) {
            if(i < orders.length) {
                this.orders[i] = orders[i];
                if(orders[i].isActive()) {
                    nActiveOrders++;
                }
            } else {
                this.orders[i] = new Order();
            }
        }
    }

    public CoffeeServerID getServerId() {
        return serverId;
    }

    public Order getOrder(int index) {
        return orders[index];
    }

    public void set(AdData adData) {
        this.serverId = adData.serverId;
        this.nActiveOrders = adData.nActiveOrders;
        System.arraycopy(adData.orders, 0, this.orders, 0, nActiveOrders);
        set();
    }

    public OrderID placeOrder() {
        OrderID newId = doPlaceOrder();
        set();
        return newId;
    }

    public void orderPlaced(OrderID orderId) {
        doOrderPlaced(orderId);
        set();
    }

    public void orderRemoved(OrderID orderId) {
        doCancel(Collections.singletonList(orderId));
        set();
    }

    public void setMaking(Iterable<OrderID> orderIDs) {
        doSetMaking(orderIDs);
        set();
    }

    public void setMade(Iterable<OrderID> orderIDs) {
        doSetMade(orderIDs);
        set();
    }

    public void pour(OrderID orderID) {
        doPour(orderID);
        set();
    }

    public void cancel(OrderID orderID) {
        cancel(Collections.singletonList(orderID));
    }

    public void cancel(Iterable<OrderID> orderIDs) {
        doCancel(orderIDs);
        set();
    }

    public void reset(Iterable<OrderID> orderIDs) {
        doReset(orderIDs);
        set();
    }

    private void ensureNoDuplicates(Iterable<OrderID> orderIDs) {
        for(OrderID orderId1: orderIDs) {
            boolean matched = false;
            for(OrderID orderId2: orderIDs) {
                if(orderId1.equals(orderId2)) {
                    if(matched) {
                        throw new IllegalArgumentException("Duplicate order ids");
                    } else {
                        matched = true;
                    }
                }
            }
        }
    }

    private OrderID doPlaceOrder() {
        if(nActiveOrders >= N_ORDERS) {
            throw new RuntimeException("Queue not large enough");
        }
        return orders[nActiveOrders++].queue(genId());
    }

    private void doOrderPlaced(OrderID orderID) {
        if(nActiveOrders >= N_ORDERS) {
            throw new RuntimeException("Queue not large enough");
        }
        orders[nActiveOrders++].queue(orderID);
    }

    private void doSetMaking(Iterable<OrderID> orderIDs) {
        ensureNoDuplicates(orderIDs);
        for(OrderID orderId: orderIDs) {
            int index = getOrderIndexForId(orderId);
            Order o = getOrder(index);
            if(o.getStatus() != OrderStatus.QUEUED) {
                throw new IllegalArgumentException("Order " + index + " is not queued");
            }
            o.making();
        }
    }

    private void doSetMade(Iterable<OrderID> orderIDs) {
        ensureNoDuplicates(orderIDs);
        for(OrderID orderId: orderIDs) {
            int index = getOrderIndexForId(orderId);
            Order o = getOrder(index);
            if(o.getStatus() != OrderStatus.BEING_MADE && o.getStatus() != OrderStatus.QUEUED) {
                throw new IllegalArgumentException("Order " + index + " is not queued or being made");
            }
            o.readyToPour();
        }
    }

    private void doPour(OrderID orderId) {
        int index = getOrderIndexForId(orderId);
        Order o = getOrder(index);
        if(o.getStatus() != OrderStatus.READ_TO_POUR) {
            throw new IllegalArgumentException("Order " + index + " is not ready to pour");
        }
        removeOrderIndex(index);
    }

    private void doCancel(Iterable<OrderID> orderIDs) {
        ensureNoDuplicates(orderIDs);
        for(OrderID orderId: orderIDs) {
            removeOrderIndex(getOrderIndexForId(orderId));
        }
    }

    private void doReset(Iterable<OrderID> orderIDs) {
        ensureNoDuplicates(orderIDs);
        for(OrderID orderId: orderIDs) {
            int index = getOrderIndexForId(orderId);
            Order o = getOrder(index);
            if(o.getStatus() == OrderStatus.QUEUED) {
                throw new IllegalArgumentException("Order " + index + " is already queued");
            }
            o.reset();
        }
    }

    private void removeOrderIndex(int index) {
        int lastIndex = getNActiveOrders() - 1;
        for(int i = index; i < lastIndex; i++) {
            orders[i].pullFrom(orders[i + 1]);
        }
        orders[lastIndex].pullFrom(null);
        nActiveOrders--;
    }

    public int getOrderIndexForIdNoThrow(OrderID id) {
        for(int i = 0; i < getNActiveOrders(); i++) {
            if(getOrder(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private int getOrderIndexForId(OrderID id) {
        int index = getOrderIndexForIdNoThrow(id);
        if(index < 0) {
            throw new IllegalArgumentException("No such order");
        }
        return index;
    }

    public int getNActiveOrders() {
        return nActiveOrders;
    }

    private OrderID genId() {
        outer:
        for(int i = 1; i <= N_ORDERS; i++) {
            for(int j = 0; j < nActiveOrders; j++) {
                if(orders[j].getId().getId() == i) {
                    continue outer;
                }
            }
            return new OrderID(i);
        }
        throw new RuntimeException("No valid id found");
    }

    public void log() {
        Log.d(TAG, "AD DATA DUMP----------------------------");
        for(int i = 0; i < nActiveOrders; i++) {
            Log.d(TAG, "Order[" + i + "] = " + orders[i]);
        }
        Log.d(TAG, "----------------------------------------");
    }

    @Override
    protected String check() {
        return null;
    }

    @Override
    public void putDataInBuffer(ByteBuffer out) {
        serverId.putDataInBuffer(out);
        for(int i = 0; i < nActiveOrders; i++) {
            orders[i].putDataInBuffer(out);
        }
    }

    @Override
    protected void getDataFromBuffer(ByteBuffer in) throws StructureFormatException {
        if(in.remaining() < MIN_SIZE_BYTES) {
            throw new StructureFormatException("At least " + MIN_SIZE_BYTES + " bytes are required");
        }
        serverId.readFromBuffer(in);
        nActiveOrders = 0;
        while(nActiveOrders < N_ORDERS && in.remaining() >= Order.SIZE_BYTES) {
            orders[nActiveOrders++].readFromBuffer(in);
        }
    }

    @Override
    public int getRequiredBytes() {
        return getSizeBytes(nActiveOrders);
    }

    private static int getSizeBytes(int nActiveOrders) {
        return CoffeeServerID.SIZE_BYTES + (nActiveOrders * Order.SIZE_BYTES);
    }

    @Override
    public String getPrettyString() {
        StringBuilder s = new StringBuilder(serverId.getPrettyString());
        for(int i = 0; i < getNActiveOrders(); i++) {
            s.append(" | ").append(getOrder(i).getPrettyString());
        }
        return s.toString();
    }
}
