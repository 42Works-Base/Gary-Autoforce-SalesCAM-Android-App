package com.autoforcecam.app.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.autoforcecam.app.interfaces.NetWatcherListener;

public class NetWatcher extends BroadcastReceiver {

    private NetWatcherListener netwatcherListener;
    private int  TYPE_WIFI = 1;
    private int TYPE_MOBILE = 2;
    private int TYPE_NOT_CONNECTED = 0;

    public NetWatcher() {}

    public NetWatcher(NetWatcherListener netwatcherListener) {
        this.netwatcherListener = netwatcherListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (netwatcherListener != null) {
            if (isUserOnline(context)) {
                netwatcherListener.onNetworkConnectionChanged(true);
            } else {
                netwatcherListener.onNetworkConnectionChanged(false);
            }
        }
    }



    private final boolean isUserOnline(Context context) {
        boolean status = false;
        int conn = getConnectivityStatus(context);
        if (conn == TYPE_WIFI) {
            status = true;
            Log.e("Netwatcher", "WIFI Enabled.");
        } else if (conn == TYPE_MOBILE) {
            status = true;
            Log.e("Netwatcher", "WIFI Enabled.");
        } else if (conn == TYPE_NOT_CONNECTED) {
            status = false;
            Log.e("Netwatcher", "Not connected to Internet");
        }

        return status;
    }

    private final int getConnectivityStatus(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork != null) {
                if (activeNetwork.getType() == 1) {
                    return TYPE_WIFI;
                }

                if (activeNetwork.getType() == 0) {
                    return TYPE_MOBILE;
                }
            }
        }
        return TYPE_NOT_CONNECTED;
    }

}
