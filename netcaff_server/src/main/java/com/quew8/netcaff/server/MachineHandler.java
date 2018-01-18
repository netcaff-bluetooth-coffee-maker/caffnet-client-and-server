package com.quew8.netcaff.server;

import android.os.Handler;
import android.os.Message;

import com.quew8.netcaff.server.machine.Machine;
import com.quew8.netcaff.server.machine.RxReply;
import com.quew8.properties.deferred.Deferred;
import com.quew8.properties.deferred.ProgressiveDeferred;
import com.quew8.properties.deferred.ProgressivePromise;
import com.quew8.properties.deferred.Promise;

/**
 * @author Quew8
 */
class MachineHandler extends Handler {
    private final Machine m;
    private Deferred<Void> readDeferred = null;
    private int makingN = 0;
    private ProgressiveDeferred<Void> makingDeferred = null;
    private RxReply makingFailure = null;
    private ProgressiveDeferred<Void> pouringDeferred = null;
    private RxReply pouringFailure = null;
    private ProgressiveDeferred<Void> dumpingDeferred = null;
    private RxReply dumpingFailure = null;

    MachineHandler(Machine m) {
        super();
        this.m = m;
    }

    Promise<Void> read() {
        if(readDeferred == null) {
            readDeferred = new Deferred<>();
            m.read(this);
        }
        return readDeferred.promise();
    }

    ProgressivePromise<Void> make(int n) {
        if(makingDeferred == null) {
            makingN = n;
            makingFailure = null;
            makingDeferred = new ProgressiveDeferred<>();
            m.make(this, n);
        }
        if(makingN != n) {
            throw new IllegalStateException("Already making " + makingN + ", cannot make " + n);
        }
        return makingDeferred.promise();
    }

    RxReply getMakingFailure() {
        return makingFailure;
    }

    ProgressivePromise<Void> pour() {
        if(pouringDeferred == null) {
            pouringFailure = null;
            pouringDeferred = new ProgressiveDeferred<>();
            m.pour(this);
        }
        return pouringDeferred.promise();
    }

    RxReply getPouringFailure() {
        return pouringFailure;
    }


    ProgressivePromise<Void> dump() {
        if(dumpingDeferred == null) {
            dumpingFailure = null;
            dumpingDeferred = new ProgressiveDeferred<>();
            m.dump(this);
        }
        return dumpingDeferred.promise();
    }

    RxReply getDumpingFailure() {
        return dumpingFailure;
    }

    @Override
    public void handleMessage(Message msg) {
        switch(msg.what) {
            case Machine.MSG_GET: {
                switch(msg.arg1) {
                    case Machine.MSG_NATURE_FINISHED: {
                        readDeferred.resolve(null);
                        readDeferred = null;
                        break;
                    }
                    case Machine.MSG_NATURE_FAILED: {
                        readDeferred.fail();
                        readDeferred = null;
                        break;
                    }
                }
                break;
            }
            case Machine.MSG_MAKE: {
                switch(msg.arg1) {
                    case Machine.MSG_NATURE_FAILED: {
                        makingDeferred.fail();
                        makingDeferred = null;
                        break;
                    }
                    case Machine.MSG_NATURE_PERFORMING: {
                        makingDeferred.progressed();
                        break;
                    }
                    case Machine.MSG_NATURE_FINISHED: {
                        makingDeferred.resolve(null);
                        makingDeferred = null;
                        break;
                    }
                }
                break;
            }
            case Machine.MSG_POUR: {
                switch(msg.arg1) {
                    case Machine.MSG_NATURE_FAILED: {
                        pouringDeferred.fail();
                        pouringDeferred = null;
                        break;
                    }
                    case Machine.MSG_NATURE_PERFORMING: {
                        pouringDeferred.progressed();
                        break;
                    }
                    case Machine.MSG_NATURE_FINISHED: {
                        pouringDeferred.resolve(null);
                        pouringDeferred = null;
                        break;
                    }
                }
                break;
            }
            case Machine.MSG_DUMP: {
                switch(msg.arg1) {
                    case Machine.MSG_NATURE_FAILED: {
                        dumpingDeferred.fail();
                        dumpingDeferred = null;
                        break;
                    }
                    case Machine.MSG_NATURE_PERFORMING: {
                        dumpingDeferred.progressed();
                        break;
                    }
                    case Machine.MSG_NATURE_FINISHED: {
                        dumpingDeferred.resolve(null);
                        dumpingDeferred = null;
                        break;
                    }
                }
                break;
            }
            default: {
                throw new RuntimeException("Unknown message what " + msg.what);
            }
        }
    }
}
