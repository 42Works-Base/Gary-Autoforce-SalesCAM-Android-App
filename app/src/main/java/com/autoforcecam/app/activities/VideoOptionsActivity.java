package com.autoforcecam.app.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.autoforcecam.app.R;
import com.autoforcecam.app.adapters.SpinnerArrayAdapter;
import com.autoforcecam.app.base.BaseActivity;
import com.autoforcecam.app.interfaces.PermissionsInterface;
import com.autoforcecam.app.models.UploadModel;
import com.autoforcecam.app.models.VideoSegmentModel;
import com.autoforcecam.app.responses.VideoOptions;
import com.autoforcecam.app.utils.AppUtils;
import com.autoforcecam.app.utils.MediaWorker;
import com.autoforcecam.app.utils.S3UploadService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hbb20.CountryCodePicker;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import io.michaelrocks.libphonenumber.android.PhoneNumberUtil;
import io.michaelrocks.libphonenumber.android.Phonenumber;

/* Created by JSP@nesar */

public class VideoOptionsActivity extends BaseActivity implements PermissionsInterface{

    private String  strEmails = "", strPhones = "",videoType="";

    RadioGroup radioGroup;
    RadioButton radioSMS,radioEmail;
    private boolean askPermissionsAgain = true;
    ActivityResultLauncher<Intent> startForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @SuppressLint("Range")
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK) {
                try {

                    String email="";

                    Uri uri = result.getData().getData();
                    Cursor cursor = getContentResolver().query(uri, null, null, null, null);

                    if (cursor.getCount() > 0) {
                        while (cursor.moveToNext()) {
                            String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                            Cursor cur1 = getContentResolver().query(
                                    ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
                                    ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                                    new String[]{id}, null);
                            while (cur1.moveToNext()) {
                                //to get the contact names
                                String name=cur1.getString(cur1.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                                Log.e("moveToNext Name :", name);
                                email = cur1.getString(cur1.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                                Log.e("moveToNext Email", email);

                               edt_email.setText(email);
                            }
                            cur1.close();
                        }
                    }
                    
                    cursor.moveToFirst();
                    if (cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) == 1) {
                        String id = String.valueOf(cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID)));

                        Cursor phoneCursor = VideoOptionsActivity.this.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);
                        while (phoneCursor.moveToNext()) {
                            String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            String normalizedPhoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER));
                            Log.v("onActivityResult", "phone # - " + phoneNumber);
                            PhoneNumberUtil phoneUtil = PhoneNumberUtil.createInstance(VideoOptionsActivity.this);

                            if (phoneNumber.startsWith("+")) {

                                Phonenumber.PhoneNumber ph = phoneUtil.parse(phoneNumber, null);

                                Log.v("onActivityResult", "normalized ph # - " + ph.getCountryCode() + " " + ph.getNationalNumber());

                                try {
                                    String defaultCountrycode = getSessionManager().getPreformattedMessage().getCc_suffix_iso();
                                    //  ccp.setDetectCountryWithAreaCode(ph.getCountryCode());
                                    ccp.setCountryForPhoneCode(ph.getCountryCode());
                                    // ccp.resetToDefaultCountry();
                                } catch (Exception e) {
                                }

                                edt_mobile.setText("" + ph.getNationalNumber());

                                int ii = phoneUtil.getCountryCodeForRegion(phoneNumber);

                                Log.v("onActivityResult", "normalized ii # - " + ii);
                            } else {

                                edt_mobile.setText(phoneNumber);
                            }

                            Log.v("onActivityResult", "normalized phone # - " + normalizedPhoneNumber);
                            String name = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));

                            Log.v("onActivityResult", "normalized name # - " + name);

                            edt_name.setText(name);


                            break;
                        }
                    }


                    if (email.isEmpty()){
                        edt_email.setText("");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    });

    private Collection<String> permissions = Arrays.asList(
            Manifest.permission.READ_CONTACTS);
    private PermissionsInterface permissionsInterface;
    LinearLayout defaultSettingsLayout,videoDetailLayout,introOutroLayout;
    private RelativeLayout rl_custom_intro, rl_custom_outro, rl_custom_music;
    private RadioGroup group_intro, group_outro, group_music, group_compress,group_dolby;

    LinearLayout mainLayout;
    private AppCompatRadioButton intro_custom, intro_default, intro_none, outro_custom,
                                    outro_default, outro_none, music_custom, music_default, music_none,compress_yes,compress_no,
            dolby_no,dolby_yes;
    private ArrayList<VideoOptions> introList, outroList, bgMusicList;
    private ArrayList<VideoSegmentModel> videoSegmentNames = new ArrayList<>();
    private AppCompatSpinner spinner_intro, spinner_outro, spinner_music,spinner_videoType;
    private int defaultIntroPosition = -1, defaultOutroPosition = -1, defaultBGMusicPosition = -1;
    private boolean allEmpty = false, isCompressionRequired = true, mergedFileContainsAudio = false,isDolbyRequired = true;
    private String mergedFilePath = "", selectedOverlayUrl = "",selectedOverlayId="", selectedIntroContainsAudio = "",
                    selected720IntroUrl = "", selected1080IntroUrl = "", selectedOutroContainsAudio = "",
                    selected720OutroUrl = "", selected1080OutroUrl = "", selectedMusicUrl = "", videoOrientation = "landscape";
    private int selectedIntroLength = 0,  selectedOutroLength = 0, selectedMusicLength = 0,
            mergeTotalDuration = 0;


    EditText edt_mobile,edt_name,edt_email,edt_your_name,edt_video_title,edt_desc,edtEmailMsg;
    private CountryCodePicker ccp;

    @Override
    protected int getLayoutView() {
        return R.layout.act_show_options;
    }

    @Override
    protected Context getActivityContext() {
        return VideoOptionsActivity.this;
    }

    @Override
    protected void initView() {

        permissionsInterface = this;

        defaultSettingsLayout = findViewById(R.id.defaultSettingsLayout);
        edt_video_title = findViewById(R.id.edt_video_title);
        edt_desc = findViewById(R.id.edt_desc);
        edtEmailMsg = findViewById(R.id.edtEmailMsg);
        mainLayout = findViewById(R.id.mainLayout);
        videoDetailLayout = findViewById(R.id.videoDetailLayout);
        introOutroLayout = findViewById(R.id.introOutroLayout);
        intro_custom = findViewById(R.id.intro_custom);
        intro_default = findViewById(R.id.intro_default);
        outro_custom = findViewById(R.id.outro_custom);
        outro_default = findViewById(R.id.outro_default);
        music_custom = findViewById(R.id.music_custom);
        music_default = findViewById(R.id.music_default);
        spinner_intro = findViewById(R.id.spinner_intro);
        spinner_videoType = findViewById(R.id.spinner_videoType);
        spinner_outro = findViewById(R.id.spinner_outro);
        spinner_music = findViewById(R.id.spinner_music);
        intro_none = findViewById(R.id.intro_none);
        outro_none = findViewById(R.id.outro_none);
        music_none = findViewById(R.id.music_none);
        compress_yes = findViewById(R.id.compress_yes);
        compress_no = findViewById(R.id.compress_no);
        dolby_yes = findViewById(R.id.dolby_yes);
        dolby_no = findViewById(R.id.dolby_no);
        rl_custom_intro = findViewById(R.id.rl_custom_intro);
        rl_custom_outro = findViewById(R.id.rl_custom_outro);
        rl_custom_music = findViewById(R.id.rl_custom_music);
        group_intro = findViewById(R.id.group_intro);
        group_outro = findViewById(R.id.group_outro);
        group_music = findViewById(R.id.group_music);
        group_dolby = findViewById(R.id.group_dolby);
        group_compress = findViewById(R.id.group_compress);
        edt_mobile = findViewById(R.id.edt_mobile);
        edt_name = findViewById(R.id.edt_name);
        edt_email = findViewById(R.id.edt_email);
        edt_your_name = findViewById(R.id.edt_your_name);
        ccp = findViewById(R.id.ccp);

        radioGroup = findViewById(R.id.radioGroup);
        radioEmail = findViewById(R.id.radioEmail);
        radioSMS = findViewById(R.id.radioSMS);

    }

    @Override
    protected void initData() {

        int l=getSessionManager().getVideoType().length;
        String[] items=new String[l];

        for (int i=0;i<getSessionManager().getVideoType().length;i++){
            items[i]=getSessionManager().getVideoType()[i].getName();
        }

        edt_your_name.setText(getSessionManager().getMyName());
        String newString = getSessionManager().getVideoType()[0].getSms();

        String output=newString.replace("[NAME]", edt_name.getText().toString()).replace("[Subject]", videoType)
                .replace("[Your Name]", edt_your_name.getText().toString());

        edt_desc.setText(output);

        String emailString = getSessionManager().getVideoType()[0].getEmailBody();

        String emailOutput=emailString.replace("[NAME]", edt_name.getText().toString()).replace("[Subject]", videoType)
                   .replace("[Your Name]", edt_your_name.getText().toString());

        edtEmailMsg.setText(emailOutput);

        String pathNames = getIntent().getStringExtra("videoSegmentNames");
        mergedFilePath = getIntent().getStringExtra("mergedFilePath");
        mergedFileContainsAudio = getIntent().getBooleanExtra("mergedFileContainsAudio", false);
        mergeTotalDuration = getIntent().getIntExtra("mergeTotalDuration", 0);
        selectedOverlayUrl = getIntent().getStringExtra("selectedOverlayUrl");
        selectedOverlayId = getIntent().getStringExtra("selectedOverlayId");

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<VideoSegmentModel>>() {}.getType();
        videoSegmentNames = gson.fromJson(pathNames, type);

        introList = new ArrayList<>();
        outroList = new ArrayList<>();
        bgMusicList = new ArrayList<>();

        VideoOptions[] intros = getSessionManager().getIntros();
        VideoOptions[] outros = getSessionManager().getOutros();
        VideoOptions[] bgMusic = getSessionManager().getBgMusic();


        ccp.registerCarrierNumberEditText(edt_mobile);


        try{
            String defaultCountrycode = getSessionManager().getPreformattedMessage().getCc_suffix_iso();
            ccp.setDefaultCountryUsingNameCode(defaultCountrycode);
            ccp.resetToDefaultCountry();
        }catch (Exception e){}



        if((intros.length==0 && outros.length==0) && bgMusic.length==0){
            allEmpty = true;
        }

        try {

            for (int i = 0; i < intros.length; i++) {

                String orientation = intros[i].getOrientation();
                if (videoOrientation.equalsIgnoreCase(orientation)) {
                    introList.add(intros[i]);
                    if (intros[i].getIs_default().equals("1")) {
                        defaultIntroPosition = i;
                    }
                }

            }

        } catch (Exception e) {}

        try {

            for (int i = 0; i < outros.length; i++) {

                String orientation = outros[i].getOrientation();
                if (videoOrientation.equalsIgnoreCase(orientation)) {
                    outroList.add(outros[i]);
                    if (outros[i].getIs_default().equals("1")) {
                        defaultOutroPosition = i;
                    }
                }
            }

        } catch (Exception e) {}

        try {

            for (int i = 0; i < bgMusic.length; i++) {
                bgMusicList.add(bgMusic[i]);
                if (bgMusic[i].getIs_default().equals("1")) {
                    defaultBGMusicPosition = i;
                }
            }

        } catch (Exception e) {}

        new SpinnerArrayAdapter(context, spinner_intro, introList, R.layout.spinneritem,
                false, R.color.colorBlack, R.color.colorHintGrey).setAdapter();
        new SpinnerArrayAdapter(context, spinner_outro, outroList, R.layout.spinneritem,
                false, R.color.colorBlack, R.color.colorHintGrey).setAdapter();
        new SpinnerArrayAdapter(context, spinner_music, bgMusicList, R.layout.spinneritem,
                false, R.color.colorBlack, R.color.colorHintGrey).setAdapter();



        ArrayAdapter ad = new ArrayAdapter(this, android.R.layout.simple_spinner_item, items);

        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_videoType.setAdapter(ad);

        spinner_videoType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                videoType=items[position];

                edt_your_name.setText(getSessionManager().getMyName());
                String newString = getSessionManager().getVideoType()[position].getSms();

                String output=newString.replace("[NAME]", edt_name.getText().toString()).replace("[Subject]", videoType)
                        .replace("[Your Name]", edt_your_name.getText().toString());

                edt_desc.setText(output);

                String emailString = getSessionManager().getVideoType()[position].getEmailBody();

                String emailOutput=emailString.replace("[NAME]", edt_name.getText().toString()).replace("[Subject]", videoType)
                        .replace("[Your Name]", edt_your_name.getText().toString());

                edtEmailMsg.setText(emailOutput);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton rb=(RadioButton)findViewById(checkedId);
                if (rb==radioSMS){
                    edt_desc.setVisibility(View.VISIBLE);
                    edtEmailMsg.setVisibility(View.GONE);
                }

                if (rb==radioEmail){
                    edtEmailMsg.setVisibility(View.VISIBLE);
                    edt_desc.setVisibility(View.GONE);
                }
            }
        });

        if (introList.size() == 0) {
            intro_default.setEnabled(false);    intro_default.setAlpha(0.5f);
            intro_custom.setEnabled(false);     intro_custom.setAlpha(0.5f);
            intro_none.setChecked(true);
        }

        if (outroList.size() == 0) {
            outro_default.setEnabled(false);    outro_default.setAlpha(0.5f);
            outro_custom.setEnabled(false);     outro_custom.setAlpha(0.5f);
            outro_none.setChecked(true);
        }

        if (bgMusicList.size() == 0) {
            music_default.setEnabled(false);    music_default.setAlpha(0.5f);
            music_custom.setEnabled(false);     music_custom.setAlpha(0.5f);
            music_none.setChecked(true);
        }

        if (defaultIntroPosition == -1) {
            intro_default.setEnabled(false);
            intro_default.setAlpha(0.5f);
            intro_none.setChecked(true);
            rl_custom_intro.setAlpha(1f);
            spinner_intro.setEnabled(true);
            rl_custom_intro.setVisibility(View.GONE);
        } else {
            intro_default.setEnabled(true);
            intro_default.setAlpha(1f);
            intro_default.setChecked(true);
            rl_custom_intro.setAlpha(0.5f);
            spinner_intro.setEnabled(false);
            rl_custom_intro.setVisibility(View.VISIBLE);
            spinner_intro.setSelection(defaultIntroPosition);
        }

        if (defaultOutroPosition == -1) {
            outro_default.setEnabled(false);
            outro_default.setAlpha(0.5f);
            outro_none.setChecked(true);
            rl_custom_outro.setAlpha(1f);
            spinner_outro.setEnabled(true);
            rl_custom_outro.setVisibility(View.GONE);
        } else {
            outro_default.setEnabled(true);
            outro_default.setAlpha(1.0f);
            outro_default.setChecked(true);
            rl_custom_outro.setAlpha(0.5f);
            spinner_outro.setEnabled(false);
            rl_custom_outro.setVisibility(View.VISIBLE);
            spinner_outro.setSelection(defaultOutroPosition);
        }

        if (defaultBGMusicPosition == -1) {
            music_default.setEnabled(false);
            music_default.setAlpha(0.5f);
            music_none.setChecked(true);
            rl_custom_music.setAlpha(1f);
            spinner_music.setEnabled(true);
            rl_custom_music.setVisibility(View.GONE);
        }else {
            music_default.setEnabled(true);
            music_default.setAlpha(1.0f);
            music_default.setChecked(true);
            rl_custom_music.setAlpha(0.5f);
            spinner_music.setEnabled(false);
            rl_custom_music.setVisibility(View.VISIBLE);
            spinner_music.setSelection(defaultBGMusicPosition);
        }


        Log.e("getDefaultIntro", "getDefaultIntro: intro "+getSessionManager().getDefaultIntro()+" outro "+getSessionManager().getDefaultOutro() + " music "+
                getSessionManager().getDefaultMusic() +" compress "+getSessionManager().isCompressionRequired());


        if (!getSessionManager().getDefaultIntro().isEmpty()){
            if (getSessionManager().getDefaultIntro().equals("default")){
                rl_custom_intro.setVisibility(View.VISIBLE);
                rl_custom_intro.setAlpha(0.5f);
                spinner_intro.setEnabled(false);
                intro_default.setChecked(true);
                spinner_intro.setSelection(defaultIntroPosition);
            }else if (getSessionManager().getDefaultIntro().equals("custom")){
                rl_custom_intro.setVisibility(View.VISIBLE);
                rl_custom_intro.setAlpha(1f);
                spinner_intro.setEnabled(true);
                intro_custom.setChecked(true);
            }else {
                intro_none.setChecked(true);
                rl_custom_intro.setVisibility(View.GONE);
            }
        }

        if (!getSessionManager().getDefaultOutro().isEmpty()){
            if (getSessionManager().getDefaultOutro().equals("default")){
                rl_custom_outro.setVisibility(View.VISIBLE);
                rl_custom_outro.setAlpha(0.5f);
                spinner_outro.setEnabled(false);
                outro_default.setChecked(true);
                spinner_outro.setSelection(defaultOutroPosition);
            }else if (getSessionManager().getDefaultOutro().equals("custom")){
                rl_custom_outro.setVisibility(View.VISIBLE);
                rl_custom_outro.setAlpha(1f);
                spinner_outro.setEnabled(true);
                outro_custom.setChecked(true);
            }else {
                outro_none.setChecked(true);
                rl_custom_outro.setVisibility(View.GONE);
            }
        }

        if (!getSessionManager().getDefaultMusic().isEmpty()){
            if (getSessionManager().getDefaultMusic().equals("default")){
                rl_custom_music.setVisibility(View.VISIBLE);
                rl_custom_music.setAlpha(0.5f);
                spinner_music.setEnabled(false);
                spinner_music.setSelection(defaultBGMusicPosition);
                music_default.setChecked(true);
            }else if (getSessionManager().getDefaultMusic().equals("custom")){
                rl_custom_music.setVisibility(View.VISIBLE);
                rl_custom_music.setAlpha(1f);
                spinner_music.setEnabled(true);
                music_custom.setChecked(true);
            }else {
                music_none.setChecked(true);
                rl_custom_music.setVisibility(View.GONE);
            }
        }

        if (getSessionManager().isCompressionRequired()){
            isCompressionRequired = true;
            compress_yes.setChecked(true);
        }else {
            isCompressionRequired = false;
            compress_no.setChecked(true);
        }

        if (getSessionManager().isDolbyRequired()){
            isDolbyRequired = true;
            dolby_yes.setChecked(true);
        }else {
            isDolbyRequired = false;
            dolby_no.setChecked(true);
        }


        edt_mobile.addTextChangedListener(new TextWatcher(){

            @Override
            public void afterTextChanged(Editable arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,
                                          int arg2, int arg3) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onTextChanged(CharSequence arg0, int start, int before,
                                      int count) {
                if (arg0.length() == 0) {
                    // No entered text so will show hint
                    edt_mobile.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
                } else {
                    edt_mobile.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                }
            }
        });

        edt_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                int l=getSessionManager().getVideoType().length;
                String[] items=new String[l];

                for (int i=0;i<getSessionManager().getVideoType().length;i++){
                    items[i]=getSessionManager().getVideoType()[i].getName();
                    if (getSessionManager().getVideoType()[i].getName().equals(videoType)){
                        String newString = getSessionManager().getVideoType()[i].getSms();

                        String output=newString.replace("[NAME]", edt_name.getText().toString()).replace("[Subject]", videoType)
                                .replace("[Your Name]", edt_your_name.getText().toString());

                        edt_desc.setText(output);

                        String emailString = getSessionManager().getVideoType()[i].getEmailBody();

                        String emailOutput=emailString.replace("[NAME]", edt_name.getText().toString()).replace("[Subject]", videoType)
                                .replace("[Your Name]", edt_your_name.getText().toString());

                        edtEmailMsg.setText(emailOutput);
                    }
                }

            }
        });

        edt_your_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                int l=getSessionManager().getVideoType().length;
                String[] items=new String[l];

                for (int i=0;i<getSessionManager().getVideoType().length;i++){
                    items[i]=getSessionManager().getVideoType()[i].getName();
                    if (getSessionManager().getVideoType()[i].getName().equals(videoType)){
                        String newString = getSessionManager().getVideoType()[i].getSms();

                        String output=newString.replace("[NAME]", edt_name.getText().toString()).replace("[Subject]", videoType)
                                .replace("[Your Name]", edt_your_name.getText().toString());

                        edt_desc.setText(output);

                        String emailString = getSessionManager().getVideoType()[i].getEmailBody();

                        String emailOutput=emailString.replace("[NAME]", edt_name.getText().toString()).replace("[Subject]", videoType)
                                .replace("[Your Name]", edt_your_name.getText().toString());

                        edtEmailMsg.setText(emailOutput);
                    }
                }

            }
        });

        edt_email.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (edt_email.getText().toString().isEmpty()){

                    radioGroup.setVisibility(View.GONE);
                    edt_desc.setVisibility(View.VISIBLE);
                    edtEmailMsg.setVisibility(View.GONE);
                }else {
                    radioGroup.setVisibility(View.VISIBLE);
                    if(radioSMS.isChecked()){
                        edt_desc.setVisibility(View.VISIBLE);
                        edtEmailMsg.setVisibility(View.GONE);
                    }else {
                        edt_desc.setVisibility(View.GONE);
                        edtEmailMsg.setVisibility(View.VISIBLE);
                    }
                }
            }
        });


    }

    @Override
    protected void initListener() {

        mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
            }
        });

        defaultSettingsLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
            }
        });

        videoDetailLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
            }
        });

        introOutroLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
            }
        });

        findViewById(R.id.btn_share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(AppUtils.isEditTextEmpty(edt_video_title)){
                    showToast(getString(R.string.empty_title));
                    return;
                }

                if(isInternetAvailable()) {

                    UploadModel uploadModel = getSessionManager().getUploadData();
                    if (null == uploadModel.getTimeStamp()) {
                        checkDataToSend();
                    } else {
                        showToast("Please wait until you receive a notification that the previous video upload has been completed.");
                    }

                } else {
                    showToast(getString(R.string.api_error_internet));
                }


//                if(allEmpty){
//                    shareAndUpload();
//
//                } else {
//                    checkVideoOptions(true);
//                }

            }
        });



        findViewById(R.id.btn_upload).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkWhetherToShowPopup(false);
//                if(allEmpty){
//                    checkWhetherToShowPopup();
//                } else {
//                    checkVideoOptions(false);
//                }

            }
        });

        findViewById(R.id.btn_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                hideKeyboard(edt_name);

                if(AppUtils.isEditTextEmpty(edt_name)){
                    showToast(getString(R.string.empty_cname));
                    return;
                }
                if(AppUtils.isEditTextEmpty(edt_your_name)){
                    showToast(getString(R.string.empty_yname));
                    return;
                }

                strEmails = edt_email.getText().toString();
                strPhones = edt_mobile.getText().toString();

                if (strPhones.trim().length() == 0) {
                    showToast(getString(R.string.empty_mobile));
                    return;
                }

//                if (strEmails.trim().length() == 0) {
//                    showToast(getString(R.string.empty_email));
//                    return;
//                }




                if (strEmails.trim().length() == 0 && strPhones.trim().length() == 0) {
                    showToast(getString(R.string.empty_email));
                    return;
                }

                if (strEmails.trim().length() != 0) {

                    strEmails = strEmails.replace(" ", "");
                    String[] emails = strEmails.split(",");

                    for (int i = 0; i < emails.length; i++) {

                        String email = emails[i].trim();

                        if (email.trim().length() == 0) {
                            showToast(getString(R.string.empty_email));
                            return;
                        }

                        if (!AppUtils.isValidEmail(email)) {
                            showToast(getString(R.string.invalid_email));
                            return;
                        }

                    }

                    strEmails = "";
                    for (int i = 0; i < emails.length; i++) {
                        if (i == 0) {
                            strEmails = emails[i];
                        } else {
                            strEmails = strEmails + "," + emails[i];
                        }
                    }

                } else {

                    if (strPhones.trim().length() == 0) {
                        showToast(getString(R.string.empty_mobile));
                        return;
                    }

                }


                if(strPhones.trim().length()!=0){

                    String countryCode = ccp.getSelectedCountryCode();

                    strPhones = strPhones.replace(" ", "");
                    String[] phones = strPhones.split(",");

                    strPhones = "";
                    for (int i = 0; i < phones.length; i++) {
                        if(i==0){
                            strPhones = "+" + countryCode + phones[i];
                        } else {
                            strPhones = strPhones + ",+" + countryCode + phones[i];
                        }
                    }
                }

                if (edt_email.getText().toString().trim().isEmpty()){
                    radioGroup.setVisibility(View.GONE);
                }else {
                    radioGroup.setVisibility(View.VISIBLE);
                    if (radioSMS.isChecked()){
                        edt_email.setVisibility(View.VISIBLE);
                        edtEmailMsg.setVisibility(View.GONE);
                    }else {
                        edt_email.setVisibility(View.GONE);
                        edtEmailMsg.setVisibility(View.VISIBLE);
                    }

                }

                defaultSettingsLayout.setVisibility(View.GONE);
               introOutroLayout.setVisibility(View.GONE);
               videoDetailLayout.setVisibility(View.VISIBLE);

            }
        });



        findViewById(R.id.introOutroDownImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                defaultSettingsLayout.setVisibility(View.GONE);
                introOutroLayout.setVisibility(View.VISIBLE);
                videoDetailLayout.setVisibility(View.GONE);

            }
        });

        findViewById(R.id.introDownImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                defaultSettingsLayout.setVisibility(View.VISIBLE);
                introOutroLayout.setVisibility(View.GONE);
                videoDetailLayout.setVisibility(View.GONE);

            }
        });



        findViewById(R.id.backImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                defaultSettingsLayout.setVisibility(View.VISIBLE);
                introOutroLayout.setVisibility(View.GONE);
                videoDetailLayout.setVisibility(View.GONE);

            }
        });


        findViewById(R.id.btn_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                defaultSettingsLayout.setVisibility(View.VISIBLE);
                introOutroLayout.setVisibility(View.GONE);
                videoDetailLayout.setVisibility(View.GONE);

            }
        });

        findViewById(R.id.contactButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    checkPermissions();
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }
        });

        findViewById(R.id.btn_done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(context, VideoCaptureActivity.class));
                overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                finishAffinity();
            }
        });




        findViewById(R.id.img_actionbar_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
            }
        });

        group_intro.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {

                if(i == R.id.intro_default){
                    rl_custom_intro.setVisibility(View.VISIBLE);
                    rl_custom_intro.setAlpha(0.5f);
                    spinner_intro.setEnabled(false);
                    spinner_intro.setSelection(defaultIntroPosition);
                    getSessionManager().setDefaultIntro("default");
                } else if(i == R.id.intro_custom){
                    rl_custom_intro.setVisibility(View.VISIBLE);
                    rl_custom_intro.setAlpha(1f);
                    spinner_intro.setEnabled(true);
                    getSessionManager().setDefaultIntro("custom");
                } else {
                    rl_custom_intro.setVisibility(View.GONE);
                    getSessionManager().setDefaultIntro("none");
                }

            }
        });

        group_outro.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {

                if(i == R.id.outro_default){
                    rl_custom_outro.setVisibility(View.VISIBLE);
                    rl_custom_outro.setAlpha(0.5f);
                    spinner_outro.setEnabled(false);
                    getSessionManager().setDefaultOutro("default");
                    spinner_outro.setSelection(defaultOutroPosition);
                } else if(i == R.id.outro_custom){
                    rl_custom_outro.setVisibility(View.VISIBLE);
                    rl_custom_outro.setAlpha(1f);
                    spinner_outro.setEnabled(true);
                    getSessionManager().setDefaultOutro("custom");
                } else {
                    rl_custom_outro.setVisibility(View.GONE);
                    getSessionManager().setDefaultOutro("none");
                }

            }
        });

        group_music.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {

                if(i == R.id.music_default){
                    rl_custom_music.setVisibility(View.VISIBLE);
                    rl_custom_music.setAlpha(0.5f);
                    spinner_music.setEnabled(false);
                    getSessionManager().setDefaultMusic("default");
                    spinner_music.setSelection(defaultBGMusicPosition);
                } else if(i == R.id.music_custom){
                    rl_custom_music.setVisibility(View.VISIBLE);
                    rl_custom_music.setAlpha(1f);
                    spinner_music.setEnabled(true);
                    getSessionManager().setDefaultMusic("custom");
                } else {
                    rl_custom_music.setVisibility(View.GONE);
                    getSessionManager().setDefaultMusic("none");
                }

            }
        });

        group_compress.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {

                if(i == R.id.compress_yes){
                    isCompressionRequired = true;
                    getSessionManager().compressionRequired(true);
                } else {
                    isCompressionRequired = false;
                    getSessionManager().compressionRequired(false);
                }

            }
        });

        group_dolby.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {

                if(i == R.id.dolby_yes){
                    isDolbyRequired = true;
                    getSessionManager().dolbyRequired(true);
                } else {
                    isDolbyRequired = false;
                    getSessionManager().dolbyRequired(false);
                }

            }
        });

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
    }

    private void checkVideoOptions(boolean shareData) {
        if(group_intro.getCheckedRadioButtonId()==R.id.intro_default){
            selectedIntroContainsAudio = introList.get(defaultIntroPosition).getVideo_music();
            selected720IntroUrl = introList.get(defaultIntroPosition).getSTFile_url();
            selected1080IntroUrl = introList.get(defaultIntroPosition).getFile_url();
            selectedIntroLength = Integer.parseInt(introList.get(defaultIntroPosition).getLength());

        }else if(group_intro.getCheckedRadioButtonId()==R.id.intro_custom){
            selectedIntroContainsAudio = introList.get(spinner_intro.getSelectedItemPosition()).getVideo_music();
            selected720IntroUrl = introList.get(spinner_intro.getSelectedItemPosition()).getSTFile_url();
            selected1080IntroUrl = introList.get(spinner_intro.getSelectedItemPosition()).getFile_url();
            selectedIntroLength = Integer.parseInt(introList.get(spinner_intro.getSelectedItemPosition()).getLength());

        }else if(group_intro.getCheckedRadioButtonId()==R.id.intro_none){
            selectedIntroContainsAudio = "";
            selected720IntroUrl = "";
            selected1080IntroUrl = "";
            selectedIntroLength = 0;

        }


        if(group_outro.getCheckedRadioButtonId()==R.id.outro_default){
            selectedOutroContainsAudio = outroList.get(defaultOutroPosition).getVideo_music();
            selected720OutroUrl = outroList.get(defaultOutroPosition).getSTFile_url();
            selected1080OutroUrl = outroList.get(defaultOutroPosition).getFile_url();
            selectedOutroLength = Integer.parseInt(outroList.get(defaultOutroPosition).getLength());

        }else if(group_outro.getCheckedRadioButtonId()==R.id.outro_custom){
            selectedOutroContainsAudio = outroList.get(spinner_outro.getSelectedItemPosition()).getVideo_music();
            selected720OutroUrl = outroList.get(spinner_outro.getSelectedItemPosition()).getSTFile_url();
            selected1080OutroUrl = outroList.get(spinner_outro.getSelectedItemPosition()).getFile_url();
            selectedOutroLength = Integer.parseInt(outroList.get(spinner_outro.getSelectedItemPosition()).getLength());

        }else if(group_outro.getCheckedRadioButtonId()==R.id.outro_none){
            selectedOutroContainsAudio = "";
            selected720OutroUrl = "";
            selected1080OutroUrl = "";
            selectedOutroLength = 0;

        }


        if(group_music.getCheckedRadioButtonId()==R.id.music_default){
            selectedMusicUrl = bgMusicList.get(defaultBGMusicPosition).getFile_url();
            selectedMusicLength = Integer.parseInt(bgMusicList.get(defaultBGMusicPosition).getLength());

        }else if(group_outro.getCheckedRadioButtonId()==R.id.music_custom){
            selectedMusicUrl = bgMusicList.get(spinner_music.getSelectedItemPosition()).getFile_url();
            selectedMusicLength = Integer.parseInt(bgMusicList.get(spinner_music.getSelectedItemPosition()).getLength());

        }else if(group_outro.getCheckedRadioButtonId()==R.id.music_none){
            selectedMusicUrl = "";
            selectedMusicLength = 0;

        }

        if(shareData){
            shareAndUpload();
        } else {
            checkWhetherToShowPopup(false);
        }

    }

    private void startBGService(String title){

        if(isInternetAvailable()) {

            UploadModel uploadModel = getSessionManager().getUploadData();
            if (null == uploadModel.getTimeStamp()) {

                String finalIntroUrl = "", finalOutroUrl = "";
//                if(isCompressionRequired){
//                    finalIntroUrl = selected720IntroUrl;
//                    finalOutroUrl = selected720OutroUrl;
//                } else {
//                    finalIntroUrl = selected1080IntroUrl;
//                    finalOutroUrl = selected1080OutroUrl;
//                }

                finalIntroUrl = selected1080IntroUrl;
                finalOutroUrl = selected1080OutroUrl;
                Log.e("videoSegmentNames", "onClick: "+mergedFilePath.toString() );
                String smsBody=edt_desc.getText().toString();

                String emailBody="";
                if (edt_email.getText().toString().isEmpty()){
                    emailBody="";
                }else {
                    emailBody=edtEmailMsg.getText().toString();
                }


                UploadModel uploadModelNew = new UploadModel(
                        videoSegmentNames, isCompressionRequired,isDolbyRequired, mergedFilePath, selectedOverlayUrl,
                        selectedOverlayId,
                        selectedIntroContainsAudio, finalIntroUrl, selectedOutroContainsAudio,
                        finalOutroUrl, selectedMusicUrl, selectedIntroLength, selectedOutroLength,
                        selectedMusicLength, "", "", getSessionManager().getUserLocationId(),
                        getSessionManager().getUserId(), getSessionManager().getUserType(),
                        "", "", "","", title, "",
                        "", "", AppUtils.getTimeStamp().toString(), "0",
                        "0", "0",0, mergedFileContainsAudio,
                        mergeTotalDuration, "",smsBody,emailBody);

                getSessionManager().setUploadData(uploadModelNew);
                Log.e("S3Bucket", "Service >>> Started (Upload Only)");
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    context.startForegroundService(new Intent(context, S3UploadService.class));
//                } else {
//                    context.startService(new Intent(context, S3UploadService.class));
//                }

                Data data = new Data.Builder()
                        .build();

                Constraints.Builder constraints = new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED);

                OneTimeWorkRequest oneTimeRequest = new OneTimeWorkRequest.Builder(MediaWorker.class)
                        .setInputData(data)
                        .setConstraints(constraints.build())
                        .addTag("media")
                        .build();


                WorkManager.getInstance(VideoOptionsActivity.this)
                        .enqueueUniqueWork("AndroidVille", ExistingWorkPolicy.KEEP, oneTimeRequest);

                startActivity(new Intent(context, VideoUploadActivity.class));
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);

            } else {
                showToast("Please wait until you receive a notification that the previous video upload has been completed.");
            }

        } else {
            showToast(getString(R.string.api_error_internet));
        }

    }

    private void shareAndUpload(){

        Gson gson = new Gson();
        String jsonData = gson.toJson(videoSegmentNames);

        String finalIntroUrl = "", finalOutroUrl = "";
//        if(isCompressionRequired){
//            finalIntroUrl = selected720IntroUrl;
//            finalOutroUrl = selected720OutroUrl;
//        } else {
//            finalIntroUrl = selected1080IntroUrl;
//            finalOutroUrl = selected1080OutroUrl;
//        }
        finalIntroUrl = selected1080IntroUrl;
        finalOutroUrl = selected1080OutroUrl;

        String smsBody=edt_desc.getText().toString();

        String emailBody="";
        if (edt_email.getText().toString().isEmpty()){
            emailBody="";
        }else {
            emailBody=edtEmailMsg.getText().toString();
        }


        startActivity(new Intent(context, VideoDetailsActivity.class)
                .putExtra("videoSegmentNames", jsonData)
                .putExtra("mergedFilePath", mergedFilePath)
                .putExtra("isCompressionRequired", isCompressionRequired)
                .putExtra("isDolbyRequired", isDolbyRequired)
                .putExtra("selectedOverlayUrl", selectedOverlayUrl)
                .putExtra("selectedOverlayId", selectedOverlayId)
                .putExtra("selectedIntroContainsAudio", selectedIntroContainsAudio)
                .putExtra("selectedIntroUrl", finalIntroUrl)
                .putExtra("selectedOutroContainsAudio", selectedOutroContainsAudio)
                .putExtra("selectedOutroUrl", finalOutroUrl)
                .putExtra("selectedMusicUrl", selectedMusicUrl)
                .putExtra("selectedIntroLength", selectedIntroLength)
                .putExtra("selectedOutroLength", selectedOutroLength)
                .putExtra("selectedMusicLength", selectedMusicLength)
                .putExtra("mergedFileContainsAudio", mergedFileContainsAudio)
                .putExtra("mergeTotalDuration", mergeTotalDuration)
                .putExtra("smsBody", smsBody)
                .putExtra("emailBody", emailBody)
        );
        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);

    }

    public void checkWhetherToShowPopup(boolean shareData) {

        boolean showPopup = false;
        String message = "And please check your WiFi connection is 3+ bars and do not minimise the app until uploaded notification is received.";

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_ins);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);
        EditText dialogEditText=dialog.findViewById(R.id.dialogEditText);

        TextView txtMessage = dialog.findViewById(R.id.txtMessage);
        txtMessage.setText(message);

        Button btnYes = dialog.findViewById(R.id.btnYes);
        Button btn_cancel = dialog.findViewById(R.id.btn_cancel);
        btnYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialogEditText.getText().toString().trim().isEmpty()){
                    showToast("Please Enter Vehicle Rego No.");
                }else {
                    if (shareData){
                        shareAndUpload();
                    }else {
                        startBGService(dialogEditText.getText().toString().trim());
                    }

                    dialog.dismiss();
                }

            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();

//        if(isCompressionRequired){
//
//            if(mergeTotalDuration>180){
////                message = "Your video length is over 3 minutes. You must be required to keep the app open while the video uploads. You can continue to use the app normally.";
//                showPopup = true;
//            } else {
//                showPopup = false;
//            }
//
//        } else {
//
//            if(mergeTotalDuration>60){
////                message = "Your video length is over 1 minute. You must be required to keep the app open while the video uploads. You can continue to use the app normally.";
//                showPopup = true;
//            } else {
//                showPopup = false;
//            }
//
//        }
//
//        if(!showPopup){
//
//            startBGService();
//
//        }
//        else {}

    }


    private void checkPermissions() throws ExecutionException, InterruptedException {

        askPermissions(permissions, permissionsInterface);

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

        Intent pickContact = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startForResult.launch(pickContact);


    }

    public void checkDataToSend(){

//        hideKeyboard(edt_name);



        getSessionManager().setMyName(edt_your_name.getText().toString());




        checkWhetherToShowPopup();

    }

    private void startBGService(){

        if(isInternetAvailable()) {


            String finalIntroUrl = "", finalOutroUrl = "";
//        if(isCompressionRequired){
//            finalIntroUrl = selected720IntroUrl;
//            finalOutroUrl = selected720OutroUrl;
//        } else {
//            finalIntroUrl = selected1080IntroUrl;
//            finalOutroUrl = selected1080OutroUrl;
//        }
            finalIntroUrl = selected1080IntroUrl;
            finalOutroUrl = selected1080OutroUrl;


            UploadModel uploadModel = getSessionManager().getUploadData();
            if (null == uploadModel.getTimeStamp()) {

                String review = "0";
              //  if(showReview){ review = "1"; }

                String smsBody=edt_desc.getText().toString();

                String emailBody="";
                if (edt_email.getText().toString().isEmpty()){
                    emailBody="";
                }else {
                    emailBody=edtEmailMsg.getText().toString();
                }
                UploadModel uploadModelNew = new UploadModel(
                        videoSegmentNames, isCompressionRequired,isDolbyRequired, mergedFilePath, selectedOverlayUrl,selectedOverlayId,
                        selectedIntroContainsAudio, finalIntroUrl, selectedOutroContainsAudio,
                        finalOutroUrl, selectedMusicUrl, selectedIntroLength, selectedOutroLength,
                        selectedMusicLength, "", "", getSessionManager().getUserLocationId(),
                        getSessionManager().getUserId(), getSessionManager().getUserType(),
                        edt_your_name.getText().toString(), edt_name.getText().toString(), strEmails,
                        strPhones, edt_video_title.getText().toString(), "",
                        "", videoType.toLowerCase(Locale.ROOT), AppUtils.getTimeStamp().toString(), "0",
                        "1", review,0, mergedFileContainsAudio,
                        mergeTotalDuration, "",smsBody,emailBody);

                getSessionManager().setUploadData(uploadModelNew);
                Log.e("S3Bucket", "Service >>> Started (Upload n Share");
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    context.startForegroundService(new Intent(context, S3UploadService.class));
//                } else {
//                    context.startService(new Intent(context, S3UploadService.class));
//                }

                Data data = new Data.Builder()
                        .build();

                Constraints.Builder constraints = new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED);

                OneTimeWorkRequest oneTimeRequest = new OneTimeWorkRequest.Builder(MediaWorker.class)
                        .setInputData(data)
                        .setConstraints(constraints.build())
                        .addTag("media")
                        .build();


                WorkManager.getInstance(VideoOptionsActivity.this)
                        .enqueueUniqueWork("AndroidVille", ExistingWorkPolicy.KEEP, oneTimeRequest);

                startActivity(new Intent(context, VideoUploadActivity.class));
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);

            } else {
                showToast("Please wait until you receive a notification that the previous video upload has been completed.");
            }

        } else {
            showToast(getString(R.string.api_error_internet));
        }

    }

    public void checkWhetherToShowPopup() {

        boolean showPopup = false;
        String message = "And please check your WiFi connection is 3+ bars and do not minimise the app until uploaded notification is received.";

        if(isCompressionRequired){

            if(mergeTotalDuration>180){
//                message = "Your video length is over 3 minutes. You must be required to keep the app open while the video uploads. You can continue to use the app normally.";
                showPopup = true;
            } else {
                showPopup = false;
            }

        } else {

            if(mergeTotalDuration>60){
//                message = "Your video length is over 1 minute. You must be required to keep the app open while the video uploads. You can continue to use the app normally.";
                showPopup = true;
            } else {
                showPopup = false;
            }

        }

//        if(!showPopup){
//
//            startBGService();
//
//        } else {
//
//            Dialog dialog = new Dialog(context);
//            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//            dialog.setContentView(R.layout.dialog_ins);
//            dialog.setCanceledOnTouchOutside(false);
//            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//            dialog.setCancelable(false);
//
//            RelativeLayout dialogRelativelayout=dialog.findViewById(R.id.dialogRelativelayout);
//            dialogRelativelayout.setVisibility(View.GONE);
//            TextView txtMessage = dialog.findViewById(R.id.txtMessage);
//            txtMessage.setText(message);
//
//            Button btnYes = dialog.findViewById(R.id.btnYes);
//            btnYes.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    startBGService();
//                    dialog.dismiss();
//                }
//            });
//
//            dialog.show();
//
//        }

        startBGService();

    }


    private void closeKeyBoard(){
        View view = this.getCurrentFocus();
        if (view != null){
            InputMethodManager imm = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm =  (InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);

        var view = getCurrentFocus();

        /* If no view currently has focus, create a new one,
         * just so we can grab a window token from it */
        if (view == null) {
            view = new View(this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

}