package com.autoforcecam.app.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.autoforcecam.app.CircleRecycler.CircleRecyclerView;
import com.autoforcecam.app.CircleRecycler.CircularHorizontalMode;
import com.autoforcecam.app.CircleRecycler.ItemViewMode;
import com.autoforcecam.app.R;
import com.autoforcecam.app.adapters.OverlayAdapter;
import com.autoforcecam.app.base.BaseActivity;
import com.autoforcecam.app.interfaces.MediaUploadInterface;
import com.autoforcecam.app.interfaces.PermissionsInterface;
import com.autoforcecam.app.interfaces.PlayerInterface;
import com.autoforcecam.app.models.VideoSegmentModel;
import com.autoforcecam.app.presenters.MediaUploadPresenter;
import com.autoforcecam.app.responses.Overlays;
import com.autoforcecam.app.utils.AppUtils;
import com.autoforcecam.app.utils.SessionManager;
import com.coremedia.iso.boxes.Container;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Type;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;


/* Created by JSP@nesar */

public class VideoOverlayActivity extends BaseActivity implements PlayerInterface,
        MediaUploadInterface, PermissionsInterface {

    private PermissionsInterface permissionsInterface;
    private boolean openNextScreen = false, isBluetoothConnected = false,
            isPermissionsGranted = false,
            askPermissionsAgain = true;

    private final String baseVideoPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/SalesCAM";

    private String isScalingRequired = "1";
    private int height = 0, width = 0, frameRate = 0;
    private String mergedFilePath = "";
    private ConstraintLayout progess_layout;

    private ImageView img_actionbar_cross;
    private ProgressBar circular_progress;
    Boolean isMerged=false;
    private TextView txt_actionbar_more, txt_message,progress_text,progress_tv;
    private LinearLayout ll_info;
    private RelativeLayout rl_preview, rl_progress,main_layout;
    private ArrayList<VideoSegmentModel> videoSegmentNames = new ArrayList<>();
    private CircleRecyclerView recycler_overlay;
    private ItemViewMode mItemViewMode;
    private LinearLayoutManager linearLayoutManager;
    private OverlayAdapter overlayAdapter;
    private ArrayList<Overlays> selectedOverlayList = new ArrayList<>();
    private VideoView mVideoView;
    private MediaController mediaController;
    private int totalDuration = 0;
    private MediaUploadPresenter mediaUploadPresenter;
    private boolean mergedFileContainsAudio = false;

    private Collection<String> permissions = Arrays.asList(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE);


    @Override
    protected int getLayoutView() {
        return R.layout.act_video_overlay;
    }

    @Override
    protected Context getActivityContext() {
        return VideoOverlayActivity.this;
    }

    @Override
    protected void onResume() {
        super.onResume();


        int currentAPIVersion = Build.VERSION.SDK_INT;
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
        }
    }

    @Override
    protected void initView() {

        recycler_overlay = findViewById(R.id.recycler_overlay);
        rl_preview = findViewById(R.id.rl_preview);
        txt_actionbar_more = findViewById(R.id.txt_actionbar_more);
        img_actionbar_cross = findViewById(R.id.img_actionbar_cross);
        ll_info = findViewById(R.id.ll_info);
        rl_progress = findViewById(R.id.rl_progress);
        txt_message = findViewById(R.id.txt_message);
        mVideoView = findViewById(R.id.mVideoView);
        circular_progress = findViewById(R.id.circular_progress);
        progress_text = findViewById(R.id.progress_text);
        progess_layout = findViewById(R.id.progess_layout);
        progress_tv = findViewById(R.id.progress_tv);
        main_layout = findViewById(R.id.main_layout);

    }

    @Override
    protected void initData() {
        permissionsInterface = this;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mediaUploadPresenter = new MediaUploadPresenter(getAPIInterface());
        mediaUploadPresenter.attachView(this);
        main_layout.setVisibility(View.GONE);
        progess_layout.setVisibility(View.VISIBLE);

        txt_actionbar_more.setVisibility(View.VISIBLE);
        img_actionbar_cross.setVisibility(View.VISIBLE);
        txt_actionbar_more.setEnabled(false);
        txt_actionbar_more.setText("NEXT");

        int width = getSessionManager().getScreenWidth();
        int height = width/16;
        height = height * 9;

        rl_preview.getLayoutParams().width = width;
        rl_preview.getLayoutParams().height = height;

        linearLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        recycler_overlay.setLayoutManager(linearLayoutManager);
        mItemViewMode = new CircularHorizontalMode();
        recycler_overlay.setViewMode(mItemViewMode);
        recycler_overlay.setNeedCenterForce(false);

        overlayAdapter = new OverlayAdapter(context, this, selectedOverlayList);
        recycler_overlay.setAdapter(overlayAdapter);

        loadOverlaysToAdapter();

        String pathNames = getIntent().getStringExtra("videoSegmentNames");
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<VideoSegmentModel>>() {}.getType();
        videoSegmentNames = gson.fromJson(pathNames, type);


        initVideo();
    }

    @Override
    protected void initListener() {

        txt_actionbar_more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int currentPosition = linearLayoutManager.findFirstVisibleItemPosition();
                String selectedOverlayId = selectedOverlayList.get(currentPosition).getId();
                String selectedOverlayUrl = selectedOverlayList.get(currentPosition).getImage_url();

                getSessionManager().setVidLanOverlayId(selectedOverlayId);

                Log.e("videoSegmentNames", "onClick: "+videoSegmentNames.toString() );
                Gson gson = new Gson();
                String jsonData = gson.toJson(videoSegmentNames);

                startActivity(new Intent(context, VideoOptionsActivity.class)
                        .putExtra("videoSegmentNames", jsonData)
                        .putExtra("mergedFileContainsAudio", mergedFileContainsAudio)
                        .putExtra("mergeTotalDuration", totalDuration)
                        .putExtra("mergedFilePath", mergedFilePath)
                        .putExtra("selectedOverlayId", selectedOverlayId)
                        .putExtra("selectedOverlayUrl", selectedOverlayUrl)
                );
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);

            }
        });

        img_actionbar_cross.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(context, VideoCaptureActivity.class));
        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
        finishAffinity();
    }

    private void loadOverlaysToAdapter() {

        ArrayList<Overlays> videoOverlayList = getSessionManager().getVideoOverlayList();

        String defaultOverlayId = getSessionManager().getVidLanOverlayId();
        int position = 0;

        if(videoOverlayList.size()!=0) {

            for (int i = 0; i < videoOverlayList.size(); i++) {
                if (videoOverlayList.get(i).getOrientation().equalsIgnoreCase("landscape")) {
                    selectedOverlayList.add(videoOverlayList.get(i));
                    String overlayId = videoOverlayList.get(i).getId();
                    if (defaultOverlayId.equals(overlayId)) {
                        position = selectedOverlayList.size();
                    }
                }
            }


            overlayAdapter.updateList(selectedOverlayList);
            for (int i=0;i<selectedOverlayList.size();i++){
                Log.e("loadOverlaysToAdapter", "compare: "+selectedOverlayList.get(i).getId() +"   " +
                        getSessionManager().getSelectedOverlayUrl());
                if (!getSessionManager().getSelectedOverlayUrl().isEmpty()){
                    if (selectedOverlayList.get(i).getId().equals(getSessionManager().getSelectedOverlayUrl())){
                        Log.e("loadOverlaysToAdapter", "true:  "+i+"    "+selectedOverlayList.get(i).getImage_url());
                        recycler_overlay.scrollToPosition(i);
                    }
                }

            }
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

        }

    }

    private void loadVideoPlayer(){

        mergedFileContainsAudio = isVideoHaveAudioTrack(mergedFilePath);

        txt_actionbar_more.setEnabled(true);

        mediaController= new MediaController(this);
        mediaController.setAnchorView(mVideoView);
        Uri uri = Uri.parse(mergedFilePath);
        mVideoView.setMediaController(mediaController);
        mVideoView.setVideoURI(uri);
        mVideoView.requestFocus();
        mVideoView.start();

        try {
            String videoPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/SalesCAM";
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(videoPath))));
        } catch (Exception e){}

    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        try{ mVideoView.seekTo( 100 ); }catch (Exception e){ }



    }

    @Override
    protected void onPause() {
        super.onPause();
        try{ mVideoView.stopPlayback();  }catch (Exception e){ }
    }

    @SuppressLint("MissingPermission")
    private boolean isBluetoothHeadsetAvailable(){

        boolean isAvailable = false;

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            isAvailable = false;
        } else {

            if (!mBluetoothAdapter.isEnabled()) {
                isAvailable = false;
            } else {
                if (mBluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) == BluetoothAdapter.STATE_CONNECTED) {
                    isAvailable = true;
                } else {
                    isAvailable = false;
                }
            }
        }

        return isAvailable;

    }

    @Override
    public void openMediaPlayerOptions() {

        if(null!=mediaController){

            if(mediaController.isShowing()){
                mediaController.hide();
            } else {
                mediaController.show(0);
            }

        }

    }

    private String getVideoResolution(String filePath){

        try {
            MediaPlayer mp = new MediaPlayer();
            mp.setDataSource(filePath);
            mp.prepare();
            int width = mp.getVideoWidth();
            int height = mp.getVideoHeight();
            return width + "x" + height;
        } catch (Exception e){
            return "0x0";
        }

    }

    private void renameFile(String inputPath) {
        try {
            String basePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/SalesCAM";
            File directory = new File(basePath);
            if (!directory.exists()) {
                directory.mkdir();
            }
            String outputPath = basePath + "/" + "Merged_" + AppUtils.getTimeStamp() + ".mp4";
            File from = new File(inputPath);
            File to = new File(outputPath);
            from.renameTo(to);
            mergedFilePath = outputPath;
            videoSegmentNames.get(0).setVideoPath(mergedFilePath);
        } catch (Exception e) {}
    }


    public void rename(Uri uri) {

        String basePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/SalesCAM";
        File directory = new File(basePath);
        if(!directory.exists()){ directory.mkdir(); }

        String outputPath = basePath + "/" + "Merged_" + AppUtils.getTimeStamp() + ".mp4";
        //create content values with new name and update
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, outputPath);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.getContentResolver().update(uri, contentValues, null);
        }
    }

    public static String copyFileToInternalStorage(Context context, Uri uri, String newDirName) {
        Uri returnUri = uri;

        Cursor returnCursor = context.getContentResolver().query(returnUri, new String[]{
                OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE
        }, null, null, null);

        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        String name = "Merged_" + AppUtils.getTimeStamp() + ".mp4";
        String size = (Long.toString(returnCursor.getLong(sizeIndex)));

        String videoPath = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            videoPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
        } else {
            videoPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        }

        File dir = new File(videoPath + "/" + newDirName);
        if (!dir.exists()) {
            dir.mkdir();
        }

        File output = new File(videoPath + "/" + newDirName + "/" + name);

        try {

            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(output);
            int read = 0;
            int bufferSize = 1024;
            final byte[] buffers = new byte[bufferSize];
            while ((read = inputStream.read(buffers)) != -1) {
                outputStream.write(buffers, 0, read);
            }

            inputStream.close();
            outputStream.close();

        }
        catch (Exception e) {

            Log.e("Exception", e.getMessage());
        }

        return output.getPath();
    }

    private int getDuration(String filePath) {

        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(filePath);
        String durationStr = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        Log.e("getDuration", "getDuration: "+durationStr );

        if (durationStr!=null){
            long d = Long.parseLong(durationStr) / 1000;
            int duration = (int)d;
            return duration;
        }else {
            return 0;
        }


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

    // TODO FFMPEG Methods
//
//    private void mergeVideos(){
//
//        String[] command = getVideoMergeCommand();
//
//        rl_progress.setVisibility(View.VISIBLE);
//
//        if(videoSegmentNames.size()==1){
//            txt_message.setText("Segments scaling is in progress. Please be patient as this may take a few minutes.");
//        } else {
//            txt_message.setText("Segments merging is in progress. Please be patient as this may take a few minutes.");
//        }
//
//        Config.enableStatisticsCallback(new StatisticsCallback() {
//            @Override
//            public void apply(Statistics statistics) {
//                float progress = Float.parseFloat(String.valueOf(statistics.getTime())) / totalDuration;
//                float progressFinal = progress * 100;
//                Log.d("Merge Progress", "Progress : " + progressFinal);
//            }
//        });
//
//        FFmpeg.executeAsync(command, new ExecuteCallback() {
//
//            @Override
//            public void apply(final long executionId, final int returnCode) {
//
//                rl_progress.setVisibility(View.GONE);
//
//                if (returnCode == RETURN_CODE_SUCCESS) {
//                    loadVideoPlayer();
//                    txt_actionbar_more.setEnabled(true);
//                } else if (returnCode == RETURN_CODE_CANCEL) {
//                    showToast(getString(R.string.api_error));
//                    txt_actionbar_more.setEnabled(false);
//                } else {
//                    showToast(getString(R.string.api_error));
//                    txt_actionbar_more.setEnabled(false);
//                }
//            }
//        });
//
//    }
//
//    private String[] getVideoMergeCommand(){
//
//        mergedFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/SalesCAM/" +
//                "Merged_" + AppUtils.getTimeStamp() + ".mp4";
//
//        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/SalesCAM");
//        if (!dir.exists()) {
//            dir.mkdirs();
//        }
//
//        ArrayList<String> parts = new ArrayList<>();
//
//        parts.add("-y");
//
//        for(int i=0; i<videoSegmentNames.size(); i++){
//
//            parts.add("-i");
//            parts.add(videoSegmentNames.get(i).getVideoPath());
//
//        }
//
//        parts.add("-filter_complex");
//
//        if(videoSegmentNames.size()==1){
//
//            parts.add("[0:v][0:a] concat=n=1:v=1:a=1 [outv][outa]");
//
//        } else {
//
//            String fadeAffectVideos = "";
//
//            for(int i=0; i<videoSegmentNames.size(); i++){
//
//                if(i==0){
//                    fadeAffectVideos = fadeAffectVideos + "[" + i + ":v]scale=1920:1080:force_original_aspect_ratio=decrease,pad=1920:1080:(ow-iw)/2:(oh-ih)/2,setsar=1[v" + i + "];";
//                } else{
//                    fadeAffectVideos = fadeAffectVideos + "[" + i + ":v]scale=1920:1080:force_original_aspect_ratio=decrease,pad=1920:1080:(ow-iw)/2:(oh-ih)/2,setsar=1,fade=type=in:duration=1[v" + i + "];";
//                }
//
//            }
//
//            String strVideoAudio = "";
//
//            for(int i=0; i<videoSegmentNames.size(); i++){
//                strVideoAudio = strVideoAudio + "[v" + i + "][" + i +":a]";
//            }
//
//            strVideoAudio = fadeAffectVideos + strVideoAudio + " concat=n=" + videoSegmentNames.size() + ":v=1:a=1 [outv][outa]";
//            parts.add(strVideoAudio);
//
//        }
//
//        parts.add("-map");      parts.add("[outv]");
//        parts.add("-map");      parts.add("[outa]");
//        parts.add("-ab");       parts.add("48000");
//        parts.add("-ac");       parts.add("2");
//        parts.add("-ar");       parts.add("22050");
//        parts.add("-vcodec");   parts.add("libx264");
//        parts.add("-vsync");   parts.add("0");
////        parts.add("-r");
////        parts.add("30");
////         parts.add("-s");
////         parts.add("1920:1080");
//
//        parts.add("-preset");   parts.add("ultrafast");
//        parts.add(mergedFilePath);
//
//        String[] command = new String[parts.size()];
//        for(int i=0; i<parts.size(); i++){
//            command[i] = parts.get(i);
//        }
//
//        return command;
//
//    }

    @Override
    protected void onDestroy() {
        try{ mediaUploadPresenter.detachView(); } catch (Exception e){}
        super.onDestroy();
    }

    @Override
    public void onSuccess_MediaUpload(String postId, String mediaUrl) {}

    @Override
    public void onUserBlocked(String message) {
        hideProgressDialog();
        showToast(message);
        getSessionManager().clearAllData();
        startActivity(new Intent(context, LoginActivity.class));
        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
        finishAffinity();
    }

    @Override
    public void onError(String message) {}



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

       // initVideo();

    }

    @Override
    public void onPermissionsCancelled(boolean isSettingsOpened) {

        if(isSettingsOpened){
            askPermissionsAgain = true;
        } else {
            askPermissions(permissions, permissionsInterface);
        }

    }

    private void checkPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            askPermissions(permissions, permissionsInterface);
        } else {
            isPermissionsGranted = true;
            if(getSessionManager().getAudioSource().equals("1")){  }

        }
    }


   void initVideo(){
       getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
       txt_actionbar_more.setEnabled(false);
        for(int i=0; i<videoSegmentNames.size(); i++){
            boolean containsAudio = isVideoHaveAudioTrack(videoSegmentNames.get(i).getVideoPath());
            videoSegmentNames.get(i).setContainsAudio(containsAudio);
            int duration = getDuration(videoSegmentNames.get(i).getVideoPath());
            videoSegmentNames.get(i).setLength(duration);
        }


       if(videoSegmentNames.size()==1){
           if(!videoSegmentNames.get(0).isVideoSelected()){
               renameFile(videoSegmentNames.get(0).getVideoPath());

           } else {
               mergedFilePath = videoSegmentNames.get(0).getVideoPath();
           }

       }

//        if(videoSegmentNames.size()==1){
//            if(!videoSegmentNames.get(0).isVideoSelected()){
//              //  copyFileToInternalStorage(this, Uri.parse(videoSegmentNames.get(0).getVideoPath()), "SalesCAM");
//           //     rename(Uri.parse(videoSegmentNames.get(0).getVideoPath()));
//                 renameFile(videoSegmentNames.get(0).getVideoPath());
//            } else {
//                mergedFilePath = videoSegmentNames.get(0).getVideoPath();
//            }
//
//        }

        for(int i=0; i< videoSegmentNames.size(); i++){
            totalDuration = totalDuration + videoSegmentNames.get(i).getLength();
        }

        if(videoSegmentNames.size()==1){

            mergedFilePath=videoSegmentNames.get(0).getVideoPath();


            new VideoCheckAsyncTask().execute();
        } else {
            new MergeAsyncTask().execute();
        }

//       if(videoSegmentNames.size()==1){
//
//           loadVideoPlayer();
//           txt_actionbar_more.setEnabled(true);
//       } else {
//           new MergeAsyncTask().execute();
//       }


       if(isInternetAvailable()){
            mediaUploadPresenter.updateAnalytics(
                    getSessionManager().getUserId(),
                    getSessionManager().getUserLocationId(),
                    "video",
                    getSessionManager().getUserType()
            );
        }

       if(isBluetoothHeadsetAvailable()){
           showToast("Please disconnect your Bluetooth mic so you can listen to the audio on this video preview.");
       }
    }

    private class MergeAsyncTask extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progress_text.setVisibility(View.VISIBLE);
            progress_text.setText("Video Merging Now In Process");
       //     showProgressDialog("Segments merging is in progress...");
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            Log.e("onProgressUpdate", "onProgressUpdate: "+values[0] );
            Log.e("onProgressUpdate", "onProgressUpdate111: "+values );
            circular_progress.setProgress(Integer.parseInt(values[0]));
            progress_tv.setText(values[0]+"%");

        }

        @Override
        protected String doInBackground(String... strings) {

            try {
                publishProgress(String.valueOf(0));
                List<Movie> movies = new LinkedList<Movie>();

                for(int i=0; i<videoSegmentNames.size(); i++){
                    Movie m = MovieCreator.build(videoSegmentNames.get(i).getVideoPath());
                    movies.add(m);
                }

                List<Track> videoTracks = new LinkedList<Track>();
                List<Track> audioTracks = new LinkedList<Track>();
                for (Movie m : movies) {
                    for (Track track : m.getTracks()) {
                        if (track.getHandler().equals("vide")) {
                            videoTracks.add(track);
                        }
                        if (track.getHandler().equals("soun")) {
                            audioTracks.add(track);
                        }

                    }
                }
                publishProgress(String.valueOf(30));
                Movie concatMovie = new Movie();
                concatMovie.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
                concatMovie.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
                publishProgress(String.valueOf(50));
                Container out2 = new DefaultMp4Builder().build(concatMovie);
                publishProgress(String.valueOf(80));

                mergedFilePath = baseVideoPath + "/" + "Merged_" + AppUtils.getTimeStamp() + ".mp4";

                FileChannel fc = new RandomAccessFile(mergedFilePath, "rw").getChannel();
                fc.position(0);
                out2.writeContainer(fc);
                fc.close();
                publishProgress(String.valueOf(90));

            } catch (Exception e){
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //hideProgressDialog();

            isMerged=true;
            deleteAllSegments();

            new VideoCheckAsyncTask().execute();
        }
    }

    private class VideoCheckAsyncTask extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            main_layout.setVisibility(View.GONE);
            progess_layout.setVisibility(View.VISIBLE);
//            if(videoSegmentNames.size()==1){
//                txt_message.setText("Segments scaling is in progress. Please be patient as this may take a few minutes.");
//            } else {
//                txt_message.setText("Segments merging is in progress. Please be patient as this may take a few minutes.");
//            }
//
//            rl_progress.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... strings) {

            boolean isResolutionOK = false, isFrameRateOK = false;
            if (!isMerged){
                circular_progress.setProgress(0);
            }

            try {

                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(mergedFilePath);
                height = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                width = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));

                if(height==720 && width==1280){
                    isResolutionOK = true;
                }

                if (!isMerged){
                    circular_progress.setProgress(50);
                }
                MediaExtractor extractor = new MediaExtractor();
                extractor.setDataSource(mergedFilePath);
                int numTracks = extractor.getTrackCount();
                for (int j = 0; j < numTracks; ++j) {
                    MediaFormat format = extractor.getTrackFormat(j);
                    String mime = format.getString(MediaFormat.KEY_MIME);
                    if (mime.startsWith("video/")) {
                        if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                            frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
                        }
                    }
                }
                if (!isMerged){
                    circular_progress.setProgress(80);
                }

                if(frameRate==30){
                    isFrameRateOK = true;
                }
                circular_progress.setProgress(100);

            }catch (Exception e){}

            if(!isResolutionOK || !isFrameRateOK){
                isScalingRequired = "1";
            } else {
                isScalingRequired = "0";
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
       //     rl_progress.setVisibility(View.GONE);

            main_layout.setVisibility(View.VISIBLE);
            progess_layout.setVisibility(View.GONE);
            loadVideoPlayer();
        }
    }

//    void showDialog(){
//        Dialog dialog = new Dialog(context);
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//        dialog.setContentView(R.layout.dialog_intro);
//        dialog.setCanceledOnTouchOutside(true);
//        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//        dialog.setCancelable(true);
//
////        TextView txt_message = dialog.findViewById(R.id.txt_message);
////
////        try {
////            SessionManager sessionManager = new SessionManager(context);
////            UploadModel uploadModel = sessionManager.getUploadData();
////            if (null == uploadModel.getTimeStamp()) {
////                txt_message.setText("Would you like to Logout?");
////            } else {
////                txt_message.setText("Your video is being uploaded. Logging out of the application would cancel the video upload. Still want to log out?");
////            }
////        } catch (Exception e){}
//
//        Button btn_yes = dialog.findViewById(R.id.btn_yes);
//        btn_yes.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//
////                try{ context.stopService(new Intent(context, UploadService.class)); } catch (Exception e){}
////                try{ context.stopService(new Intent(context, CompressUploadService.class)); } catch (Exception e){}
//
//                new SessionManager(context).clearAllData();
//                context.startActivity(new Intent(context, LoginActivity.class));
//                context.overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
//                context.finishAffinity();
//                dialog.dismiss();
//            }
//
//        });
//
//        Button btn_no = dialog.findViewById(R.id.btn_no);
//        btn_no.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                dialog.dismiss();
//            }
//        });
//    }

    private void deleteAllSegments(){

        try{

            String baseVideoPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/ServiceCAM";

            File directory = new File(baseVideoPath);
            if(directory.exists()){

                File[] files = directory.listFiles();
                for (int i = 0; i < files.length; i++){
                    if(files[i].getName().startsWith("Seg")){
                        files[i].delete();
                    }
                }

            }

        } catch (Exception e){

        }

    }

//    private void mergeVideos(){
//
//        String[] command = getVideoMergeCommand();
//
//        rl_progress.setVisibility(View.VISIBLE);
//
//        if(videoSegmentNames.size()==1){
//            txt_message.setText("Segments scaling is in progress. Please be patient as this may take a few minutes.");
//        } else {
//            txt_message.setText("Segments merging is in progress. Please be patient as this may take a few minutes.");
//        }
//
//        Config.enableStatisticsCallback(new StatisticsCallback() {
//            @Override
//            public void apply(Statistics statistics) {
//                float progress = Float.parseFloat(String.valueOf(statistics.getTime())) / totalDuration;
//                float progressFinal = progress * 100;
//                Log.d("Merge Progress", "Progress : " + progressFinal);
//            }
//        });
//
//        FFmpeg.executeAsync(command, new ExecuteCallback() {
//
//            @Override
//            public void apply(final long executionId, final int returnCode) {
//
//
//                Log.e("apply", "apply: "+returnCode+" "+executionId );
//                rl_progress.setVisibility(View.GONE);
//
//                if (returnCode == RETURN_CODE_SUCCESS) {
//                    loadVideoPlayer();
//                    txt_actionbar_more.setEnabled(true);
//                } else if (returnCode == RETURN_CODE_CANCEL) {
//                    showToast(getString(R.string.api_error));
//                    txt_actionbar_more.setEnabled(false);
//                } else {
//                    showToast(getString(R.string.api_error));
//                    txt_actionbar_more.setEnabled(false);
//                }
//            }
//        });
//
//    }

    private String[] getVideoMergeCommand(){

        mergedFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/SalesCAM/" +
                "Merged_" + AppUtils.getTimeStamp() + ".mp4";

        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/SalesCAM");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        ArrayList<String> parts = new ArrayList<>();

        parts.add("-y");

        for(int i=0; i<videoSegmentNames.size(); i++){

            parts.add("-i");
            parts.add(videoSegmentNames.get(i).getVideoPath());

        }

        parts.add("-filter_complex");

        if(videoSegmentNames.size()==1){

            parts.add("[0:v][0:a] concat=n=1:v=1:a=1 [outv][outa]");

        }
        else {

            String fadeAffectVideos = "";

            for(int i=0; i<videoSegmentNames.size(); i++){

                if(i==0){
                    fadeAffectVideos = fadeAffectVideos + "[" + i + ":v]scale=1920:1080:force_original_aspect_ratio=decrease,pad=1920:1080:(ow-iw)/2:(oh-ih)/2,setsar=1[v" + i + "];";
                } else{
                    fadeAffectVideos = fadeAffectVideos + "[" + i + ":v]scale=1920:1080:force_original_aspect_ratio=decrease,pad=1920:1080:(ow-iw)/2:(oh-ih)/2,setsar=1,fade=type=in:duration=1[v" + i + "];";
                }


            }

            String strVideoAudio = "";

            for(int i=0; i<videoSegmentNames.size(); i++){
                strVideoAudio = strVideoAudio + "[v" + i + "][" + i +":a]";
            }

            strVideoAudio = fadeAffectVideos + strVideoAudio + " concat=n=" + videoSegmentNames.size() + ":v=1:a=1 [outv][outa]";
            parts.add(strVideoAudio);

        }

        parts.add("-map");      parts.add("[outv]");
        parts.add("-map");      parts.add("[outa]");
        parts.add("-ab");       parts.add("48000");
        parts.add("-ac");       parts.add("2");
        parts.add("-ar");       parts.add("22050");
        parts.add("-vcodec");   parts.add("libx264");
        parts.add("-vsync");   parts.add("0");
//        parts.add("-r");
//        parts.add("30");
//         parts.add("-s");
//         parts.add("1920:1080");

        parts.add("-preset");   parts.add("ultrafast");
        parts.add(mergedFilePath);

        String[] command = new String[parts.size()];
        for(int i=0; i<parts.size(); i++){
            command[i] = parts.get(i);
        }

        return command;

    }



}
