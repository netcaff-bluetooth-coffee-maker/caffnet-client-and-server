package com.quew8.netcaff.server;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.quew8.netcaff.lib.server.Order;

import java.util.Observable;
import java.util.Observer;


/**
 * @author Quew8
 */
public class OrderAdapter extends BaseAdapter {
    private final ServerCoffeeServer coffeeServer;
    private final Activity activity;

    OrderAdapter(@NonNull Activity activity, ServerCoffeeServer coffeeServer) {
        this.activity = activity;
        this.coffeeServer = coffeeServer;
        this.coffeeServer.getAdData().addModifiedCallback(this::mod);
    }

    private void mod() {
        this.activity.runOnUiThread(this::notifyDataSetChanged);
    }

    @Override
    public int getCount() {
        return coffeeServer.getAdData().getNActiveOrders();
    }

    @Override
    public Order getItem(int position) {
        return coffeeServer.getAdData().getOrder(position);
    }

    @Override
    public long getItemId(int position) {
        return coffeeServer.getAdData().getOrder(position).getId().hashCode();
    }

    @Override
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
    }

    private class OrderWatcher implements Observer {
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
        public void update(Observable o, Object arg) {
            activity.runOnUiThread(() -> timeReadyField.setText((String) arg));
        }
    }
}
