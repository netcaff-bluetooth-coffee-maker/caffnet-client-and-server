package com.quew8.properties.deferred;

import android.util.Log;

import java.util.ArrayList;

/**
 * @author Quew8
 */
public class Deferred<T> implements Promise<T> {
    private ArrayList<ResolvedCallback<T>> onResolved;
    private ArrayList<Runnable> onFailed;
    private boolean resolved;
    private T resolution;
    private boolean failed;

    public Deferred() {
        this.onResolved = new ArrayList<>();
        this.onFailed = new ArrayList<>();
        this.resolved = false;
        this.resolution = null;
        this.failed = false;
    }

    @Override
    public boolean isResolved() {
        return resolved;
    }

    @Override
    public boolean isFailed() {
        return failed;
    }

    @Override
    public Deferred<T> done(ResolvedCallback<T> onDone) {
        if(isResolved()) {
            onDone.resolved(resolution);
        } else {
            onResolved.add(onDone);
        }
        return this;
    }

    @Override
    public Deferred<T> fail(Runnable onFail) {
        if(isFailed()) {
            onFail.run();
        } else {
            onFailed.add(onFail);
        }
        return this;
    }

    private void checkRunning() {
        if(isFailed()) {
            throw new IllegalStateException("Deferred has already failed");
        }
        if(isResolved()) {
            throw new IllegalStateException("Deferred has already been resolved");
        }
    }

    public void resolve(T resolution) {
        checkRunning();
        this.resolved = true;
        this.resolution = resolution;
        for(ResolvedCallback<T> callback: onResolved) {
            callback.resolved(resolution);
        }
    }

    public void fail() {
        checkRunning();
        this.failed = true;
        for(Runnable callback: onFailed) {
            callback.run();
        }
    }

    public Promise<T> promise() {
        return this;
    }
}
