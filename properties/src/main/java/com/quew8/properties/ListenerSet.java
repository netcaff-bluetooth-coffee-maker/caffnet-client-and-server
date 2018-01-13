package com.quew8.properties;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author Quew8
 */
public class ListenerSet<T> {
    private final static ArrayList<Integer> AVAILABLE_HANDLES = new ArrayList<>();
    private static int MAX_HANDLE = 0;

    private ArrayList<ListenerHandle<T>> listeners = new ArrayList<>();

    public ListenerHandle<T> addListener(T listener) {
        ListenerHandle<T> lh = new ListenerHandle<>(listener);
        listeners.add(lh);
        return lh;
    }

    public void removeListener(ListenerHandle<T> handle) {
        if(handle == null) {
            throw new IllegalArgumentException("Null handle passed to removeListener");
        }
        for(int i = 0; i < listeners.size(); i++) {
            if(listeners.get(i).equals(handle)) {
                listeners.get(i).releaseHandle();
                listeners.remove(i);
                return;
            }
        }
    }

    public void notify(ListenerAction<T> action) {
        ArrayList<ListenerHandle<T>> listenersCopy = new ArrayList<>();
        listenersCopy.addAll(listeners);
        for(int i = 0; i < listenersCopy.size(); i++) {
            action.action(listenersCopy.get(i).listener);
        }
    }

    public interface ListenerAction<T> {
        void action(T listener);
    }

    public static class ListenerHandle<T> {
        private final int handle;
        private final T listener;

        private ListenerHandle(T listener) {
            this.handle = getHandle();
            this.listener = listener;
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) return true;
            if(o == null || getClass() != o.getClass()) return false;

            ListenerHandle<?> that = (ListenerHandle<?>) o;

            return handle == that.handle;
        }

        @Override
        public int hashCode() {
            return handle;
        }

        private void releaseHandle() {
            AVAILABLE_HANDLES.add(handle);
        }
    }

    private static int getHandle() {
        if(AVAILABLE_HANDLES.isEmpty()) {
            return ++MAX_HANDLE;
        } else {
            return AVAILABLE_HANDLES.remove(0);
        }
    }
}
