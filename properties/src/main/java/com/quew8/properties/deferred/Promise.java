package com.quew8.properties.deferred;

import java.util.ArrayList;

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

    static GroupDeferredBuilder when() {
        return new GroupDeferredBuilder();
    }

    static GroupDeferredBuilder when(Promise<?> p) {
        GroupDeferredBuilder gdb = when();
        gdb.andWhen(p);
        return gdb;
    }

    class GroupDeferred extends Deferred<Void> {
        private int i;
        private int n;
        private boolean anyFails;

        private GroupDeferred(int n) {
            this.i = 0;
            this.n = n;
            this.anyFails = false;
            if(n == 0) {
                resolve(null);
            }
        }

        private void onDone() {
            i++;
            if(i >= n) {
                if(anyFails) {
                    fail();
                } else {
                    resolve(null);
                }
            }
        }

        private void onFail() {
            anyFails = true;
            onDone();
        }
    }

    class GroupDeferredBuilder {
        private final ArrayList<Promise<?>> promises;

        private GroupDeferredBuilder() {
            this.promises = new ArrayList<>();
        }

        public GroupDeferredBuilder andWhen(Promise<?> p) {
            promises.add(p);
            return this;
        }

        public Promise<Void> promise() {
            GroupDeferred gd = new GroupDeferred(promises.size());
            for(int i = 0; i < promises.size(); i++) {
                promises.get(i)
                        .done(gd::onDone);
            }
            return gd.promise();
        }
    }

}
