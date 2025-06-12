package com.autoforcecam.app.activities;

import static androidx.camera.core.impl.ImageOutputConfig.OPTION_SUPPORTED_RESOLUTIONS;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.autoforcecam.app.R;
import com.autoforcecam.app.base.BaseActivity;
import com.autoforcecam.app.dialog.LogoutDialog;
import com.autoforcecam.app.interfaces.MediaInterface;
import com.autoforcecam.app.interfaces.NetWatcherListener;
import com.autoforcecam.app.interfaces.PermissionsInterface;
import com.autoforcecam.app.models.UploadModel;
import com.autoforcecam.app.models.VideoSegmentModel;
import com.autoforcecam.app.presenters.MediaPresenter;
import com.autoforcecam.app.responses.OverlayResponse;
import com.autoforcecam.app.responses.Overlays;
import com.autoforcecam.app.utils.AppUtils;
import com.autoforcecam.app.utils.MediaWorker;
import com.autoforcecam.app.utils.NetWatcher;
import com.autoforcecam.app.utils.NetworkDetector;
import com.autoforcecam.app.utils.S3UploadService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.hbisoft.pickit.PickiT;
import com.hbisoft.pickit.PickiTCallbacks;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;

/* Created by JSP@nesar */

public class VideoCaptureActivity extends BaseActivity implements PermissionsInterface,
        View.OnClickListener, NetWatcherListener, PickiTCallbacks {
    String phoneMode="";
    PreviewView previewView;
    @SuppressLint("RestrictedApi")
    private VideoCapture videoCapture;
    Camera camera;
    CameraManager manager;
    ListenableFuture<ProcessCameraProvider> cameraProviderListenableFuture;

    private Collection<String> permissions = Arrays.asList(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE);

    private PickiT pickiT;

    private Handler timerHandler;
    private Runnable timerRunnable;

    private Handler timerHandlerService;
    private Runnable timerRunnableService;

    private NetWatcher netwatcher;

    ActivityResultLauncher<Intent> someActivityResultLauncher;

    private BroadcastReceiver broadcast_reciever;
    private IntentFilter intentFilter;

    private PermissionsInterface permissionsInterface;
    private ImageView img_flash, img_pause, img_stop, img_play, img_delete, img_save, img_bluetooth,
                        img_no_mic, imgZoomInc, imgZoomDec, img_cross, img_focus;
    private RelativeLayout rl_start, rl_gallery, rl_timer, rl_actionbar, rl_bottom_options,
                            rl_actionbar2, rl_bottom_options2, cameraCover;

    private TextView txt_timer;
    private ArrayList<VideoSegmentModel> videoSegmentNames = new ArrayList<>();

    private boolean openNextScreen = false, isBluetoothConnected = false, mRecording = false,
            isPermissionsGranted = false, refreshCameraView = false, isShowingImportPopup = false,
            askPermissionsAgain = true;
    private int count = 0, videoDuration = 0, lastSegmentDuration = 0;
    private float zoomValue = 1.0f;
    private AudioManager audioManager;
    private List<String> selectedVideos;

    /* Torch */
    private final String TORCH_ON = "torch_on";
    private final String TORCH_OFF = "torch_off";
    private String selectedTorchMode = TORCH_OFF;

    /* Focus */
    private final String FOCUS_ON = "torch_on";
    private final String FOCUS_OFF = "torch_off";
    private String selectedFocusMode = FOCUS_OFF;

    /* Camera (Front/Back) */
    private final String CAMERA_FRONT = "front";
    private final String CAMERA_BACK = "back";
    private String selectedCameraPosition = CAMERA_BACK;
    private Dialog orientationLockPopup;

    /* Instructions Popups */
    private int currentPopupPosition = 1;

    private LinearLayout linearIns1, linearInsImport;
    private RelativeLayout relativeIns, relativeIns2, relativeIns3, relativeIns4, relativeIns5,
            relativeExit1, relativeExit2, relativeExit3, relativeExit4, relativeExit5, relativeExitImport;
    private Button btnPre1, btnPre2, btnPre3, btnPre4, btnPre5;
    private Button btnNext1, btnNext2, btnNext3, btnNext4, btnNext5;
    private ImageView imgLogo;
    RelativeLayout startProgressLayout;
    TextView progressTextView;

    private Handler insTimerHandler;
    private Runnable insTimerRunnable;

    @Override
    protected int getLayoutView() {
        return R.layout.act_video_capture;
    }

    @Override
    protected Context getActivityContext() {
        return VideoCaptureActivity.this;
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();



        Log.e("checkOrientationAndRefresh11", "checkOrientationAndRefresh: "+videoSegmentNames.size() );
        int currentAPIVersion = Build.VERSION.SDK_INT;

        Log.e("onResume", "onPostResume: " + currentAPIVersion);
        if (currentAPIVersion > 32) {
            permissions = Arrays.asList(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.ACCESS_MEDIA_LOCATION);

        } else {
            permissions = Arrays.asList(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if(askPermissionsAgain) {
            checkPermissions();
            checkAutoRotateStatus();
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

        List<CameraInfo> cameraInfos = cameraSelector.filter(cameraProvider.getAvailableCameraInfos());

        Preview preview = new Preview.Builder().build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());


        videoCapture = new VideoCapture.Builder()
                .setVideoFrameRate(30)
                .setDefaultResolution(new Size(1280,720))
                .setMaxResolution(new Size(1280,720))
                .build();

//        videoCapture = new VideoCapture.Builder()
//                .setVideoFrameRate(30)
//                .build();

        camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture);

        manager = (CameraManager)context.getSystemService(CAMERA_SERVICE);
    }


    @SuppressLint({"RestrictedApi"})
    private void recordVideo(String code) {

        if (videoCapture != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                Log.e("recordVideo", "recordVideo: permission" );
                showToast("Permission is missing");
                return;
            }


            if (code.equals("1111")){


            }else if (code.equals("1112")){


            }else if (code.equals("1113")){
                rl_start.setEnabled(true);


            }else if (code.equals("1114")){

            }

            videoCapture.startRecording(new VideoCapture.OutputFileOptions.Builder(getNewVideoFileToRecord()).build(),
                    getExecutor(),
                    new VideoCapture.OnVideoSavedCallback() {
                        @Override
                        public void onVideoSaved(@NonNull @NotNull VideoCapture.OutputFileResults outputFileResults) {
                            Log.e("recordVideo", "onVideoSaved: " + outputFileResults.toString());
                        }
                        @Override
                        public void onError(int videoCaptureError, @NonNull @NotNull String message, @Nullable @org.jetbrains.annotations.Nullable Throwable cause) {
                            Log.e("recordVideo", "onVideoSaved err : " + videoCaptureError + "  " + message + "  " + cause.getMessage()+" ");
                        }
                    });
        }
    }



    @Override
    protected void onPause() {
        super.onPause();
        try{
           // cameraView.stop();
        } catch (Exception e){}
    }

    @Override
    protected void initView() {

        previewView = findViewById(R.id.cameraView);
        startProgressLayout=findViewById(R.id.startProgressLayout);
        progressTextView=findViewById(R.id.progressTextView);
        cameraCover = findViewById(R.id.cameraCover);
        img_cross = findViewById(R.id.img_cross);
        img_no_mic = findViewById(R.id.img_no_mic);
        rl_gallery = findViewById(R.id.rl_gallery);
        img_flash = findViewById(R.id.img_flash);
        img_pause = findViewById(R.id.img_pause);
        img_stop = findViewById(R.id.img_stop);
        rl_start = findViewById(R.id.rl_start);
        img_play = findViewById(R.id.img_play);
        img_delete = findViewById(R.id.img_delete);
        img_save = findViewById(R.id.img_save);
        img_focus = findViewById(R.id.img_focus);
        rl_timer = findViewById(R.id.rl_timer);
        txt_timer = findViewById(R.id.txt_timer);
        img_bluetooth = findViewById(R.id.img_bluetooth);
        rl_actionbar = findViewById(R.id.rl_actionbar);
        rl_bottom_options = findViewById(R.id.rl_bottom_options);
        rl_actionbar2 = findViewById(R.id.rl_actionbar2);
        rl_bottom_options2 = findViewById(R.id.rl_bottom_options2);
        imgZoomInc = findViewById(R.id.imgZoomInc);
        imgZoomDec = findViewById(R.id.imgZoomDec);
        imgLogo = findViewById(R.id.imgLogo);
        linearInsImport = findViewById(R.id.linearInsImport);
        relativeExitImport = findViewById(R.id.relativeExitImport);

        linearIns1 = findViewById(R.id.linearIns1);
        relativeIns2 = findViewById(R.id.relativeIns2);
        relativeIns3 = findViewById(R.id.relativeIns3);
        relativeIns4 = findViewById(R.id.relativeIns4);
        relativeIns5 = findViewById(R.id.relativeIns5);
        relativeIns = findViewById(R.id.relativeIns);
        relativeExit1 = findViewById(R.id.relativeExit1);
        relativeExit2 = findViewById(R.id.relativeExit2);
        relativeExit3 = findViewById(R.id.relativeExit3);
        relativeExit4 = findViewById(R.id.relativeExit4);
        relativeExit5 = findViewById(R.id.relativeExit5);
        btnPre2 = findViewById(R.id.btnPre2);
        btnPre3 = findViewById(R.id.btnPre3);
        btnPre4 = findViewById(R.id.btnPre4);
        btnPre5 = findViewById(R.id.btnPre5);
        btnNext1 = findViewById(R.id.btnNext1);
        btnNext2 = findViewById(R.id.btnNext2);
        btnNext3 = findViewById(R.id.btnNext3);
        btnNext4 = findViewById(R.id.btnNext4);
        btnNext5 = findViewById(R.id.btnNext5);

    }

    @Override
    protected void initData() {

        pickiT = new PickiT(this, this, this);

        audioManager = (AudioManager) getApplicationContext().getSystemService(getApplicationContext().AUDIO_SERVICE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        permissionsInterface = this;

        netwatcher = new NetWatcher(this);
        registerReceiver(netwatcher, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        int currentAPIVersion = Build.VERSION.SDK_INT;

        if (currentAPIVersion > 32) {

            someActivityResultLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == Activity.RESULT_OK) {
                                // Here, no request code
                                Intent data = result.getData();
                                Log.e("onActivityResult", "onActivityResult: "+data.getData() );
                                Uri uri = data.getData();

                                if (AppUtils.isOneDrive(uri) || AppUtils.isDropBox(uri) || AppUtils.isGoogleDrive(uri)) {
                                    showProgressDialog("");
                                    pickiT.getPath(data.getData(), Build.VERSION.SDK_INT);
                                } else {
                                    pickiT.getPath(data.getData(), Build.VERSION.SDK_INT);
                                    // checkSelectedVideo(newPath);


                                }


                            }
                        }
                    });
        }

        initVideoTimer();
        initBroadcastReceiver();

        if(getSessionManager().getAudioSource().equals("2")){
            img_bluetooth.setVisibility(View.GONE);
            img_no_mic.setVisibility(View.VISIBLE);
            startInternalAudioSource(true);
        }else if(getSessionManager().getAudioSource().equals("1")){
            img_bluetooth.setImageResource(R.drawable.ic_bluetooth_inactive);
            img_bluetooth.setVisibility(View.VISIBLE);
            img_no_mic.setVisibility(View.GONE);
        } else {
            img_bluetooth.setVisibility(View.GONE);
            img_no_mic.setVisibility(View.GONE);
            startInternalAudioSource(false);
        }

    }

    @Override
    protected void initListener() {


        imgLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mRecording) {

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
            }
        });

        img_bluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkForBluetoothAvailability();
            }
        });

        img_cross.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(context, HomeActivity.class)
                        .putExtra("showOverlayProgress", false)
                );
                overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                finish();
            }
        });

        findViewById(R.id.rl_selected_media).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(!mRecording) {

                    startActivity(new Intent(context, ImageCaptureActivity.class));
                    overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                    finish();

                }

            }
        });


        rl_start.setOnClickListener(v -> {

            NotificationManager n = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            if(n.isNotificationPolicyAccessGranted()) {

                AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

                if (phoneMode=="") {

                    switch (audioManager.getRingerMode()) {
                        case AudioManager.RINGER_MODE_SILENT:
                            phoneMode="silent";
                            Log.i("MyApp","Silent mode");
                            break;
                        case AudioManager.RINGER_MODE_VIBRATE:
                            Log.i("MyApp","Vibrate mode");
                            phoneMode="vibrate";
                            break;
                        case AudioManager.RINGER_MODE_NORMAL:
                            phoneMode="ring";
                            Log.i("MyApp","Normal mode");
                            break;
                    }
                }


                audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);

                startProgressLayout.setVisibility(View.VISIBLE);

                new CountDownTimer(3000, 1000) {
                    public void onTick(long millisUntilFinished) {
                        int ss= (int) ((millisUntilFinished / 1000)+1);
                        Log.e("millisUntilFinished", "millisUntilFinished: "+ss);
                        progressTextView.setText(""+ss);



                    }

                    public void onFinish() {
                        startProgressLayout.setVisibility(View.GONE);
                        if(getSessionManager().getAudioSource().equals("1")){

                            if(isBluetoothConnected){

                                rl_start.setVisibility(View.GONE);
                                img_pause.setVisibility(View.VISIBLE);
                                img_stop.setVisibility(View.VISIBLE);
                                videoSegmentNames = new ArrayList<>();
                                // cameraView.captureVideo(getNewVideoFileToRecord());
                                recordVideo("1111");
                                startTimer();

                            } else {

                                showToast("Please connect Bluetooth audio source.");

                            }

                        } else {

                            rl_start.setVisibility(View.GONE);
                            img_pause.setVisibility(View.VISIBLE);
                            img_stop.setVisibility(View.VISIBLE);
                            videoSegmentNames = new ArrayList<>();
                            //  cameraView.captureVideo(getNewVideoFileToRecord());
                            recordVideo("1111");
                            startTimer();

                        }
                    }
                }.start();



            }else{

                Log.e("initListener", "initListener: "+getSessionManager().getFirstTime() );

                if (getSessionManager().getFirstTime()==true){
                    getSessionManager().setFirstTime(false);

                    Dialog dialog = new Dialog(context);
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    dialog.setContentView(R.layout.dialog_message1);
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    dialog.setCancelable(false);


                    Button btn_ok = dialog.findViewById(R.id.btn_ok);
                    btn_ok.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                            // Ask the user to grant access
                            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                            startActivityForResult(intent,1113);

                        }

                    });

                    dialog.show();

                }else {
                    startProgressLayout.setVisibility(View.VISIBLE);

                    new CountDownTimer(3000, 1000) {
                        public void onTick(long millisUntilFinished) {
                            int ss= (int) ((millisUntilFinished / 1000)+1);
                            Log.e("millisUntilFinished", "millisUntilFinished: "+ss);
                            progressTextView.setText(""+ss);

                        }

                        public void onFinish() {
                            startProgressLayout.setVisibility(View.GONE);
                            if(getSessionManager().getAudioSource().equals("1")){

                                if(isBluetoothConnected){

                                    rl_start.setVisibility(View.GONE);
                                    img_pause.setVisibility(View.VISIBLE);
                                    img_stop.setVisibility(View.VISIBLE);
                                    videoSegmentNames = new ArrayList<>();
                                    // cameraView.captureVideo(getNewVideoFileToRecord());
                                    recordVideo("1111");
                                    startTimer();

                                } else {

                                    showToast("Please connect Bluetooth audio source.");

                                }

                            } else {

                                rl_start.setVisibility(View.GONE);
                                img_pause.setVisibility(View.VISIBLE);
                                img_stop.setVisibility(View.VISIBLE);
                                videoSegmentNames = new ArrayList<>();
                                //  cameraView.captureVideo(getNewVideoFileToRecord());
                                recordVideo("1111");
                                startTimer();

                            }
                        }
                    }.start();
                }
            }

        });



        img_pause.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View view) {

                videoCapture.stopRecording();
                stopTimer();

                img_save.setVisibility(View.VISIBLE);

                img_delete.setAlpha(1.0f);
                img_delete.setEnabled(true);

                img_pause.setVisibility(View.GONE);
                img_stop.setVisibility(View.GONE);
                img_play.setVisibility(View.VISIBLE);
                img_delete.setVisibility(View.VISIBLE);

            }
        });

        img_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                NotificationManager n = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                if(n.isNotificationPolicyAccessGranted()) {

                    AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

                    if (phoneMode=="") {

                        switch (audioManager.getRingerMode()) {
                            case AudioManager.RINGER_MODE_SILENT:
                                phoneMode="silent";
                                Log.i("MyApp","Silent mode");
                                break;
                            case AudioManager.RINGER_MODE_VIBRATE:
                                Log.i("MyApp","Vibrate mode");
                                phoneMode="vibrate";
                                break;
                            case AudioManager.RINGER_MODE_NORMAL:
                                phoneMode="ring";
                                Log.i("MyApp","Normal mode");
                                break;
                        }
                    }


                    audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);


                    startProgressLayout.setVisibility(View.VISIBLE);

                    new CountDownTimer(3000, 1000) {
                        public void onTick(long millisUntilFinished) {
                            int ss= (int) ((millisUntilFinished / 1000)+1);
                            Log.e("millisUntilFinished", "millisUntilFinished: "+ss);
                            progressTextView.setText(""+ss);



                        }

                        public void onFinish() {
                            startProgressLayout.setVisibility(View.GONE);
                            if(getSessionManager().getAudioSource().equals("1")){

                                if(isBluetoothConnected){

                                    img_save.setVisibility(View.GONE);
                                    img_pause.setVisibility(View.VISIBLE);
                                    img_stop.setVisibility(View.VISIBLE);
                                    img_play.setVisibility(View.GONE);
                                    img_delete.setVisibility(View.GONE);

                                    final Handler handler = new Handler(Looper.getMainLooper());
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            recordVideo("1112");
                                            startTimer();

                                        }
                                    }, 1500);




                                } else {

                                    showToast("Please connect Bluetooth audio source.");

                                }

                            } else {

                                img_save.setVisibility(View.GONE);
                                img_pause.setVisibility(View.VISIBLE);
                                img_stop.setVisibility(View.VISIBLE);
                                img_play.setVisibility(View.GONE);
                                img_delete.setVisibility(View.GONE);

                                recordVideo("1112");
                                startTimer();

                            }
                        }
                    }.start();


                }else{

                    if (getSessionManager().getFirstTime()==true){
                        getSessionManager().setFirstTime(false);
                        Dialog dialog = new Dialog(context);
                        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                        dialog.setContentView(R.layout.dialog_message1);
                        dialog.setCanceledOnTouchOutside(false);
                        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                        dialog.setCancelable(false);


                        Button btn_ok = dialog.findViewById(R.id.btn_ok);
                        btn_ok.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                                // Ask the user to grant access
                                Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                                startActivityForResult(intent,1114);

                            }

                        });


                        dialog.show();

                    }else {
                        startProgressLayout.setVisibility(View.VISIBLE);

                        new CountDownTimer(3000, 1000) {
                            public void onTick(long millisUntilFinished) {
                                int ss= (int) ((millisUntilFinished / 1000)+1);
                                Log.e("millisUntilFinished", "millisUntilFinished: "+ss);
                                progressTextView.setText(""+ss);



                            }

                            public void onFinish() {
                                startProgressLayout.setVisibility(View.GONE);
                                if(getSessionManager().getAudioSource().equals("1")){

                                    if(isBluetoothConnected){

                                        img_save.setVisibility(View.GONE);
                                        img_pause.setVisibility(View.VISIBLE);
                                        img_stop.setVisibility(View.VISIBLE);
                                        img_play.setVisibility(View.GONE);
                                        img_delete.setVisibility(View.GONE);

                                        final Handler handler = new Handler(Looper.getMainLooper());
                                        handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                recordVideo("1112");
                                                startTimer();

                                            }
                                        }, 1500);




                                    } else {

                                        showToast("Please connect Bluetooth audio source.");

                                    }

                                } else {

                                    img_save.setVisibility(View.GONE);
                                    img_pause.setVisibility(View.VISIBLE);
                                    img_stop.setVisibility(View.VISIBLE);
                                    img_play.setVisibility(View.GONE);
                                    img_delete.setVisibility(View.GONE);

                                    recordVideo("1112");
                                    startTimer();

                                }
                            }
                        }.start();
                    }




                }







            }
        });

        img_stop.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View view) {

                img_play.setEnabled(false);
                img_stop.setEnabled(false);
                img_save.setEnabled(false);
                img_delete.setEnabled(false);

                openNextScreen = true;
                videoCapture.stopRecording();
                stopTimer();


                Log.e("videoSegmentNames stop", "onClick: "+videoSegmentNames.toString() );

                Gson gson = new Gson();
                String jsonData = gson.toJson(videoSegmentNames);
                startActivity(new Intent(context, VideoOverlayActivity.class)
                        .putExtra("videoSegmentNames", jsonData));
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);

            }
        });

        img_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e("videoSegmentNames", "onClick: "+videoSegmentNames.toString() );

                Gson gson = new Gson();
                String jsonData = gson.toJson(videoSegmentNames);
                startActivity(new Intent(context, VideoOverlayActivity.class)
                        .putExtra("videoSegmentNames", jsonData));
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);

            }
        });

        relativeExitImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isShowingImportPopup = false;
                getSessionManager().setImportInstructionsStatus(false);
                relativeIns.setVisibility(View.GONE);
                linearInsImport.setVisibility(View.GONE);

                Intent intent;

                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    intent = new Intent(Intent.ACTION_GET_CONTENT);
                } else {
                    intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                }

                intent.setType("video/mp4");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
                startActivityForResult(Intent.createChooser(intent, "ChooseFile"), 111);

            }
        });

        rl_gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(!mRecording) {

                    if(getSessionManager().showImportInstructions()){
                        isShowingImportPopup = true;
                        relativeIns.setVisibility(View.VISIBLE);
                        linearInsImport.setVisibility(View.VISIBLE);

                    } else
                    {

                        int currentAPIVersion = Build.VERSION.SDK_INT;

                        if (currentAPIVersion > 32){
                            someActivityResultLauncher.launch(new Intent(MediaStore.ACTION_PICK_IMAGES).setType("video/mp4"));
                        }else {

                            Intent intent;

                            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                                intent = new Intent(Intent.ACTION_GET_CONTENT);
                            } else {
                                intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                            }

                            intent.setType("video/mp4");
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
                            startActivityForResult(intent, 111);

                        }

                    }

                }

            }
        });

        imgZoomInc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(zoomValue<4){
                    zoomValue = zoomValue + 0.2f;
                    camera.getCameraControl().setZoomRatio(zoomValue);
                    //cameraView.setZoom(zoomValue);
                }

            }
        });

        imgZoomDec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(zoomValue>1){
                    zoomValue = zoomValue - 0.2f;
                    camera.getCameraControl().setZoomRatio(zoomValue);

                }
            }
        });

        relativeExit1.setOnClickListener(this);
        relativeExit2.setOnClickListener(this);
        relativeExit3.setOnClickListener(this);
        relativeExit4.setOnClickListener(this);
        relativeExit5.setOnClickListener(this);
        btnPre2.setOnClickListener(this);
        btnPre3.setOnClickListener(this);
        btnPre4.setOnClickListener(this);
        btnPre5.setOnClickListener(this);
        btnNext1.setOnClickListener(this);
        btnNext2.setOnClickListener(this);
        btnNext3.setOnClickListener(this);
        btnNext4.setOnClickListener(this);
        btnNext5.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {

        if(v.getId() == R.id.relativeExit1 || v.getId() == R.id.relativeExit2 ||
                v.getId() == R.id.relativeExit3 || v.getId() == R.id.relativeExit4 ||
                v.getId() == R.id.relativeExit5)
        {
            getSessionManager().setVideoInstructionsStatus(false);
            currentPopupPosition = 1;
            AppUtils.isVideoInstructionsShown = true;
            relativeIns.setVisibility(View.GONE);
            linearIns1.setVisibility(View.GONE);
            relativeIns2.setVisibility(View.GONE);
            relativeIns3.setVisibility(View.GONE);
            relativeIns4.setVisibility(View.GONE);
            relativeIns5.setVisibility(View.GONE);
        }

        if(v.getId() == R.id.btnPre2 || v.getId() == R.id.btnPre3 ||
                v.getId() == R.id.btnPre4 || v.getId() == R.id.btnPre5)
        {
            currentPopupPosition--;
            checkIntructionsPopupStatus();
        }

        if(v.getId() == R.id.btnNext1 || v.getId() == R.id.btnNext2 ||
                v.getId() == R.id.btnNext3 || v.getId() == R.id.btnNext4 ||
                v.getId() == R.id.btnNext5)
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
        // DO Nothing
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


                                WorkManager.getInstance(VideoCaptureActivity.this)
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

    private File getNewVideoFileToRecord(){

        String videoPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/SalesCAM";
        String vidFileName = "Segment" + (count + 1) + ".mp4";
        count++;

        File dir = new File(videoPath);
        if (!dir.exists()) {
            dir.mkdirs();
        } else {
            File vFile = new File(videoPath, vidFileName);
            if (vFile.exists()) {
                vFile.delete();
            }
        }

        videoSegmentNames.add(
                new VideoSegmentModel(
                        AppUtils.getTimeStamp(),
                        videoPath + "/" + vidFileName,
                        "",
                        false,
                        "",
                        0,
                        true
                )
        );

        File videoFile = new File(videoPath, vidFileName);

        return videoFile;
    }

    private void checkIntructionsPopupStatus(){

        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {

            if (getSessionManager().showVideoInstructions()) {

                if (!AppUtils.isVideoInstructionsShown) {

                    relativeIns.setVisibility(View.VISIBLE);

                    linearIns1.setVisibility(View.GONE);
                    relativeIns2.setVisibility(View.GONE);
                    relativeIns3.setVisibility(View.GONE);
                    relativeIns4.setVisibility(View.GONE);
                    relativeIns5.setVisibility(View.GONE);

                    if (currentPopupPosition == 1) {
                        linearIns1.setVisibility(View.VISIBLE);
                    }
                    if (currentPopupPosition == 2) {
                        relativeIns2.setVisibility(View.VISIBLE);
                    }
                    if (currentPopupPosition == 3) {
                        relativeIns3.setVisibility(View.VISIBLE);
                    }
                    if (currentPopupPosition == 4) {
                        relativeIns4.setVisibility(View.VISIBLE);
                    }
                    if (currentPopupPosition == 5) {
                        relativeIns5.setVisibility(View.VISIBLE);
                    }
                    if (currentPopupPosition == 6) {
                        currentPopupPosition = 1;
                        AppUtils.isVideoInstructionsShown = true;
                        relativeIns.setVisibility(View.GONE);
                    }

                    if (currentPopupPosition != 6) {
//                        initInsTimer();
                    }

                }

            }

        } else {

            relativeIns.setVisibility(View.GONE);
            if (insTimerHandler != null) {
                insTimerHandler.removeCallbacks(insTimerRunnable);
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

    // TODO Permissions Methods

    private void checkPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            askPermissions(permissions, permissionsInterface);
        } else {
            isPermissionsGranted = true;
            if(getSessionManager().getAudioSource().equals("1")){ checkForBluetoothAvailability(); }
            checkOrientationAndRefresh();
            checkIntructionsPopupStatus();
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

        openPermissionsScreen(
                isPermanentalyDenied,
                message,
                this,
                permissions);

    }

    @Override
    public void onPermissionsGranted() {
        isPermissionsGranted = true;
        if(getSessionManager().getAudioSource().equals("1")){ checkForBluetoothAvailability(); }
        checkOrientationAndRefresh();
        checkIntructionsPopupStatus();

    }

    @Override
    public void onPermissionsCancelled(boolean isSettingsOpened) {

        if(isSettingsOpened){
            askPermissionsAgain = true;
        } else {
            askPermissions(permissions, permissionsInterface);
        }

    }

    // TODO Camera Methods

    public void resetFocusMode(View v) {

        if(!mRecording) {

            switch (selectedFocusMode) {

                case FOCUS_ON:
                    selectedFocusMode = FOCUS_OFF;
                    img_focus.setImageResource(R.drawable.ic_autofocus_off);
                  //  cameraView.setFocus(CameraKit.Constants.FOCUS_OFF);
                    break;

                case FOCUS_OFF:
                    selectedFocusMode = FOCUS_ON;
                    img_focus.setImageResource(R.drawable.ic_autofocus_on);
                   // cameraView.setFocus(CameraKit.Constants.FOCUS_TAP);
                    break;

            }

            restartCameraWithSmallDelay();

        }

    }

    public void resetTorchMode(View v) {

        if(!mRecording) {

            switch (selectedTorchMode) {

                case TORCH_ON:
                    selectedTorchMode = TORCH_OFF;
                    img_flash.setImageResource(R.drawable.ic_flash_off);
                    camera.getCameraControl().enableTorch(false);
                  //  cameraView.setFlash(CameraKit.Constants.FLASH_OFF);
                    break;

                case TORCH_OFF:
                    selectedTorchMode = TORCH_ON;
                    img_flash.setImageResource(R.drawable.ic_flash_on);
                    camera.getCameraControl().enableTorch(true);
                    //cameraView.setFlash(CameraKit.Constants.FLASH_TORCH);
                    break;

            }

        }

    }

    public void changeCameraPosition(View v) {

        if(!mRecording) {

            selectedTorchMode = TORCH_OFF;
            img_flash.setImageResource(R.drawable.ic_flash_off);
            camera.getCameraControl().enableTorch(false);

            switch (selectedCameraPosition) {

                case CAMERA_FRONT:
                    selectedCameraPosition = CAMERA_BACK;
                    cameraProviderListenableFuture=ProcessCameraProvider.getInstance(VideoCaptureActivity.this);
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
                    cameraProviderListenableFuture=ProcessCameraProvider.getInstance(VideoCaptureActivity.this);
                    cameraProviderListenableFuture.addListener(() ->{
                        try {
                            ProcessCameraProvider cameraProvider= cameraProviderListenableFuture.get();
                       //     isFront=true;
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

    }

    // TODO Others

    public void openDeleteSegmentPopup(View v){

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_deletesegment);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button btn_yes = dialog.findViewById(R.id.btn_yes);
        btn_yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String videoPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/SalesCAM";

                File dir = new File(videoPath);
                if (dir.exists()) {

                    int lastPosition = videoSegmentNames.size() - 1;

                    File video = new File(dir, videoSegmentNames.get(lastPosition).getVideoPath());

                    if (video.exists()) {
                        video.delete();
                    }

                    videoSegmentNames.remove(lastPosition);

                    if(videoSegmentNames.size()==0){
                        img_save.setVisibility(View.GONE);
                    }

                }

                img_delete.setEnabled(false);
                img_delete.setAlpha(0.2f);
                dialog.dismiss();

                removeDurationOfDeletedSegment();

            }

        });

        Button btn_no = dialog.findViewById(R.id.btn_no);
        btn_no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();

    }

    public void openAudioSourcePopup(View v){

        if(!mRecording) {

            Dialog dialog = new Dialog(context);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_audiosource);
            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(false);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

            RadioGroup radioGroup = dialog.findViewById(R.id.radioGroup);
            AppCompatRadioButton internal_mic = dialog.findViewById(R.id.internal_mic);
            AppCompatRadioButton external_mic = dialog.findViewById(R.id.external_mic);

            AppCompatRadioButton no_mic = dialog.findViewById(R.id.no_mic);

            if (getSessionManager().getAudioSource().equals("2")) {
                no_mic.setChecked(true);
            } else if (getSessionManager().getAudioSource().equals("1")) {
                external_mic.setChecked(true);
            } else {
                internal_mic.setChecked(true);
            }

            Button btn_save = dialog.findViewById(R.id.btn_save);
            btn_save.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if(radioGroup.getCheckedRadioButtonId()==R.id.internal_mic){
                        getSessionManager().setAudioSource("0");
                        startInternalAudioSource(false);

                    }else if(radioGroup.getCheckedRadioButtonId()==R.id.external_mic){

                        getSessionManager().setAudioSource("1");
                        checkForBluetoothAvailability();
                    }else if(radioGroup.getCheckedRadioButtonId()==R.id.no_mic){
                        getSessionManager().setAudioSource("2");
                        startInternalAudioSource(true);

                    }


                    dialog.dismiss();

                }

            });

            ImageView img_close = dialog.findViewById(R.id.img_close);
            img_close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            dialog.show();

        }

    }


    // TODO Timer Methods

    private void initVideoTimer(){

        timerHandler = new Handler();

        timerRunnable = new Runnable() {
            @Override
            public void run() {

                videoDuration++;
                lastSegmentDuration++;
                updateVideoTime();
                timerHandler.postDelayed(timerRunnable, 1000);

            }
        };

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

    private void startTimer(){
        mRecording = true;
        rl_timer.setVisibility(View.VISIBLE);
        lastSegmentDuration = 0;
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void stopTimer(){
        mRecording = false;
        try{  timerHandler.removeCallbacks(timerRunnable); } catch (Exception e){}
    }

    private void removeDurationOfDeletedSegment(){
        videoDuration = videoDuration - lastSegmentDuration;
        updateVideoTime();
    }

    private void updateVideoTime(){

        int min = videoDuration/60;
        int sec = videoDuration%60;

        String duration = "";

        if(min<10){
            duration = "0" + min + ":";
        } else {
            duration = min + ":";
        }

        if(sec<10){
            duration = duration + "0" + sec;
        } else {
            duration = duration + sec;
        }

        txt_timer.setText(duration);

    }

    private void openTapToFocusPopup(){

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_message);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView txt_heading = dialog.findViewById(R.id.txt_heading);
        AppCompatCheckBox chkBox = dialog.findViewById(R.id.chkBox);

        ImageView img_close = dialog.findViewById(R.id.img_close);
        img_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(chkBox.isChecked()){
                    getSessionManager().setTaptofocus(false);
                }

                dialog.dismiss();

            }
        });

        dialog.show();

    }

    // TODO Bluetooth Methods

    @SuppressLint("MissingPermission")
    private void checkForBluetoothAvailability(){

        if(!mRecording) {

            img_bluetooth.setImageResource(R.drawable.ic_bluetooth_inactive);
            img_bluetooth.setVisibility(View.VISIBLE);
            img_no_mic.setVisibility(View.GONE);

            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                showToast("Device does not support bluetooth audio source.");
            } else {

                if (!mBluetoothAdapter.isEnabled()) {
                    showToast("Bluetooth audio source not found.");
                } else {
                    if (mBluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) == BluetoothAdapter.STATE_CONNECTED) {
                        startBluetoothAudioSource();
                    } else {
                        showToast("Please connect Bluetooth audio source.");
                    }
                }
            }

        }

    }

    private void startBluetoothAudioSource(){

        isBluetoothConnected = false;
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        registerReceiver(mBluetoothScoReceiver, intentFilter);

        // Start Bluetooth SCO.
        audioManager.setMode(audioManager.MODE_NORMAL);
        audioManager.setBluetoothScoOn(true);
        audioManager.startBluetoothSco();
        // Stop Speaker.
        audioManager.setSpeakerphoneOn(false);
        audioManager.setMicrophoneMute(false);

    }

    private void startInternalAudioSource(boolean muteMicrophone){

        isBluetoothConnected = false;
        img_bluetooth.setVisibility(View.GONE);
        img_bluetooth.setImageResource(R.drawable.ic_bluetooth_inactive);

        if(muteMicrophone) img_no_mic.setVisibility(View.VISIBLE);
        else img_no_mic.setVisibility(View.GONE);

        try {

            // Stop Bluetooth SCO.
            audioManager.stopBluetoothSco();
            audioManager.setMode(audioManager.MODE_NORMAL);
            audioManager.setBluetoothScoOn(false);
            // Start Speaker.
            audioManager.setSpeakerphoneOn(true);
            audioManager.setMicrophoneMute(muteMicrophone);

            try{ unregisterReceiver(mBluetoothScoReceiver); } catch (Exception e){}

        }catch (Exception e){

        }

    }

    private BroadcastReceiver mBluetoothScoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
            if (AudioManager.SCO_AUDIO_STATE_CONNECTED == state) {
                isBluetoothConnected = true;
                img_bluetooth.setImageResource(R.drawable.ic_bluetooth_active);
            } else {
                isBluetoothConnected = false;
                img_bluetooth.setImageResource(R.drawable.ic_bluetooth_inactive);
            }
        }
    };

    // TODO VIDEO Picker Methods



    // orientation Methods

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        checkOrientationAndRefresh();
    }

    @SuppressLint("RestrictedApi")
    private void checkOrientationAndRefresh(){

        cameraCover.setVisibility(View.VISIBLE);

        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {

            rl_actionbar.setVisibility(View.VISIBLE);
            rl_bottom_options.setVisibility(View.VISIBLE);
            rl_actionbar2.setVisibility(View.GONE);
            rl_bottom_options2.setVisibility(View.GONE);
            imgZoomInc.setVisibility(View.GONE);
            imgZoomDec.setVisibility(View.GONE);

            Log.e("checkOrientationAndRefresh", "checkOrientationAndRefresh: "+videoSegmentNames.size() );


            if(videoSegmentNames.size()>0){
                rl_timer.setVisibility(View.VISIBLE);
            }

            hideVideoOrientationLockPopup();
           // cameraView.setMinimumHeight(getSessionManager().getScreenWidth());
            restartCameraView();

        }
        else {

            rl_actionbar.setVisibility(View.GONE);
            rl_bottom_options.setVisibility(View.GONE);
            rl_actionbar2.setVisibility(View.VISIBLE);
            rl_bottom_options2.setVisibility(View.VISIBLE);
            imgZoomInc.setVisibility(View.GONE);
            imgZoomDec.setVisibility(View.GONE);
            rl_timer.setVisibility(View.GONE);

            showVideoOrientationLockPopup();

            if(mRecording) {

                // Video Stopped

               videoCapture.stopRecording();
                stopTimer();
                img_save.setVisibility(View.VISIBLE);
                img_delete.setAlpha(1.0f);
                img_delete.setEnabled(true);
                img_pause.setVisibility(View.GONE);
                img_stop.setVisibility(View.GONE);
                img_play.setVisibility(View.VISIBLE);
                img_delete.setVisibility(View.VISIBLE);

                showToast("Video recording is paused.");

            }

          //  cameraView.setMinimumHeight(getSessionManager().getScreenHeight());
            restartCameraView();

        }

        checkIntructionsPopupStatus();

        if(isShowingImportPopup){

            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                relativeIns.setVisibility(View.VISIBLE);
                linearInsImport.setVisibility(View.VISIBLE);
            } else {
                relativeIns.setVisibility(View.GONE);
                linearInsImport.setVisibility(View.GONE);
            }
        }

    }

    private void restartCameraWithSmallDelay(){

        showProgressDialog("Please wait...");
        showProgressDialog("Please wait...");

//        try{ cameraView.stop(); } catch (Exception e){}

//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (isPermissionsGranted)  {
//                    cameraView.start();
//                }
//                hideProgressDialog();
//            }
//        }, 300);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                cameraCover.setVisibility(View.GONE);
                hideProgressDialog();
            }
        }, 700);

    }

    private void restartCameraView(){


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isPermissionsGranted)  {

                  //  cameraView.start();

                    cameraProviderListenableFuture=ProcessCameraProvider.getInstance(VideoCaptureActivity.this);
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

                }
            }
        }, 500);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                cameraCover.setVisibility(View.GONE);
            }
        }, 1500);

    }

    private void showVideoOrientationLockPopup(){

        if(null == orientationLockPopup) {
            orientationLockPopup = new Dialog(context);
            orientationLockPopup.requestWindowFeature(Window.FEATURE_NO_TITLE);
            orientationLockPopup.setContentView(R.layout.dialog_message2);
            orientationLockPopup.setCanceledOnTouchOutside(false);
            orientationLockPopup.setCancelable(false);
            orientationLockPopup.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        if(!orientationLockPopup.isShowing()){
            orientationLockPopup.show();
        }

    }

    private void hideVideoOrientationLockPopup(){

        if(null != orientationLockPopup) {
            orientationLockPopup.dismiss();
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

    private int getDuration(String filePath) {

        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(filePath);
        String durationStr = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long d = Long.parseLong(durationStr) / 1000;
        int duration = (int)d;
        return duration;

    }

    private boolean isVideoHaveAudioTrack(String path) {

        try {

            boolean audioTrack = false;

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(path);
            String hasAudioStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO);
            if (hasAudioStr.equals("yes")) {
                audioTrack = true;
            } else {
                audioTrack = false;
            }

            return audioTrack;

        }catch (Exception e){
            return false;
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

        Log.e("PickiTonCompleteListener", "PickiTonCompleteListener: "+path );
        hideProgressDialog();

        if(wasSuccessful) {
            checkSelectedVideo(path);
        } else {
            showToast(getString(R.string.api_error));
            Log.e("Error", Reason);
        }

    }

    private void checkSelectedVideo(String path){

        if(isVideoHaveAudioTrack(path)) {

            rl_timer.setVisibility(View.VISIBLE);

            videoSegmentNames.add(
                    new VideoSegmentModel(
                            AppUtils.getTimeStamp(),
                            path,
                            "",
                            true,
                            "",
                            0,
                            true
                    )
            );

            int duration = getDuration(path);
            videoDuration = videoDuration + duration;
            lastSegmentDuration = duration;
            updateVideoTime();
            img_save.setVisibility(View.VISIBLE);
            rl_start.setVisibility(View.GONE);
            img_delete.setAlpha(1.0f);
            img_delete.setEnabled(true);
            img_pause.setVisibility(View.GONE);
            img_stop.setVisibility(View.GONE);
            img_play.setVisibility(View.VISIBLE);
            img_delete.setVisibility(View.VISIBLE);

        } else {
            showToast("Please select video containing audio track.");
        }

    }

    private boolean videoFileIsCorrupted(String path){

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        try {
            retriever.setDataSource(this, Uri.parse(path));
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

        String hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO);
        return "yes".equals(hasVideo);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.e("onActivityResult", "onActivityResult: "+requestCode+" "+resultCode );

        NotificationManager n = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if(n.isNotificationPolicyAccessGranted()) {

        }
        if (requestCode==1113 && n.isNotificationPolicyAccessGranted()){

            rl_start.setEnabled(false);
            Handler handler1 = new Handler();
            handler1.postDelayed(new Runnable() {
                @Override
                public void run() {

                    AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

                    if (phoneMode=="") {

                        switch (audioManager.getRingerMode()) {
                            case AudioManager.RINGER_MODE_SILENT:
                                phoneMode="silent";
                                Log.i("MyApp","Silent mode");
                                break;
                            case AudioManager.RINGER_MODE_VIBRATE:
                                Log.i("MyApp","Vibrate mode");
                                phoneMode="vibrate";
                                break;
                            case AudioManager.RINGER_MODE_NORMAL:
                                phoneMode="ring";
                                Log.i("MyApp","Normal mode");
                                break;
                        }
                    }


                    audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);

                    startProgressLayout.setVisibility(View.VISIBLE);

                    new CountDownTimer(3000, 1000) {
                        public void onTick(long millisUntilFinished) {
                            int ss= (int) ((millisUntilFinished / 1000)+1);
                            Log.e("millisUntilFinished", "millisUntilFinished: "+ss);
                            progressTextView.setText(""+ss);



                        }

                        public void onFinish() {
                            startProgressLayout.setVisibility(View.GONE);
                            if(getSessionManager().getAudioSource().equals("1")){

                                if(isBluetoothConnected){

                                    rl_start.setVisibility(View.GONE);
                                    img_pause.setVisibility(View.VISIBLE);
                                    img_stop.setVisibility(View.VISIBLE);
                                    videoSegmentNames = new ArrayList<>();
                                    // cameraView.captureVideo(getNewVideoFileToRecord());
                                    recordVideo("1113");
                                    startTimer();

                                } else {

                                    showToast("Please connect Bluetooth audio source.");

                                }

                            } else {

                                rl_start.setVisibility(View.GONE);
                                img_pause.setVisibility(View.VISIBLE);
                                img_stop.setVisibility(View.VISIBLE);
                                videoSegmentNames = new ArrayList<>();
                                //  cameraView.captureVideo(getNewVideoFileToRecord());
                                recordVideo("1113");
                                startTimer();

                            }
                        }
                    }.start();
                }
            }, 2000);

        }
        else if (requestCode==1114 && n.isNotificationPolicyAccessGranted()){


            img_play.setEnabled(false);
            Handler handler1 = new Handler();
            handler1.postDelayed(new Runnable() {
                @Override
                public void run() {

                    AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

                    if (phoneMode=="") {

                        switch (audioManager.getRingerMode()) {
                            case AudioManager.RINGER_MODE_SILENT:
                                phoneMode="silent";
                                Log.i("MyApp","Silent mode");
                                break;
                            case AudioManager.RINGER_MODE_VIBRATE:
                                Log.i("MyApp","Vibrate mode");
                                phoneMode="vibrate";
                                break;
                            case AudioManager.RINGER_MODE_NORMAL:
                                phoneMode="ring";
                                Log.i("MyApp","Normal mode");
                                break;
                        }
                    }


                    audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);

                    startProgressLayout.setVisibility(View.VISIBLE);

                    new CountDownTimer(3000, 1000) {
                        public void onTick(long millisUntilFinished) {
                            int ss= (int) ((millisUntilFinished / 1000)+1);
                            Log.e("millisUntilFinished", "millisUntilFinished: "+ss);
                            progressTextView.setText(""+ss);



                        }

                        public void onFinish() {
                            startProgressLayout.setVisibility(View.GONE);
                            if(getSessionManager().getAudioSource().equals("1")){

                                if(isBluetoothConnected){

                                    img_save.setVisibility(View.GONE);
                                    img_pause.setVisibility(View.VISIBLE);
                                    img_stop.setVisibility(View.VISIBLE);
                                    img_play.setVisibility(View.GONE);
                                    img_delete.setVisibility(View.GONE);

                                    final Handler handler = new Handler(Looper.getMainLooper());
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            recordVideo("1114");
                                            startTimer();

                                        }
                                    }, 1500);




                                } else {

                                    showToast("Please connect Bluetooth audio source.");

                                }

                            } else {

                                img_save.setVisibility(View.GONE);
                                img_pause.setVisibility(View.VISIBLE);
                                img_stop.setVisibility(View.VISIBLE);
                                img_play.setVisibility(View.GONE);
                                img_delete.setVisibility(View.GONE);

                                recordVideo("1114");
                                startTimer();

                            }
                        }
                    }.start();

                }
            }, 2000);


        }



        if (n.isNotificationPolicyAccessGranted()==false){
            showToast("Permission denied");
            rl_start.setEnabled(false);
            Handler handler1 = new Handler();
            handler1.postDelayed(new Runnable() {
                @Override
                public void run() {

                    startProgressLayout.setVisibility(View.VISIBLE);

                    new CountDownTimer(3000, 1000) {
                        public void onTick(long millisUntilFinished) {
                            int ss= (int) ((millisUntilFinished / 1000)+1);
                            Log.e("millisUntilFinished", "millisUntilFinished: "+ss);
                            progressTextView.setText(""+ss);



                        }

                        public void onFinish() {
                            startProgressLayout.setVisibility(View.GONE);
                            if(getSessionManager().getAudioSource().equals("1")){

                                if(isBluetoothConnected){

                                    rl_start.setVisibility(View.GONE);
                                    img_pause.setVisibility(View.VISIBLE);
                                    img_stop.setVisibility(View.VISIBLE);
                                    videoSegmentNames = new ArrayList<>();
                                    // cameraView.captureVideo(getNewVideoFileToRecord());
                                    recordVideo("1113");
                                    startTimer();

                                } else {

                                    showToast("Please connect Bluetooth audio source.");

                                }

                            } else {

                                rl_start.setVisibility(View.GONE);
                                img_pause.setVisibility(View.VISIBLE);
                                img_stop.setVisibility(View.VISIBLE);
                                videoSegmentNames = new ArrayList<>();
                                //  cameraView.captureVideo(getNewVideoFileToRecord());
                                recordVideo("1113");
                                startTimer();

                            }
                        }
                    }.start();
                }
            }, 2000);
        }


        if (requestCode == 111) {

            Uri uri = data.getData();

            if (AppUtils.isOneDrive(uri) || AppUtils.isDropBox(uri) || AppUtils.isGoogleDrive(uri)) {
                showProgressDialog("");
                pickiT.getPath(data.getData(), Build.VERSION.SDK_INT);
            } else {
                String newPath = AppUtils.copyFileToInternalStorage(this, uri, "SalesCAM");
                checkSelectedVideo(newPath);
            }

        }


    }


}
