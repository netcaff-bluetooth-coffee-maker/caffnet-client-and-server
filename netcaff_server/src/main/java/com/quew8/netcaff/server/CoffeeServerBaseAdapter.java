package com.quew8.netcaff.server;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * @author Quew8
 */
abstract class CoffeeServerBaseAdapter<T> extends BaseAdapter {
    private static final int VIEW_TYPE_EMPTY = 0,
            VIEW_TYPE_ITEM = 1;
    private final Activity activity;
    private final int layoutId;
    private final int emptyLayoutId;
    private ServerCoffeeServer coffeeServer;

    CoffeeServerBaseAdapter(@NonNull Activity activity, int layoutId, int emptyLayoutId) {
        this.activity = activity;
        this.layoutId = layoutId;
        this.emptyLayoutId = emptyLayoutId;
        this.coffeeServer = null;
    }

    void setCoffeeServer(ServerCoffeeServer coffeeServer) {
        if(this.coffeeServer != null) {
            unlistenTo(this.coffeeServer);
        }
        this.coffeeServer = coffeeServer;
        if(this.coffeeServer != null) {
            listenTo(this.coffeeServer);
        }
        mod();
    }

    void mod() {
        this.activity.runOnUiThread(this::notifyDataSetChanged);
    }

    @Override
    public int getCount() {
        if(coffeeServer != null) {
            int n = getCount(coffeeServer);
            if(n == 0) {
                return 1;
            } else {
                return n;
            }
        } else {
            return 0;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if(coffeeServer != null) {
            if(getCount(coffeeServer) == 0) {
                return VIEW_TYPE_EMPTY;
            } else {
                return VIEW_TYPE_ITEM;
            }
        } else {
            return VIEW_TYPE_ITEM;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public T getItem(int position) {
        if(coffeeServer != null) {
            if(getCount(coffeeServer) == 0) {
                return null;
            } else {
                return getItem(coffeeServer, position);
            }
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
        if(type == VIEW_TYPE_EMPTY) {
            if(convertView == null) {
                convertView = LayoutInflater.from(activity).inflate(emptyLayoutId, container, false);
            }
            return convertView;
        } else {
            IWatcher<T> w;
            if(convertView == null) {
                convertView = LayoutInflater.from(activity).inflate(layoutId, container, false);
                w = getWatcher(convertView);
                convertView.setTag(w);
            } else {
                w = castToWatcher(convertView.getTag());
            }
            T data = getItem(position);
            w.update(coffeeServer, data);
            return convertView;
        }
    }

    void runOnUiThread(Runnable r) {
        activity.runOnUiThread(r);
    }

    abstract int getCount(ServerCoffeeServer server);
    abstract T getItem(ServerCoffeeServer server, int position);
    abstract void listenTo(ServerCoffeeServer server);
    abstract void unlistenTo(ServerCoffeeServer server);
    abstract IWatcher<T> getWatcher(View v);
    abstract IWatcher<T> castToWatcher(Object o);

    interface IWatcher<T> {
        void update(ServerCoffeeServer server, T data);
    }
}
