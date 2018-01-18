package com.quew8.netcaff.server;

import android.os.Handler;
import android.util.Log;

import com.quew8.netcaff.lib.machine.MachineConstants;
import com.quew8.netcaff.lib.server.AdData;
import com.quew8.netcaff.lib.server.CoffeeServer;
import com.quew8.netcaff.lib.server.Duration;
import com.quew8.netcaff.lib.server.Level;
import com.quew8.netcaff.lib.server.Order;
import com.quew8.netcaff.lib.server.OrderId;
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
import com.quew8.netcaff.server.machine.TxCommand;
import com.quew8.properties.BaseProperty;
import com.quew8.properties.Property;
import com.quew8.properties.PropertyListProperty;
import com.quew8.properties.ReadOnlyProperty;
import com.quew8.properties.ReadOnlyListProperty;
import com.quew8.properties.ValueListProperty;
import com.quew8.properties.deferred.ProgressivePromise;
import com.quew8.properties.deferred.Promise;

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
    private final HashMap<OrderId, String> orderOwners;
    private final PropertyListProperty<AssignedMachine> machines;

    private final Handler handler;
    private boolean checkLevelsLoopStarted = false;
    private boolean checkLevelsLoopWaiting = false;

    ServerCoffeeServer(int id, UserList userList) {
        super(id);
        this.accessList = new AccessList(userList);
        this.orderOwners = new HashMap<>();
        this.h = new Handler();
        this.advertisementData = new Property<>(null);
        this.machines = new PropertyListProperty<>();
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
                        OrderId orderId = getAdData().placeOrder();
                        orderOwners.put(orderId, username);
                        update();
                        getReply().set(orderId, ReplyType.OK, duration);
                    }
                } else {
                    OrderId orderId = getRequest().getOrderId();
                    int orderIndex;
                    AssignedMachine machine;
                    if((orderIndex = getAdData().getOrderIndexForIdNoThrow(orderId)) < 0) {
                        throw new ServerRequestException("Unrecognized order ID", ReplyType.ERROR);
                    } else if(!isUserOwnerOf(orderId, username)) {
                        throw new ServerRequestException("You don't own this order", ReplyType.AUTH_FAILED);
                    } else {
                        if(getRequest().getRequestType() == RequestType.POUR) {
                            if(getAdData().getOrder(orderIndex).getStatus() != OrderStatus.READ_TO_POUR) {
                                throw new ServerRequestException("Order not ready to pour", ReplyType.ERROR);
                            } else if(isPouring(machine = getMachineProcessingOrder(orderId))) {
                                throw new ServerRequestException("Someone is already pouring", ReplyType.ERROR);
                            } else {
                                pour(machine, orderId)
                                        .progressed(() -> getReply().set(orderId, ReplyType.OK))
                                        .fail(() -> {
                                            String errorMessage;
                                            RxReply reply = machine.handler.getPouringFailure();
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
                                            getReply().set(orderId, ReplyType.ERROR);
                                        });
                            }
                        } else if(getRequest().getRequestType() == RequestType.CANCEL) {
                            if(getAdData().getOrder(orderIndex).getStatus() == OrderStatus.QUEUED) {
                                getAdData().cancel(orderId);
                            } else {
                                machine = getMachineProcessingOrder(orderId);
                                AdData.AdDataEdit edit = getAdData().edit();
                                cancelOrder(edit, machine, orderId);
                                edit.apply();
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
        AdData.AdDataEdit edit = getAdData().edit();
        for(int i = 0; i < getAdData().getNActiveOrders(); i++) {
            Order o = getAdData().getOrder(i);
            if(o.getStatus() == OrderStatus.READ_TO_POUR && o.isTimedOut()) {
                AssignedMachine m = getMachineProcessingOrder(o.getId());
                cancelOrder(edit, m, o.getId());
            }
        }
        ArrayList<OrderId> assignedOrders = new ArrayList<>();
        for(AssignedMachine m: machines) {
            if(m.isCoffeeOld()) {
                cancelAllOrders(edit, m);
                dump(m);
            } else {
                int unusedTaken = 0;
                int availableTaken = 0;
                int unused = getCountUnusedMadeCups(m);
                int available = Math.min(getCountMakeable(m), MachineConstants.MAX_BATCH_SIZE);
                for(int i = 0; i < getAdData().getNActiveOrders() &&
                        (unused + available - unusedTaken - availableTaken) > 0; i++) {

                    Order o = getAdData().getOrder(i);
                    if(!assignedOrders.contains(o.getId())) {
                        if(o.getStatus() == OrderStatus.QUEUED) {
                            if(unused - unusedTaken > 0) {
                                Log.d(TAG, "Assigning order " + o.getId() + " to old cup");
                                m.reassignOrders.add(o.getId());
                                unusedTaken++;
                                assignedOrders.add(o.getId());
                            } else if(available - availableTaken > 0) {
                                Log.d(TAG, "Assigning order " + o.getId() + " to be made");
                                m.orders.add(o.getId());
                                availableTaken++;
                                assignedOrders.add(o.getId());
                            }
                        }
                    }
                }
                if(unusedTaken + availableTaken > 0) {
                    updateMachineOrders(edit, m);
                }
            }
        }
        edit.apply();
    }

    private int getTotalOrderableCups() {
        int n = 0;
        for(AssignedMachine m: machines) {
            if(!m.isCoffeeOld()) {
                int unused = getCountUnusedMadeCups(m);
                if(unused > 0) {
                    n += unused;
                } else {
                    n += getCountMakeable(m);
                }
            }
        }
        return n;
    }

    private static class MachineStreamData {
        private long time;
        private int minLevel;
    }

    private int getTotalQueueTime() {
        /*MachineStreamData[] streams = new MachineStreamData[machines.size()];
        for(int i = 0; i < machines.size(); i++) {
            AssignedMachine machine = machines.get(i);
            streams[i] = new MachineStreamData();
            streams[i].time = 0;
            streams[i].minLevel = getMinLevel(machine);
            if(machine.dumping) {
                streams[i].time +=
            }
        }*/
        int time = 0;
        for(int i = 0; i < getAdData().getNActiveOrders(); i++) {
            Order order = getAdData().getOrder(i);
            switch(order.getStatus()) {
                case QUEUED: time += MachineConstants.COFFEE_MAKING_TIME_MS; break;
                case BEING_MADE: time += MachineConstants.COFFEE_MAKING_TIME_MS; break;
                case READ_TO_POUR: time += MachineConstants.AVG_COFFEE_POURING_WAIT_TIME_MS; break;
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

    String getOwnerOfOrder(OrderId orderId) {
        return orderOwners.getOrDefault(orderId, "Unknown");
    }

    private boolean isUserOwnerOf(OrderId orderId, String username) {
        String owner = orderOwners.getOrDefault(orderId, null);
        return owner != null && username.equals(owner);
    }

    public ReadOnlyProperty<byte[]> getOrderData() {
        return advertisementData;
    }

    private int getIndexOfMachine(Machine m) {
        for(int i = 0; i < machines.size(); i++) {
            if(machines.get(i).wrapsMachine(m)) {
                return i;
            }
        }
        return -1;
    }

    ReadOnlyListProperty<AssignedMachine> getMachines() {
        return machines;
    }

    public void addMachine(Machine m) {
        this.machines.add(new AssignedMachine(m));
        update();
    }

    public void removeMachine(Machine m) {
        int index = getIndexOfMachine(m);
        if(index < 0) {
            throw new IllegalArgumentException("This machine isn't registered with the server");
        }
        AssignedMachine am = this.machines.removeIndex(index);
        disconnect(am);
    }

    private Promise<Void> updateMachineLevels(Predicate<AssignedMachine> filter) {
        Promise.GroupDeferredBuilder builder = Promise.when();
        for(AssignedMachine m: machines) {
            if(filter.test(m)) {
                builder.andWhen(read(m));
            }
        }
        return builder.promise()
                .always(this::updateLevels);
    }

    public Promise<Void> updateMachineLevels() {
        return updateMachineLevels((m) -> true);
    }

    private Promise<Void> updateEmptyMachineLevels() {
        return updateMachineLevels((m) -> getMinLevel(m) <= 0 && m.getNCups() <= 0 && isIdle(m));
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
            updateEmptyMachineLevels().always(
                    () -> handler.postDelayed(
                            () -> {
                                checkLevelsLoopWaiting = false;
                                checkLevelsLoop();
                            }, CHECK_LEVELS_LOOP_WAIT_MS
                    )
            );
        }
    }

    private void updateLevels() {
        int accWater = 0;
        int accGrounds = 0;
        for(AssignedMachine m: machines) {
            accWater += m.getWaterLevel();
            accGrounds += m.getCoffeeLevel();
        }
        if(getLevels().getWaterLevel() != accWater || getLevels().getGroundsLevel() != accGrounds) {
            getLevels().set(Level.fromN(accGrounds), Level.fromN(accWater));
            update();
        }
    }

    private AssignedMachine getMachineProcessingOrder(OrderId orderId) {
        for(AssignedMachine m: machines) {
            if(m.orders.indexOf(orderId) >= 0) {
                return m;
            }
        }
        throw new IllegalArgumentException("No machines are processing this order (" + orderId.toString() + ")");
    }

    private boolean isPouring(AssignedMachine m) {
        return m.pouringId != null;
    }

    private boolean isIdle(AssignedMachine m) {
        return m.orders.isEmpty();
    }

    private int getCountMakeable(AssignedMachine m) {
        if(isIdle(m)) {
            return getMinLevel(m);
        } else {
            return 0;
        }
    }

    private int getMinLevel(AssignedMachine m) {
        return Math.min(m.getWaterLevel(), m.getCoffeeLevel());
    }

    private int getCountUnusedMadeCups(AssignedMachine m) {
        return m.getNCups() - m.orders.size();
    }

    private void disconnect(AssignedMachine m) {
        getAdData().reset(m.orders);
        m.orders.clear();
    }

    private void updateMachineOrders(AdData.AdDataEdit edit, AssignedMachine m) {
        if(m.machine.getState().get() == Machine.MachineState.IDLE) {
            make(m, m.orders.size());
        } else {
            m.orders.addAll(m.reassignOrders);
            edit.made(m.reassignOrders);
            m.reassignOrders.clear();
            m.handler.postDelayed(this::update, CoffeeServer.READY_COFFEE_TIMEOUT_MS);
        }
    }

    private void cancelOrder(AdData.AdDataEdit edit, AssignedMachine m, OrderId orderId) {
        edit.cancelled(orderId);
        m.orders.remove(orderId);
    }

    private void cancelAllOrders(AdData.AdDataEdit edit, AssignedMachine m) {
        edit.cancelled(m.orders.getValue());
        m.orders.clear();
    }

    private Promise<Void> read(AssignedMachine m) {
        return m.handler.read();
    }

    private ProgressivePromise<Void> make(AssignedMachine m, int n) {
        return m.handler.make(n)
                .progressed(() -> getAdData().setMaking(m.orders))
                .fail(m.orders::clear)
                .done(() -> {
                    m.handler.postDelayed(this::update, CoffeeServer.READY_COFFEE_TIMEOUT_MS);
                    m.handler.postDelayed(this::update, MachineConstants.MAX_COFFEE_AGE_MS);
                    getAdData().setMade(m.orders);
                });
    }

    private ProgressivePromise<Void> pour(AssignedMachine m, OrderId orderId) {
        m.pouringId = orderId;
        return m.handler.pour()
                .fail(() -> m.pouringId = null)
                .done(() -> {
                    getAdData().pour(m.pouringId);
                    m.orders.remove(m.pouringId);
                    m.pouringId = null;
                    update();
                });
    }

    private void dump(AssignedMachine m) {
        if(!m.dumping) {
            if(!m.orders.isEmpty()) {
                throw new IllegalStateException("Cannot dump whilst orders are assigned");
            }
            m.dumping = true;
            doDump(m);
        }
    }

    private void doDump(AssignedMachine m) {
        Log.d(TAG, m.getDeviceName() + " Dumping");
        m.handler.dump()
                .done(() -> {
                    m.dumping = false;
                    update();
                })
                .fail(() -> {
                    Log.d(TAG, "Dump failed (" + m.handler.getDumpingFailure() + ")");
                    if(m.handler.getDumpingFailure() == RxReply.ERR_NO_MUG) {
                        m.handler.postDelayed(() -> doDump(m), ServerCoffeeServer.RETRY_DUMPING_MS);
                    } else {
                        m.dumping = false;
                    }
                });
    }

    public static class AssignedMachine extends BaseProperty<AssignedMachine> {
        private final Machine machine;
        private final MachineHandler handler;
        private final ValueListProperty<OrderId> orders;
        private final ArrayList<OrderId> reassignOrders;
        private OrderId pouringId = null;
        private boolean dumping = false;

        private AssignedMachine(Machine machine) {
            this.machine = machine;
            this.handler = new MachineHandler(machine);
            this.orders = new ValueListProperty<>();
            this.reassignOrders = new ArrayList<>();
            dependsOn(machine);
            dependsOn(orders);
        }

        ReadOnlyListProperty<OrderId> getOrders() {
            return orders;
        }

        Machine.MachineState getState() {
            return machine.getState().get();
        }

        int getNCups() {
            return machine.getNCups().get();
        }

        int getWaterLevel() {
            return machine.getWaterLevel().get();
        }

        int getCoffeeLevel() {
            return machine.getCoffeeLevel().get();
        }

        long getTimeLastMade() {
            return machine.getTimeLastMade().get();
        }

        boolean isCoffeeOld() {
            return machine.isCoffeeOld();
        }

        boolean isTalkingAbout(TxCommand command) {
            return machine.isTalkingAbout(command);
        }

        String getDeviceName() {
            return machine.getDeviceName();
        }

        @Override
        public AssignedMachine getValue() {
            return this;
        }

        private boolean wrapsMachine(Machine machine) {
            return getDeviceName().equals(machine.getDeviceName());
        }
    }
}
