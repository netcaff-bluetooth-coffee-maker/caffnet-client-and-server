package com.quew8.netcaff.lib.server;

import com.quew8.properties.ListenerSet;
import com.quew8.properties.ListenerSet.ListenerHandle;

import java.nio.ByteBuffer;

/**
 * @author Quew8
 */
public abstract class CharacteristicStruct extends AlignedStruct {
    private final ListenerSet<WrittenCallback> writeCallbacks = new ListenerSet<>();
    private final ListenerSet<ModifiedCallback> modifiedCallbacks = new ListenerSet<>();

    public abstract String getPrettyString();

    public ListenerHandle<WrittenCallback> addWrittenCallback(WrittenCallback callback) {
        return writeCallbacks.addListener(callback);
    }

    public void removeWrittenCallback(ListenerHandle<WrittenCallback> handle) {
        writeCallbacks.removeListener(handle);
    }

    public ListenerHandle<ModifiedCallback> addModifiedCallback(ModifiedCallback callback) {
        return modifiedCallbacks.addListener(callback);
    }

    public void removeModifiedCallback(ListenerHandle<ModifiedCallback> handle) {
        modifiedCallbacks.removeListener(handle);
    }

    @Override
    public void readFromBuffer(ByteBuffer in) throws StructureFormatException {
        super.readFromBuffer(in);
        writeCallbacks.notify(WrittenCallback::onWritten);
    }

    protected void set() {
        modifiedCallbacks.notify(ModifiedCallback::onModified);
    }

    public interface WrittenCallback {
        void onWritten();
    }

    public interface ModifiedCallback {
        void onModified();
    }
}
