package com.autoforcecam.app.activities;

import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.autoforcecam.app.R;
import com.autoforcecam.app.base.BaseActivity;
import com.autoforcecam.app.interfaces.MediaUploadInterface;
import com.autoforcecam.app.presenters.MediaUploadPresenter;

import java.util.ArrayList;

/* Created by JSP@nesar */

public class SavedActivity extends BaseActivity implements MediaUploadInterface {


    private String imageFilePath = "";
    RelativeLayout messagelayout,closeLayout;
    private MediaUploadPresenter mediaUploadPresenter;
    LinearLayout mainLayout;
    TextView messageText;
    RelativeLayout progess_layout;
    ProgressBar circular_progress;
    TextView progress_tv;

    @Override
    protected int getLayoutView() {
        return R.layout.act_saved;
    }

    @Override
    protected Context getActivityContext() {
        return SavedActivity.this;
    }

    @Override
    protected void initView() {
        mainLayout=findViewById(R.id.mainLayout);
        progess_layout=findViewById(R.id.progess_layout);
        circular_progress=findViewById(R.id.circular_progress);
        progress_tv=findViewById(R.id.progress_tv);
        messagelayout=findViewById(R.id.messagelayout);
        closeLayout=findViewById(R.id.closeLayout);
        messageText=findViewById(R.id.messageText);

    }

    @Override
    protected void initData() {

        imageFilePath = getIntent().getStringExtra("imageFilePath");

        mediaUploadPresenter = new MediaUploadPresenter(getAPIInterface());
        mediaUploadPresenter.attachView(this);

        if(isInternetAvailable()){
            mediaUploadPresenter.updateAnalytics(
                    getSessionManager().getUserId(),
                    getSessionManager().getUserLocationId(),
                    "photo",
                    getSessionManager().getUserType()
            );
        }

    }

    @Override
    protected void initListener() {

        findViewById(R.id.btn_done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                startActivity(new Intent(context, ImageCaptureActivity.class));
                overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                finishAffinity();

            }
        });

        findViewById(R.id.btn_upload).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    uploadImageOnServer(imageFilePath, "");
            }
        });

        closeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                messagelayout.setVisibility(View.GONE);
            }
        });

    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
    }

    @Override
    protected void onDestroy() {
        try{ mediaUploadPresenter.detachView(); } catch (Exception e){}
        super.onDestroy();
    }

    // TODO API Methods

    private void uploadImageOnServer(String imagePath, String description){

        if(isInternetAvailable()){

           // showProgressDialog("Uploading is in progress...");
            mainLayout.setVisibility(View.GONE);
            progess_layout.setVisibility(View.VISIBLE);



            mediaUploadPresenter.uploadMedia(
                    imagePath,
                    getSessionManager().getUserLocationId(),
                    getSessionManager().getUserId(),
                    "image",
                    "facebook",
                    description,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    getSessionManager().getUserType()
            );


            new CountDownTimer(5000, 1000) {
                public void onTick(long millisUntilFinished) {
                    int ss= (int) ((millisUntilFinished / 1000)+1);
                    Log.e("millisUntilFinished", "millisUntilFinished: "+ss);

                    if (ss==5){
                        progress_tv.setText(10+"%");
                        circular_progress.setProgress(10);
                    }else if(ss==4){
                        progress_tv.setText(30+"%");
                        circular_progress.setProgress(30);
                    }else if(ss==3){
                        progress_tv.setText(50+"%");
                        circular_progress.setProgress(50);
                    }else if(ss==2){
                        progress_tv.setText(70+"%");
                        circular_progress.setProgress(70);
                    }
                }
                public void onFinish() {
                    progress_tv.setText(90+"%");
                    circular_progress.setProgress(90);
                }
            }.start();

        }else {
            showToast(getString(R.string.api_error_internet));
        }

    }

    @Override
    public void onSuccess_MediaUpload(String postId, String mediaUrl) {

        progress_tv.setText(100+"%");
        circular_progress.setProgress(100);

        startActivity(new Intent(context, UploadedActivity.class)
                .putExtra("mediaLink", mediaUrl)
                .putExtra("postId", postId)
        );
        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);

    }

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
    public void onError(String message) {
        hideProgressDialog();
        progress_tv.setText(100+"%");
        circular_progress.setProgress(100);
        messagelayout.setVisibility(View.VISIBLE);
        messageText.setText(message);
    }

}
