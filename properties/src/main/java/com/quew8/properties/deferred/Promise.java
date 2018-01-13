package com.quew8.properties.deferred;

/**
 * @author Quew8
 */
public interface Promise<T> {
    boolean isResolved();
    boolean isFailed();
    default boolean isRunning() {
        return !(isResolved() || isFailed());
    }
    Promise<T> done(ResolvedCallback<T> onDone);
    default Promise<T> done(Runnable onDone) {
        return done((r) -> onDone.run());
    }
    Promise<T> fail(Runnable onFail);
    default Promise<T> always(Runnable always) {
        return done(always)
                .fail(always);
    }
}
