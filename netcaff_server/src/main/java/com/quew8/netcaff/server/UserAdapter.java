package com.quew8.netcaff.server;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.quew8.netcaff.lib.access.ServedAccessCode;
import com.quew8.netcaff.server.access.User;

import java.util.Observable;
import java.util.Observer;


/**
 * @author Quew8
 */
public class UserAdapter extends BaseAdapter {
    private static final String TAG = UserAdapter.class.getSimpleName();

    private final ServerCoffeeServer coffeeServer;
    private final Activity activity;

    UserAdapter(@NonNull Activity activity, ServerCoffeeServer coffeeServer) {
        this.activity = activity;
        this.coffeeServer = coffeeServer;
        this.coffeeServer.getAccessList().getNAccessCodes().addListener(this::mod);
        this.coffeeServer.getAccessList().getUsers().getNUsers().addListener(this::mod);
    }

    private void mod(int o, int n) {
        this.activity.runOnUiThread(this::notifyDataSetChanged);
    }

    @Override
    public int getCount() {
        return coffeeServer.getAccessList().getUsers().getNUsers().get();
    }

    @Override
    public User getItem(int position) {
        return coffeeServer.getAccessList().getUsers().getUser(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup container) {
        UserWatcher uw;
        if(convertView == null) {
            convertView = LayoutInflater.from(activity).inflate(R.layout.user_item, container, false);
            uw = new UserWatcher(
                    convertView.findViewById(R.id.user_username_field),
                    convertView.findViewById(R.id.user_token_field),
                    convertView.findViewById(R.id.user_token_expiry_field)
            );
            convertView.setTag(uw);
        } else {
            uw = (UserWatcher) convertView.getTag();
        }
        User u = getItem(position);
        ServedAccessCode uac = coffeeServer.getAccessList().getAccessCodeForUser(u.getUsername());
        uw.usernameField.setText(u.getUsername());
        if(uac == null) {
            uw.codeField.setText("None");
            uw.untilTime.setUntil(-1);
        } else {
            uw.codeField.setText(uac.getAccessCode().toHexString());
            uw.untilTime.setUntil(uac.getExpires());
        }
        return convertView;
    }

    private class UserWatcher implements Observer {
        private final UntilTime untilTime;
        private final TextView usernameField;
        private final TextView codeField;
        private final TextView codeExpiryField;

        UserWatcher(TextView usernameField, TextView codeField, TextView codeExpiryField) {
            this.untilTime = new UntilTime();
            this.usernameField = usernameField;
            this.codeField = codeField;
            this.codeExpiryField = codeExpiryField;
            this.untilTime.addObserver(this);
        }

        @Override
        public void update(Observable o, Object arg) {
            activity.runOnUiThread(() -> codeExpiryField.setText((String) arg));
        }
    }
}
