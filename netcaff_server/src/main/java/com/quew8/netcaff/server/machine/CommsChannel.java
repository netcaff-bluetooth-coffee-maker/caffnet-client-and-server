package com.quew8.netcaff.server.machine;

import android.os.Handler;
import android.util.Log;

import java.util.EnumMap;

/**
 * @author Quew8
 */
class CommsChannel {
    private static final String TAG = CommsChannel.class.getSimpleName();
    private static final int TIMEOUT_MS = 5000;
    private static final int MAXIMUM_ATTEMPTS = 2;

    private SerialChannel serialChannel;
    private Handler handler;
    private EnumMap<TxCommand, ConversationHandle> conversations;

    CommsChannel(SerialChannel serialChannel) {
        this.serialChannel = serialChannel;
        serialChannel.setMessageCallback(this::onMessageReceieved);
        this.handler = new Handler();
        this.conversations = new EnumMap<>(TxCommand.class);
    }

    void startConversation(TxCommand command, ConversationCallback callback) {
        if(conversations.get(command) != null) {
            throw new IllegalStateException("There is already a conversation with that command underway");
        }
        this.conversations.put(command, new ConversationHandle(command, callback));
        serialChannel.writeMessage(command);
    }

    private void onMessageReceieved(TxCommand command, RxReply reply, int data) {
        ConversationHandle conversation = conversations.get(command);
        if(conversation != null) {
            if(command == conversation.command) {
                if(reply == RxReply.ERR_CHECKSUM || reply == RxReply.ERR_UNKNOWN) {
                    conversation.failedAttempts++;
                    if(conversation.failedAttempts >= MAXIMUM_ATTEMPTS) {
                        conversation.failed(MessageFailureReason.TOO_MANY_TRIES);
                    } else {
                        serialChannel.writeMessage(command);
                    }
                } else {
                    conversation.messageReceived(reply, data);
                }
            } else {
                Log.d(TAG, "Reply to " + command + " ignored");
            }
        } else {
            Log.d(TAG, "Unexpected reply");
        }
    }

    boolean isConversationActive(TxCommand msg) {
        return conversations.containsKey(msg) && conversations.get(msg) != null;
    }

    void close() {
        for(TxCommand cmd: TxCommand.values()) {
            ConversationHandle conversationHandle = conversations.get(cmd);
            if(conversationHandle != null) {
                conversationHandle.terminate();
            }
        }
        serialChannel.close();
    }

    public class ConversationHandle {
        private final String TAG = ConversationHandle.class.getSimpleName();

        private final TxCommand command;
        private final ConversationCallback callback;
        private boolean acknowledged = false;
        private boolean finished = false;
        private int failedAttempts = 0;
        private MessageFailureReason failureReason = null;

        private ConversationHandle(TxCommand command, ConversationCallback callback) {
            Log.d(TAG, "STARTING CONVERSATION: " + command);
            this.command = command;
            this.callback = callback;
            CommsChannel.this.handler.postDelayed(this::onTimeout, TIMEOUT_MS);
        }

        private void terminate() {
            failed(MessageFailureReason.CONNECTION_TERMINATED);
        }

        private void onTimeout() {
            if(!acknowledged) {
                failed(MessageFailureReason.TIMEOUT);
            }
        }

        TxCommand getCommand() {
            return command;
        }

        public MessageFailureReason getFailureReason() {
            return failureReason;
        }

        void acknowledged() {
            this.acknowledged = true;
        }

        void finished() {
            Log.d(TAG, "STOPPING CONVERSATION: " + command);
            this.finished = true;
            CommsChannel.this.conversations.put(command, null);
        }

        private void messageReceived(RxReply reply, int data) {
            if(!finished) {
                callback.onReply(this, reply, data);
            }
        }

        private void failed(MessageFailureReason reason) {
            if(!finished) {
                failureReason = reason;
                finished();
                callback.onFailure(this);
            }
        }
    }

    public enum MessageFailureReason {
        TIMEOUT, TOO_MANY_TRIES, CONNECTION_TERMINATED
    }

    public interface ConversationCallback {
        void onReply(ConversationHandle handle, RxReply reply, int data);
        void onFailure(ConversationHandle handle);
    }
}
