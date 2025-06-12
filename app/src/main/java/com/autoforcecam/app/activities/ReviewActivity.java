package com.autoforcecam.app.activities;

import android.content.Context;
import android.content.Intent;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatEditText;

import com.autoforcecam.app.R;
import com.autoforcecam.app.base.BaseActivity;
import com.autoforcecam.app.interfaces.SendInterface;
import com.autoforcecam.app.presenters.SendPresenter;
import com.autoforcecam.app.utils.AppUtils;
import com.autoforcecam.app.utils.TagEditText;


/* Created by JSP@nesar */

public class ReviewActivity extends BaseActivity implements SendInterface {

    private String mediaType = "", mediaLink = "", sendType = "", postId = "", emails = "",
            mobile = "", yourName = "", customerName = "", model = "", password = "", countryCode = "",
            strMobiles = "";
    private ImageView img_sendby;
    private TextView txt_email, txt_mobile;
    private EditText edt_message;
    private SendPresenter sendPresenter;

    @Override
    protected int getLayoutView() {
        return R.layout.act_review;
    }

    @Override
    protected Context getActivityContext() {
        return ReviewActivity.this;
    }

    @Override
    protected void initView() {

        img_sendby = findViewById(R.id.img_sendby);
        txt_email = findViewById(R.id.txt_email);
        txt_mobile = findViewById(R.id.txt_mobile);
        edt_message = findViewById(R.id.edt_message);

    }

    @Override
    protected void initData() {

        sendPresenter = new SendPresenter(getAPIInterface());
        sendPresenter.attachView(this);

        yourName = getIntent().getStringExtra("yourName");
        customerName = getIntent().getStringExtra("customerName");
        model = getIntent().getStringExtra("model");
        mediaType = getIntent().getStringExtra("mediaType");
        mediaLink = getIntent().getStringExtra("mediaLink");
        sendType = getIntent().getStringExtra("sendType");
        postId = getIntent().getStringExtra("postId");
        emails = getIntent().getStringExtra("emails");
        countryCode = getIntent().getStringExtra("countryCode");
        mobile = getIntent().getStringExtra("mobile");
        password = getIntent().getStringExtra("password");

        String message = "";

        if(sendType.equalsIgnoreCase("sms")){

            strMobiles = "";
            String strMobiShow = "";

            String[] mobi = mobile.split(",");
            for(int i=0; i<mobi.length; i++){
                if(i==0){
                    strMobiles = countryCode + mobi[i];
                    strMobiShow = "+" + countryCode + mobi[i];
                } else {
                    strMobiles = strMobiles + "," + countryCode + mobi[i];
                    strMobiShow = strMobiShow + ", +" + countryCode + mobi[i];
                }
            }


            txt_mobile.setText(strMobiShow);
            img_sendby.setImageResource(R.mipmap.bg_chat);
            txt_mobile.setVisibility(View.VISIBLE);
            txt_email.setVisibility(View.GONE);

            try {

                if(mediaType.equalsIgnoreCase("image")){
                    message = getSessionManager().getPreformattedMessage().getPreformatted_sms();
                } else {
                    message = getSessionManager().getPreformattedMessage().getPreformatted_sms_yt();
                }

                message = message.replace("[NAME]", customerName);
                message = message.replace("[Year Make Model]", model);
                message = message.replace("[PHOTO_LINK]", mediaLink);
                message = message.replace("[VIDEO_LINK]", mediaLink);
                message = message.replace("[Your Name]", yourName);
                message = message.replace("[PASSWORD]", password);
                edt_message.setText(message);

            }catch (Exception e){

            }

        } else {

            txt_email.setText(emails);
            img_sendby.setImageResource(R.drawable.ic_email);
            txt_mobile.setVisibility(View.GONE);
            txt_email.setVisibility(View.VISIBLE);

            try {

                if(mediaType.equalsIgnoreCase("image")){
                    message = getSessionManager().getPreformattedMessage().getEmail_body();
                } else {
                    message = getSessionManager().getPreformattedMessage().getEmail_body_yt();
                }

                message = message.replace("[NAME]", customerName);
                message = message.replace("[Year Make Model]", model);
                message = message.replace("[PHOTO_LINK]", mediaLink);
                message = message.replace("[VIDEO_LINK]", mediaLink);
                message = message.replace("[Your Name]", yourName);
                message = message.replace("[PASSWORD]", password);
                edt_message.setText(message);

            }catch (Exception e){

            }

        }

    }

    @Override
    protected void initListener() {

        findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                finish();
                overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);

            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        hideKeyboard(edt_message);
    }

    @Override
    protected void onDestroy() {
        try{ sendPresenter.detachView(); } catch (Exception e){}
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
    }

    public void checkDataToSend(View v){

        hideKeyboard(edt_message);

        if(AppUtils.isEditTextEmpty(edt_message)){
            showToast(getString(R.string.empty_msg));
            return;
        }


        if(sendType.equalsIgnoreCase("sms")){

            if(isInternetAvailable()){

                showProgressDialog("");

                if(mediaType.equalsIgnoreCase("image")){

                    sendPresenter.sendSmsOrEmail(
                            "",
                            edt_message.getText().toString(),
                            "",
                            strMobiles,
                            edt_message.getText().toString(),
                            "",
                            mediaLink,
                            getSessionManager().getUserLocationId(),
                            sendType,
                            postId,
                            getSessionManager().getUserId(),
                            getSessionManager().getUserType()

                    );

                } else {

                    sendPresenter.sendSmsOrEmail(
                            "",
                            "",
                            edt_message.getText().toString(),
                            strMobiles,
                            "",
                            edt_message.getText().toString(),
                            mediaLink,
                            getSessionManager().getUserLocationId(),
                            sendType,
                            postId,
                            getSessionManager().getUserId(),
                            getSessionManager().getUserType()

                    );

                }

            }else {
                showToast(getString(R.string.api_error_internet));
            }

        } else {

            if(isInternetAvailable()){

                showProgressDialog("");

                if(mediaType.equalsIgnoreCase("image")){

                    sendPresenter.sendSmsOrEmail(
                            emails,
                            edt_message.getText().toString(),
                            "",
                            "",
                            edt_message.getText().toString(),
                            "",
                            mediaLink,
                            getSessionManager().getUserLocationId(),
                            sendType,
                            postId,
                            getSessionManager().getUserId(),
                            getSessionManager().getUserType()

                    );

                } else {

                    sendPresenter.sendSmsOrEmail(
                            emails,
                            "",
                            edt_message.getText().toString(),
                            "",
                            "",
                            edt_message.getText().toString(),
                            mediaLink,
                            getSessionManager().getUserLocationId(),
                            sendType,
                            postId,
                            getSessionManager().getUserId(),
                            getSessionManager().getUserType()

                    );

                }


            }else {
                showToast(getString(R.string.api_error_internet));
            }

        }

    }

    @Override
    public void onSuccess_ShareMedia(String sendType, String message) {

        hideProgressDialog();

        if(sendType.equalsIgnoreCase("email")) {

            showToast("Email has been sent successfully.");

            if(mobile.trim().length()!=0) {

                startActivity(new Intent(context, ReviewActivity.class)
                        .putExtra("yourName", yourName)
                        .putExtra("customerName", customerName)
                        .putExtra("model", model)
                        .putExtra("mediaType", mediaType)
                        .putExtra("mediaLink", mediaLink)
                        .putExtra("sendType", "sms")
                        .putExtra("emails", "")
                        .putExtra("mobile", mobile)
                        .putExtra("countryCode", countryCode)
                        .putExtra("postId", postId)
                        .putExtra("password", password)
                );
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                finish();

            } else {

                finish();
                overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);

            }

        } else {

            showToast("SMS has been sent successfully.");

            finish();
            overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);

        }

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
