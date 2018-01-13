package com.quew8.netcaff.server;

import android.os.Handler;
import android.util.Log;

import com.quew8.netcaff.lib.TimeUtil;
import com.quew8.netcaff.lib.machine.MachineConstants;
import com.quew8.netcaff.lib.server.AdData;
import com.quew8.netcaff.lib.server.CoffeeServer;
import com.quew8.netcaff.lib.server.Duration;
import com.quew8.netcaff.lib.server.Level;
import com.quew8.netcaff.lib.server.Order;
import com.quew8.netcaff.lib.server.OrderID;
import com.quew8.netcaff.lib.server.OrderStatus;
import com.quew8.netcaff.lib.server.ReplyType;
import com.quew8.netcaff.lib.server.RequestType;
import com.quew8.netcaff.lib.server.Structs;
import com.quew8.netcaff.lib.server.UserAccessCode;
import com.quew8.netcaff.lib.access.AccessException;
import com.quew8.netcaff.server.access.AccessList;
import com.quew8.netcaff.server.access.UserList;
import com.quew8.netcaff.server.machine.Machine;
import com.quew8.netcaff.server.machine.RxReply;
import com.quew8.properties.BooleanProperty;
import com.quew8.properties.IntegerProperty;
import com.quew8.properties.ListenerSet;
import com.quew8.properties.Property;
import com.quew8.properties.PropertyChangeListener;
import com.quew8.properties.ReadOnlyBooleanProperty;
import com.quew8.properties.ReadOnlyIntegerProperty;
import com.quew8.properties.ReadOnlyProperty;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Predicate;

/**
 * @author Quew8
 */
public class ServerCoffeeServer extends CoffeeServer {
    private static final String TAG = ServerCoffeeServer.class.getSimpleName();
    private static final long CHECK_LEVELS_LOOP_WAIT_MS = 3000;
    private static final long RETRY_DUMPING_MS = 3000;

    private final Handler h;
    private final Property<byte[]> advertisementData;
    private final AccessList accessList;
    private final HashMap<OrderID, String> orderOwners;
    private final ArrayList<MachineHandle> machines;
    private final IntegerProperty nMachines;
    private final BooleanProperty machineChange;

    private final Handler handler;
    private boolean checkLevelsLoopStarted = false;
    private boolean checkLevelsLoopWaiting = false;

    ServerCoffeeServer(int id, UserList userList) {
        super(id);
        this.accessList = new AccessList(userList);
        this.orderOwners = new HashMap<>();
        this.h = new Handler();
        this.advertisementData = new Property<>(null);
        this.machines = new ArrayList<>();
        this.nMachines = new IntegerProperty(0);
        this.machineChange = new BooleanProperty(false);
        this.handler = new Handler();
        getAdData().addModifiedCallback(this::onAdDataModified);
        constructOrderData();
        getRequest().addWrittenCallback(this::onRequestWritten);
        getPassword().addWrittenCallback(this::onPasswordWritten);
    }

    private void onAdDataModified() {
        constructOrderData();
    }

    private void onRequestWritten() {
        h.post(this::doRequestWritten);
    }

    private void onPasswordWritten() {
        h.post(this::doPasswordWritten);
    }

    private void doPasswordWritten() {
        try {
            String username = getUserName().get();
            byte[] password = getPassword().getValue();
            UserAccessCode accessCode = accessList.validateUser(username, password);
            getResponseUserAccessCode().set(accessCode);
        } catch(AccessException ex) {
            getLoginError().set(ex.getMessage());
            getResponseUserAccessCode().setNone();
        }
    }

    private void doRequestWritten() {
        try {
            try {
                UserAccessCode uac = getRequest().getAccessCode();
                String username = accessList.validateAccessCode(uac);
                if(getRequest().getRequestType() == RequestType.RESERVED) {
                    throw new ServerRequestException("Reserved request type", ReplyType.ERROR);
                } else if(getRequest().getRequestType() == RequestType.ORDER) {
                    if(getAdData().getNActiveOrders() >= AdData.N_ORDERS) {
                        throw new ServerRequestException("Queue has overflowed", ReplyType.ERROR);
                    } else if(getTotalOrderableCups() <= 0) {
                        throw new ServerRequestException("There is no available supply", ReplyType.LEVELS_LOW);
                    } else {
                        Duration duration = Duration.fromMS(getTotalQueueTime() + MachineConstants.COFFEE_MAKING_TIME_MS);
                        OrderID orderId = getAdData().placeOrder();
                        orderOwners.put(orderId, username);
                        update();
                        getReply().set(orderId, ReplyType.OK, duration);
                    }
                } else {
                    OrderID orderId = getRequest().getOrderId();
                    int orderIndex;
                    MachineHandle machineHandle;
                    if((orderIndex = getAdData().getOrderIndexForIdNoThrow(orderId)) < 0) {
                        throw new ServerRequestException("Unrecognized order ID", ReplyType.ERROR);
                    } else if(!isUserOwnerOf(orderId, username)) {
                        throw new ServerRequestException("You don't own this order", ReplyType.AUTH_FAILED);
                    } else {
                        if(getRequest().getRequestType() == RequestType.ORDER) {
                            if(getAdData().getOrder(orderIndex).getStatus() != OrderStatus.READ_TO_POUR) {
                                throw new ServerRequestException("Order not ready to pour", ReplyType.ERROR);
                            } else if((machineHandle = getMachineProcessingOrder(orderId)).isPouring()) {
                                throw new ServerRequestException("Someone is already pouring", ReplyType.ERROR);
                            } else {
                                machineHandle.requestPour(orderId);
                            }
                        } else if(getRequest().getRequestType() == RequestType.CANCEL) {
                            if(getAdData().getOrder(orderIndex).getStatus() == OrderStatus.QUEUED) {
                                getAdData().cancel(orderId);
                            } else {
                                getMachineProcessingOrder(orderId).cancel(orderId);
                            }
                            getReply().set(orderId, ReplyType.OK, null);
                        } else {
                            throw new ServerRequestException("Unknown error", ReplyType.ERROR);
                        }
                    }
                }
            } catch(AccessException ex) {
                throw new ServerRequestException(ex);
            }
        } catch(ServerRequestException ex) {
            getError().set(ex.getMessage());
            getReply().setError(ex.getReplyType());
        }
    }

    private void update() {
        Log.d(TAG, "UPDATING--------------------");
        for(int i = 0; i < getAdData().getNActiveOrders(); i++) {
            Order o = getAdData().getOrder(i);
            if(o.getStatus() == OrderStatus.READ_TO_POUR && o.isTimedOut()) {
                MachineHandle machineHandle = getMachineProcessingOrder(o.getId());
                machineHandle.cancel(o.getId());
            }
        }
        ArrayList<OrderID> assignedOrders = new ArrayList<>();
        for(MachineHandle mh: machines) {
            if(mh.hasTimedOut()) {
                mh.cancelAll();
                mh.dump();
            } else {
                int unusedTaken = 0;
                int availableTaken = 0;
                int unused = mh.getUnusedMadeCups();
                int available = Math.min(mh.getCanMakeN(), MachineConstants.MAX_BATCH_SIZE);
                for(int i = 0; i < getAdData().getNActiveOrders() &&
                        (unused + available - unusedTaken - availableTaken) > 0; i++) {

                    Order o = getAdData().getOrder(i);
                    if(!assignedOrders.contains(o.getId())) {
                        if(o.getStatus() == OrderStatus.QUEUED) {
                            if(unused - unusedTaken > 0) {
                                Log.d(TAG, "Assigning order " + o.getId() + " to old cup");
                                mh.reassignOrders.add(o.getId());
                                unusedTaken++;
                                assignedOrders.add(o.getId());
                            } else if(available - availableTaken > 0) {
                                Log.d(TAG, "Assigning order " + o.getId() + " to be made");
                                mh.orders.add(o.getId());
                                availableTaken++;
                                assignedOrders.add(o.getId());
                            }
                        }
                    }
                }
                if(unusedTaken + availableTaken > 0) {
                    mh.makeOrders();
                }
            }
        }
    }

    private int getTotalOrderableCups() {
        int n = 0;
        for(MachineHandle mh: machines) {
            if(!mh.hasTimedOut()) {
                int unused = mh.getUnusedMadeCups();
                if(unused > 0) {
                    n += unused;
                } else {
                    n += mh.getCanMakeN();
                }
            }
        }
        return n;
    }

    private int getTotalQueueTime() {
        int time = 0;
        for(int i = 0; i < getAdData().getNActiveOrders(); i++) {
            Order order = getAdData().getOrder(i);
            switch(order.getStatus()) {
                case QUEUED: time += MachineConstants.COFFEE_MAKING_TIME_MS; break;
                case BEING_MADE: time += MachineConstants.COFFEE_MAKING_TIME_MS; break;
                case READ_TO_POUR: time += MachineConstants.AVG_COFFEE_POURING_TIME_MS; break;
                default: throw new IllegalArgumentException(order.getStatus().toString());
            }
        }
        return time;
    }

    private void constructOrderData() {
        advertisementData.set(Structs.writeOut(getAdData()));
    }

    AccessList getAccessList() {
        return accessList;
    }

    String getOwnerOfOrder(OrderID orderId) {
        return orderOwners.getOrDefault(orderId, "Unknown");
    }

    private boolean isUserOwnerOf(OrderID orderId, String username) {
        String owner = orderOwners.getOrDefault(orderId, null);
        return owner != null && username.equals(owner);
    }

    public ReadOnlyProperty<byte[]> getOrderData() {
        return advertisementData;
    }

    ReadOnlyIntegerProperty getNMachines() {
        return nMachines;
    }

    ReadOnlyBooleanProperty getMachineChange() {return machineChange;}

    Machine getMachine(int index) {
        return machines.get(index).m;
    }

    AbstractList<OrderID> getMachineOrders(int index) {
        return machines.get(index).orders;
    }

    public void addMachine(Machine m) {
        MachineHandle mh = new MachineHandle(m);
        this.machines.add(mh);
        this.nMachines.set(this.machines.size());
        mh.read(()->{});
    }

    public void removeMachine(Machine m) {
        int index = -1;
        for(int i = 0; i < this.machines.size(); i++) {
            if(this.machines.get(i).m == m) {
                index = i;
                break;
            }
        }
        if(index < 0) {
            throw new IllegalArgumentException("This machine isn't registered with the server");
        }
        MachineHandle mh = this.machines.remove(index);
        mh.machineDisconnect();
        this.nMachines.set(this.machines.size());
    }

    private void machineChange() {
        this.machineChange.set(!this.machineChange.get());
    }

    public void updateMachineLevels(Runnable r) {
        CounterRunnable cr = new CounterRunnable(machines.size(), r);
        for(MachineHandle mh: machines) {
            mh.read(cr);
        }
    }

    void startCheckLevelsLoop() {
        if(!checkLevelsLoopStarted) {
            checkLevelsLoopStarted = true;
            checkLevelsLoop();
        }
    }

    private void checkLevelsLoop() {
        if(!checkLevelsLoopWaiting) {
            checkLevelsLoopWaiting = true;
            updateEmptyMachineLevels(() -> handler.postDelayed(() -> {
                checkLevelsLoopWaiting = false;
                checkLevelsLoop();
            }, CHECK_LEVELS_LOOP_WAIT_MS));
        }
    }

    private void updateEmptyMachineLevels(Runnable r) {
        Predicate<MachineHandle> filter = (mh) -> mh.getMinLevel() <= 0
                && mh.getMadeCups() <= 0
                && mh.isIdle();
        int n = (int) machines.stream().filter(filter).count();
        if(n > 0) {
            CounterRunnable cr = new CounterRunnable(n, r);
            machines.stream().filter(filter).forEach((mh) -> mh.read(cr));
        } else {
            r.run();
        }
    }

    private void updateLevels() {
        int accWater = 0;
        int accGrounds = 0;
        for(MachineHandle mh: machines) {
            accWater += mh.getWaterLevel();
            accGrounds += mh.getGroundsLevel();
        }
        getLevels().set(Level.fromN(accGrounds), Level.fromN(accWater));
        update();
    }

    private MachineHandle getMachineProcessingOrder(OrderID orderId) {
        for(MachineHandle machineHandle: machines) {
            if(machineHandle.orders.contains(orderId)) {
                return machineHandle;
            }
        }
        throw new IllegalArgumentException("No machines are processing this order (" + orderId.toString() + ")");
    }

    private void runIn(Runnable r, long delayMs) {
        h.postDelayed(r, delayMs);
    }

    private void updateIn(long delayMillis) {
        runIn(this::update, delayMillis);
    }

    private class MachineHandle implements Machine.MakeCallback, Machine.PourCallback, Machine.DumpCallback {
        private final Machine m;
        private final ArrayList<OrderID> orders;
        private final ArrayList<OrderID> reassignOrders;
        private OrderID pouringId = null;
        private boolean dumping = false;
        private ArrayList<Runnable> readDeferred = null;
        private final ListenerSet.ListenerHandle<PropertyChangeListener<Machine.MachineState>> stateListenerHandle;
        private final ListenerSet.ListenerHandle<PropertyChangeListener<Integer>> coffeeListenerHandle;
        private final ListenerSet.ListenerHandle<PropertyChangeListener<Integer>> waterListenerHandle;
        private final ListenerSet.ListenerHandle<PropertyChangeListener<Integer>> nCupsListenerHandle;

        private MachineHandle(Machine m) {
            this.m = m;
            this.orders = new ArrayList<>();
            this.reassignOrders = new ArrayList<>();
            this.stateListenerHandle = m.getState().addListener((n,o) -> machineChange());
            this.coffeeListenerHandle = m.getCoffeeLevel().addListener((n,o) -> machineChange());
            this.waterListenerHandle = m.getWaterLevel().addListener((n,o) -> machineChange());
            this.nCupsListenerHandle = m.getNCups().addListener((n,o) -> machineChange());
        }

        private void machineDisconnect() {
            m.getState().removeListener(this.stateListenerHandle);
            m.getCoffeeLevel().removeListener(this.coffeeListenerHandle);
            m.getWaterLevel().removeListener(this.waterListenerHandle);
            m.getNCups().removeListener(this.nCupsListenerHandle);
            getAdData().reset(orders);
            orders.clear();
        }

        void makeOrders() {
            if(m.getState().get() == Machine.MachineState.IDLE) {
                m.make(this, orders.size());
            } else {
                orders.addAll(reassignOrders);
                getAdData().setMade(reassignOrders);
                reassignOrders.clear();
                updateIn(CoffeeServer.READY_COFFEE_TIMEOUT_MS);
            }
            machineChange();
        }

        void requestPour(OrderID orderId) {
            if(pouringId != null) {
                throw new IllegalStateException("Machine is already pouring");
            }
            pouringId = orderId;
            m.pour(this);
            machineChange();
        }

        void cancel(OrderID orderID) {
            getAdData().cancel(orderID);
            orders.remove(orderID);
            machineChange();
        }

        void cancelAll() {
            getAdData().cancel(orders);
            orders.clear();
            machineChange();
        }

        void dump() {
            if(!dumping) {
                dumping = true;
                doDump();
            }
        }

        private void doDump() {
            m.dump(this);
            machineChange();
        }

        boolean isPouring() {
            return pouringId != null;
        }

        boolean hasTimedOut() {
            return m.isCoffeeOld();
        }

        boolean isIdle() {
            return orders.isEmpty();
        }

        int getCanMakeN() {
            if(m.getState().get() != Machine.MachineState.IDLE) {
                return 0;
            } else {
                return getMinLevel();
            }
        }

        int getMinLevel() {
            return Math.min(getWaterLevel(), getGroundsLevel());
        }

        int getUnusedMadeCups() {
            return getMadeCups() - orders.size();
        }

        int getWaterLevel() {
            return m.getWaterLevel().get();
        }

        int getGroundsLevel() {
            return m.getCoffeeLevel().get();
        }

        int getMadeCups() {
            return m.getNCups().get();
        }

        void read(Runnable r) {
            if(readDeferred == null) {
                readDeferred = new ArrayList<>();
                m.read((success, available, water, coffee) -> {
                    machineChange();
                    updateLevels();
                    for(Runnable runnable: readDeferred) {
                        runnable.run();
                    }
                    readDeferred = null;
                });
                machineChange();
            }
            readDeferred.add(r);
        }

        @Override
        public void making(boolean success, RxReply reply) {
            if(success) {
                Log.d(TAG, "Making: " + machines.get(0).m.toString());
                getAdData().setMaking(orders);
            } else {
                Log.d(TAG, "Failed to request coffee");
                orders.clear();
            }
        }

        @Override
        public void made(RxReply reply) {
            Log.d(TAG, "Made: " + machines.get(0).m.toString());
            updateIn(CoffeeServer.READY_COFFEE_TIMEOUT_MS);
            updateIn(MachineConstants.MAX_COFFEE_AGE_MS);
            machineChange();
            getAdData().setMade(orders);
        }

        @Override
        public void pouring(boolean success, RxReply reply) {
            if(success) {
                getReply().set(pouringId, ReplyType.OK);
                Log.d(TAG, "Pouring: " + machines.get(0).m.toString());
            } else {
                machineChange();
                String errorMessage;
                if(reply == null) {
                    errorMessage = "Communication with machine failed";
                } else {
                    switch(reply) {
                        case ERR_NO_MUG: {
                            errorMessage = "There is no mug present";
                            break;
                        }
                        case ERR_NO_COFFEE: {
                            errorMessage = "There is no coffee available";
                            break;
                        }
                        default: {
                            errorMessage = "Unexpected reply (" + reply.toString() + ")";
                        }
                    }
                }
                getError().set(errorMessage);
                getReply().set(pouringId, ReplyType.ERROR);
                pouringId = null;
            }
        }

        @Override
        public void dumped(RxReply reply) {
            Log.d(TAG, "Dumped");
            machineChange();
            dumping = false;
            update();
        }

        @Override
        public void dumping(boolean success, RxReply reply) {
            if(success) {
                Log.d(TAG, "Dumping: " + machines.get(0).m.toString());
            } else {
                machineChange();
                if(reply == RxReply.ERR_NO_MUG) {
                    Log.d(TAG, "Couldn't dump. Retrying in " + RETRY_DUMPING_MS + "ms");
                    runIn(this::doDump, RETRY_DUMPING_MS);
                } else {
                    String errorMessage;
                    if(reply == null) {
                        errorMessage = "Communication with machine failed";
                    } else {
                        switch(reply) {
                            case ERR_NO_COFFEE: {
                                errorMessage = "There is no coffee to dump";
                                break;
                            }
                            default: {
                                errorMessage = "Unexpected reply (" + reply.toString() + ")";
                            }
                        }
                    }
                    dumping = false;
                    Log.d(TAG, "Dumping error: " + errorMessage);
                }
            }
        }

        @Override
        public void poured(RxReply reply) {
            Log.d(TAG, "Poured: " + machines.get(0).m.toString() + "(" + orders.size() + ")");
            machineChange();
            getAdData().pour(pouringId);
            orders.remove(pouringId);
            pouringId = null;
            update();
        }
    }

    private class CounterRunnable implements Runnable {
        private int i;
        private final int n;
        private final Runnable r;

        CounterRunnable(int n, Runnable r) {
            this.i = 0;
            this.n = n;
            this.r = r;
            if(n == 0) {
                r.run();
            }
        }

        @Override
        public void run() {
            if(++i >= n) {
                r.run();
            }
        }
    }
}
