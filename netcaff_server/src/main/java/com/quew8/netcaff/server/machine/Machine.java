package com.quew8.netcaff.server.machine;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.util.Log;

import com.quew8.netcaff.lib.TimeUtil;
import com.quew8.properties.BaseProperty;
import com.quew8.properties.IntegerProperty;
import com.quew8.properties.LongProperty;
import com.quew8.properties.Property;
import com.quew8.properties.ReadOnlyIntegerProperty;
import com.quew8.properties.ReadOnlyLongProperty;
import com.quew8.properties.ReadOnlyProperty;
import static com.quew8.netcaff.lib.machine.MachineConstants.*;

/**
 * @author Quew8
 */
public class Machine extends BaseProperty<Machine> {
    private static final String TAG = Machine.class.getSimpleName();

    public static final int MSG_GET = 1, MSG_MAKE = 2, MSG_POUR = 3, MSG_DUMP = 4;
    public static final int MSG_NATURE_FAILED = 1, MSG_NATURE_PERFORMING = 2, MSG_NATURE_FINISHED = 4;
    public static final int FAILURE_NONE = 1, FAILURE_COMMS = 2, FAILURE_REPLY = 3;

    private final MachineHandlerThread thread;
    private final Property<MachineState> state;
    private final IntegerProperty nCups, waterLevel, coffeeLevel;
    private final LongProperty timeStartedMaking;
    private final LongProperty timeLastMade;
    private final LongProperty timeStartedPouring;
    private final String usbDeviceName;
    private final CommsChannel comms;
    private Handler readHandler;
    private Handler makeHandler;
    private Handler pourHandler;
    private Handler dumpHandler;

    private Machine(MachineHandlerThread thread, UsbDevice usbDevice, SerialChannel serialChannel) {
        this.thread = thread;
        this.state = new Property<>(MachineState.IDLE);
        this.nCups = new IntegerProperty(0);
        this.waterLevel = new IntegerProperty(0);
        this.coffeeLevel = new IntegerProperty(0);
        this.timeStartedMaking = new LongProperty(-1);
        this.timeLastMade = new LongProperty(-1);
        this.timeStartedPouring = new LongProperty(-1);
        this.usbDeviceName = usbDevice.getDeviceName();
        this.comms = new CommsChannel(serialChannel);
        dependsOn(state);
        dependsOn(nCups);
        dependsOn(waterLevel);
        dependsOn(coffeeLevel);
        dependsOn(timeStartedMaking);
        dependsOn(timeLastMade);
        dependsOn(timeStartedPouring);
        dependsOn(comms);
    }

    public void read(Handler readHandler) {
        this.readHandler = readHandler;
        this.thread.runOnThread(() -> comms.startConversation(TxCommand.GET, iGetCallback));
    }

    public void make(Handler makeHandler, int n) {
        if(state.get() != MachineState.IDLE) {
            throw new IllegalStateException("Cannot make a coffee whilst not idle");
        }
        TxCommand txCommand;
        if(n == 1) {
            txCommand = TxCommand.MAKE_1;
        } else if(n == 2) {
            txCommand = TxCommand.MAKE_2;
        } else if(n == 3) {
            txCommand = TxCommand.MAKE_3;
        } else {
            throw new IllegalArgumentException("Don't know how to make " + Integer.toString(n) + " coffees at once");
        }
        this.waterLevel.set(this.waterLevel.get() - n);
        this.coffeeLevel.set(this.coffeeLevel.get() - n);
        this.makeHandler = makeHandler;
        this.thread.runOnThread(() -> this.comms.startConversation(txCommand, iMakeCallback));
    }

    public void pour(Handler pourHandler) {
        if(state.get() != MachineState.MADE) {
            throw new IllegalStateException("Cannot pour a coffee whilst not made");
        }
        this.pourHandler = pourHandler;
        this.thread.runOnThread(() -> this.comms.startConversation(TxCommand.POUR, iPouringCallback));
    }

    public void dump(Handler dumpHandler) {
        if(state.get() != MachineState.MADE) {
            throw new IllegalStateException("Cannot dump a coffee whilst not made");
        }
        this.dumpHandler = dumpHandler;
        this.thread.runOnThread(() -> this.comms.startConversation(TxCommand.DUMP, iDumpingCallback));
    }

    public ReadOnlyProperty<MachineState> getState() {
        return state;
    }

    public ReadOnlyIntegerProperty getNCups() {
        return nCups;
    }

    public ReadOnlyIntegerProperty getWaterLevel() {
        return waterLevel;
    }

    public ReadOnlyIntegerProperty getCoffeeLevel() {
        return coffeeLevel;
    }

    public ReadOnlyLongProperty getTimeStartedMaking() {
        return timeStartedMaking;
    }

    public ReadOnlyLongProperty getTimeLastMade() {
        return timeLastMade;
    }

    public ReadOnlyLongProperty getTimeStartedPouring() {
        return timeStartedPouring;
    }

    public boolean isCoffeeOld() {
        return timeLastMade.get() >= 0 && TimeUtil.diffTimeMillis(timeLastMade.get()) >= MAX_COFFEE_AGE_MS;
    }

    public boolean isTalkingAbout(TxCommand command) {
        return comms.isConversationActive(command);
    }

    void disconnected() {
        comms.close();
    }

    private CommsChannel.ConversationCallback iGetCallback = new CommsChannel.ConversationCallback() {
        private int available = -1, water = -1, coffee = -1;

        @Override
        public void onReply(CommsChannel.ConversationHandle handle, RxReply reply, int data) {
            Log.d(TAG, "onReply(" + reply + ", " + data + ")");
            switch(reply) {
                case CUPS_READY: available = data; break;
                case WATER_LEVEL: water = data; break;
                case GROUNDS_LEVEL: coffee = data; break;
                default: {}
            }
            if(available >= 0 && water >= 0 && coffee >= 0) {
                if(available > 0) {
                    if(nCups.get() != available) {
                        timeLastMade.set(0);
                    }
                } else {
                    timeLastMade.set(-1);
                }
                nCups.set(available);
                waterLevel.set(water);
                coffeeLevel.set(coffee);
                if(available > 0) {
                    state.set(MachineState.MADE);
                } else {
                    state.set(MachineState.IDLE);
                }
                available = -1;
                water = -1;
                coffee = -1;
                handle.finished();
                readHandler.sendMessage(readHandler.obtainMessage(
                        MSG_GET,
                        MSG_NATURE_FINISHED,
                        FAILURE_NONE,
                        null
                ));
            }
        }

        @Override
        public void onFailure(CommsChannel.ConversationHandle handle) {
            available = -1;
            water = -1;
            coffee = -1;
            readHandler.sendMessage(readHandler.obtainMessage(
                    MSG_GET,
                    MSG_NATURE_FAILED,
                    FAILURE_COMMS,
                    handle.getFailureReason()
            ));
        }
    };

    private CommsChannel.ConversationCallback iMakeCallback = new CommsChannel.ConversationCallback() {

        @Override
        public void onReply(CommsChannel.ConversationHandle handle, RxReply reply, int data) {
            Log.d(TAG, "onReply(" + reply + ", " + data + ")");
            switch(reply) {
                case PERFORMING: {
                    handle.acknowledged();
                    timeStartedMaking.set(TimeUtil.currentTimeMillis());
                    state.set(MachineState.MAKING);
                    makeHandler.sendMessage(makeHandler.obtainMessage(
                            MSG_MAKE, MSG_NATURE_PERFORMING, FAILURE_NONE, reply
                    ));
                    break;
                }
                case COMPLETE: {
                    nCups.set(handle.getCommand().getNCups());
                    timeStartedMaking.set(-1);
                    timeLastMade.set(TimeUtil.currentTimeMillis());
                    state.set(MachineState.MADE);
                    handle.finished();
                    makeHandler.sendMessage(makeHandler.obtainMessage(
                            MSG_MAKE, MSG_NATURE_FINISHED, FAILURE_NONE, reply
                    ));
                    break;
                }
                case ERR_TOO_MUCH_COFFEE:
                case ERR_WATER_LOW:
                case ERR_GROUNDS_LOW:
                case ERR_WATER_AND_GROUNDS_LOW: {
                    handle.finished();
                    makeHandler.sendMessage(makeHandler.obtainMessage(
                            MSG_MAKE, MSG_NATURE_FAILED, FAILURE_REPLY, reply
                    ));
                }
                default: {}
            }
        }

        @Override
        public void onFailure(CommsChannel.ConversationHandle handle) {
            makeHandler.sendMessage(makeHandler.obtainMessage(
                    MSG_MAKE, MSG_NATURE_FAILED, FAILURE_COMMS, handle.getFailureReason()
            ));
        }
    };

    private CommsChannel.ConversationCallback iPouringCallback = new CommsChannel.ConversationCallback() {

        @Override
        public void onReply(CommsChannel.ConversationHandle handle, RxReply reply, int data) {
            Log.d(TAG, "onReply(" + reply + ", " + data + ")");
            switch(reply) {
                case PERFORMING: {
                    handle.acknowledged();
                    timeStartedPouring.set(TimeUtil.currentTimeMillis());
                    state.set(MachineState.POURING);
                    pourHandler.sendMessage(pourHandler.obtainMessage(
                            MSG_POUR, MSG_NATURE_PERFORMING, FAILURE_NONE, reply
                    ));
                    break;
                }
                case COMPLETE: {
                    timeStartedPouring.set(-1);
                    nCups.set(nCups.get() - handle.getCommand().getNCups());
                    if(nCups.get() > 0) {
                        state.set(MachineState.MADE);
                    } else {
                        state.set(MachineState.IDLE);
                        timeLastMade.set(-1);
                    }
                    handle.finished();
                    pourHandler.sendMessage(pourHandler.obtainMessage(
                            MSG_POUR, MSG_NATURE_FINISHED, FAILURE_NONE, reply
                    ));
                    break;
                }
                case ERR_NO_COFFEE:
                case ERR_NO_MUG: {
                    handle.finished();
                    pourHandler.sendMessage(pourHandler.obtainMessage(
                            MSG_POUR, MSG_NATURE_FAILED, FAILURE_REPLY, reply
                    ));
                }
                default: {}
            }
        }

        @Override
        public void onFailure(CommsChannel.ConversationHandle handle) {
            pourHandler.sendMessage(pourHandler.obtainMessage(
                    MSG_POUR, MSG_NATURE_FAILED, FAILURE_COMMS, handle.getFailureReason()
            ));
        }
    };

    private CommsChannel.ConversationCallback iDumpingCallback = new CommsChannel.ConversationCallback() {

        @Override
        public void onReply(CommsChannel.ConversationHandle handle, RxReply reply, int data) {
            Log.d(TAG, "onReply(" + reply + ", " + data + ")");
            switch(reply) {
                case PERFORMING: {
                    handle.acknowledged();
                    timeStartedPouring.set(TimeUtil.currentTimeMillis());
                    state.set(MachineState.POURING);
                    dumpHandler.sendMessage(dumpHandler.obtainMessage(
                            MSG_DUMP, MSG_NATURE_PERFORMING, FAILURE_NONE, reply
                    ));
                    break;
                }
                case COMPLETE: {
                    timeStartedPouring.set(-1);
                    nCups.set(nCups.get() - handle.getCommand().getNCups());
                    if(nCups.get() > 0) {
                        state.set(MachineState.MADE);
                    } else {
                        state.set(MachineState.IDLE);
                        timeLastMade.set(-1);
                    }
                    handle.finished();
                    dumpHandler.sendMessage(dumpHandler.obtainMessage(
                            MSG_DUMP, MSG_NATURE_FINISHED, FAILURE_NONE, reply
                    ));
                    break;
                }
                case ERR_NO_COFFEE:
                case ERR_NO_MUG: {
                    handle.finished();
                    dumpHandler.sendMessage(dumpHandler.obtainMessage(
                            MSG_DUMP, MSG_NATURE_FAILED, FAILURE_REPLY, reply
                    ));
                }
                default: {}
            }
        }

        @Override
        public void onFailure(CommsChannel.ConversationHandle handle) {
            dumpHandler.sendMessage(dumpHandler.obtainMessage(
                    MSG_DUMP, MSG_NATURE_FAILED, FAILURE_COMMS, handle.getFailureReason()
            ));
        }
    };

    @Override
    public Machine getValue() {
        return this;
    }

    public String getDeviceName() {
        return usbDeviceName;
    }

    @Override
    public String toString() {
        return "Machine{" +
                "state=" + state.get() +
                ", nCups=" + nCups.get() +
                ", waterLevel=" + waterLevel.get() +
                ", coffeeLevel=" + coffeeLevel.get() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        Machine machine = (Machine) o;

        return usbDeviceName.equals(machine.usbDeviceName);
    }

    @Override
    public int hashCode() {
        return usbDeviceName.hashCode();
    }

    static Machine fromUSB(MachineHandlerThread thread, UsbManager usbManager, UsbDevice device) {
        return new Machine(thread, device, SerialChannel.fromUSB(usbManager, device));
    }

    public enum MachineState {
        IDLE, MAKING, MADE, POURING
    }
}
