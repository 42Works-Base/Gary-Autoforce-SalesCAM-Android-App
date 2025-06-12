package com.autoforcecam.app.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageView;

import com.autoforcecam.app.R;
import com.autoforcecam.app.base.BaseActivity;
import com.autoforcecam.app.interfaces.MediaUploadInterface;
import com.autoforcecam.app.presenters.MediaUploadPresenter;
import com.bumptech.glide.Glide;

import java.io.File;

/* Created by JSP@nesar */

public class ImageDetailsActivity extends BaseActivity implements MediaUploadInterface {

    private EditText edt_desc;
    private MediaUploadPresenter mediaUploadPresenter;
    private String imageFilePath = "";

    ImageView imageView;

    @Override
    protected int getLayoutView() {
        return R.layout.act_image_description;
    }

    @Override
    protected Context getActivityContext() {
        return ImageDetailsActivity.this;
    }

    @Override
    protected void initView() {

        edt_desc = findViewById(R.id.edt_desc);
        imageView =findViewById(R.id.aaa);

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

       // File file=new File(imageFilePath);

        Log.e("exists", "initData: "+imageFilePath );
        Glide.with(context)
                .load(Uri.parse(imageFilePath))
                .into(imageView);

    }

    @Override
    protected void initListener() {

        findViewById(R.id.btn_post).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadImageOnServer(imageFilePath, edt_desc.getText().toString());
            }
        });

        findViewById(R.id.btn_exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                startActivity(new Intent(context, ImageCaptureActivity.class));
                overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                finishAffinity();

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

        try {
            mediaUploadPresenter.detachView();
        } catch (Exception e) {
        }

        super.onDestroy();
    }

    // TODO API Methods

    private void uploadImageOnServer(String imagePath, String description){

        if(isInternetAvailable()){

            showProgressDialog("Uploading is in progress...");
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

        }else {
            showToast(getString(R.string.api_error_internet));
        }

    }

    @Override
    public void onSuccess_MediaUpload(String postId, String mediaUrl) {

        hideProgressDialog();

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
        showToast(message);
    }

}