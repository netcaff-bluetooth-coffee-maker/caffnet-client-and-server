package com.quew8.netcaff.server;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.quew8.netcaff.lib.access.ServedAccessCode;
import com.quew8.netcaff.server.access.User;
import com.quew8.properties.ListenerSet;
import com.quew8.properties.PropertyChangeListener;

import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;


/**
 * @author Quew8
 */
public class UserAdapter extends CoffeeServerBaseAdapter<User> {
    private ListenerSet.ListenerHandle<PropertyChangeListener<Map<String, ServedAccessCode>>> accessCodesListener;
    private ListenerSet.ListenerHandle<PropertyChangeListener<List<User>>> usersListener;

    UserAdapter(@NonNull Activity activity) {
        super(activity, R.layout.user_item, R.layout.no_users_item);
    }

    @Override
    int getCount(ServerCoffeeServer server) {
        return server.getAccessList().getUsers().size();
    }

    @Override
    User getItem(ServerCoffeeServer server, int position) {
        return server.getAccessList().getUsers().get(position);
    }

    @Override
    void listenTo(ServerCoffeeServer server) {
        accessCodesListener = server.getAccessList().getAccessCodes().addListener(this::mod);
        usersListener = server.getAccessList().getUsers().addListener(this::mod);
    }

    @Override
    void unlistenTo(ServerCoffeeServer server) {
        server.getAccessList().getAccessCodes().removeListener(accessCodesListener);
        server.getAccessList().getUsers().removeListener(usersListener);
    }

    @Override
    UserWatcher getWatcher(View v) {
        return new UserWatcher(
                v.findViewById(R.id.user_username_field),
                v.findViewById(R.id.user_token_field),
                v.findViewById(R.id.user_token_expiry_field)
        );
    }

    @Override
    UserWatcher castToWatcher(Object o) {
        return (UserWatcher) o;
    }

    private void mod(Object o) {
        this.mod();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private class UserWatcher implements IWatcher<User>, Observer {
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
        public void update(ServerCoffeeServer server, User data) {
            ServedAccessCode uac = server.getAccessList().getAccessCodeForUser(data.getUsername());
            usernameField.setText(data.getUsername());
            if(uac == null) {
                codeField.setText(R.string.no_token);
                untilTime.setUntil(-1);
            } else {
                codeField.setText(uac.getAccessCode().toHexString());
                untilTime.setUntil(uac.getExpires());
            }
        }

        @Override
        public void update(Observable o, Object arg) {
            runOnUiThread(() -> codeExpiryField.setText((String) arg));
        }
    }
}
