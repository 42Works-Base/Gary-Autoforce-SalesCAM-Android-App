package com.autoforcecam.app.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.autoforcecam.app.CircleRecycler.CircleRecyclerView;
import com.autoforcecam.app.CircleRecycler.CircularHorizontalMode;
import com.autoforcecam.app.CircleRecycler.ItemViewMode;
import com.autoforcecam.app.R;
import com.autoforcecam.app.adapters.OverlayAdapter;
import com.autoforcecam.app.base.BaseActivity;
import com.autoforcecam.app.cropper.CropImage;
import com.autoforcecam.app.dialog.LogoutDialog;
import com.autoforcecam.app.interfaces.NetWatcherListener;
import com.autoforcecam.app.interfaces.PermissionsInterface;
import com.autoforcecam.app.models.UploadModel;
import com.autoforcecam.app.responses.Overlays;
import com.autoforcecam.app.utils.AppUtils;
import com.autoforcecam.app.utils.MediaWorker;
import com.autoforcecam.app.utils.NetWatcher;
import com.autoforcecam.app.utils.NetworkDetector;
import com.autoforcecam.app.utils.PathUtil;
import com.autoforcecam.app.utils.S3UploadService;
import com.google.common.util.concurrent.ListenableFuture;
import com.hbisoft.pickit.PickiT;
import com.hbisoft.pickit.PickiTCallbacks;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/* Created by JSP@nesar */

public class ImageCaptureActivity extends BaseActivity implements PermissionsInterface,
        View.OnClickListener, NetWatcherListener, PickiTCallbacks {

    private ImageCapture imageCapture;
    Camera camera;
    CameraManager manager;

    ListenableFuture<ProcessCameraProvider> cameraProviderListenableFuture;
    private Collection<String> permissions = Arrays.asList(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE);

    private NetWatcher netwatcher;
    private ImageView img_cross;
    private PickiT pickiT;

    private BroadcastReceiver broadcast_reciever;
    private IntentFilter intentFilter;

    private PermissionsInterface permissionsInterface;
    private PreviewView previewView;
    private ImageView img_flash, img_selected_media, img_delete, img_temp;
    private RelativeLayout rl_camera, rl_start, rl_gallery, rl_flash, rl_change_camera,
            rl_selected_media;
    private String picName = "", finalPath = "";
    private CircleRecyclerView recycler_overlay;
    private ItemViewMode mItemViewMode;
    private LinearLayoutManager linearLayoutManager;
    private OverlayAdapter overlayAdapter;
    private ArrayList<Overlays> imageOverlayList = new ArrayList<>();
    private ArrayList<Overlays> selectedOverlayList = new ArrayList<>();
    private int differenceInPosition = 0;
    private boolean isSelectingImage = false, askPermissionsAgain = true;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private Handler timerHandlerService;
    private Runnable timerRunnableService;

    /* Flash */
    private final String FLASH_MODE_ON = "flash_on";
    private final String FLASH_MODE_OFF = "flash_off";
    private String selectedFlashMode = FLASH_MODE_OFF;

    /* Torch */
    private final String TORCH_ON = "torch_on";
    private final String TORCH_OFF = "torch_off";
    private String selectedTorchMode = TORCH_OFF;

    ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                // Callback is invoked after the user selects a media item or closes the
                // photo picker.
                if (uri != null) {
                    Log.d("pickMedia", "Selected URI: " + uri);
                    CropImage.activity(uri)
                            .start(ImageCaptureActivity.this);
                } else {
                    Log.d("pickMedia", "No media selected");
                }
            });


    /* Camera (Front/Back) */
    private final String CAMERA_FRONT = "front";
    private final String CAMERA_BACK = "back";
    private String selectedCameraPosition = CAMERA_BACK;

    /* Instructions Popups */
    private int currentPopupPosition = 1;
    private LinearLayout linearIns1, linearIns2;
    private RelativeLayout relativeIns, relativeIns3, relativeIns4, relativeIns5, relativeIns6,
            relativeExit1, relativeExit2, relativeExit3, relativeExit4, relativeExit5, relativeExit6;
    private Button btnPre2, btnPre3, btnPre4, btnPre5, btnPre6;
    private Button btnNext1, btnNext2, btnNext3, btnNext4, btnNext5, btnNext6;
    private ImageView imgLogo;
    private Handler insTimerHandler;
    private Runnable insTimerRunnable;

    @Override
    protected int getLayoutView() {
        return R.layout.act_image_capture;
    }

    @Override
    protected Context getActivityContext() {
        return ImageCaptureActivity.this;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        if (insTimerHandler != null) {
            insTimerHandler.removeCallbacks(insTimerRunnable);
        }

        savedInstanceState.putInt("currentPopupPosition", currentPopupPosition);
        savedInstanceState.putString("selectedFlashMode", selectedFlashMode);
        savedInstanceState.putString("selectedTorchMode", selectedTorchMode);
        savedInstanceState.putString("selectedCameraPosition", selectedCameraPosition);

    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        currentPopupPosition = savedInstanceState.getInt("currentPopupPosition");
        selectedFlashMode = savedInstanceState.getString("selectedFlashMode");
        selectedTorchMode = savedInstanceState.getString("selectedTorchMode");
        selectedCameraPosition = savedInstanceState.getString("selectedCameraPosition");
        setSavedCameraOptions();

    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion > 32){
            permissions = Arrays.asList(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_MEDIA_IMAGES);

        }else {
            permissions = Arrays.asList(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if(askPermissionsAgain) {
            try {
                checkPermissions();
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            checkAutoRotateStatus();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        if(!isSelectingImage) {
            Log.e("camerastatus", "stop");
           // cameraView.stop();
        }

    }

    private Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    @SuppressLint("RestrictedApi")
    private void startCamera(ProcessCameraProvider cameraProvider, Integer selector) {
        cameraProvider.unbindAll();

        CameraSelector.DEFAULT_BACK_CAMERA.getCameraFilterSet();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(selector)
                .build();


        Preview preview = new Preview.Builder().build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());


        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .build();

        ImageCapture.Builder builder = new ImageCapture.Builder();
        imageCapture = builder
                .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation())
                .build();

        camera = cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture,imageAnalysis, preview);

        manager = (CameraManager)context.getSystemService(CAMERA_SERVICE);
    }


    void captureImage() {

        ContentValues contentValues=new ContentValues();



        picName = "pic_" + System.currentTimeMillis()+".png";
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME,picName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE,"image/png");
        contentValues.put(
                MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + File.separator + "SalesCAM/Pics");




        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/SalesCAM/Pics";
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File photo = new File(dir, picName);

        ImageCapture.OutputFileOptions  outputFileOptions=new ImageCapture.OutputFileOptions.Builder(getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues).build();

        imageCapture.takePicture(outputFileOptions, getExecutor(), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@androidx.annotation.NonNull ImageCapture.OutputFileResults outputFileResults) {

                File file= new File(outputFileResults.getSavedUri().getPath());
                finalPath=photo.getPath();


                Log.e("onImageSaved", "onImageSaved: "+outputFileResults.getSavedUri() );

                Log.e("onImageSaved", "finalPath: "+file );
                Log.e("onImageSaved", "finalPath: "+finalPath );


                String image_url = "", resolution = "", strOrientation = "";

        //        sendPathToCropping(outputFileResults.getSavedUri().getPath());

                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {

                    if(selectedOverlayList.size()!=0){
                        int currentPosition = linearLayoutManager.findFirstVisibleItemPosition();
                        String overlayId = selectedOverlayList.get(currentPosition).getId();
                        getSessionManager().setPicLanOverlayId(selectedOverlayList.get(currentPosition).getId());
                        image_url = selectedOverlayList.get(currentPosition).getImage_url();
                    } else {
                        getSessionManager().setPicLanOverlayId("");
                    }

                    resolution = AppUtils.picLandscapeResolution;
                    strOrientation = "landscape";

                } else {

                    if(selectedOverlayList.size()!=0){
                        int currentPosition = linearLayoutManager.findFirstVisibleItemPosition();
                        String overlayId = selectedOverlayList.get(currentPosition).getId();
                        getSessionManager().setPicPorOverlayId(overlayId);
                        image_url = selectedOverlayList.get(currentPosition).getImage_url();
                    } else {
                        getSessionManager().setPicPorOverlayId("");
                    }

                    resolution = AppUtils.picPortraitResolution;
                    strOrientation = "portrait";

                }

                startActivity(new Intent(context, ImagePreviewActivity.class)
                        .putExtra("differenceInPosition", differenceInPosition)
                        .putExtra("isImageSelected", false)
                        .putExtra("capturedImagePath", photo.getPath())
                        .putExtra("picName", picName)
                        .putExtra("selectedCameraPosition",selectedCameraPosition)
                        .putExtra("logoImageURL", image_url)
                        .putExtra("resolution", resolution)
                        .putExtra("orientation", strOrientation)
                );
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);

            }

            @Override
            public void onError(@androidx.annotation.NonNull ImageCaptureException exception) {

                Log.e("onImageSaved", "onError: "+exception );



            }
        });

    }


    @Override
    protected void initView() {

        previewView = findViewById(R.id.cameraView);
        rl_gallery = findViewById(R.id.rl_gallery);
        img_flash = findViewById(R.id.img_flash);
        rl_flash = findViewById(R.id.rl_flash);
        rl_start = findViewById(R.id.rl_start);
        rl_camera = findViewById(R.id.rl_camera);
        img_delete = findViewById(R.id.img_delete);
        img_cross = findViewById(R.id.img_cross);
        img_temp = findViewById(R.id.img_temp);
        recycler_overlay = findViewById(R.id.recycler_overlay);
        rl_change_camera = findViewById(R.id.rl_change_camera);
        img_selected_media = findViewById(R.id.img_selected_media);
        rl_selected_media = findViewById(R.id.rl_selected_media);
        imgLogo = findViewById(R.id.imgLogo);

        linearIns1 = findViewById(R.id.linearIns1);
        linearIns2 = findViewById(R.id.linearIns2);
        relativeIns3 = findViewById(R.id.relativeIns3);
        relativeIns4 = findViewById(R.id.relativeIns4);
        relativeIns5 = findViewById(R.id.relativeIns5);
        relativeIns6 = findViewById(R.id.relativeIns6);
        relativeIns = findViewById(R.id.relativeIns);
        relativeExit1 = findViewById(R.id.relativeExit1);
        relativeExit2 = findViewById(R.id.relativeExit2);
        relativeExit3 = findViewById(R.id.relativeExit3);
        relativeExit4 = findViewById(R.id.relativeExit4);
        relativeExit5 = findViewById(R.id.relativeExit5);
        relativeExit6 = findViewById(R.id.relativeExit6);
        btnPre2 = findViewById(R.id.btnPre2);
        btnPre3 = findViewById(R.id.btnPre3);
        btnPre4 = findViewById(R.id.btnPre4);
        btnPre5 = findViewById(R.id.btnPre5);
        btnPre6 = findViewById(R.id.btnPre6);
        btnNext1 = findViewById(R.id.btnNext1);
        btnNext2 = findViewById(R.id.btnNext2);
        btnNext3 = findViewById(R.id.btnNext3);
        btnNext4 = findViewById(R.id.btnNext4);
        btnNext5 = findViewById(R.id.btnNext5);
        btnNext6 = findViewById(R.id.btnNext6);

    }

    @Override
    protected void initData() {

        Log.e("Counter", " > " + getSessionManager().getCounterValue());

        pickiT = new PickiT(this, this, this);

        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        permissionsInterface = this;

        netwatcher = new NetWatcher(this);
        registerReceiver(netwatcher, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        initBroadcastReceiver();

        linearLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        recycler_overlay.setLayoutManager(linearLayoutManager);
        mItemViewMode = new CircularHorizontalMode();
        recycler_overlay.setViewMode(mItemViewMode);
        recycler_overlay.setNeedCenterForce(false);

        selectedOverlayList = new ArrayList<>();
        overlayAdapter = new OverlayAdapter(context, selectedOverlayList);
        recycler_overlay.setAdapter(overlayAdapter);

        imageOverlayList = getSessionManager().getImageOverlayList();
        if(null==imageOverlayList){ imageOverlayList = new ArrayList<>(); }

        loadOverlaysToAdapter();

    }

    @Override
    protected void initListener() {

        img_cross.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        imgLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                UploadModel uploadModel = getSessionManager().getUploadData();
                if(null==uploadModel.getTimeStamp()){
                    new LogoutDialog((AppCompatActivity)context).showDialog();
                } else {
                    if(new NetworkDetector(context).isInternetAvailable()){
                        showToastLong("A video is currently being uploaded. Please wait for it to complete in order to log out.");
                    } else {
                        showToastLong("No Internet Connection, kindly connect to the internet in order to log out.");
                    }
                }

            }
        });

        rl_flash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetFlashMode();
            }
        });

        rl_change_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeCameraPosition();
            }
        });

        rl_selected_media.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(context, VideoCaptureActivity.class));
                overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                finish();
            }
        });


        rl_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recycler_overlay.setEnabled(false);
               captureImage();
            }
        });

        rl_gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                isSelectingImage = true;

                selectImage();

            }
        });

        relativeExit1.setOnClickListener(this);
        relativeExit2.setOnClickListener(this);
        relativeExit3.setOnClickListener(this);
        relativeExit4.setOnClickListener(this);
        relativeExit5.setOnClickListener(this);
        relativeExit6.setOnClickListener(this);

        btnPre2.setOnClickListener(this);
        btnPre3.setOnClickListener(this);
        btnPre4.setOnClickListener(this);
        btnPre5.setOnClickListener(this);
        btnPre6.setOnClickListener(this);
        btnNext1.setOnClickListener(this);
        btnNext2.setOnClickListener(this);
        btnNext3.setOnClickListener(this);
        btnNext4.setOnClickListener(this);
        btnNext5.setOnClickListener(this);
        btnNext6.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {

        if(v.getId() == R.id.relativeExit1 || v.getId() == R.id.relativeExit2 ||
           v.getId() == R.id.relativeExit3 || v.getId() == R.id.relativeExit4 ||
           v.getId() == R.id.relativeExit5 || v.getId() == R.id.relativeExit6)
        {
            getSessionManager().setImageInstructionsStatus(false);
            currentPopupPosition = 1;
            AppUtils.isImageInstructionsShown = true;
            relativeIns.setVisibility(View.GONE);
            linearIns1.setVisibility(View.GONE);
            linearIns2.setVisibility(View.GONE);
            relativeIns3.setVisibility(View.GONE);
            relativeIns4.setVisibility(View.GONE);
            relativeIns5.setVisibility(View.GONE);
            relativeIns6.setVisibility(View.GONE);
        }

        if(v.getId() == R.id.btnPre2 || v.getId() == R.id.btnPre3 ||
                v.getId() == R.id.btnPre4 || v.getId() == R.id.btnPre5 ||
                v.getId() == R.id.btnPre6)
        {
            currentPopupPosition--;
            checkIntructionsPopupStatus();
        }

        if(v.getId() == R.id.btnNext1 || v.getId() == R.id.btnNext2 ||
                v.getId() == R.id.btnNext3 || v.getId() == R.id.btnNext4 ||
                v.getId() == R.id.btnNext5 || v.getId() == R.id.btnNext6)
        {
            currentPopupPosition++;
            checkIntructionsPopupStatus();
        }

    }

    @Override
    protected void onDestroy() {
        try { unregisterReceiver(broadcast_reciever); } catch (Exception e) { }
        try { unregisterReceiver(netwatcher); } catch (Exception e) { }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(context, HomeActivity.class)
                .putExtra("showOverlayProgress", false)
        );
        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
        finish();
    }

    private void setCameraSizeAccordingToResolution(){

        try {

            String strResolution = selectedOverlayList.get(0).getRatio();
            String strWidth = strResolution.split("x")[0];
            String strHeight = strResolution.split("x")[1];
            int width1 = Integer.parseInt(strWidth);
            int height1 = Integer.parseInt(strHeight);

            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {

                int height = getSessionManager().getScreenWidth();
                int width = (height/3)*4;

                int width2 = getSessionManager().getScreenHeight() - getSessionManager().getWidth();

                differenceInPosition = 0;

                if(width2>width){
                    differenceInPosition = (width2 - width)/2;
                }

                rl_camera.getLayoutParams().width = width;
                rl_camera.getLayoutParams().height = height;

            } else {

                int screenWidth = getSessionManager().getScreenWidth();
                rl_camera.getLayoutParams().width = screenWidth;
                rl_camera.getLayoutParams().height = screenWidth;

            }

        } catch (Exception e){
            showToast(getString(R.string.api_error));
        }

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

    private void checkIntructionsPopupStatus(){

        if(getSessionManager().showImageInstructions()){

            if(!AppUtils.isImageInstructionsShown){

                relativeIns.setVisibility(View.VISIBLE);

                linearIns1.setVisibility(View.GONE);
                linearIns2.setVisibility(View.GONE);
                relativeIns3.setVisibility(View.GONE);
                relativeIns4.setVisibility(View.GONE);
                relativeIns5.setVisibility(View.GONE);
                relativeIns6.setVisibility(View.GONE);

                if(currentPopupPosition == 1) { linearIns1.setVisibility(View.VISIBLE); }
                if(currentPopupPosition == 2) { linearIns2.setVisibility(View.VISIBLE); }
                if(currentPopupPosition == 3) { relativeIns3.setVisibility(View.VISIBLE); }
                if(currentPopupPosition == 4) { relativeIns4.setVisibility(View.VISIBLE); }
                if(currentPopupPosition == 5) { relativeIns5.setVisibility(View.VISIBLE); }
                if(currentPopupPosition == 6) { relativeIns6.setVisibility(View.VISIBLE); }
                if(currentPopupPosition == 7) {
                    currentPopupPosition = 1;
                    AppUtils.isImageInstructionsShown = true;
                    relativeIns.setVisibility(View.GONE);
                }

                if(currentPopupPosition != 7) {
//                    initInsTimer();
                }

            }

        }

    }

    private void initInsTimer(){

        if (insTimerHandler != null) {
            insTimerHandler.removeCallbacks(insTimerRunnable);
        }

        insTimerHandler = new Handler();
        insTimerRunnable = new Runnable() {
            @Override
            public void run() {
                currentPopupPosition++;
                checkIntructionsPopupStatus();
            }
        };

        if(currentPopupPosition == 1){
            insTimerHandler.postDelayed(insTimerRunnable, 3500);
        } else {
            insTimerHandler.postDelayed(insTimerRunnable, 3000);
        }

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
    public void onNetworkConnectionChanged(boolean isConnected) {

        if(isConnected){
            startServiceWithDelay();
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


                                WorkManager.getInstance(ImageCaptureActivity.this)
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

    private void loadOverlaysToAdapter() {

        selectedOverlayList = new ArrayList<>();
        int orientation = getResources().getConfiguration().orientation;

        int position = 0;

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {

            String defaultOverlayId = getSessionManager().getPicLanOverlayId();

            for (int i = 0; i < imageOverlayList.size(); i++) {
                if (imageOverlayList.get(i).getOrientation().equalsIgnoreCase("landscape")) {
                    selectedOverlayList.add(imageOverlayList.get(i));

                }
            }


        } else {

            String defaultOverlayId = getSessionManager().getPicPorOverlayId();

            for (int i = 0; i < imageOverlayList.size(); i++) {
                if (imageOverlayList.get(i).getOrientation().equalsIgnoreCase("portrait")) {
                    selectedOverlayList.add(imageOverlayList.get(i));
                }
            }



        }

        overlayAdapter.updateList(selectedOverlayList);

//        if(selectedOverlayList.size()!=0) {
//
//            setCameraSizeAccordingToResolution();
//
//            int finalPosition = position;
//
//            Handler handler1 = new Handler();
//            handler1.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    recycler_overlay.scrollToPosition(finalPosition - 1);
//                }
//            }, 1500);
//
//            Handler handler2 = new Handler();
//            handler2.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    recycler_overlay.setNeedCenterForce(true);
//                }
//            }, 2000);
//
//        }






        for(int i=0;i<selectedOverlayList.size();i++){
            Log.e("loadOverlaysToAdapter", "selectedOverlayList: "+selectedOverlayList.get(i).getId() );

            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                Log.e("loadOverlaysToAdapter", "getPicLanOverlayId: "+getSessionManager().getPicLanOverlayId() );
                if (!getSessionManager().getPicLanOverlayId().isEmpty()){


                    if(selectedOverlayList.get(i).getId().equals(getSessionManager().getPicLanOverlayId())){
                        Log.e("loadOverlaysToAdapter", "getPicLanOverlayId: "+getSessionManager().getPicLanOverlayId()+"  "+selectedOverlayList.get(i).getId() );
                        recycler_overlay.scrollToPosition(i);
                    }
                }
            }else {
                if (!getSessionManager().getPicPorOverlayId().isEmpty()){
                    Log.e("loadOverlaysToAdapter", "getPicPorOverlayId: "+getSessionManager().getPicPorOverlayId() );

                    if(selectedOverlayList.get(i).getId().equals(getSessionManager().getPicPorOverlayId())){
                        Log.e("loadOverlaysToAdapter", "getPicPorOverlayId: "+getSessionManager().getPicPorOverlayId()+"  "+selectedOverlayList.get(i).getId() );
                        recycler_overlay.scrollToPosition(i);
                    }
                }
            }


        }
    }

    // TODO Media Save Methods

    private class MyAsyncTask extends AsyncTask<String, String, String> {

        private Bitmap bitmapImage;
        private byte[] source;

        public MyAsyncTask(Bitmap bitmapImage, byte[] source) {
            this.bitmapImage = bitmapImage;
            this.source = source;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog("");
        }

        @Override
        protected String doInBackground(String... strings) {
            savePicture(bitmapImage, source);
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            hideProgressDialog();



        }
    }

    private void savePicture(Bitmap bitmapImage, byte[] source) {

        byte[] png;

        if(null!=source){
            png = source;
        } else {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
            png = stream.toByteArray();
        }

        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/SalesCAM/Pics";
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        picName = "pic_" + AppUtils.getTimeStamp() + ".png";

        File photo = new File(dir, picName);

        if (photo.exists()) {
            photo.delete();
        }

        try {

            FileOutputStream fos = new FileOutputStream(photo.getPath());
            fos.write(png);
            fos.close();

            finalPath = photo.getPath();

        } catch (java.io.IOException e) {
        }

    }

    // TODO Permissions Methods

    private void checkPermissions() throws ExecutionException, InterruptedException {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            askPermissions(permissions, permissionsInterface);
        } else {
            if(!isSelectingImage) {
                Log.e("camerastatus", "start");
                cameraProviderListenableFuture=ProcessCameraProvider.getInstance(ImageCaptureActivity.this);
                cameraProviderListenableFuture.addListener(() ->{
                    try {
                        ProcessCameraProvider cameraProvider= cameraProviderListenableFuture.get();
                        // isFront=false;
                        startCamera(cameraProvider,CameraSelector.LENS_FACING_BACK);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                },getExecutor());

                checkIntructionsPopupStatus();
            }
        }

    }

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
    public void onPermissionsCancelled(boolean isSettingsOpened) {

        if(isSettingsOpened){
            askPermissionsAgain = true;
        } else {
            askPermissions(permissions, permissionsInterface);
        }

    }

    @Override
    public void onPermissionsGranted()  {

        if(!isSelectingImage) {
            Log.e("camerastatus", "start");

            cameraProviderListenableFuture=ProcessCameraProvider.getInstance(ImageCaptureActivity.this);
            cameraProviderListenableFuture.addListener(() ->{
                try {
                    ProcessCameraProvider cameraProvider= cameraProviderListenableFuture.get();
                    // isFront=false;
                    startCamera(cameraProvider,CameraSelector.LENS_FACING_BACK);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            },getExecutor());

            checkIntructionsPopupStatus();
        }

    }

    // TODO Camera Methods

    private void setSavedCameraOptions() {

        // Camera Position

        if (selectedCameraPosition == CAMERA_FRONT) {
            selectedCameraPosition = CAMERA_BACK;
            cameraProviderListenableFuture=ProcessCameraProvider.getInstance(ImageCaptureActivity.this);
            cameraProviderListenableFuture.addListener(() ->{
                try {
                    ProcessCameraProvider cameraProvider= cameraProviderListenableFuture.get();
                    //  isFront=false;
                    startCamera(cameraProvider,CameraSelector.LENS_FACING_BACK);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            },getExecutor());
          //  cameraView.setFacing(CameraKit.Constants.FACING_FRONT);
        } else {
            selectedCameraPosition = CAMERA_FRONT;
            cameraProviderListenableFuture=ProcessCameraProvider.getInstance(ImageCaptureActivity.this);
            cameraProviderListenableFuture.addListener(() ->{
                try {
                    ProcessCameraProvider cameraProvider= cameraProviderListenableFuture.get();
                    //  isFront=false;
                    startCamera(cameraProvider,CameraSelector.LENS_FACING_FRONT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            },getExecutor());
        }

        // Flash Mode



        // Torch Mode


    }

    private void resetFlashMode() {

        switch (selectedFlashMode) {

            case FLASH_MODE_ON:
                selectedFlashMode = FLASH_MODE_OFF;
                img_flash.setImageResource(R.drawable.ic_flash_off);
                camera.getCameraControl().enableTorch(false);
                break;

            case FLASH_MODE_OFF:
                selectedFlashMode = FLASH_MODE_ON;
                img_flash.setImageResource(R.drawable.ic_flash_on);
                camera.getCameraControl().enableTorch(true);
                break;

        }

    }

    private void resetTorchMode() {

        switch (selectedTorchMode) {

            case TORCH_ON:
                selectedTorchMode = TORCH_OFF;
                img_flash.setImageResource(R.drawable.ic_flash_off);
                camera.getCameraControl().enableTorch(false);
                break;

            case TORCH_OFF:
                selectedTorchMode = TORCH_ON;
                img_flash.setImageResource(R.drawable.ic_flash_on);
                camera.getCameraControl().enableTorch(true);
                break;

        }

    }

    private void changeCameraPosition() {

        switch (selectedCameraPosition) {

            case CAMERA_FRONT:
            //    cameraView.setFacing(CameraKit.Constants.FACING_BACK);
                selectedCameraPosition = CAMERA_BACK;
                cameraProviderListenableFuture=ProcessCameraProvider.getInstance(ImageCaptureActivity.this);
                cameraProviderListenableFuture.addListener(() ->{
                    try {
                        ProcessCameraProvider cameraProvider= cameraProviderListenableFuture.get();
                        //  isFront=false;
                        startCamera(cameraProvider,CameraSelector.LENS_FACING_BACK);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                },getExecutor());
                break;

            case CAMERA_BACK:
                selectedCameraPosition = CAMERA_FRONT;
              //  cameraView.setFacing(CameraKit.Constants.FACING_FRONT);
                cameraProviderListenableFuture=ProcessCameraProvider.getInstance(ImageCaptureActivity.this);
                cameraProviderListenableFuture.addListener(() ->{
                    try {
                        ProcessCameraProvider cameraProvider= cameraProviderListenableFuture.get();
                        //  isFront=false;
                        startCamera(cameraProvider,CameraSelector.LENS_FACING_FRONT);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                },getExecutor());
                break;

        }

    }

    // TODO Image Picker Methods

    private void selectImage(){
        String[] mimeTypes = {"image/jpeg", "image/jpg", "image/png"};

        Intent intent =new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);


        pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());

//        Intent intent;
//
//        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
//            intent = new Intent(Intent.ACTION_GET_CONTENT);
//        } else {
//            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//        }
//
//        intent.setType("image/*");
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
//        startActivityForResult(Intent.createChooser(intent, "ChooseFile"), 111);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK) {

            if (requestCode == 111) {

                Uri uri = data.getData();
                Log.e("loadBitmaps", "uri: "+uri );

                CropImage.activity(uri)
                        .start(ImageCaptureActivity.this);

//                if (AppUtils.isOneDrive(uri) || AppUtils.isDropBox(uri) || AppUtils.isGoogleDrive(uri)) {
//                    showProgressDialog("");
//                    Log.e("loadBitmaps", "data: "+data.getData() );
//                    pickiT.getPath(data.getData(), Build.VERSION.SDK_INT);
//                } else {
//                    String newPath = AppUtils.copyFileToInternalStorage(this, uri, "/SalesCAM/Pics");
//                    Log.e("loadBitmaps", "newPath: "+newPath );
//
//                    sendPathToCropping(newPath);
//                }


            }

            if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                if (resultCode == RESULT_OK) {
                    try {
                        Uri resultUri = result.getUri();
                        String outputFile = PathUtil.getPath(context, resultUri);
                        Log.e("loadBitmaps", "outputFile: "+outputFile );

                        String strOrientation = "";

                        int orientation = getResources().getConfiguration().orientation;
                        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            strOrientation = "landscape";
                        } else {
                            strOrientation = "portrait";
                        }

                        startActivity(new Intent(context, ImageSelectActivity.class)
                                .putExtra("selectedImagePath", outputFile)
                                .putExtra("orientation", strOrientation)
                        );
                        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);

                    } catch (Exception e) {
                    }

                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    Exception error = result.getError();
                }
            }

        } else {
            isSelectingImage = false;
        }

    }

    private void checkAutoRotateStatus(){

        try{

            if (android.provider.Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) == 1){
                // Do Nothing
            }
            else{
                showToast("Please turn ON auto-rotation.");
            }

        } catch (Exception e){

        }

    }

    // TODO PickIt Library Methods

    @Override
    public void PickiTonUriReturned() {}
    @Override
    public void PickiTonStartListener() {}
    @Override
    public void PickiTonProgressUpdate(int progress) {}
    @Override
    public void PickiTonMultipleCompleteListener(ArrayList<String> paths, boolean wasSuccessful, String Reason) {}
    @Override
    public void PickiTonCompleteListener(String path, boolean wasDriveFile, boolean wasUnknownProvider, boolean wasSuccessful, String Reason) {

        hideProgressDialog();

        if(wasSuccessful) {
            String strOrientation = "";

            sendPathToCropping(path);
        } else {
            showToast(getString(R.string.api_error));
            Log.e("Error", Reason);
        }

    }

    private void sendPathToCropping(String path){

        File userImageFile = new File(path);
        int ratioWidth = 1, ratioHeight = 1, width = 720, height = 1280;

        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            width = Integer.parseInt(AppUtils.picLandscapeResolution.split("x")[0]);
            height = Integer.parseInt(AppUtils.picLandscapeResolution.split("x")[1]);
            ratioWidth = 4;
            ratioHeight = 3;
        } else {
            width = Integer.parseInt(AppUtils.picPortraitResolution.split("x")[0]);
            height = Integer.parseInt(AppUtils.picPortraitResolution.split("x")[1]);
            ratioWidth = 1;
            ratioHeight = 1;
        }

        Log.e("sendPathToCropping", "sendPathToCropping: "+userImageFile );


        CropImage.activity(Uri.fromFile(userImageFile))
                .setAspectRatio(ratioWidth, ratioHeight)
                .setRequestedSize(width, height)
                .start(ImageCaptureActivity.this);
    }

}
