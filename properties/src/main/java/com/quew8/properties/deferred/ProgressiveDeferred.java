package com.quew8.properties.deferred;

import java.util.ArrayList;

/**
 * @author Quew8
 */
public class ProgressiveDeferred<T> extends Deferred<T> implements ProgressivePromise<T> {
    private final ArrayList<ProgressedCallback> onProgressed;
    private int progression = 0;

    public ProgressiveDeferred() {
        this.onProgressed = new ArrayList<>();
    }

    public void progressed() {
        checkRunning();
        this.progression++;
        for(ProgressedCallback callback: onProgressed) {
            callback.progreessed(progression);
        }
    }

    @Override
    public int getProgression() {
        return progression;
    }

    @Override
    public ProgressiveDeferred<T> progressed(ProgressedCallback onProgressed) {
        for(int i = 0; i < getProgression(); i++) {
            onProgressed.progreessed(i);
        }
        this.onProgressed.add(onProgressed);
        return this;
    }

    @Override
    public ProgressiveDeferred<T> done(ResolvedCallback<T> onDone) {
        super.done(onDone);
        return this;
    }

    @Override
    public ProgressiveDeferred<T> done(Runnable onDone) {
        super.done(onDone);
        return this;
    }

    @Override
    public ProgressiveDeferred<T> always(Runnable always) {
        super.always(always);
        return this;
    }

    @Override
    public ProgressiveDeferred<T> fail(Runnable onFail) {
        super.fail(onFail);
        return this;
    }

    @Override
    public ProgressiveDeferred<T> promise() {
        return this;
    }
}
