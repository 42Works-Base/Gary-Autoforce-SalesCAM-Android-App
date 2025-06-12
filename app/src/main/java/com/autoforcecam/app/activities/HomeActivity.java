package com.autoforcecam.app.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.View;
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
import com.autoforcecam.app.interfaces.MediaInterface;
import com.autoforcecam.app.interfaces.NetWatcherListener;
import com.autoforcecam.app.interfaces.PermissionsInterface;
import com.autoforcecam.app.models.UploadModel;
import com.autoforcecam.app.presenters.MediaPresenter;
import com.autoforcecam.app.responses.OverlayResponse;
import com.autoforcecam.app.responses.Overlays;
import com.autoforcecam.app.utils.MediaWorker;
import com.autoforcecam.app.utils.NetWatcher;
import com.autoforcecam.app.utils.S3UploadService;
import com.hbisoft.pickit.PickiT;
import com.hbisoft.pickit.PickiTCallbacks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

/* Created by JSP@nesar */

public class HomeActivity extends BaseActivity implements NetWatcherListener, MediaInterface, PickiTCallbacks ,
        PermissionsInterface{

    private ImageView imgPhoto, imgVideo, imgShare;
    private RelativeLayout relativeRating;

    private NetWatcher netwatcher;
    private MediaPresenter mediaPresenter;

    private Handler timerHandlerService;
    private Runnable timerRunnableService;

    private BroadcastReceiver broadcast_reciever;
    private IntentFilter intentFilter;
    private boolean showOverlayProgress = false;

    private PickiT pickiT;

    private PermissionsInterface permissionsInterface;

    Boolean askPermissionsAgain = true;
    private Collection<String> permissions = Arrays.asList(
            Manifest.permission.POST_NOTIFICATIONS);

    @Override
    protected int getLayoutView() {
        return R.layout.act_home;
    }

    @Override
    protected Context getActivityContext() {
        return HomeActivity.this;
    }

    @Override
    protected void initView() {

        imgPhoto = findViewById(R.id.imgPhoto);
        imgVideo = findViewById(R.id.imgVideo);
        imgShare = findViewById(R.id.imgShare);
        relativeRating = findViewById(R.id.relativeRating);

    }

    @Override
    protected void initData() {

        permissionsInterface = this;

        showOverlayProgress = getIntent().getBooleanExtra("showOverlayProgress", false);

        netwatcher = new NetWatcher(this);
        registerReceiver(netwatcher, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        mediaPresenter = new MediaPresenter(getAPIInterface());
        mediaPresenter.attachView(this);

        initBroadcastReceiver();

        if(getSessionManager().getCounterValue() == 5){
            relativeRating.setVisibility(View.VISIBLE);
        } else {
            relativeRating.setVisibility(View.GONE);
        }

        pickiT = new PickiT(this, this, this);

    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        pickiT.deleteTemporaryFile(this);


        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion > 32){
            if(askPermissionsAgain) {
                try {
                    checkPermissions();
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void checkPermissions() throws ExecutionException, InterruptedException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            askPermissions(permissions, permissionsInterface);
        }

    }

    @Override
    protected void initListener() {

        findViewById(R.id.btnRatingYes).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getSessionManager().setCounterValue(7);
                relativeRating.setVisibility(View.GONE);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=" + getApplicationContext().getPackageName()));
                startActivity(intent);
            }
        });

        findViewById(R.id.btnRatingNo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getSessionManager().setCounterValue(7);
                relativeRating.setVisibility(View.GONE);
            }
        });

        imgPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(context, ImageCaptureActivity.class));
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                finish();
            }
        });

        imgVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(context, VideoCaptureActivity.class));
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                finish();
            }
        });

        imgShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(context, ShareActivity.class));
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            }
        });

    }



    private void initBroadcastReceiver(){

        intentFilter = new IntentFilter();
        intentFilter.addAction("refreshVideoUpload");
        intentFilter.addAction("userBlocked");

        if (broadcast_reciever == null) {

            broadcast_reciever = new BroadcastReceiver() {

                @Override
                public void onReceive(Context arg0, Intent intent) {

                    String action = intent.getAction();

                    if (action.equals("refreshVideoUpload")) {
                        startServiceWithDelay();
                    }

                    if (action.equals("userBlocked")) {
                        String message = intent.getStringExtra("message");
                        onUserBlocked(message);

                    }
                }

            };

        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.registerReceiver(broadcast_reciever, intentFilter, Context.RECEIVER_EXPORTED);
        }

    }

    @Override
    public void onSuccess_GetOverlays(OverlayResponse overlayResponse) {

        hideProgressDialog();

        ArrayList<Overlays> imageOverlayList = new ArrayList<>();
        ArrayList<Overlays> videoOverlayList = new ArrayList<>();

        try {

            Overlays[] overlaysImage = overlayResponse.getData().getPhoto_overlays();
            for (int i = 0; i < overlaysImage.length; i++) {
                imageOverlayList.add(overlaysImage[i]);
            }

            Overlays[] overlaysVideo = overlayResponse.getData().getVideo_overlays();
            for (int i = 0; i < overlaysVideo.length; i++) {
                videoOverlayList.add(overlaysVideo[i]);
            }

        } catch (Exception e) {

            showToast(getString(R.string.api_error));
            finish();

        }

        getSessionManager().setImageOverlayList(imageOverlayList);
        getSessionManager().setVideoOverlayList(videoOverlayList);
        getSessionManager().setPreformattedMessage(overlayResponse.getData().getSettings());
        getSessionManager().setIntros(overlayResponse.getData().getIntros());
        getSessionManager().setOutros(overlayResponse.getData().getOutros());
        getSessionManager().setBgMusic(overlayResponse.getData().getBg_music());
        getSessionManager().setVideoType(overlayResponse.getData().getVideoType());

    }

    public void onUserBlocked(String message) {
        hideProgressDialog();
        showToast(message);
        getSessionManager().clearAllData();
        startActivity(new Intent(context, LoginActivity.class));
        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
        finishAffinity();
    }

    @Override
    public void onError(String message) {
        hideProgressDialog();
        showToast(message);
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
                        if(uploadModel.getUploadError().equalsIgnoreCase("1")){
                            if(isInternetAvailable()) {
                                Log.e("S3Bucket", "S3 Upload Service >>> Started");
//                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                                    context.startForegroundService(new Intent(context, S3UploadService.class));
//                                } else {
//                                    context.startService(new Intent(context, S3UploadService.class));
//                                }

                                Data data = new Data.Builder()
                                        .build();

                                Constraints.Builder constraints = new Constraints.Builder()
                                        .setRequiredNetworkType(NetworkType.CONNECTED);

                                OneTimeWorkRequest oneTimeRequest = new OneTimeWorkRequest.Builder(MediaWorker.class)
                                        .setInputData(data)
                                        .setConstraints(constraints.build())
                                        .addTag("media")
                                        .build();


                                WorkManager.getInstance(HomeActivity.this)
                                        .enqueueUniqueWork("AndroidVille", ExistingWorkPolicy.KEEP, oneTimeRequest);


                            } else {
                                showToast(getString(R.string.api_error_internet));
                            }
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
        try { unregisterReceiver(broadcast_reciever); } catch (Exception e) { }
        try { unregisterReceiver(netwatcher); } catch (Exception e) { }
        try { mediaPresenter.detachView(); } catch (Exception e) { }
        super.onDestroy();
    }

    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {

        if(isConnected){
            startServiceWithDelay();
            if(showOverlayProgress) {
                showProgressDialog("");
                showOverlayProgress = false;
            }
            mediaPresenter.getOverlayData(
                    getSessionManager().getUserLocationId(),
                    getSessionManager().getUserId(),
                    getSessionManager().getUserType()
            );
        }

    }

    @Override
    public void PickiTonUriReturned() {}
    @Override
    public void PickiTonStartListener() {}
    @Override
    public void PickiTonProgressUpdate(int progress) {}
    @Override
    public void PickiTonCompleteListener(String path, boolean wasDriveFile, boolean wasUnknownProvider, boolean wasSuccessful, String Reason) {}
    @Override
    public void PickiTonMultipleCompleteListener(ArrayList<String> paths, boolean wasSuccessful, String Reason) {}


    @Override
    public void onPermissionsDenied(boolean isPermanentalyDenied) {

        askPermissionsAgain = false;

        String message = "";

        if (isPermanentalyDenied) {
            message = getString(R.string.permanentlyDeniedMessage);
        } else {
            if (permissions.size() == 1) {
                message = getString(R.string.permissionRequired);
            } else {
                message = getString(R.string.permissionsRequired);
            }

        }

        openPermissionsScreen(isPermanentalyDenied, message, this, permissions);

    }

    @Override
    public void onPermissionsCancelled(boolean isSettingsOpened)  {

        if(isSettingsOpened){
            askPermissionsAgain = true;
        } else {
            askPermissions(permissions, permissionsInterface);
        }

    }

    @Override
    public void onPermissionsGranted() {

    }

}
