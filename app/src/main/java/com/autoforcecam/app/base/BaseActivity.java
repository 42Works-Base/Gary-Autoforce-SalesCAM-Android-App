package com.autoforcecam.app.base;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.autoforcecam.app.dialog.MyProgressDialog;
import com.autoforcecam.app.interfaces.PermissionsInterface;
import com.autoforcecam.app.retrofit.APIClient;
import com.autoforcecam.app.retrofit.APIInterface;
import com.autoforcecam.app.utils.NetworkDetector;
import com.autoforcecam.app.utils.S3UploadService;
import com.autoforcecam.app.utils.SessionManager;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.Collection;
import java.util.List;

/* Created by JSP@nesar */

public abstract class BaseActivity extends AppCompatActivity {

    public Context context;
    private MyProgressDialog myProgressDialog;
    private int progressDialogCount = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        setContentView(getLayoutView());
        context = getActivityContext();
        initView();
        initData();
        initListener();

    }


    /* Abstract Methods */

    protected abstract int getLayoutView();
    protected abstract Context getActivityContext();
    protected abstract void initView();
    protected abstract void initData();
    protected abstract void initListener();



    /* Permissions */

    public void askPermissions(final Collection<String> permissions, final PermissionsInterface permissionsInterface){

        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= Build.VERSION_CODES.M) {

            Dexter.withContext((Activity)context)
                    .withPermissions(permissions)
                   .withListener(new MultiplePermissionsListener() {

                @Override
                public void onPermissionsChecked(MultiplePermissionsReport report) {

                    boolean grantedStatus = report.areAllPermissionsGranted();
                    boolean permanentlyDenied = report.isAnyPermissionPermanentlyDenied();

                    if(permanentlyDenied){
                        permissionsInterface.onPermissionsDenied(true);
                    }else {
                        if (grantedStatus) {
                            permissionsInterface.onPermissionsGranted();
                        } else {
                            permissionsInterface.onPermissionsDenied(false);
                        }
                    }

                }

                @Override
                public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                    token.continuePermissionRequest();
                }

            }).check();

        }else {

            permissionsInterface.onPermissionsGranted();

        }

    }


    /* Permissions Dialog */

    public void openPermissionsScreen(final boolean isPermanentalyDenied, String message,
                                      final PermissionsInterface permissionsInterface, final Collection<String> permissions){

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setTitle("Information");
        alertDialog.setMessage(message);
        alertDialog.setCancelable(false);

        alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

                dialog.cancel();

                if(isPermanentalyDenied){   // Settings screen

                    permissionsInterface.onPermissionsCancelled(true);

                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivityForResult(intent, 0);

                }else {     // retry permissions
                    askPermissions(permissions, permissionsInterface);
                }

            }
        });

        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                permissionsInterface.onPermissionsCancelled(false);
            }
        });

        alertDialog.show();

    }


    /* Public Methods */

    public SessionManager getSessionManager(){
        return new SessionManager(context);

    }

    public APIInterface getAPIInterface(){
        return APIClient.getClient(context).create(APIInterface.class);
    }

    public APIInterface getUploadAPIInterface(){
        return APIClient.getUploadClient(context).create(APIInterface.class);
    }

    public APIInterface getShotAPIInterface(){
        return APIClient.getShotClient(context).create(APIInterface.class);
    }

    public boolean isInternetAvailable(){
        return new NetworkDetector(context).isInternetAvailable();
    }

    public boolean isWifiConnected(){
        return new NetworkDetector(context).isWifiConnected();
    }

    public void showToast(String message){

        if (message.trim().length() != 0) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }

    }

    public void showToastLong(String message){

        if (message.trim().length() != 0) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }

    }

    public void showProgressDialog(String message){

        if(myProgressDialog==null){
            myProgressDialog = new MyProgressDialog(context);
        }

        if(!myProgressDialog.isDialogVisisble()){
            myProgressDialog.showDialog(message);
        }

        progressDialogCount++;

    }

    public void hideProgressDialog(){

        if(myProgressDialog!=null && myProgressDialog.isDialogVisisble()){
            progressDialogCount--;
            if(progressDialogCount<1) {
                myProgressDialog.hideDialog();
            }
        }

    }

    public void updatePercentProgressDialog(String percent){

        if(myProgressDialog!=null){
            myProgressDialog.updatePercent(percent);
        }

    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)context.getSystemService(context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public String getDeviceID(){
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }




}
