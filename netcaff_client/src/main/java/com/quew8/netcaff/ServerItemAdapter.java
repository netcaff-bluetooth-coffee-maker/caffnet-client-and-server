package com.quew8.netcaff;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.quew8.netcaff.ble.CoffeeManager;
import com.quew8.netcaff.ble.CoffeeScanner;

/**
 * @author Quew8
 */
public class ServerItemAdapter extends BaseAdapter implements CoffeeScanner.ScanCallback {
    private final Context context;
    private CoffeeManager coffeeManager;
    private static final int VIEW_TYPE_NO_SERVERS = 0,
            VIEW_TYPE_SERVER = 1;

    ServerItemAdapter(@NonNull Context context) {
        this.context = context;
        this.coffeeManager = null;
    }

    void bind(CoffeeManager m) {
        this.coffeeManager = m;
        notifyDataSetChanged();
    }

    @Override
    public void onScanStarted(CoffeeScanner scanner) {
        notifyDataSetChanged();
    }

    @Override
    public void onScanEnded(CoffeeScanner scanner) {
        notifyDataSetChanged();
    }

    @Override
    public void onDiscovered(CoffeeScanner scanner, ClientCoffeeServer server) {
    }

    @Override
    public void onScanned(CoffeeScanner scanner, ClientCoffeeServer server) {
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if(position > 0) {
            return VIEW_TYPE_SERVER;
        } else {
            int n = coffeeManager.getServerList().getCountServers();
            if(n == 0) {
                return VIEW_TYPE_NO_SERVERS;
            } else {
                return VIEW_TYPE_SERVER;
            }
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getCount() {
        if(coffeeManager == null) {
            return 0;
        } else {
            int n = coffeeManager.getServerList().getCountServers();
            if(n == 0) {
                return 1;
            } else {
                return n;
            }
        }
    }

    @Override
    public ClientCoffeeServer getItem(int position) {
        int n = coffeeManager.getServerList().getCountServers();
        if(position > 0 || n > 0) {
            return coffeeManager.getServerList().getServerByIndex(position);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup container) {
        int type = getItemViewType(position);
        if(type == VIEW_TYPE_SERVER) {
            ServerItemWatcher mw;
            if(convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.server_item, container, false);
                mw = new ServerItemWatcher(
                        convertView.findViewById(R.id.server_item_found_content),
                        convertView.findViewById(R.id.server_item_not_found_content),
                        convertView.findViewById(R.id.server_item_id_field),
                        convertView.findViewById(R.id.server_item_address_field),
                        convertView.findViewById(R.id.server_item_connect_btn)
                );
                convertView.setTag(mw);
            } else {
                mw = (ServerItemWatcher) convertView.getTag();
            }
            ClientCoffeeServer d = getItem(position);
            mw.server = d;
            mw.idField.setText(d.getAdData().getServerId().getPrettyString());
            if(d.getDevice() != null) {
                mw.addressField.setText(d.getDevice().getAddress());
                mw.foundRoot.setVisibility(View.VISIBLE);
                mw.notFoundRoot.setVisibility(View.GONE);
            } else {
                mw.addressField.setText("");
                mw.foundRoot.setVisibility(View.GONE);
                mw.notFoundRoot.setVisibility(View.VISIBLE);
            }
            mw.connectBtn.setEnabled(!coffeeManager.getScanner().isScanning().get());
        } else if(type == VIEW_TYPE_NO_SERVERS) {
            if(convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.no_servers_item, container, false);
            }
        }
        return convertView;
    }

    private class ServerItemWatcher {
        private final View foundRoot;
        private final View notFoundRoot;
        private final TextView idField;
        private final TextView addressField;
        private ClientCoffeeServer server;
        private final Button connectBtn;

        private ServerItemWatcher(View foundRoot, View notFoundRoot,
                                  TextView idField, TextView addressField,
                                  Button connectBtn) {

            this.foundRoot = foundRoot;
            this.notFoundRoot = notFoundRoot;
            this.idField = idField;
            this.addressField = addressField;
            this.connectBtn = connectBtn;
            connectBtn.setOnClickListener(this::onClick);
        }

        private void onClick(View view) {
            Intent intent = new Intent(context, ServerActivity.class);
            intent.putExtra(AbstractServerActivity.EXTRA_SERVER_ID, server.getId());
            context.startActivity(intent);
        }
    }
}
