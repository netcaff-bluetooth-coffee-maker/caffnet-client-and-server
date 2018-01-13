package com.quew8.netcaff.server;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.quew8.netcaff.lib.server.OrderID;
import com.quew8.netcaff.server.machine.Machine;
import com.quew8.netcaff.server.machine.TxCommand;

import java.util.List;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;


/**
 * @author Quew8
 */

public class MachineAdapter extends BaseAdapter {
    private static final String TAG = MachineAdapter.class.getSimpleName();

    private final ServerCoffeeServer coffeeServer;
    private final Activity activity;

    MachineAdapter(@NonNull Activity activity, ServerCoffeeServer coffeeServer) {
        this.activity = activity;
        this.coffeeServer = coffeeServer;
        this.coffeeServer.getNMachines().addListener(this::mod);
        this.coffeeServer.getMachineChange().addListener((o,n) -> mod(0,0));
    }

    private void mod(int o, int n) {
        this.activity.runOnUiThread(this::notifyDataSetChanged);
    }

    @Override
    public int getCount() {
        return coffeeServer.getNMachines().get();
    }

    @Override
    public Machine getItem(int position) {
        return coffeeServer.getMachine(position);
    }

    @Override
    public long getItemId(int position) {
        return coffeeServer.getMachine(position).getDeviceName().hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup container) {
        MachineWatcher mw;
        if(convertView == null) {
            convertView = LayoutInflater.from(activity).inflate(R.layout.machine_item, container, false);
            mw = new MachineWatcher(
                    convertView.findViewById(R.id.machine_name_field),
                    convertView.findViewById(R.id.machine_cups_field),
                    convertView.findViewById(R.id.machine_water_field),
                    convertView.findViewById(R.id.machine_coffee_field),
                    convertView.findViewById(R.id.machine_state_field),
                    convertView.findViewById(R.id.machine_orders_field),
                    convertView.findViewById(R.id.machine_last_made_field),
                    convertView.findViewById(R.id.machine_getting_switch),
                    convertView.findViewById(R.id.machine_making_switch),
                    convertView.findViewById(R.id.machine_pouring_switch),
                    convertView.findViewById(R.id.machine_dumping_switch)
            );
            convertView.setTag(mw);
        } else {
            mw = (MachineWatcher) convertView.getTag();
        }
        Machine m = coffeeServer.getMachine(position);
        List<OrderID> orders = coffeeServer.getMachineOrders(position);
        mw.nameField.setText(m.getDeviceName());
        mw.cupsField.setText(String.format(Locale.getDefault(), "%d", m.getNCups().get()));
        mw.waterField.setText(String.format(Locale.getDefault(), "%d", m.getWaterLevel().get()));
        mw.coffeeField.setText(String.format(Locale.getDefault(), "%d", m.getCoffeeLevel().get()));
        mw.stateField.setText(m.getState().get().toString());
        mw.ordersField.setText(ordersListToString(orders));
        mw.sinceTime.setSince(m.getTimeLastMade().get());
        setVisible(mw.gettingSwitch, m.isTalkingAbout(TxCommand.GET));
        setVisible(mw.makingSwitch, m.isTalkingAbout(TxCommand.MAKE_1) ||
                m.isTalkingAbout(TxCommand.MAKE_2) ||
                m.isTalkingAbout(TxCommand.MAKE_3));
        setVisible(mw.pouringSwitch, m.isTalkingAbout(TxCommand.POUR));
        setVisible(mw.dumpingSwitch, m.isTalkingAbout(TxCommand.DUMP));
        return convertView;
    }

    private static void setVisible(View v, boolean b) {
        v.setVisibility(b ? View.VISIBLE : View.INVISIBLE);
    }

    private static String ordersListToString(List<OrderID> orders) {
        if(orders.isEmpty()) {
            return "None";
        } else {
            return String.join(", ", orders.stream().map(OrderID::toString).toArray(String[]::new));
        }
    }

    private class MachineWatcher implements Observer {
        private final SinceTime sinceTime;
        private final TextView nameField;
        private final TextView cupsField;
        private final TextView waterField;
        private final TextView coffeeField;
        private final TextView stateField;
        private final TextView ordersField;
        private final TextView lastMadeField;
        private final View gettingSwitch;
        private final View makingSwitch;
        private final View pouringSwitch;
        private final View dumpingSwitch;

        MachineWatcher(TextView nameField, TextView cupsField, TextView waterField, TextView coffeeField,
                       TextView stateField, TextView ordersField, TextView lastMadeField,
                       View gettingSwitch, View makingSwitch, View pouringSwitch, View dumpingSwitch) {

            this.sinceTime = new SinceTime();
            this.nameField = nameField;
            this.cupsField = cupsField;
            this.waterField = waterField;
            this.coffeeField = coffeeField;
            this.stateField = stateField;
            this.ordersField = ordersField;
            this.lastMadeField = lastMadeField;
            this.gettingSwitch = gettingSwitch;
            this.makingSwitch = makingSwitch;
            this.pouringSwitch = pouringSwitch;
            this.dumpingSwitch = dumpingSwitch;
            this.sinceTime.addObserver(this);
        }

        @Override
        public void update(Observable o, Object arg) {
            activity.runOnUiThread(() -> lastMadeField.setText((String) arg));
        }
    }
}
