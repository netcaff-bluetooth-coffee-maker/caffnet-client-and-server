package com.quew8.netcaff.server;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.quew8.netcaff.lib.server.CharacteristicStruct;
import com.quew8.netcaff.lib.server.Order;
import com.quew8.properties.ListenerSet;

import java.util.Observable;
import java.util.Observer;


/**
 * @author Quew8
 */
public class OrderAdapter extends CoffeeServerBaseAdapter<Order> {
    private ListenerSet.ListenerHandle<CharacteristicStruct.ModifiedCallback> handle;

    OrderAdapter(@NonNull Activity activity) {
        super(activity, R.layout.order_item, R.layout.no_orders_item);
    }

    @Override
    void listenTo(ServerCoffeeServer server) {
        handle = server.getAdData().addModifiedCallback(this::mod);
    }

    @Override
    void unlistenTo(ServerCoffeeServer server) {
        server.getAdData().removeModifiedCallback(handle);
    }

    @Override
    public Order getItem(ServerCoffeeServer coffeeServer, int position) {
        return coffeeServer.getAdData().getOrder(position);
    }

    @Override
    int getCount(ServerCoffeeServer server) {
        return server.getAdData().getNActiveOrders();
    }

    @Override
    OrderWatcher getWatcher(View v) {
        return new OrderWatcher(
                v.findViewById(R.id.id_text),
                v.findViewById(R.id.status_text),
                v.findViewById(R.id.order_time_ready_field),
                v.findViewById(R.id.order_ordered_by_label)
        );
    }

    @Override
    OrderWatcher castToWatcher(Object o) {
        return (OrderWatcher) o;
    }

    /*@Override
    public View getView(int position, View convertView, ViewGroup container) {
        OrderWatcher ow;
        if(convertView == null) {
            convertView = LayoutInflater.from(activity).inflate(R.layout.order_item, container, false);
            ow = new OrderWatcher(
                    convertView.findViewById(R.id.id_text),
                    convertView.findViewById(R.id.status_text),
                    convertView.findViewById(R.id.order_time_ready_field),
                    convertView.findViewById(R.id.order_ordered_by_label)
            );
            convertView.setTag(ow);
        } else {
            ow = (OrderWatcher) convertView.getTag();
        }
        Order o = coffeeServer.getAdData().getOrder(position);
        ow.idText.setText(o.getId().toString());
        ow.statusText.setText(o.getStatus().toString());
        ow.sinceTime.setSince(o.getTimeReady());
        ow.orderedByField.setText(coffeeServer.getOwnerOfOrder(o.getId()));
        return convertView;
    }*/

    private class OrderWatcher implements IWatcher<Order>, Observer {
        private final TextView idText, statusText, timeReadyField, orderedByField;
        private final SinceTime sinceTime;

        OrderWatcher(TextView idText, TextView statusText, TextView timeReadyField, TextView orderedByField) {
            this.idText = idText;
            this.statusText = statusText;
            this.timeReadyField = timeReadyField;
            this.orderedByField = orderedByField;
            this.sinceTime = new SinceTime();
            this.sinceTime.addObserver(this);
        }

        @Override
        public void update(ServerCoffeeServer server, Order data) {
            idText.setText(data.getId().toString());
            statusText.setText(data.getStatus().toString());
            sinceTime.setSince(data.getTimeReady());
            orderedByField.setText(server.getOwnerOfOrder(data.getId()));
        }

        @Override
        public void update(Observable o, Object arg) {
            runOnUiThread(() -> timeReadyField.setText((String) arg));
        }
    }
}
