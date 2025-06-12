package com.autoforcecam.app.activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.autoforcecam.app.R;
import com.autoforcecam.app.base.BaseActivity;
import com.autoforcecam.app.interfaces.NetWatcherListener;
import com.autoforcecam.app.models.UploadModel;
import com.autoforcecam.app.utils.MediaWorker;
import com.autoforcecam.app.utils.NetWatcher;
import com.autoforcecam.app.utils.S3UploadService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import org.jsoup.Jsoup;

/* Created by JSP@nesar */

public class SplashActivity extends BaseActivity implements NetWatcherListener {

    private boolean isRunning = false;
    private boolean isUpdated = true;
    private Dialog dialog;
    private ImageView img_logo;
    private NetWatcher netwatcher;
    private Handler timerHandlerService;
    private Runnable timerRunnableService;
    private RelativeLayout rl_height, rl_height2;

    @Override
    protected int getLayoutView() {
        return R.layout.act_splash;
    }

    @Override
    protected Context getActivityContext() {
        return SplashActivity.this;
    }

    @Override
    protected void initView() {

        img_logo = findViewById(R.id.img_logo);
        rl_height = findViewById(R.id.rl_height);
        rl_height2 = findViewById(R.id.rl_height2);


    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        isRunning = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isRunning = false;
    }

    @Override
    protected void initData() {

        String token = getSessionManager().getFCMToken();

        getNativeLibraryDir(this);
        netwatcher = new NetWatcher(this);
        registerReceiver(netwatcher, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

    }

    @Override
    protected void initListener() {

        img_logo.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                img_logo.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                getSessionManager().setScreenWidth(img_logo.getWidth());
                getSessionManager().setScreenHeight(img_logo.getHeight());
            }
        });

        rl_height.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rl_height.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                // For Landscape camera view Use
                getSessionManager().setWidth(rl_height.getHeight());
            }
        });

        rl_height2.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rl_height2.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                // For Landscape camera view Use
                getSessionManager().setWidth2(rl_height2.getHeight());
            }
        });
    }

    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {

        if(isConnected){

            try {

                UploadModel uploadModel = getSessionManager().getUploadData();
                if(null==uploadModel.getTimeStamp()){
                    // Do Nothing
                } else {
                    getSessionManager().clearUploadCount();
                    Log.e("S3Bucket", "Service >>> Starting (Splash)");
                    startServiceWithDelay();
                }

            } catch (Exception e){}

//            if(isGooglePlayServicesAvailable(SplashActivity.this)){
//                if(isInternetAvailable()){
//                    new CheckUpdateAsyncTask().execute();
//                }
//            }else {
//                showToast(getString(R.string.error_play_services));
//            }

            startDelayHandler();

        } else {

            showToastLong(getString(R.string.api_error_internet));

        }

    }

    private void startServiceWithDelay() {

        if (timerHandlerService != null) {
            timerHandlerService.removeCallbacks(timerRunnableService);
        }

        timerHandlerService = new Handler();
        timerRunnableService = new Runnable() {
            @Override
            public void run() {

                try {

                    UploadModel uploadModel = getSessionManager().getUploadData();
                    if(null!=uploadModel.getTimeStamp()){
                        if(isInternetAvailable()) {
                            Log.e("S3Bucket", "S3 Upload Service >>> Started (Splash)");
//                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                                context.startForegroundService(new Intent(context, S3UploadService.class));
//                            } else {
//                                context.startService(new Intent(context, S3UploadService.class));
//                            }

                            Data data = new Data.Builder()
                                    .build();

                            Constraints.Builder constraints = new Constraints.Builder()
                                    .setRequiredNetworkType(NetworkType.CONNECTED);

                            OneTimeWorkRequest oneTimeRequest = new OneTimeWorkRequest.Builder(MediaWorker.class)
                                    .setInputData(data)
                                    .setConstraints(constraints.build())
                                    .addTag("media")
                                    .build();


                            WorkManager.getInstance(SplashActivity.this)
                                    .enqueueUniqueWork("AndroidVille", ExistingWorkPolicy.KEEP, oneTimeRequest);
                        } else {
                            showToast(getString(R.string.api_error_internet));
                        }
                    }

                } catch (Exception e){}

                timerHandlerService.removeCallbacks(timerRunnableService);
            }
        };

        timerHandlerService.postDelayed(timerRunnableService, 1000);

    }

    @Override
    protected void onDestroy() {
        try { unregisterReceiver(netwatcher); } catch (Exception e) { }
        super.onDestroy();
    }

    private void startDelayHandler(){

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                // Check whether activity is terminated or not
                if (isRunning) {

                    if (isUpdated) {

                        if (getSessionManager().isUserLoggedIn()) {

                            getSessionManager().setIncrementCounter(false);
                            int counter = getSessionManager().getCounterValue();
                            if(counter<5){
                                counter++;
                                getSessionManager().setCounterValue(counter);
                            }

                            startActivity(new Intent(context, HomeActivity.class)
                                    .putExtra("showOverlayProgress", true)
                            );
                            overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);

                        } else {
                            getSessionManager().setIncrementCounter(true);
                            startActivity(new Intent(context, LoginActivity.class));
                            overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                        }

                        finish();
                    }
                }

            }
        }, 3000);

    }

    private boolean isGooglePlayServicesAvailable(Activity activity) {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(activity);
        if(status != ConnectionResult.SUCCESS) {
            if(googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(activity, status, 2404).show();
            }
            return false;
        }
        return true;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    private class CheckUpdateAsyncTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {

            String newVersion = "0.0";

            try {
                newVersion = Jsoup.connect("https://play.google.com/store/apps/details?id=" + getApplicationContext().getPackageName() + "&hl=en")
                        .timeout(30000)
                        .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                        .referrer("http://www.google.com")
                        .get()
                        .select(".hAyfc .htlgb")
                        .get(7)
                        .ownText();
                return newVersion;
            } catch (Exception e) {
                return newVersion;
            }

        }


        @Override
        protected void onPostExecute(String newVersion) {
            matchUpdate(newVersion);
        }

        @Override
        protected void onPreExecute() {     }

        @Override
        protected void onProgressUpdate(String... text) {     }

    }

    public void matchUpdate(String newVersion){

        try {

            if(newVersion.equalsIgnoreCase("0.0")){
                Log.e("updated version code", " error in getting version ");
                isUpdated = true;
            }else {
                PackageInfo packageInfo = null;
                try {
                    packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                int version_code = packageInfo.versionCode;
                String version_name = packageInfo.versionName;
                Log.e("updated version code", String.valueOf(version_code) + "  " + version_name);
                if (Float.parseFloat(version_name) < Float.parseFloat(newVersion)) {
                    isUpdated = false;
                }else{
                    isUpdated = true;
                }

            }

        } catch (Exception e) {
            isUpdated = true;
            Log.e("updated version code", " error in getting version ");
        }

        if(isUpdated) {

            // App is already updated

        }else {

            showUpdatePopup();

        }


    }

    private void showUpdatePopup(){

        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_updateapp);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button btn_update = dialog.findViewById(R.id.btn_update);
        btn_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=" + getApplicationContext().getPackageName()));
                startActivity(intent);
                dialog.dismiss();
                finish();
            }
        });

        dialog.show();

    }

    private static String getNativeLibraryDir(Context context) {
        ApplicationInfo appInfo = context.getApplicationInfo();
        Log.e("getNativeLibraryDir", "getNativeLibraryDir: "+appInfo.nativeLibraryDir );
        return appInfo.nativeLibraryDir;
    }
}
