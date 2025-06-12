package com.autoforcecam.app.activities;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.autoforcecam.app.R;
import com.autoforcecam.app.base.BaseActivity;
import com.autoforcecam.app.models.UploadModel;
import com.autoforcecam.app.models.VideoSegmentModel;
import com.autoforcecam.app.utils.AppUtils;
import com.autoforcecam.app.utils.MediaWorker;
import com.autoforcecam.app.utils.S3UploadService;
import com.autoforcecam.app.utils.SoftInputAssist;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hbb20.CountryCodePicker;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Locale;

/* Created by JSP@nesar */

public class VideoDetailsActivity extends BaseActivity {

    private EditText edt_title, edt_desc, edt_name, edt_cname, edt_year, edt_email;
    private AppCompatEditText edt_mobile;
    private ImageView imgToggleReview;
    private TextView txtMessage;
    private RadioGroup groupVideoType;
    private CountryCodePicker ccp;
    private ArrayList<VideoSegmentModel> videoSegmentNames = new ArrayList<>();
    private boolean isCompressionRequired = true, isDolbyRequired=true,showReview = false, mergedFileContainsAudio = false;
    private String shotId = "", mergedFilePath = "", shotJson = "", videoSubject = "Intro",
            strEmails = "", strPhones = "", selectedOverlayUrl = "",selectedOverlayId="", selectedIntroContainsAudio = "",
            selectedIntroUrl = "",selectedIntroId = "", selectedOutroContainsAudio = "", selectedOutroUrl = "", selectedMusicUrl = "",
    smsBody,emailBody;
    private int selectedIntroLength = 0,  selectedOutroLength = 0, selectedMusicLength = 0,
            mergeTotalDuration = 0;

    SoftInputAssist softInputAssist;

    @Override
    protected int getLayoutView() {
        return R.layout.act_details;
    }

    @Override
    protected Context getActivityContext() {
        return VideoDetailsActivity.this;
    }

    @Override
    protected void initView() {
        edt_title = findViewById(R.id.edt_title);
        edt_desc = findViewById(R.id.edt_desc);
        edt_name = findViewById(R.id.edt_name);
        edt_cname = findViewById(R.id.edt_cname);
        edt_year = findViewById(R.id.edt_year);
        edt_email = findViewById(R.id.edt_email);
        edt_mobile = findViewById(R.id.edt_mobile);
        imgToggleReview = findViewById(R.id.imgToggleReview);
        groupVideoType = findViewById(R.id.groupVideoType);
        ccp = findViewById(R.id.ccp);
        txtMessage = findViewById(R.id.txtMessage);

        softInputAssist=new SoftInputAssist(this);
    }

    @Override
    protected void initData() {

        edt_name.setText(getSessionManager().getMyName());
        edt_email.setVisibility(View.GONE);

        String pathNames = getIntent().getStringExtra("videoSegmentNames");
        mergedFilePath = getIntent().getStringExtra("mergedFilePath");
        selectedOverlayUrl = getIntent().getStringExtra("selectedOverlayUrl");
        selectedOverlayId = getIntent().getStringExtra("selectedOverlayId");
        selectedIntroContainsAudio = getIntent().getStringExtra("selectedIntroContainsAudio");
        selectedIntroUrl = getIntent().getStringExtra("selectedIntroUrl");
        selectedIntroId = getIntent().getStringExtra("selectedIntroUrl");
        selectedOutroContainsAudio = getIntent().getStringExtra("selectedOutroContainsAudio");
        selectedOutroUrl = getIntent().getStringExtra("selectedOutroUrl");
        selectedMusicUrl = getIntent().getStringExtra("selectedMusicUrl");
        selectedIntroLength = getIntent().getIntExtra("selectedIntroLength", 0);
        selectedOutroLength = getIntent().getIntExtra("selectedOutroLength", 0);
        selectedMusicLength = getIntent().getIntExtra("selectedMusicLength", 0);
        mergedFileContainsAudio = getIntent().getBooleanExtra("mergedFileContainsAudio", false);
        mergeTotalDuration = getIntent().getIntExtra("mergeTotalDuration", 0);

        isCompressionRequired = getIntent().getBooleanExtra("isCompressionRequired", true);
        isDolbyRequired = getIntent().getBooleanExtra("isDolbyRequired", true);
        smsBody = getIntent().getStringExtra("smsBody");
        emailBody = getIntent().getStringExtra("emailBody");

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<VideoSegmentModel>>() {}.getType();
        videoSegmentNames = gson.fromJson(pathNames, type);

       // edt_title.setText(getSessionManager().getPreformattedMessage().getYt_title_text());
        edt_desc.setText(getSessionManager().getPreformattedMessage().getYt_description_text());

        ccp.registerCarrierNumberEditText(edt_mobile);

        try{
            String defaultCountrycode = getSessionManager().getPreformattedMessage().getCc_suffix_iso();
            ccp.setDefaultCountryUsingNameCode(defaultCountrycode);
            ccp.resetToDefaultCountry();
        }catch (Exception e){}

    }

    @Override
    protected void initListener() {

        imgToggleReview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showReview = !showReview;
                if(showReview){
                    imgToggleReview.setImageResource(R.drawable.ic_on_with_text);
                }else {
                    imgToggleReview.setImageResource(R.drawable.ic_off_with_text);
                }
            }
        });

        findViewById(R.id.btn_done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyboard(edt_name);
                startActivity(new Intent(context, VideoCaptureActivity.class));
                overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                finishAffinity();
            }
        });

        findViewById(R.id.btn_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                hideKeyboard(edt_name);

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

            }
        });

        groupVideoType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {

                if(i == R.id.rdBtnIntro){
                    videoSubject = "Intro";
                    showReview = false;
                    edt_email.setVisibility(View.GONE);
                    edt_email.setText("");
                    txtMessage.setText(R.string.if_you_wish_to_share_a_link2);
                } else if(i == R.id.rdBtnWalk){
                    videoSubject = "General";
                    edt_email.setVisibility(View.VISIBLE);
                    showReview = false;
                    txtMessage.setText(R.string.if_you_wish_to_share_a_link);
                } else {
                    videoSubject = "Thank You";
                    edt_email.setVisibility(View.VISIBLE);
                    showReview = true;
                    txtMessage.setText(R.string.if_you_wish_to_share_a_link);
                }

            }
        });

    }

    @Override
    public void onBackPressed() {}

    public void checkDataToSend(){

        hideKeyboard(edt_name);

        if(AppUtils.isEditTextEmpty(edt_title)){
            showToast(getString(R.string.empty_title));
            return;
        }

        if(AppUtils.isEditTextEmpty(edt_desc)){
            showToast(getString(R.string.empty_desc));
            return;
        }

        if(AppUtils.isEditTextEmpty(edt_name)){
            showToast(getString(R.string.empty_yname));
            return;
        }

        getSessionManager().setMyName(edt_name.getText().toString());

        if(AppUtils.isEditTextEmpty(edt_cname)){
            showToast(getString(R.string.empty_cname));
            return;
        }

        strEmails = edt_email.getText().toString();
        strPhones = edt_mobile.getText().toString();

        if(videoSubject.equals("Intro")){

            if (strPhones.trim().length() == 0) {
                showToast(getString(R.string.empty_mobile));
                return;
            }

        } else {

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

        checkWhetherToShowPopup();

    }

    private void startBGService(){

        if(isInternetAvailable()) {

            UploadModel uploadModel = getSessionManager().getUploadData();
            if (null == uploadModel.getTimeStamp()) {

                String review = "0";
                if(showReview){ review = "1"; }

                UploadModel uploadModelNew = new UploadModel(
                        videoSegmentNames, isCompressionRequired,isDolbyRequired, mergedFilePath, selectedOverlayUrl,selectedOverlayId,
                        selectedIntroContainsAudio, selectedIntroUrl, selectedOutroContainsAudio,
                        selectedOutroUrl, selectedMusicUrl, selectedIntroLength, selectedOutroLength,
                        selectedMusicLength, "", "", getSessionManager().getUserLocationId(),
                        getSessionManager().getUserId(), getSessionManager().getUserType(),
                        edt_name.getText().toString(), edt_cname.getText().toString(), strEmails,
                        strPhones, edt_title.getText().toString(), edt_desc.getText().toString(),
                        "", videoSubject.toLowerCase(Locale.ROOT), AppUtils.getTimeStamp().toString(), "0",
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


                WorkManager.getInstance(VideoDetailsActivity.this)
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

    @Override
    protected void onDestroy() {
        softInputAssist.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        softInputAssist.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        softInputAssist.onResume();
        super.onResume();
    }
}
