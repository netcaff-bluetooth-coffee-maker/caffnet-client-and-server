package com.quew8.netcaff.server;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.view.View;
import android.widget.TextView;

import com.quew8.netcaff.lib.server.CharacteristicStruct;
import com.quew8.properties.ListenerSet;

/**
 * @author Quew8
 */
class CharacteristicView {
    private CaffNetServerActivity activity;
    private final TextView titleField;
    private final TextView contentField;
    private final View modifiedNotif;
    private final View writtenNotif;
    private final String title;
    private CharacteristicStruct struct;
    private ListenerSet.ListenerHandle<CharacteristicStruct.ModifiedCallback> modifiedCallbackListenerHandle;
    private ListenerSet.ListenerHandle<CharacteristicStruct.WrittenCallback> writtenCallbackListenerHandle;
    private Animator modifiedAnimator = null;
    private Animator writtenAnimator = null;

    CharacteristicView(CaffNetServerActivity activity, View parent, String title) {
        this.activity = activity;
        this.titleField = parent.findViewById(R.id.characteristic_title);
        this.contentField = parent.findViewById(R.id.characteristic_content);
        this.modifiedNotif = parent.findViewById(R.id.characteristic_modified_switch);
        this.writtenNotif = parent.findViewById(R.id.characteristic_written_switch);
        this.title = title;
        this.struct = null;

        this.modifiedNotif.setVisibility(View.GONE);
        this.writtenNotif.setVisibility(View.GONE);
        doUpdate();
    }

    void setStruct(CharacteristicStruct struct) {
        if(this.struct != null) {
            struct.removeWrittenCallback(writtenCallbackListenerHandle);
            struct.removeModifiedCallback(modifiedCallbackListenerHandle);
        }
        this.struct = struct;
        if(this.struct != null) {
            writtenCallbackListenerHandle = struct.addWrittenCallback(this::onWritten);
            modifiedCallbackListenerHandle = struct.addModifiedCallback(this::onSet);
        }
        update();
    }

    private void onSet() {
        activity.runOnUiThread(this::doModifiedUpdate);
    }

    private void onWritten() {
        activity.runOnUiThread(this::doWrittenUpdate);
    }

    private void update() {
        activity.runOnUiThread(this::doUpdate);
    }

    private void doWrittenUpdate() {
        if(writtenAnimator == null) {
            writtenAnimator = makeAnimator(writtenNotif);
        }
        if(writtenAnimator.isRunning()) {
            writtenAnimator.end();
        }
        writtenAnimator.start();
        doUpdate();
    }

    private void doModifiedUpdate() {
        if(modifiedAnimator == null) {
            modifiedAnimator = makeAnimator(modifiedNotif);
        }
        if(modifiedAnimator.isRunning()) {
            modifiedAnimator.end();
        }
        modifiedAnimator.start();
        doUpdate();
    }

    private void doUpdate() {
        titleField.setText(title);
        if(struct != null) {
            contentField.setText(struct.getPrettyString());
        } else {
            contentField.setText("-");
        }
    }

    private Animator makeAnimator(View v) {
        Animator a = AnimatorInflater.loadAnimator(activity, R.animator.fade_out);
        a.setTarget(v);
        a.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                v.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                v.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animator) {}

            @Override
            public void onAnimationRepeat(Animator animator) {}
        });
        return a;
    }
}
