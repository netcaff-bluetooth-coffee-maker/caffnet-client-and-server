package com.quew8.netcaff;

import android.content.Context;
import android.graphics.drawable.Animatable;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.quew8.netcaff.lib.server.CoffeeServer;
import com.quew8.netcaff.lib.server.Order;
import com.quew8.netcaff.lib.server.OrderID;
import com.quew8.netcaff.lib.server.OrderStatus;
import com.quew8.properties.ReadOnlyBooleanProperty;
import com.quew8.properties.deferred.Promise;

import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

/**
 * @author Quew8
 */
public class ServerActivityItemAdapter extends BaseAdapter {
    private static final String TAG = ServerActivityItemAdapter.class.getSimpleName();
    private static final int VIEW_TYPE_STATUS = 0,
            VIEW_TYPE_ORDER = 1,
            VIEW_TYPE_ERROR = 2;

    private final Context context;
    private final AdapterItemsCallback callback;
    private ClientCoffeeServer coffeeServer;
    private String errorString = null;

    ServerActivityItemAdapter(@NonNull Context context, AdapterItemsCallback callback) {
        this.context = context;
        this.callback = callback;
        this.coffeeServer = null;
        callback.isWorking().addListener(this::onIsWorkingChange);
    }

    void setServer(ClientCoffeeServer coffeeServer) {
        this.coffeeServer = coffeeServer;
        this.errorString = null;
        this.notifyDataSetChanged();
    }

    void setErrorString(String errorString) {
        this.errorString = errorString;
    }

    private void onIsWorkingChange(boolean isWorking, boolean wasWorking) {
        notifyDataSetChanged();
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        switch(position) {
            case 0: return VIEW_TYPE_STATUS;
            case 1: return errorString != null ? VIEW_TYPE_ERROR : VIEW_TYPE_ORDER;
            default: return VIEW_TYPE_ORDER;
        }
    }

    @Override
    public int getCount() {
        if(coffeeServer == null) {
            return 0;
        }
        return 1 + (errorString != null ? 1 : 0) + coffeeServer.getAdData().getNActiveOrders();
    }

    @Override
    public Object getItem(int position) {
        switch(position) {
            case 0: return coffeeServer;
            case 1: return errorString != null ? errorString : coffeeServer.getAdData().getOrder(position - 1);
            default: return coffeeServer.getAdData().getOrder(position - (errorString != null ? 2 : 1));
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup container) {
        switch(getItemViewType(position)) {
            case VIEW_TYPE_STATUS: {
                StatusWatcher sw;
                if(convertView == null) {
                    convertView = LayoutInflater.from(context).inflate(R.layout.activity_server_status, container, false);
                    sw = new StatusWatcher(
                            convertView.findViewById(R.id.server_status_image),
                            convertView.findViewById(R.id.server_status_water_field),
                            convertView.findViewById(R.id.server_status_coffee_field),
                            convertView.findViewById(R.id.server_status_refresh_btn)
                    );
                    convertView.setTag(sw);
                } else {
                    sw = (StatusWatcher) convertView.getTag();
                }
                CoffeeServer server = (CoffeeServer) getItem(position);
                sw.update(server);
                if(sw.image.getDrawable() instanceof Animatable) {
                    Animatable anim = (Animatable) sw.image.getDrawable();
                    if(!anim.isRunning()) {
                        anim.start();
                    }
                }
                break;
            }
            case VIEW_TYPE_ERROR: {
                ErrorWatcher ew;
                if(convertView == null) {
                    convertView = LayoutInflater.from(context).inflate(R.layout.error_item, container, false);
                    ew = new ErrorWatcher(
                            convertView.findViewById(R.id.error_item_content)
                    );
                    convertView.setTag(ew);
                } else {
                    ew = (ErrorWatcher) convertView.getTag();
                }
                String errorString = (String) getItem(position);
                ew.update(errorString);
                break;
            }
            case VIEW_TYPE_ORDER: {
                OrderWatcher ow;
                if(convertView == null) {
                    convertView = LayoutInflater.from(context).inflate(R.layout.order_item, container, false);
                    ow = new OrderWatcher(
                            convertView.findViewById(R.id.order_item_status_image),
                            convertView.findViewById(R.id.order_item_status_field),
                            convertView.findViewById(R.id.order_item_id_field),
                            convertView.findViewById(R.id.order_item_duration_field),
                            convertView.findViewById(R.id.order_item_cancel_btn),
                            convertView.findViewById(R.id.order_item_pour_btn)
                    );
                    convertView.setTag(ow);
                } else {
                    ow = (OrderWatcher) convertView.getTag();
                }
                Order o = (Order) getItem(position);
                ow.orderId = o.getId();
                switch(o.getStatus()) {
                    case QUEUED: {
                        ow.statusImage.setImageDrawable(context.getDrawable(R.drawable.clock_animated));
                        break;
                    }
                    case BEING_MADE: {
                        ow.statusImage.setImageDrawable(context.getDrawable(R.drawable.coffee_stir_animated));
                        break;
                    }
                    case READ_TO_POUR: {
                        ow.statusImage.setImageDrawable(context.getDrawable(R.drawable.coffee_steam_animated));
                        break;
                    }
                }
                if(ow.statusImage.getDrawable() instanceof Animatable) {
                    Animatable anim = (Animatable) ow.statusImage.getDrawable();
                    if(!anim.isRunning()) {
                        anim.start();
                    }
                }
                ow.statusField.setText(o.getStatus().toString());
                ow.idField.setText(o.getId().getPrettyString());
                ow.untilTime.setUntil(coffeeServer.getTimeUntilOrderReady(o.getId()));
                ow.pourBtn.setVisibility(o.getStatus() == OrderStatus.READ_TO_POUR ? View.VISIBLE : View.GONE);
                ow.pourBtn.setEnabled(!callback.isWorking().get());
                ow.cancelBtn.setEnabled(!callback.isWorking().get());
                break;
            }
        }
        return convertView;
    }

    private class StatusWatcher {
        private final ImageView image;
        private final TextView waterField, coffeeField;
        private final Button refreshBtn;

        StatusWatcher(ImageView image, TextView waterField, TextView coffeeField, Button refreshBtn) {
            this.image = image;
            this.waterField = waterField;
            this.coffeeField = coffeeField;
            this.refreshBtn = refreshBtn;
            refreshBtn.setOnClickListener(this::onRefreshBtnClick);
        }

        private void update(CoffeeServer server) {
            waterField.setText(String.format(Locale.getDefault(), "%d", server.getLevels().getWaterLevel()));
            coffeeField.setText(String.format(Locale.getDefault(), "%d", server.getLevels().getGroundsLevel()));
            refreshBtn.setEnabled(!callback.isWorking().get());
        }

        private void onRefreshBtnClick(View v) {
            refreshBtn.setText(R.string.request_reading_levels_label);
            callback.refreshLevels().always(() -> {
                update(coffeeServer);
                refreshBtn.setText(R.string.request_read_levels_label);
            });
        }
    }

    private class ErrorWatcher {
        private final TextView contentField;

        ErrorWatcher(TextView contentField) {
            this.contentField = contentField;
        }

        private void update(String errorString) {
            contentField.setText(errorString);
        }
    }

    private class OrderWatcher implements Observer {
        private final ImageView statusImage;
        private final TextView statusField, idField, durationField;
        private final Button cancelBtn, pourBtn;
        private OrderID orderId;
        private final UntilTime untilTime;

        OrderWatcher(ImageView statusImage, TextView statusField, TextView idField, TextView durationField,
                     Button cancelBtn, Button pourBtn) {

            this.statusImage = statusImage;
            this.statusField = statusField;
            this.idField = idField;
            this.durationField = durationField;
            this.cancelBtn = cancelBtn;
            this.pourBtn = pourBtn;
            this.untilTime = new UntilTime();
            untilTime.addObserver(this);
            cancelBtn.setOnClickListener(this::onCancelBtnClick);
            pourBtn.setOnClickListener(this::onPourBtnClick);
        }

        @Override
        public void update(Observable o, Object arg) {
            durationField.setText((String) arg);
        }

        private void onPourBtnClick(View v) {
            pourBtn.setText(R.string.pouring_label);
            callback.pour(orderId).always(() -> pourBtn.setText(R.string.pour_label));
        }

        private void onCancelBtnClick(View v) {
            cancelBtn.setText(R.string.cancelling_label);
            callback.cancel(orderId).always(() -> cancelBtn.setText(R.string.cancel_label));
        }
    }

    interface AdapterItemsCallback {
        Promise<Void> refreshLevels();
        Promise<Void> pour(OrderID orderId);
        Promise<Void> cancel(OrderID orderId);
        ReadOnlyBooleanProperty isWorking();
    }
}
