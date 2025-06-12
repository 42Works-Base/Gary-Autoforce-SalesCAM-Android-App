package com.autoforcecam.app.utils;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.ListenableWorker;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.autoforcecam.app.R;
import com.autoforcecam.app.models.UploadModel;
import com.autoforcecam.app.models.VideoSegmentModel;
import com.autoforcecam.app.responses.ShotResponse;
import com.autoforcecam.app.responses.SuccessResponse;
import com.autoforcecam.app.retrofit.APIClient;
import com.autoforcecam.app.retrofit.APIInterface;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jakewharton.retrofit2.adapter.rxjava2.HttpException;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import kotlin.jvm.internal.Intrinsics;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public class MediaWorker extends Worker {

    public static final String CHANNEL_ID = "SalesCAMServiceChannel";
    private String defaultErrorMessage = "Something went wrong. Please try again.";
    private SessionManager sessionManager;
    private UploadModel uploadModel;
    private ArrayList<VideoSegmentModel> videoSegmentNames = new ArrayList<>();
    String shotJson = "", shotId = "";
    private Context context;


    public MediaWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context=context;
    }

    @SuppressLint("RestrictedApi")
    @NonNull
    @Override
    public Result doWork() {

        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Background Service")
                .setContentText("App is running a background service.")
                .setSmallIcon(R.drawable.ic_notify_small)
                .build();


        sessionManager = new SessionManager(context);

        uploadModel = sessionManager.getUploadData();

        if (null != uploadModel.getTimeStamp()) {

            if (uploadModel.getUploadError().equals("1")) {
                sendUserNotification("4");
            } else {
                sendUserNotification("1");
            }

            sessionManager.setUploadErrorStatus("0");

            if(uploadModel.getMergedS3SegmentPath().isEmpty()){

                uploadToFirebaseStorage();

            } else {

                shotJson = uploadModel.getShotJson();
                shotId = uploadModel.getShotId();

                if(shotJson.isEmpty()){
                    createShotJson();
                }

                if(shotId.isEmpty()){
                    hitShotStackAPI();
                } else {
                    checkDataToSend();
                }

            }

        }
        else {
            WorkManager.getInstance(context).cancelAllWork();

        }

        return new Result.Success();
    }




    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "ServiceCAM Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager =  (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(serviceChannel);
        }
    }


    /*
    Step 3 - Upload To Firebase Storage
    */
    private void uploadToFirebaseStorage(){

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        Log.e("videoSegmentNames serivce", "onClick: "+uploadModel.getMergedFilePath() );

        String fileName = "Android_" + AppUtils.get5DigitsRandom() + "_" + AppUtils.getTimeStamp();
        Uri file = Uri.fromFile(new File(uploadModel.getMergedFilePath()));
        StorageReference riversRef = storageRef.child("salescam/video/" +  fileName + ".mp4");
        UploadTask uploadTask = riversRef.putFile(file);

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                sendErrorNotification();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                Task<Uri> task = taskSnapshot.getMetadata().getReference().getDownloadUrl();
                task.addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        String videoLink = uri.toString();
                        uploadModel.setMergedS3SegmentPath(videoLink);
                        sessionManager.setUploadData(uploadModel);

                        shotJson = uploadModel.getShotJson();
                        shotId = uploadModel.getShotId();
                        if(shotJson.isEmpty()){ createShotJson(); }
                        if(shotId.isEmpty()){ hitShotStackAPI(); } else { checkDataToSend(); }

                    }
                });

            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {

                double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                Log.e( "onProgress: ", "Upload is " + progress + "% done");

                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("progress").putExtra("progress", progress));


            }
        });

    }



    private void createShotJson(){

        int totalSegmentsLength = uploadModel.getMergeTotalDuration();

        JsonObject parentObject = new JsonObject();

        // Server Callback
        parentObject.addProperty("callback", APIClient.SHOT_CALLBACK);

        // Disk
        parentObject.addProperty("disk", "mount");

        // Instance to support output upto 2GB
        parentObject.addProperty("instance", "a1");

        // Output Configuration
        JsonObject outputObject = new JsonObject();
        outputObject.addProperty("format", "mp4");
        if(uploadModel.isCompressionRequired()){
            outputObject.addProperty("resolution", "hd");
            outputObject.addProperty("scaleTo", "hd");
        } else {
            outputObject.addProperty("resolution", "1080");
//            outputObject.addProperty("scaleTo", "1080");
        }
        outputObject.addProperty("aspectRatio", "16:9");
        outputObject.addProperty("fps", 30);

        parentObject.add("output", outputObject);

        // TimeLine
        JsonObject timelineObject = new JsonObject();
        timelineObject.addProperty("background", "#000000");

        // Tracks
        JsonArray tracksArray = new JsonArray();

        // Track Object >>> Overlay
        JsonObject overlayTrackObject = new JsonObject();

        JsonArray overlayClipsArray = new JsonArray();

        JsonObject overlayClipObject = new JsonObject();
        overlayClipObject.addProperty("start", uploadModel.getSelectedIntroLength());
        overlayClipObject.addProperty("length", (uploadModel.getSelectedIntroLength() + totalSegmentsLength));
        overlayClipObject.addProperty("fit", "cover");
        overlayClipObject.addProperty("position", "bottom");

        JsonObject overlayAssetObject = new JsonObject();
        overlayAssetObject.addProperty("type", "image");
        overlayAssetObject.addProperty("src", uploadModel.getSelectedOverlayUrl());

        overlayClipObject.add("asset", overlayAssetObject);

        overlayClipsArray.add(overlayClipObject);

        overlayTrackObject.add("clips", overlayClipsArray);

        tracksArray.add(overlayTrackObject);

        // Track Object >>> Intro, Captured Segments and Outro
        JsonObject videoTrackObject = new JsonObject();

        JsonArray videoClipsArray = new JsonArray();

        // Intro Video
        if(uploadModel.getSelectedIntroLength()!=0){

            JsonObject introClipObject = new JsonObject();
            introClipObject.addProperty("start", 0);
            introClipObject.addProperty("length", uploadModel.getSelectedIntroLength());
            introClipObject.addProperty("fit", "contain");

            JsonObject introAssetObject = new JsonObject();
            introAssetObject.addProperty("type", "video");
            introAssetObject.addProperty("src", uploadModel.getSelectedIntroUrl());
            if(uploadModel.getSelectedIntroContainsAudio().equals("1")){
                introAssetObject.addProperty("volume", 1);
            }

            introClipObject.add("asset", introAssetObject);
            videoClipsArray.add(introClipObject);

        }

        // Merged File Path
        int mergedStartPoint = uploadModel.getSelectedIntroLength();
        JsonObject segmentClipObject = new JsonObject();
        segmentClipObject.addProperty("start", mergedStartPoint);
        segmentClipObject.addProperty("length", uploadModel.getMergeTotalDuration());
        segmentClipObject.addProperty("fit", "contain");

        JsonObject segmentAssetObject = new JsonObject();
        segmentAssetObject.addProperty("type", "video");
        segmentAssetObject.addProperty("src", uploadModel.getMergedS3SegmentPath());
        if(uploadModel.isMergedFileContainsAudio()){
            segmentAssetObject.addProperty("volume", 1);
        }

        segmentClipObject.add("asset", segmentAssetObject);
        videoClipsArray.add(segmentClipObject);

        // Outro Video
        if(uploadModel.getSelectedOutroLength()!=0){

            JsonObject outroClipObject = new JsonObject();
            outroClipObject.addProperty("start", (uploadModel.getSelectedIntroLength() + totalSegmentsLength));
            outroClipObject.addProperty("length", uploadModel.getSelectedOutroLength());
            outroClipObject.addProperty("fit", "contain");

            JsonObject outroAssetObject = new JsonObject();
            outroAssetObject.addProperty("type", "video");
            outroAssetObject.addProperty("src", uploadModel.getSelectedOutroUrl());
            if(uploadModel.getSelectedOutroContainsAudio().equals("1")){
                outroAssetObject.addProperty("volume", 1);
            }

            outroClipObject.add("asset", outroAssetObject);
            videoClipsArray.add(outroClipObject);

        }

        videoTrackObject.add("clips", videoClipsArray);

        tracksArray.add(videoTrackObject);

        if(uploadModel.getSelectedMusicLength()!=0){

            int startPointBGMusic = 0, endPointBGMusic = 0;

            if(uploadModel.getSelectedIntroLength()==0){
                startPointBGMusic = 0;
            } else {
                if(uploadModel.getSelectedIntroContainsAudio().equals("1")){
                    startPointBGMusic = uploadModel.getSelectedIntroLength();
                } else {
                    startPointBGMusic = 0;
                }
            }

            if(uploadModel.getSelectedOutroLength()==0){
                endPointBGMusic = uploadModel.getSelectedIntroLength() + totalSegmentsLength;
            } else {
                if(uploadModel.getSelectedOutroContainsAudio().equals("1")){
                    endPointBGMusic = uploadModel.getSelectedIntroLength() + totalSegmentsLength;
                } else {
                    endPointBGMusic = uploadModel.getSelectedIntroLength() + totalSegmentsLength + uploadModel.getSelectedOutroLength();
                }
            }

            int totalLengthPlayBGMusic = endPointBGMusic - startPointBGMusic;

            // Track Object >>> BGMusic
            JsonObject musicTrackObject = new JsonObject();

            JsonArray musicClipsArray = new JsonArray();

            if(uploadModel.getSelectedMusicLength()>totalLengthPlayBGMusic){

                JsonObject musicClipObject = new JsonObject();
                musicClipObject.addProperty("start", startPointBGMusic);
                musicClipObject.addProperty("length", endPointBGMusic);

                JsonObject musicAssetObject = new JsonObject();
                musicAssetObject.addProperty("type", "audio");
                musicAssetObject.addProperty("src", uploadModel.getSelectedMusicUrl());
                musicAssetObject.addProperty("volume", 0.2);

                musicClipObject.add("asset", musicAssetObject);

                musicClipsArray.add(musicClipObject);
                musicTrackObject.add("clips", musicClipsArray);

            } else {

                int quotient = totalLengthPlayBGMusic / uploadModel.getSelectedMusicLength();
                int remainder = totalLengthPlayBGMusic % uploadModel.getSelectedMusicLength();

                for(int i=0; i<quotient; i++){

                    int startPoint = startPointBGMusic + (i*uploadModel.getSelectedMusicLength());

                    JsonObject musicClipObject = new JsonObject();
                    musicClipObject.addProperty("start", startPoint);
                    musicClipObject.addProperty("length", uploadModel.getSelectedMusicLength());

                    JsonObject musicAssetObject = new JsonObject();
                    musicAssetObject.addProperty("type", "audio");
                    musicAssetObject.addProperty("src", uploadModel.getSelectedMusicUrl());
                    musicAssetObject.addProperty("volume", 0.2);

                    musicClipObject.add("asset", musicAssetObject);

                    musicClipsArray.add(musicClipObject);

                }

                if(remainder!=0){

                    int startPoint = startPointBGMusic + (quotient*uploadModel.getSelectedMusicLength());

                    JsonObject musicClipObject = new JsonObject();
                    musicClipObject.addProperty("start", startPoint);
                    musicClipObject.addProperty("length", remainder);

                    JsonObject musicAssetObject = new JsonObject();
                    musicAssetObject.addProperty("type", "audio");
                    musicAssetObject.addProperty("src", uploadModel.getSelectedMusicUrl());
                    musicAssetObject.addProperty("volume", 0.2);

                    musicClipObject.add("asset", musicAssetObject);

                    musicClipsArray.add(musicClipObject);

                }

                musicTrackObject.add("clips", musicClipsArray);

            }

            tracksArray.add(musicTrackObject);

        }

        timelineObject.add("tracks", tracksArray);

        parentObject.add("timeline", timelineObject);

        shotJson = parentObject.toString();
        uploadModel.setShotJson(shotJson);
        sessionManager.setUploadData(uploadModel);

    }


    private void sendErrorNotification(){
        sessionManager.setUploadErrorStatus("1");
        sendUserNotification("2");
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("refreshVideoUpload"));
        WorkManager.getInstance(context).cancelAllWork();
    }



    /* Notifications */

    private void sendUserNotification(String status){

        SessionManager sessionManager = new SessionManager(context);

        if(sessionManager.isUserLoggedIn()) {

            int counter = sessionManager.getUploadCount();

            boolean skipNotifications = false;

            if(counter==0){
                skipNotifications = false;
            } else {
                if(status.equals("1") || status.equals("3") || status.equals("5")){
                    skipNotifications = false;
                } else {
                    skipNotifications = true;
                }
            }

            if(skipNotifications){

                //Do Nothing

            } else {

                String title = "Sales CAM";
                String message = "";
                int notifyId = 0;

                switch (status) {

                    case "1":
                        message = "Your video is now uploading. You will be notified when the upload has been completed.";
                        notifyId = 1000001;
                        break;

                    case "2":
                        message = "Your video failed to upload. Please retry.";
                        notifyId = 1000001;
                        break;

                    case "3":
                        message = "Your video has been successfully uploaded.";
                        notifyId = new Random().nextInt(99999);
                        break;

                    case "4":
                        sessionManager.increaseUploadCount();
                        message = "Your previous video didn't upload. Retrying to upload it.";
                        notifyId = 1000001;
                        break;

                    case "5":
                        message = "Upload Complete.";
                        notifyId = new Random().nextInt(99999);
                        break;

                }




                Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                NotificationCompat.Builder notificationBuilder =
                        new NotificationCompat.Builder(context, CHANNEL_ID)
                                .setContentTitle(title)
                                .setContentText(message)
                                .setAutoCancel(true)
                                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                                .setSound(defaultSoundUri);


                notificationBuilder.setColor(context.getResources().getColor(R.color.colorPrimary));
                notificationBuilder.setSmallIcon(R.drawable.ic_notify_small);

                NotificationManager notificationManager =
                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                // Since android Oreo notification channel is needed.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel(CHANNEL_ID, title, NotificationManager.IMPORTANCE_DEFAULT);
                    notificationManager.createNotificationChannel(channel);
                }

                notificationManager.notify(notifyId, notificationBuilder.build());



            }

        }

    }

    /* Shot Stack API */
    public void hitShotStackAPI(){

        if(new NetworkDetector(context).isInternetAvailable()) {

            CompositeDisposable compositeDisposable = new CompositeDisposable();
            APIInterface apiInterface = APIClient.getShotClient(context).create(APIInterface.class);

            RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), shotJson);

            compositeDisposable.add(
                    apiInterface
                            .processVideoData(body)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeWith(
                                    new DisposableSingleObserver<ShotResponse>() {

                                        @Override
                                        public void onSuccess(ShotResponse response) {

                                            if(response.getResponse().getId().isEmpty()){
                                                sendErrorNotification();
                                            } else {
                                                shotId = response.getResponse().getId();
                                                uploadModel.setShotId(shotId);
                                                sessionManager.setUploadData(uploadModel);
                                                checkDataToSend();
                                            }

                                        }

                                        @Override
                                        public void onError(Throwable throwable) {
                                            sendErrorNotification();
                                        }

                                    }
                            )
            );

        } else {
            sendErrorNotification();
        }

    }



    /* Our Server API */
    private void checkDataToSend(){

        String awsTempUrls = "";

        for(int i=0; i<videoSegmentNames.size(); i++){
            if(awsTempUrls.isEmpty()){
                awsTempUrls = videoSegmentNames.get(i).getS3SegmentPath();
            } else {
                awsTempUrls = awsTempUrls + "," + videoSegmentNames.get(i).getS3SegmentPath();
            }
        }

        if(new NetworkDetector(context).isInternetAvailable()) {

            CompositeDisposable compositeDisposable = new CompositeDisposable();
            APIInterface apiInterface = APIClient.getClient(context).create(APIInterface.class);
            int dolby=1;
            if (uploadModel.isDolbyRequired()){
                dolby=1;
            }else {
                dolby=0;
            }

            compositeDisposable.add(
                    apiInterface
                            .saveShotData(
                                    uploadModel.getPhone(),
                                    uploadModel.getEmail(),
                                    uploadModel.getYear(),
                                    uploadModel.getShareVideo(),
                                    uploadModel.getUserType(),
                                    uploadModel.getName(),
                                    uploadModel.getTitle(),
                                    uploadModel.getUserId(),
                                    uploadModel.getLocationId(),
                                    uploadModel.getDescription(),
                                    uploadModel.getCustomerName(),
                                    uploadModel.getTags(),
                                    uploadModel.getShotId(),
                                    "Android",
                                    sessionManager.getFCMToken(),
                                    uploadModel.getShowReview(),
                                    awsTempUrls,
                                    uploadModel.getSmsBody(),
                                    uploadModel.getEmailBody(),
                                    dolby)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeWith(
                                    new DisposableSingleObserver<SuccessResponse>() {

                                        @Override
                                        public void onSuccess(SuccessResponse response) {
                                            sessionManager.setSelectedOverlayUrl(sessionManager.getUploadData().getSelectedOverlayUId());
                                            sessionManager.clearUploadData();
                                            sendUserNotification("5");
                                            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("complete").putExtra("complete", "complete"));
                                            WorkManager.getInstance(context).cancelAllWork();

                                        }

                                        @Override
                                        public void onError(Throwable throwable) {

                                            String isBlocked = isUserBlocked(throwable);
                                            String[] strArray = isBlocked.split("---");
                                            if(strArray[0].equals("1")){
                                                sessionManager.setUploadErrorStatus("1");
                                                sendUserNotification("2");
                                                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("userBlocked").putExtra("message", strArray[1]));
                                                WorkManager.getInstance(context).cancelAllWork();

                                            } else {
                                                sendErrorNotification();
                                            }

                                        }

                                    }
                            )
            );

        } else {
            sendErrorNotification();
        }

    }

    /* Check if User is Blocked */
    public String isUserBlocked(Throwable throwable) {

        if (throwable instanceof HttpException) {
            try {
                HttpException exception = (HttpException) throwable;
                JSONObject jObjError = new JSONObject(exception.response().errorBody().string());
                String strMessage = jObjError.getString("message");
                String strBlocked = "0";
                if(jObjError.has("is_sleep_mode")){
                    boolean isBlocked = jObjError.getBoolean("is_sleep_mode");
                    if(isBlocked){ strBlocked = "1"; } else { strBlocked = "0"; }
                }
                return strBlocked + "---" + strMessage;
            }catch (Exception e){
                return "0---" + defaultErrorMessage;
            }
        }else {
            return "0---" + defaultErrorMessage;
        }

    }


}