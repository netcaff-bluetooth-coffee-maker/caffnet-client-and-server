package com.quew8.netcaff.lib.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Quew8
 */
public class AdData extends CharacteristicStruct {
    public static final int N_ORDERS = 5;
    private static final int MIN_SIZE_BYTES = getSizeBytes(0);
    public static final int MAX_SIZE_BYTES = getSizeBytes(N_ORDERS);

    private CoffeeServerId serverId;
    private final Order[] orders;
    private int nActiveOrders;
    private boolean editing = false;

    AdData(CoffeeServerId serverId, Order... orders) {
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

    public CoffeeServerId getServerId() {
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

    public AdDataEdit edit() {
        if(editing) {
            throw new IllegalStateException("Already editing this AdData");
        }
        return new AdDataEdit();
    }

    public OrderId placeOrder() {
        OrderId newId = doPlaceOrder();
        set();
        return newId;
    }

    public void orderPlaced(OrderId orderId) {
        doOrderPlaced(orderId);
        set();
    }

    public void orderRemoved(OrderId orderId) {
        doCancel(Collections.singletonList(orderId));
        set();
    }

    public void setMaking(Iterable<OrderId> orderIDs) {
        doSetMaking(orderIDs);
        set();
    }

    public void setMade(Iterable<OrderId> orderIDs) {
        doSetMade(orderIDs);
        set();
    }

    public void pour(OrderId orderId) {
        doPour(orderId);
        set();
    }

    public void cancel(OrderId orderId) {
        cancel(Collections.singletonList(orderId));
    }

    public void cancel(Iterable<OrderId> orderIDs) {
        doCancel(orderIDs);
        set();
    }

    public void reset(Iterable<OrderId> orderIDs) {
        doReset(orderIDs);
        set();
    }

    private void ensureNoDuplicates(Iterable<OrderId> orderIDs) {
        for(OrderId orderId1: orderIDs) {
            boolean matched = false;
            for(OrderId orderId2: orderIDs) {
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

    private OrderId doPlaceOrder() {
        if(nActiveOrders >= N_ORDERS) {
            throw new RuntimeException("Queue not large enough");
        }
        return orders[nActiveOrders++].queue(genId());
    }

    private void doOrderPlaced(OrderId orderId) {
        if(nActiveOrders >= N_ORDERS) {
            throw new RuntimeException("Queue not large enough");
        }
        orders[nActiveOrders++].queue(orderId);
    }

    private void doSetMaking(Iterable<OrderId> orderIDs) {
        ensureNoDuplicates(orderIDs);
        for(OrderId orderId: orderIDs) {
            int index = getOrderIndexForId(orderId);
            Order o = getOrder(index);
            if(o.getStatus() != OrderStatus.QUEUED) {
                throw new IllegalArgumentException("Order " + index + " is not queued");
            }
            o.making();
        }
    }

    private void doSetMade(Iterable<OrderId> orderIDs) {
        ensureNoDuplicates(orderIDs);
        for(OrderId orderId: orderIDs) {
            int index = getOrderIndexForId(orderId);
            Order o = getOrder(index);
            if(o.getStatus() != OrderStatus.BEING_MADE && o.getStatus() != OrderStatus.QUEUED) {
                throw new IllegalArgumentException("Order " + index + " is not queued or being made");
            }
            o.readyToPour();
        }
    }

    private void doPour(OrderId orderId) {
        int index = getOrderIndexForId(orderId);
        Order o = getOrder(index);
        if(o.getStatus() != OrderStatus.READ_TO_POUR) {
            throw new IllegalArgumentException("Order " + index + " is not ready to pour");
        }
        removeOrderIndex(index);
    }

    private void doCancel(Iterable<OrderId> orderIDs) {
        ensureNoDuplicates(orderIDs);
        for(OrderId orderId: orderIDs) {
            removeOrderIndex(getOrderIndexForId(orderId));
        }
    }

    private void doReset(Iterable<OrderId> orderIDs) {
        ensureNoDuplicates(orderIDs);
        for(OrderId orderId: orderIDs) {
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

    public int getOrderIndexForIdNoThrow(OrderId id) {
        for(int i = 0; i < getNActiveOrders(); i++) {
            if(getOrder(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private int getOrderIndexForId(OrderId id) {
        int index = getOrderIndexForIdNoThrow(id);
        if(index < 0) {
            throw new IllegalArgumentException("No such order");
        }
        return index;
    }

    public int getNActiveOrders() {
        return nActiveOrders;
    }

    private OrderId genId() {
        outer:
        for(int i = 1; i <= N_ORDERS; i++) {
            for(int j = 0; j < nActiveOrders; j++) {
                if(orders[j].getId().getId() == i) {
                    continue outer;
                }
            }
            return new OrderId(i);
        }
        throw new RuntimeException("No valid id found");
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
        return CoffeeServerId.SIZE_BYTES + (nActiveOrders * Order.SIZE_BYTES);
    }

    @Override
    public String getPrettyString() {
        StringBuilder s = new StringBuilder(serverId.getPrettyString());
        for(int i = 0; i < getNActiveOrders(); i++) {
            s.append(" | ").append(getOrder(i).getPrettyString());
        }
        return s.toString();
    }

    public class AdDataEdit {
        private final ArrayList<OrderId> making;
        private final ArrayList<OrderId> made;
        private final ArrayList<OrderId> cancelled;
        private boolean edited = false;

        private AdDataEdit() {
            AdData.this.editing = true;
            this.making = new ArrayList<>();
            this.made = new ArrayList<>();
            this.cancelled = new ArrayList<>();
        }

        public void making(OrderId making) {
            checkEditing();
            this.making.add(making);
        }

        public void making(Collection<OrderId> making) {
            checkEditing();
            this.making.addAll(making);
        }

        public void made(OrderId made) {
            checkEditing();
            this.made.add(made);
        }

        public void made(Collection<OrderId> made) {
            checkEditing();
            this.made.addAll(made);
        }

        public void cancelled(OrderId cancelled) {
            checkEditing();
            this.cancelled.add(cancelled);
        }

        public void cancelled(Collection<OrderId> cancelled) {
            checkEditing();
            this.cancelled.addAll(cancelled);
        }

        private void checkEditing() {
            if(this.edited) {
                throw new IllegalStateException("Edit already applied");
            }
        }

        public void apply() {
            this.edited = true;
            AdData.this.editing = false;
            doSetMaking(making);
            doSetMade(made);
            doCancel(cancelled);
            set();
        }
    }
}
