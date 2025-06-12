package com.autoforcecam.app.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.camera.core.CameraSelector;
import androidx.camera.lifecycle.ProcessCameraProvider;

import com.autoforcecam.app.R;
import com.autoforcecam.app.base.BaseActivity;
import com.autoforcecam.app.interfaces.PermissionsInterface;
import com.autoforcecam.app.interfaces.SendInterface;
import com.autoforcecam.app.presenters.SendPresenter;
import com.autoforcecam.app.utils.AppUtils;
import com.autoforcecam.app.utils.SoftInputAssist;
import com.hbb20.CountryCodePicker;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

import io.michaelrocks.libphonenumber.android.PhoneNumberUtil;
import io.michaelrocks.libphonenumber.android.Phonenumber;

/* Created by JSP@nesar */

public class UploadedActivity extends BaseActivity implements SendInterface, PermissionsInterface {

    private boolean askPermissionsAgain = true;
    SoftInputAssist softInputAssist;
    private String mediaLink = "", postId = "";
    private EditText edt_name, edt_cname, edt_year, edt_email,edt_mobile;
    private CountryCodePicker ccp;
    private SendPresenter sendPresenter;
    private PermissionsInterface permissionsInterface;
    LinearLayout popupLayout;
    RelativeLayout emailLayout,smsLayout;
    TextView email_txt_heading,sms_txt_heading;
    ImageView email_img_close,sms_img_close;
    RelativeLayout mainLayout;
    LinearLayout defaultSettingsLayout;


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
                        Cursor phoneCursor = UploadedActivity.this.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);
                        while (phoneCursor.moveToNext()) {
                            String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            String normalizedPhoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER));
                            Log.v("onActivityResult", "phone # - " + phoneNumber);
                            PhoneNumberUtil phoneUtil = PhoneNumberUtil.createInstance(UploadedActivity.this);

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

                            edt_cname.setText(name);
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
    @Override
    protected int getLayoutView() {
        return R.layout.act_uploaded;
    }

    @Override
    protected Context getActivityContext() {
        return UploadedActivity.this;
    }

    @Override
    protected void initView() {
        popupLayout = findViewById(R.id.popupLayout);
        emailLayout = findViewById(R.id.emailLayout);
        smsLayout = findViewById(R.id.smsLayout);
        email_txt_heading = findViewById(R.id.email_txt_heading);
        sms_txt_heading = findViewById(R.id.sms_txt_heading);
        email_img_close = findViewById(R.id.email_img_close);
        sms_img_close = findViewById(R.id.sms_img_close);
        defaultSettingsLayout = findViewById(R.id.defaultSettingsLayout);
        mainLayout = findViewById(R.id.mainLayout);

        edt_name = findViewById(R.id.edt_name);
        edt_cname = findViewById(R.id.edt_cname);
        edt_year = findViewById(R.id.edt_year);
        edt_email = findViewById(R.id.edt_email);
        edt_mobile = findViewById(R.id.edt_mobile);
        ccp = findViewById(R.id.ccp);

        edt_email.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        edt_email.setRawInputType(InputType.TYPE_CLASS_TEXT);
        edt_mobile.setImeOptions(EditorInfo.IME_ACTION_DONE);


        softInputAssist=new SoftInputAssist(this);
    }

    @Override
    protected void initData() {
        permissionsInterface = this;

        edt_name.setText(getSessionManager().getMyName());


        sendPresenter = new SendPresenter(getAPIInterface());
        sendPresenter.attachView(this);

        mediaLink = getIntent().getStringExtra("mediaLink");
        postId = getIntent().getStringExtra("postId");

        ccp.registerCarrierNumberEditText(edt_mobile);

        try{
            String defaultCountrycode = getSessionManager().getPreformattedMessage().getCc_suffix_iso();
            ccp.setDefaultCountryUsingNameCode(defaultCountrycode);
            ccp.resetToDefaultCountry();
        }catch (Exception e){}

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

        findViewById(R.id.btn_done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                startActivity(new Intent(context, ImageCaptureActivity.class));
                overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                finishAffinity();

            }
        });

        findViewById(R.id.btn_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                checkDataToSend();

            }
        });
        findViewById(R.id.contactButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    checkPermissions();
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        email_img_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               emailLayout.setVisibility(View.GONE);
            }
        });

        sms_img_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                smsLayout.setVisibility(View.GONE);
            }
        });

    }

    @Override
    public void onBackPressed() {

        super.onBackPressed();
        startActivity(new Intent(context, ImageCaptureActivity.class));
        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
        finishAffinity();

    }

    public void checkDataToSend(){

       // hideKeyboard(edt_name);

        if(AppUtils.isEditTextEmpty(edt_name)){
            showToast(getString(R.string.empty_yname));
            return;
        }

        getSessionManager().setMyName(edt_name.getText().toString());

        if(AppUtils.isEditTextEmpty(edt_cname)){
            showToast(getString(R.string.empty_cname));
            return;
        }

        String strEmails = edt_email.getText().toString();
        String strPhones = edt_mobile.getText().toString();

        if(strEmails.trim().length()==0 && strPhones.trim().length()==0){

            showToast(getString(R.string.empty_email));
            return;

        } else {

            if(strEmails.trim().length()!=0) {

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
                    if(i==0){
                        strEmails = emails[i];
                    } else {
                        strEmails = strEmails + "," + emails[i];
                    }
                }

            } else {

                if(AppUtils.isEditTextEmpty(edt_mobile)){
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

            if(isInternetAvailable()){

                if(strEmails.trim().length()!=0){

                    String message = getSessionManager().getPreformattedMessage().getEmail_body();
                    message = message.replace("[NAME]", edt_cname.getText().toString());
                    message = message.replace("[Year Make Model]", "");
                    message = message.replace("[PHOTO_LINK]", mediaLink);
                    message = message.replace("[VIDEO_LINK]", "");
                    message = message.replace("[Your Name]", edt_name.getText().toString());
                    message = message.replace("[PASSWORD]", "");

                    showProgressDialog("");

                    sendPresenter.sendSmsOrEmail(
                            strEmails,
                            message,
                            "",
                            "",
                            message,
                            "",
                            mediaLink,
                            getSessionManager().getUserLocationId(),
                            "email",
                            postId,
                            getSessionManager().getUserId(),
                            getSessionManager().getUserType()

                    );

                }

                if(strPhones.trim().length()!=0){

                    String message = getSessionManager().getPreformattedMessage().getPreformatted_sms();
                    message = message.replace("[NAME]", edt_cname.getText().toString());
                    message = message.replace("[Year Make Model]", "");
                    message = message.replace("[PHOTO_LINK]", mediaLink);
                    message = message.replace("[VIDEO_LINK]", "");
                    message = message.replace("[Your Name]", edt_name.getText().toString());
                    message = message.replace("[PASSWORD]", "");

                    sendPresenter.sendSmsOrEmail(
                            "",
                            message,
                            "",
                            strPhones,
                            message,
                            "",
                            mediaLink,
                            getSessionManager().getUserLocationId(),
                            "sms",
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
    protected void onDestroy() {
        softInputAssist.onDestroy();
        try{ sendPresenter.detachView(); } catch (Exception e){}
        super.onDestroy();
    }

    @Override
    public void onSuccess_ShareMedia(String sendType, String message) {

        hideProgressDialog();

        edt_cname.setText("");

        popupLayout.setVisibility(View.VISIBLE);
        if(sendType.equals("sms")){
            edt_mobile.setText("");
            smsLayout.setVisibility(View.VISIBLE);
         //   showPopUpDialog("Your SMS has successfully sent");
            //showToast("SMS has been sent successfully.");
        } else {
            edt_email.setText("");
            emailLayout.setVisibility(View.VISIBLE);
           // showPopUpDialog("Your email has successfully sent");
           // showToast("Email has been sent successfully.");
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
      //  showPopUpDialog(message);
        sms_txt_heading.setText(message);
        smsLayout.setVisibility(View.VISIBLE);
        //showToast(message);

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

    void showPopUpDialog(String message){
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(R.layout.dialog_popup);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        TextView title=dialog.findViewById(R.id.txt_heading);
        ImageView close=dialog.findViewById(R.id.img_close);
        title.setText(message);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
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
