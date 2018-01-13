package com.quew8.netcaff;

import android.content.Context;
import android.content.SharedPreferences;

import com.quew8.netcaff.lib.server.CoffeeServerID;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Quew8
 */
public class SavedServers {
    private static final String TAG = SavedServers.class.getSimpleName();
    private static final String SAVED_SERVERS = "SAVED_SERVERS";

    private SavedServers() {}

    public static CoffeeServerID[] getSavedServerIDs(Context context, CoffeeServerID... addIns) {
        SharedPreferences prefs = context.getSharedPreferences(SavedServers.class.getCanonicalName(), Context.MODE_PRIVATE);
        Set<String> idStrings = prefs.getStringSet(SAVED_SERVERS, null);
        if(idStrings == null) {
            idStrings = new HashSet<>();
        }
        HashSet<String> goodIDs = new HashSet<>();
        boolean goneBad = false;
        ArrayList<CoffeeServerID> serverIds = new ArrayList<>();
        for(String idString: idStrings) {
            CoffeeServerID id = CoffeeServerID.fromPrefString(idString);
            if(id != null) {
                boolean duplicate = false;
                for(int i = 0; i < serverIds.size(); i++) {
                    if(serverIds.get(i).equals(id)) {
                        duplicate = true;
                        break;
                    }
                }
                if(!duplicate) {
                    serverIds.add(id);
                } else {
                    goneBad = true;
                }
            } else {
                goneBad = true;
            }
        }
        if(goneBad || addIns.length > 0) {
            outer:
            for(CoffeeServerID addIn: addIns) {
                for(int i = 0; i < serverIds.size(); i++) {
                    if(serverIds.get(i).equals(addIn)) {
                        continue outer;
                    }
                }
                goodIDs.add(CoffeeServerID.toPrefString(addIn));
            }
            prefs.edit().putStringSet(SAVED_SERVERS, goodIDs).commit();
        }
        return serverIds.toArray(new CoffeeServerID[serverIds.size()]);
    }
}
