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
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatEditText;

import com.autoforcecam.app.R;
import com.autoforcecam.app.base.BaseActivity;
import com.autoforcecam.app.interfaces.ShareInterface;
import com.autoforcecam.app.models.UploadModel;
import com.autoforcecam.app.models.VideoSegmentModel;
import com.autoforcecam.app.presenters.SharePresenter;
import com.autoforcecam.app.utils.AppUtils;
import com.hbb20.CountryCodePicker;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Locale;

/* Created by JSP@nesar */

public class ShareDetailsActivity extends BaseActivity implements ShareInterface {

    private EditText edt_name, edt_cname, edt_email;
    private TextView txt_title, txtMessage;
    private AppCompatEditText edt_mobile;
    private CountryCodePicker ccp;
    private boolean showReview = false;
    private String videoSubject = "", strEmails = "", strPhones = "", vimeoLink = "";
    private SharePresenter sharePresenter;

    @Override
    protected int getLayoutView() {
        return R.layout.act_share_details;
    }

    @Override
    protected Context getActivityContext() {
        return ShareDetailsActivity.this;
    }

    @Override
    protected void initView() {
        txt_title = findViewById(R.id.txt_title);
        txtMessage = findViewById(R.id.txtMessage);
        edt_name = findViewById(R.id.edt_name);
        edt_cname = findViewById(R.id.edt_cname);
        edt_email = findViewById(R.id.edt_email);
        edt_mobile = findViewById(R.id.edt_mobile);
        ccp = findViewById(R.id.ccp);
    }

    @Override
    protected void initData() {

        edt_name.setText(getSessionManager().getMyName());

        videoSubject = getIntent().getStringExtra("videoSubject");
        vimeoLink = getIntent().getStringExtra("vimeoLink");

        txt_title.setText(videoSubject);
        txtMessage.setText(R.string.if_you_wish_to_share_a_link);

        if(videoSubject.equals("Introduction")){
            showReview = false;
            edt_email.setVisibility(View.GONE);
            edt_email.setText("");
            txtMessage.setText(R.string.if_you_wish_to_share_a_link2);
        } else if(videoSubject.equals("General")){
            edt_email.setVisibility(View.VISIBLE);
            showReview = false;
        } else {
            edt_email.setVisibility(View.VISIBLE);
            showReview = true;
        }

        ccp.registerCarrierNumberEditText(edt_mobile);

        try{
            String defaultCountrycode = getSessionManager().getPreformattedMessage().getCc_suffix_iso();
            ccp.setDefaultCountryUsingNameCode(defaultCountrycode);
            ccp.resetToDefaultCountry();
        }catch (Exception e){}

        sharePresenter = new SharePresenter(getAPIInterface());
        sharePresenter.attachView(this);

    }

    @Override
    protected void initListener() {

        findViewById(R.id.btn_done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyboard(edt_name);
                onBackPressed();
            }
        });

        findViewById(R.id.btn_submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkDataToSend();
            }
        });

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
    }

    @Override
    protected void onDestroy() {
        try { sharePresenter.detachView(); } catch (Exception e) { }
        super.onDestroy();
    }

    public void checkDataToSend(){

        hideKeyboard(edt_name);

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

        if(videoSubject.equals("Introduction")){

            if (strPhones.trim().length() == 0) {
                showToast(getString(R.string.empty_mobile));
                return;
            }

        } else
        {

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

        String review = "0";
        if(showReview){ review = "1"; }

        if(isInternetAvailable()) {
            showProgressDialog("");
            sharePresenter.shareVimeoURL(
                    strPhones, strEmails, videoSubject.toLowerCase(Locale.ROOT), "1",
                    getSessionManager().getUserType(), edt_name.getText().toString(),
                    getSessionManager().getUserId(), getSessionManager().getUserLocationId(),
                    edt_cname.getText().toString(), "Android", getSessionManager().getFCMToken(),
                    review, vimeoLink,"","",""
            );


        } else {
            showToast(getString(R.string.api_error_internet));
        }

    }


    /* API Methods*/

    @Override
    public void onSuccess(String message) {
        hideProgressDialog();
        showToast(message);
        onBackPressed();
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
