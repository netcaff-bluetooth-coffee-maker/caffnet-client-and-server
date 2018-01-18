package com.quew8.properties.deferred;

/**
 * @author Quew8
 */
public interface ProgressivePromise<T> extends Promise<T> {
    int getProgression();
    ProgressivePromise<T> done(ResolvedCallback<T> onDone);
    ProgressivePromise<T> done(Runnable onDone);
    ProgressivePromise<T> fail(Runnable onFail);
    ProgressivePromise<T> always(Runnable always);
    ProgressivePromise<T> progressed(ProgressedCallback onProgressed);
    default ProgressivePromise<T> progressed(Runnable onProgressed) {
        return progressed((p) -> onProgressed.run());
    }
}
