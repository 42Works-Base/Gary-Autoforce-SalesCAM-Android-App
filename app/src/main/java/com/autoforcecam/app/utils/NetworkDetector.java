package com.autoforcecam.app.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.widget.Toast;

public class NetworkDetector {

    Context context;
    int REQUEST_INTERNET = 198;

    public NetworkDetector(Context context) {
        this.context = context;
    }

    public final boolean isInternetAvailable() {

        // get Connectivity Manager object to check connection
        ConnectivityManager connec = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            // Check for network connections
            if (connec.getActiveNetworkInfo().getState() == NetworkInfo.State.CONNECTED ||
                    connec.getActiveNetworkInfo().getState() == NetworkInfo.State.CONNECTING) {

                return true;

            } else if (
                    connec.getActiveNetworkInfo().getState() == NetworkInfo.State.DISCONNECTED ||
                            connec.getActiveNetworkInfo().getState() == NetworkInfo.State.DISCONNECTED) {

                return false;
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return false;
    }

    public final boolean isWifiConnected() {

        // get Connectivity Manager object to check connection
        ConnectivityManager connec = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            NetworkInfo myWifi = connec.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (myWifi.isConnected()){
                return true;
            } else {
                return false;
            }

        }catch (Exception e){
            return false;
        }
    }

    public void askUserToTurnONNetwork(){

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);

        // Setting Dialog Title
        alertDialog.setTitle("Internet settings");

        // Setting Dialog Message
        alertDialog.setMessage("Internet is not enabled. Do you want to go to settings menu?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                if (context instanceof Activity) {
                    ((Activity) context).startActivityForResult(intent, REQUEST_INTERNET);
                }

            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                Toast.makeText(context, "Internet not enabled, user cancelled.", Toast.LENGTH_LONG).show();
            }
        });

        alertDialog.show();

    }

}
