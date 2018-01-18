package com.quew8.netcaff.ble;

import android.content.Context;

import com.quew8.netcaff.ClientCoffeeServer;
import com.quew8.netcaff.SavedServers;
import com.quew8.netcaff.lib.server.CoffeeServerId;

import java.util.ArrayList;

/**
 * @author Quew8
 */
public class CoffeeServerList {
    private final Context context;
    private final ArrayList<ClientCoffeeServer> servers = new ArrayList<>();

    CoffeeServerList(Context context) {
        this.context = context;
        CoffeeServerId[] rememberedServerIDs = SavedServers.getSavedServerIDs(context);
        for(CoffeeServerId serverID: rememberedServerIDs) {
            servers.add(new ClientCoffeeServer(serverID));
        }
    }

    void startingScan() {
        for(int i = 0; i < getCountServers(); i++) {
            getServerByIndex(i).setDevice(null);
        }
    }

    private int getIndexOfServer(CoffeeServerId id) {
        for(int i = 0; i < getCountServers(); i++) {
            if(getServerByIndex(i).getAdData().getServerId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    public boolean isServerInList(CoffeeServerId id) {
        return getIndexOfServer(id) >= 0;
    }

    public ClientCoffeeServer getServer(CoffeeServerId id) {
        ClientCoffeeServer server = getServerOrNull(id);
        if(server == null) {
            throw new IllegalArgumentException("No such server");
        }
        return server;
    }

    public ClientCoffeeServer getServerOrNull(CoffeeServerId id) {
        int i = getIndexOfServer(id);
        if(i >= 0) {
            return getServerByIndex(i);
        } else {
            return null;
        }
    }

    public ClientCoffeeServer getServerByIndex(int index) {
        return servers.get(index);
    }

    public int getCountServers() {
        return servers.size();
    }

    ClientCoffeeServer addServer(CoffeeServerId id) {
        ClientCoffeeServer server = new ClientCoffeeServer(id);
        servers.add(new ClientCoffeeServer(id));
        SavedServers.getSavedServerIDs(context, id);
        return server;
    }
}
