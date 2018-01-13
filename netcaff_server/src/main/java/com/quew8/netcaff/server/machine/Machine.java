package com.quew8.netcaff.server.machine;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.quew8.netcaff.lib.TimeUtil;
import com.quew8.properties.IntegerProperty;
import com.quew8.properties.LongProperty;
import com.quew8.properties.Property;
import com.quew8.properties.ReadOnlyIntegerProperty;
import com.quew8.properties.ReadOnlyLongProperty;
import com.quew8.properties.ReadOnlyProperty;
import static com.quew8.netcaff.lib.machine.MachineConstants.*;

import java.util.Calendar;

/**
 * @author Quew8
 */
public class Machine {
    private static final String TAG = Machine.class.getSimpleName();

    private Property<MachineState> state;
    private IntegerProperty nCups, waterLevel, coffeeLevel;
    private LongProperty timeLastMade;
    private final String usbDeviceName;
    private final CommsChannel comms;
    private ReadCallback readCallback;
    private MakeCallback makeCallback;
    private PourCallback pourCallback;
    private DumpCallback dumpCallback;

    private Machine(UsbDevice usbDevice, SerialChannel serialChannel) {
        this.state = new Property<>(MachineState.IDLE);
        this.nCups = new IntegerProperty(0);
        this.waterLevel = new IntegerProperty(0);
        this.coffeeLevel = new IntegerProperty(0);
        this.timeLastMade = new LongProperty(-1);
        this.usbDeviceName = usbDevice.getDeviceName();
        this.comms = new CommsChannel(serialChannel);
    }

    public void read(ReadCallback readCallback) {
        this.readCallback = readCallback;
        comms.startConversation(TxCommand.GET, iGetCallback);
    }

    public void make(MakeCallback makeCallback, int n) {
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
        this.makeCallback = makeCallback;
        this.comms.startConversation(txCommand, iMakeCallback);
    }

    public void pour(PourCallback pourCallback) {
        if(state.get() != MachineState.MADE) {
            throw new IllegalStateException("Cannot pour a coffee whilst not made");
        }
        this.pourCallback = pourCallback;
        this.comms.startConversation(TxCommand.POUR, iPouringCallback);
    }

    public void dump(DumpCallback dumpCallback) {
        if(state.get() != MachineState.MADE) {
            throw new IllegalStateException("Cannot dump a coffee whilst not made");
        }
        this.dumpCallback = dumpCallback;
        this.comms.startConversation(TxCommand.DUMP, iDumpingCallback);
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

    public ReadOnlyLongProperty getTimeLastMade() {
        return timeLastMade;
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
                readCallback.done(true, nCups.get(), waterLevel.get(), coffeeLevel.get());
            }
        }

        @Override
        public void onFailure(CommsChannel.ConversationHandle handle) {
            available = -1;
            water = -1;
            coffee = -1;
            readCallback.done(false, -1, -1, -1);
        }
    };

    private CommsChannel.ConversationCallback iMakeCallback = new CommsChannel.ConversationCallback() {

        @Override
        public void onReply(CommsChannel.ConversationHandle handle, RxReply reply, int data) {
            Log.d(TAG, "onReply(" + reply + ", " + data + ")");
            switch(reply) {
                case PERFORMING: {
                    handle.acknowledged();
                    state.set(MachineState.MAKING);
                    makeCallback.making(true, reply);
                    break;
                }
                case COMPLETE: {
                    nCups.set(handle.getCommand().getNCups());
                    timeLastMade.set(Calendar.getInstance().getTimeInMillis());
                    state.set(MachineState.MADE);
                    handle.finished();
                    makeCallback.made(reply);
                    break;
                }
                case ERR_TOO_MUCH_COFFEE:
                case ERR_WATER_LOW:
                case ERR_GROUNDS_LOW:
                case ERR_WATER_AND_GROUNDS_LOW: {
                    handle.finished();
                    makeCallback.making(false, reply);
                }
                default: {}
            }
        }

        @Override
        public void onFailure(CommsChannel.ConversationHandle handle) {
            makeCallback.making(false, null);
        }
    };

    private CommsChannel.ConversationCallback iPouringCallback = new CommsChannel.ConversationCallback() {

        @Override
        public void onReply(CommsChannel.ConversationHandle handle, RxReply reply, int data) {
            Log.d(TAG, "onReply(" + reply + ", " + data + ")");
            switch(reply) {
                case PERFORMING: {
                    handle.acknowledged();
                    state.set(MachineState.POURING);
                    pourCallback.pouring(true, reply);
                    break;
                }
                case COMPLETE: {
                    nCups.set(nCups.get() - handle.getCommand().getNCups());
                    if(nCups.get() > 0) {
                        state.set(MachineState.MADE);
                    } else {
                        state.set(MachineState.IDLE);
                        timeLastMade.set(-1);
                    }
                    handle.finished();
                    pourCallback.poured(reply);
                    break;
                }
                case ERR_NO_COFFEE:
                case ERR_NO_MUG: {
                    handle.finished();
                    pourCallback.pouring(false, reply);
                }
                default: {}
            }
        }

        @Override
        public void onFailure(CommsChannel.ConversationHandle handle) {
            pourCallback.pouring(false, null);
        }
    };

    private CommsChannel.ConversationCallback iDumpingCallback = new CommsChannel.ConversationCallback() {

        @Override
        public void onReply(CommsChannel.ConversationHandle handle, RxReply reply, int data) {
            Log.d(TAG, "onReply(" + reply + ", " + data + ")");
            switch(reply) {
                case PERFORMING: {
                    handle.acknowledged();
                    state.set(MachineState.POURING);
                    dumpCallback.dumping(true, reply);
                    break;
                }
                case COMPLETE: {
                    nCups.set(nCups.get() - handle.getCommand().getNCups());
                    if(nCups.get() > 0) {
                        state.set(MachineState.MADE);
                    } else {
                        state.set(MachineState.IDLE);
                        timeLastMade.set(-1);
                    }
                    handle.finished();
                    dumpCallback.dumped(reply);
                    break;
                }
                case ERR_NO_COFFEE:
                case ERR_NO_MUG: {
                    handle.finished();
                    dumpCallback.dumping(false, reply);
                }
                default: {}
            }
        }

        @Override
        public void onFailure(CommsChannel.ConversationHandle handle) {
            dumpCallback.dumping(false, null);
        }
    };

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

    static Machine fromUSB(UsbManager usbManager, UsbDevice device) {
        return new Machine(device, SerialChannel.fromUSB(usbManager, device));
    }

    public enum MachineState {
        IDLE, MAKING, MADE, POURING
    }

    public interface ReadCallback {
        void done(boolean success, int available, int water, int coffee);
    }

    public interface MakeCallback {
        void making(boolean success, RxReply reply);
        void made(RxReply reply);
    }

    public interface PourCallback {
        void pouring(boolean success, RxReply reply);
        void poured(RxReply reply);
    }

    public interface DumpCallback {
        void dumping(boolean success, RxReply reply);
        void dumped(RxReply reply);
    }
}
