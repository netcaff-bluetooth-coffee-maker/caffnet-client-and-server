package com.quew8.netcaff.server;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.quew8.netcaff.lib.server.OrderId;
import com.quew8.netcaff.server.machine.TxCommand;
import com.quew8.properties.ListenerSet;
import com.quew8.properties.PropertyChangeListener;

import java.util.List;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;


/**
 * @author Quew8
 */
class MachineAdapter extends CoffeeServerBaseAdapter<ServerCoffeeServer.AssignedMachine> {
    private ListenerSet.ListenerHandle<PropertyChangeListener<List<ServerCoffeeServer.AssignedMachine>>> machinesHandle;

    MachineAdapter(@NonNull Activity activity) {
        super(activity, R.layout.machine_item, R.layout.no_machines_item);
    }

    @Override
    int getCount(ServerCoffeeServer server) {
        return server.getMachines().size();
    }

    @Override
    ServerCoffeeServer.AssignedMachine getItem(ServerCoffeeServer server, int position) {
        return server.getMachines().get(position);
    }

    @Override
    void listenTo(ServerCoffeeServer server) {
        machinesHandle = server.getMachines().addListener(this::mod);
        /*machineChangeHandle = server.getMachineChange().addListener(this::mod);*/
    }

    @Override
    void unlistenTo(ServerCoffeeServer server) {
        server.getMachines().removeListener(machinesHandle);
        /*server.getMachineChange().removeListener(machineChangeHandle);*/
    }

    @Override
    MachineWatcher getWatcher(View v) {
        return new MachineWatcher(
                v.findViewById(R.id.machine_name_field),
                v.findViewById(R.id.machine_cups_field),
                v.findViewById(R.id.machine_water_field),
                v.findViewById(R.id.machine_coffee_field),
                v.findViewById(R.id.machine_state_field),
                v.findViewById(R.id.machine_orders_field),
                v.findViewById(R.id.machine_last_made_field),
                v.findViewById(R.id.machine_getting_switch),
                v.findViewById(R.id.machine_making_switch),
                v.findViewById(R.id.machine_pouring_switch),
                v.findViewById(R.id.machine_dumping_switch)
        );
    }

    @Override
    MachineWatcher castToWatcher(Object o) {
        return (MachineWatcher) o;
    }

    private void mod(Object o) {
        mod();
    }

    private static void setVisible(View v, boolean b) {
        v.setVisibility(b ? View.VISIBLE : View.INVISIBLE);
    }

    private static String ordersListToString(List<OrderId> orders) {
        if(orders.isEmpty()) {
            return "None";
        } else {
            return String.join(", ", orders.stream().map(OrderId::toString).toArray(String[]::new));
        }
    }

    private class MachineWatcher implements IWatcher<ServerCoffeeServer.AssignedMachine>, Observer {
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
        public void update(ServerCoffeeServer server, ServerCoffeeServer.AssignedMachine data) {
            List<OrderId> orders = data.getOrders().getValue();
            nameField.setText(data.getDeviceName());
            cupsField.setText(String.format(Locale.getDefault(), "%d", data.getNCups()));
            waterField.setText(String.format(Locale.getDefault(), "%d", data.getWaterLevel()));
            coffeeField.setText(String.format(Locale.getDefault(), "%d", data.getCoffeeLevel()));
            stateField.setText(data.getState().toString());
            ordersField.setText(ordersListToString(orders));
            sinceTime.setSince(data.getTimeLastMade());
            setVisible(gettingSwitch, data.isTalkingAbout(TxCommand.GET));
            setVisible(makingSwitch, data.isTalkingAbout(TxCommand.MAKE_1) ||
                    data.isTalkingAbout(TxCommand.MAKE_2) ||
                    data.isTalkingAbout(TxCommand.MAKE_3));
            setVisible(pouringSwitch, data.isTalkingAbout(TxCommand.POUR));
            setVisible(dumpingSwitch, data.isTalkingAbout(TxCommand.DUMP));
        }

        @Override
        public void update(Observable o, Object arg) {
            runOnUiThread(() -> lastMadeField.setText((String) arg));
        }
    }
}
