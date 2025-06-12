package com.autoforcecam.app.utils;


import android.content.Context;
import android.content.SharedPreferences;

import com.autoforcecam.app.models.UploadModel;
import com.autoforcecam.app.responses.DefaultResolution;
import com.autoforcecam.app.responses.LoginResponse;
import com.autoforcecam.app.responses.Overlays;
import com.autoforcecam.app.responses.PreformattedMessages;
import com.autoforcecam.app.responses.VideoOptions;
import com.autoforcecam.app.responses.VideoType;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

/* Created by JSP@nesar */

public class SessionManager {

    SharedPreferences pref, tempPref;
    SharedPreferences.Editor editor, tempEditor;
    Context context;
    Gson gson = new Gson();

    int PRIVATE_MODE = 0;

    private static final String PREF_NAME = "AutoForceCAM";
    private static final String TEMP_PREF_NAME = "TempDataAutoForceCAM";

    private static final String FirstTime = "FirstTime";

    // User Data

    private static final String IS_LOGGEDIN = "IsLoggedIn";
    private static final String USER_CODE = "UserCode";
    private static final String USER_LOCATIONID = "UserLocationId";
    private static final String USER_NAME = "UserName";
    private static final String USER_ID = "UserID";
    private static final String USER_TYPE = "UserType";
    private static final String FCM_TOKEN = "FCMToken";
    private static final String SelectedOverlayUrl = "SelectedOverlayUrl";
    private static final String DefaultIntro = "DefaultIntro";
    private static final String DefaultOutro = "DefaultOutro";
    private static final String DefaultMusic = "DefaultMusic";
    private static final String Compression = "Compression";

    private static final String Dolby = "Dolby";

    // How to use

    private static final String INSTRUCTION1 = "Instruction1";
    private static final String INSTRUCTION2 = "Instruction2";

    private static final String IMAGE_INSTRUCTIONS = "ImageInstructions";
    private static final String VIDEO_INSTRUCTIONS = "VideoInstructions";
    private static final String IMPORT_INSTRUCTIONS = "ImportInstructions";

    // Dimensions

    private static final String DEFAULT_RESOLUTION = "DefaultResolution";
    private static final String SCREEN_WIDTH = "ScreenWidth";
    private static final String SCREEN_HEIGHT = "ScreenHeight";
    private static final String WIDTH = "ForCameraFunctionality";
    private static final String WIDTH2 = "ForVideoFunctionality";

    // Dimensions

    private static final String SANDBOX_KEYS = "SandBoxKeys";
    private static final String LIVE_KEYS = "LiveKeys";

    // Overlays

    private static final String IMAGE_OVERLAY = "ImageOverlay";
    private static final String VIDEO_OVERLAY = "VideoOverlay";
    private static final String INTROS = "Intros";
    private static final String OUTROS = "Outros";
    private static final String BG_MUSIC = "BGMusic";

    private static final String Video_Type = "Video_Type";
    private static final String PIC_POR_OVERLAY_ID = "PicPortraitOverlayID";
    private static final String PIC_LAN_OVERLAY_ID = "PicLandscapeOverlayID";
    private static final String VID_LAN_OVERLAY_ID = "VideoLandscapeOverlayID";

    // Preformatted message

    private static final String PREFORMATTED_MESSAGE = "PreformattedMessage";
    private static final String REVIEW_EMAIL = "ReviewEmail";
    private static final String MY_NAME = "MyName";

    // Pre Used Vimeo Links

    private static final String INTRO_LINK = "IntroLink";
    private static final String WALK_LINK = "WalkaroundLink";
    private static final String THANK_LINK = "ThankYouLink";

    // Others

    private static final String AUDIO_SOURCE = "AudioSource";
    private static final String TAPTOFOCUS = "TapToFocus";
    private static final String OVERLAYINS = "OverlayInstructions";
    private static final String UPLOAD_DATA = "UploadData";
    private static final String INCREMENT_COUNTER = "IncrementCounter";
    private static final String COUNTER_VALUE = "CounterValue";

    public SessionManager(Context context) {

        this.context = context;

        pref = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        tempPref = context.getSharedPreferences(TEMP_PREF_NAME, PRIVATE_MODE);

        editor = pref.edit();
        tempEditor = tempPref.edit();

    }

    public void clearAllData() {
        editor.clear().commit();
    }


   //>>>>>>>>>>>>


    public void setUserCode(String code) {
        editor.putString(USER_CODE, code);
        editor.commit();
    }

    public String getUserCode() {
        return pref.getString(USER_CODE, "");
    }


     //>>>>>>>>>>>>


    public boolean getFirstTime() {
        return pref.getBoolean(FirstTime, true);
    }


    public void setFirstTime(Boolean firstTime) {
        editor.putBoolean(FirstTime, firstTime);
        editor.commit();
    }

    public void setUploadData(UploadModel uploadModel) {
        String json = gson.toJson(uploadModel);
        editor.putString(UPLOAD_DATA, json);
        editor.commit();
    }

    public void clearUploadData() {
        String json = gson.toJson("");
        editor.putString(UPLOAD_DATA, json);
        editor.commit();
    }

    public UploadModel getUploadData() {
        Type type = new TypeToken<UploadModel>() {}.getType();
        UploadModel uploadModel = null;
        try{
            uploadModel = gson.fromJson(pref.getString(UPLOAD_DATA, null), type);
        } catch (Exception e){
            uploadModel = new UploadModel();
        }

        if(null==uploadModel){
            uploadModel = new UploadModel();
        }

        return uploadModel;
    }

    public void setUploadErrorStatus(String uploadError) {

        Type type = new TypeToken<UploadModel>() {}.getType();
        UploadModel uploadModel = gson.fromJson(pref.getString(UPLOAD_DATA, null), type);
        uploadModel.setUploadError(uploadError);

        String json = gson.toJson(uploadModel);
        editor.putString(UPLOAD_DATA, json);
        editor.commit();

    }


    //>>>>>>>>>>>>


    public int getUploadCount() {

        int count = 0;

        try{
            Type type = new TypeToken<UploadModel>() {}.getType();
            UploadModel uploadModel = gson.fromJson(pref.getString(UPLOAD_DATA, null), type);
            count = uploadModel.getRetryCount();
        } catch (Exception e){}

        return count;

    }


    public void clearUploadCount() {

        Type type = new TypeToken<UploadModel>() {}.getType();
        UploadModel uploadModel = gson.fromJson(pref.getString(UPLOAD_DATA, null), type);
        uploadModel.setRetryCount(0);

        String json = gson.toJson(uploadModel);
        editor.putString(UPLOAD_DATA, json);
        editor.commit();

    }


    public void increaseUploadCount() {

        Type type = new TypeToken<UploadModel>() {}.getType();
        UploadModel uploadModel = gson.fromJson(pref.getString(UPLOAD_DATA, null), type);
        int count = uploadModel.getRetryCount();
        count++;
        uploadModel.setRetryCount(count);

        String json = gson.toJson(uploadModel);
        editor.putString(UPLOAD_DATA, json);
        editor.commit();

    }



    //>>>>>>>>>>>>


    public void setUserLocationId(String locationId) {
        editor.putString(USER_LOCATIONID, locationId);
        editor.commit();
    }

    public String getUserLocationId() {
        return pref.getString(USER_LOCATIONID, "");
    }


     //>>>>>>>>>>>>

    // 0 = INTERNAL MICROPHONE
    // 1 = BLUETOOTH HEADSET

    public void setAudioSource(String audioSource) {
        editor.putString(AUDIO_SOURCE, audioSource);
        editor.commit();
    }

    public String getAudioSource() {
        return pref.getString(AUDIO_SOURCE, "0");
    }


     //>>>>>>>>>>>>


    public void setUserName(String username) {
        editor.putString(USER_NAME, username);
        editor.commit();
    }

    public String getUserName() {
        return pref.getString(USER_NAME, "");
    }

    public void setDefaultIntro(String defaultIntro) {
        editor.putString(DefaultIntro, defaultIntro);
        editor.commit();
    }

    public String getDefaultIntro() {
        return pref.getString(DefaultIntro, "");
    }

    public void setDefaultOutro(String defaultOutro) {
        editor.putString(DefaultOutro, defaultOutro);
        editor.commit();
    }

    public String getDefaultOutro() {
        return pref.getString(DefaultOutro, "");
    }

    public void setDefaultMusic(String defaultMusic) {
        editor.putString(DefaultMusic, defaultMusic);
        editor.commit();
    }

    public String getDefaultMusic() {
        return pref.getString(DefaultMusic, "");
    }

    public void compressionRequired(boolean defaultMusic) {
        editor.putBoolean(Compression, defaultMusic);
        editor.commit();
    }

    public boolean isCompressionRequired() {
        return pref.getBoolean(Compression, true);
    }


    public void dolbyRequired(boolean dolby) {
        editor.putBoolean(Dolby, dolby);
        editor.commit();
    }

    public boolean isDolbyRequired() {
        return pref.getBoolean(Dolby, true);
    }


    //>>>>>>>>>>>>


    public void setUserId(String userId) {
        editor.putString(USER_ID, userId);
        editor.commit();
    }

    public String getUserId() {
        return pref.getString(USER_ID, "");
    }


    //>>>>>>>>>>>>


    public void setUserType(String userType) {
        editor.putString(USER_TYPE, userType);
        editor.commit();
    }

    public String getUserType() {
        return pref.getString(USER_TYPE, "");
    }

    public void setSelectedOverlayUrl(String url) {
        editor.putString(SelectedOverlayUrl, url);
        editor.commit();
    }

    public String getSelectedOverlayUrl() {
        return pref.getString(SelectedOverlayUrl, "");
    }


    //>>>>>>>>>>>>


    public void setUserLoginStatus(boolean loginStatus) {
        editor.putBoolean(IS_LOGGEDIN, loginStatus);
        editor.commit();
    }

    public boolean isUserLoggedIn() {
        return pref.getBoolean(IS_LOGGEDIN, false);
    }


    //>>>>>>>>>>>>


    public void setImageInstructionsStatus(boolean status) {
        tempEditor.putBoolean(IMAGE_INSTRUCTIONS, status);
        tempEditor.commit();
    }

    public boolean showImageInstructions() {
        return tempPref.getBoolean(IMAGE_INSTRUCTIONS, true);
    }


    //>>>>>>>>>>>>


    public void setIncrementCounter(Boolean status) {
        tempEditor.putBoolean(INCREMENT_COUNTER, status);
        tempEditor.commit();
    }

    public boolean incrementCounter() {
        return tempPref.getBoolean(INCREMENT_COUNTER, false);
    }


    //>>>>>>>>>>>>


    public void setCounterValue(int value) {
        tempEditor.putInt(COUNTER_VALUE, value);
        tempEditor.commit();
    }

    public int getCounterValue() {
        return tempPref.getInt(COUNTER_VALUE, 0);
    }


    //>>>>>>>>>>>>




    public void setFCMToken(String fcmToken) {
        tempEditor.putString(FCM_TOKEN, fcmToken);
        tempEditor.commit();
    }

    public String getFCMToken() {
        return tempPref.getString(FCM_TOKEN, "");
    }


    //>>>>>>>>>>>>


    public void setVideoInstructionsStatus(boolean status) {
        tempEditor.putBoolean(VIDEO_INSTRUCTIONS, status);
        tempEditor.commit();
    }

    public boolean showVideoInstructions() {
        return tempPref.getBoolean(VIDEO_INSTRUCTIONS, true);
    }



    //>>>>>>>>>>>>


    public void setImportInstructionsStatus(boolean status) {
        tempEditor.putBoolean(IMPORT_INSTRUCTIONS, status);
        tempEditor.commit();
    }

    public boolean showImportInstructions() {
        return tempPref.getBoolean(IMPORT_INSTRUCTIONS, true);
    }



    //>>>>>>>>>>>>


    public void setDontShowInstruction1(boolean status) {
        editor.putBoolean(INSTRUCTION1, status);
        editor.commit();
    }

    public boolean dontShowInstruction1() {
        return pref.getBoolean(INSTRUCTION1, false);
    }


    //>>>>>>>>>>>>


    public void setDontShowInstruction2(boolean status) {
        editor.putBoolean(INSTRUCTION2, status);
        editor.commit();
    }

    public boolean dontShowInstruction2() {
        return pref.getBoolean(INSTRUCTION2, false);
    }


    //>>>>>>>>>>>>


    public void setWidth(int width) {
        tempEditor.putInt(WIDTH, width);
        tempEditor.commit();
    }

    public int getWidth() {
        return tempPref.getInt(WIDTH, 0);
    }


    //>>>>>>>>>>>>


    public void setWidth2(int width) {
        tempEditor.putInt(WIDTH2, width);
        tempEditor.commit();
    }

    public int getWidth2() {
        return tempPref.getInt(WIDTH2, 0);
    }


    //>>>>>>>>>>>>


    public void setScreenWidth(int width) {
        tempEditor.putInt(SCREEN_WIDTH, width);
        tempEditor.commit();
    }

    public int getScreenWidth() {
        return tempPref.getInt(SCREEN_WIDTH, 0);
    }


    //>>>>>>>>>>>>


    public void setScreenHeight(int height) {
        tempEditor.putInt(SCREEN_HEIGHT, height);
        tempEditor.commit();
    }

    public int getScreenHeight() {
        return tempPref.getInt(SCREEN_HEIGHT, 0);
    }


    //>>>>>>>>>>>>


    public void setDefaultResolution(DefaultResolution defaultResolution) {
        String json = gson.toJson(defaultResolution);
        editor.putString(DEFAULT_RESOLUTION, json);
        editor.commit();
    }

    public DefaultResolution getDefaultResolution() {
        Type type = new TypeToken<DefaultResolution>() {}.getType();
        DefaultResolution  defaultResolution = gson.fromJson(pref.getString(DEFAULT_RESOLUTION, null), type);
        return defaultResolution;
    }


     //>>>>>>>>>>>>


    public void setSandBoxKeys(LoginResponse.Keys keys) {
        String json = gson.toJson(keys);
        editor.putString(SANDBOX_KEYS, json);
        editor.commit();
    }

    public LoginResponse.Keys getSandBoxKeys() {
        Type type = new TypeToken<LoginResponse.Keys>() {}.getType();
        LoginResponse.Keys keys = gson.fromJson(pref.getString(SANDBOX_KEYS, null), type);
        return keys;
    }


     //>>>>>>>>>>>>


    public void setLiveKeys(LoginResponse.Keys keys) {
        String json = gson.toJson(keys);
        editor.putString(LIVE_KEYS, json);
        editor.commit();
    }

    public LoginResponse.Keys getLiveKeys() {
        Type type = new TypeToken<LoginResponse.Keys>() {}.getType();
        LoginResponse.Keys keys = gson.fromJson(pref.getString(LIVE_KEYS, null), type);
        return keys;
    }


    //>>>>>>>>>>>>


    public void setImageOverlayList(ArrayList<Overlays> overlayList) {
        String json = gson.toJson(overlayList);
        editor.putString(IMAGE_OVERLAY, json);
        editor.commit();
    }

    public ArrayList<Overlays> getImageOverlayList() {
        Type type = new TypeToken<ArrayList<Overlays>>() {}.getType();
        ArrayList<Overlays> overlayList = gson.fromJson(pref.getString(IMAGE_OVERLAY, ""), type);
        if(null==overlayList){ overlayList = new ArrayList<>(); }
        return overlayList;
    }


    //>>>>>>>>>>>>




    public void setVideoOverlayList(ArrayList<Overlays> overlayList) {
        String json = gson.toJson(overlayList);
        editor.putString(VIDEO_OVERLAY, json);
        editor.commit();
    }

    public ArrayList<Overlays> getVideoOverlayList() {
        Type type = new TypeToken<ArrayList<Overlays>>() {}.getType();
        ArrayList<Overlays> overlayList = gson.fromJson(pref.getString(VIDEO_OVERLAY, ""), type);
        if(null==overlayList){ overlayList = new ArrayList<>(); }
        return overlayList;
    }


    //>>>>>>>>>>>>


    public void setPreformattedMessage(PreformattedMessages message) {
        String json = gson.toJson(message);
        editor.putString(PREFORMATTED_MESSAGE, json);
        editor.commit();
    }

    public PreformattedMessages getPreformattedMessage() {
        Type type = new TypeToken<PreformattedMessages>() {}.getType();
        PreformattedMessages message = gson.fromJson(pref.getString(PREFORMATTED_MESSAGE, ""), type);
        return message;
    }


    //>>>>>>>>>>>>


    public void setMyName(String name) {
        editor.putString(MY_NAME, name);
        editor.commit();
    }


    public String getMyName() {
        return pref.getString(MY_NAME, "");
    }


    //>>>>>>>>>>>>


    public void setIntroLink(String link) {
        editor.putString(INTRO_LINK, link);
        editor.commit();
    }


    public void setWalkLink(String link) {
        editor.putString(WALK_LINK, link);
        editor.commit();
    }


    public void setThankLink(String link) {
        editor.putString(THANK_LINK, link);
        editor.commit();
    }

    public String getIntroLink() {
        return pref.getString(INTRO_LINK, "");
    }

    public String getWalkLink() {
        return pref.getString(WALK_LINK, "");
    }

    public String getThankLink() {
        return pref.getString(THANK_LINK, "");
    }


    //>>>>>>>>>>>>


    public void setIntros(VideoOptions[] intros) {
        String json = gson.toJson(intros);
        editor.putString(INTROS, json);
        editor.commit();
    }

    public VideoOptions[] getIntros() {
        Type type = new TypeToken<VideoOptions[]>() {}.getType();
        VideoOptions[] intros = gson.fromJson(pref.getString(INTROS, ""), type);
        return intros;
    }


    //>>>>>>>>>>>>


    public void setOutros(VideoOptions[] outros) {
        String json = gson.toJson(outros);
        editor.putString(OUTROS, json);
        editor.commit();
    }

    public VideoOptions[] getOutros() {
        Type type = new TypeToken<VideoOptions[]>() {}.getType();
        VideoOptions[] outros = gson.fromJson(pref.getString(OUTROS, ""), type);
        return outros;
    }


    //>>>>>>>>>>>>


    public void setBgMusic(VideoOptions[] bgMusic) {
        String json = gson.toJson(bgMusic);
        editor.putString(BG_MUSIC, json);
        editor.commit();
    }

    public VideoOptions[] getBgMusic() {
        Type type = new TypeToken<VideoOptions[]>() {}.getType();
        VideoOptions[] bgMusic = gson.fromJson(pref.getString(BG_MUSIC, ""), type);
        return bgMusic;
    }


    //>>>>>>>>>>>>


    public void setVideoType(VideoType[] videoType) {
        String json = gson.toJson(videoType);
        editor.putString(Video_Type, json);
        editor.commit();
    }

    public VideoType[] getVideoType() {
        Type type = new TypeToken<VideoType[]>() {}.getType();
        VideoType[] videoType = gson.fromJson(pref.getString(Video_Type, ""), type);
        return videoType;
    }

    //>>>>>>>>>>>>


    public void setPicPorOverlayId(String Id) {
        editor.putString(PIC_POR_OVERLAY_ID, Id);
        editor.commit();
    }

    public String getPicPorOverlayId() {
        return pref.getString(PIC_POR_OVERLAY_ID, "");
    }


    //>>>>>>>>>>>>


    public void setPicLanOverlayId(String Id) {
        editor.putString(PIC_LAN_OVERLAY_ID, Id);
        editor.commit();
    }

    public String getPicLanOverlayId() {
        return pref.getString(PIC_LAN_OVERLAY_ID, "");
    }


    //>>>>>>>>>>>>


    public void setVidLanOverlayId(String Id) {
        editor.putString(VID_LAN_OVERLAY_ID, Id);
        editor.commit();
    }

    public String getVidLanOverlayId() {
        return pref.getString(VID_LAN_OVERLAY_ID, "");
    }


    //>>>>>>>>>>>>


    public void setTaptofocus(boolean status) {
        editor.putBoolean(TAPTOFOCUS, status);
        editor.commit();
    }

    public boolean getTapToFocus() {
        return pref.getBoolean(TAPTOFOCUS, true);
    }



    //>>>>>>>>>>>>


    public void setOverlayInstructionsStatus(boolean status) {
        editor.putBoolean(OVERLAYINS, status);
        editor.commit();
    }

    public boolean isOverlayInstructionsShown() {
        return pref.getBoolean(OVERLAYINS, false);
    }











}
