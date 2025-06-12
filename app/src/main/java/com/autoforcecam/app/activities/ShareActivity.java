package com.autoforcecam.app.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
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
import androidx.appcompat.widget.AppCompatSpinner;

import com.autoforcecam.app.R;
import com.autoforcecam.app.base.BaseActivity;
import com.autoforcecam.app.interfaces.PermissionsInterface;
import com.autoforcecam.app.interfaces.ShareInterface;
import com.autoforcecam.app.models.VideoSegmentModel;
import com.autoforcecam.app.presenters.SharePresenter;
import com.autoforcecam.app.utils.AppUtils;
import com.hbb20.CountryCodePicker;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import io.michaelrocks.libphonenumber.android.PhoneNumberUtil;
import io.michaelrocks.libphonenumber.android.Phonenumber;

/* Created by JSP@nesar */

public class ShareActivity extends BaseActivity implements PermissionsInterface, ShareInterface {

    private SharePresenter sharePresenter;

    boolean isdesShown=false;
    private LinearLayout linearBack;
    RelativeLayout mainLayout;
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

                        Cursor phoneCursor = ShareActivity.this.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);
                        while (phoneCursor.moveToNext()) {
                            String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            String normalizedPhoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER));
                            Log.v("onActivityResult", "phone # - " + phoneNumber);
                            PhoneNumberUtil phoneUtil = PhoneNumberUtil.createInstance(ShareActivity.this);

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
    private String  strEmails = "", strPhones = "",videoType="";


    EditText edt_mobile,edt_name,edt_email,edt_your_name,edt_desc,edtEmailMsg,edt_title;
    private CountryCodePicker ccp;
    private TextView txtIntroTitle, txtWalkTitle, txtThankTitle;
    private AppCompatSpinner spinner_videoType;
    private EditText edtIntroUrl;
    private Button btnIntro;
    RelativeLayout defaultShareLayout;

    LinearLayout contactLayout;

    @Override
    protected int getLayoutView() {
        return R.layout.act_share;
    }

    @Override
    protected Context getActivityContext() {
        return ShareActivity.this;
    }

    @Override
    protected void initView() {
        permissionsInterface = this;
        sharePresenter = new SharePresenter(getAPIInterface());
        sharePresenter.attachView(this);
        defaultShareLayout=findViewById(R.id.defaultShareLayout);
        contactLayout=findViewById(R.id.contactLayout);
        edtIntroUrl=findViewById(R.id.edtIntroUrl);
        btnIntro=findViewById(R.id.btnIntro);
        mainLayout=findViewById(R.id.mainLayout);

        linearBack = findViewById(R.id.linearBack);
        txtIntroTitle = findViewById(R.id.txtIntroTitle);
        spinner_videoType = findViewById(R.id.spinner_videoType);
        edt_mobile = findViewById(R.id.edt_mobile);
        edt_name = findViewById(R.id.edt_name);
        edt_email = findViewById(R.id.edt_email);
        edt_your_name = findViewById(R.id.edt_your_name);
        ccp = findViewById(R.id.ccp);
        edt_desc = findViewById(R.id.edt_desc);
        edt_title = findViewById(R.id.edt_title);
        edtEmailMsg = findViewById(R.id.edtEmailMsg);
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


        videoType=items[0];

        edt_your_name.setText(getSessionManager().getMyName());
        String newString = getSessionManager().getVideoType()[0].getSms();

        String output=newString.replace("[NAME]", edt_name.getText().toString()).replace("[Subject]", videoType)
                .replace("[VIDEO_LINK]", edtIntroUrl.getText().toString()).replace("[Your Name]", edt_your_name.getText().toString());

        edt_desc.setText(output);

        String emailString = getSessionManager().getVideoType()[0].getEmailBody();

        String emailOutput=emailString.replace("[NAME]", edt_name.getText().toString()).replace("[Subject]", videoType)
                .replace("[VIDEO_LINK]", edtIntroUrl.getText().toString()).replace("[Your Name]", edt_your_name.getText().toString());

        edtEmailMsg.setText(emailOutput);



        for (int i=0;i<getSessionManager().getVideoType().length;i++){
            items[i]=getSessionManager().getVideoType()[i].getName();

        }



        ArrayAdapter ad = new ArrayAdapter(this, android.R.layout.simple_spinner_item, items);

        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner_videoType.setAdapter(ad);

        ccp.registerCarrierNumberEditText(edt_mobile);

        try{
            String defaultCountrycode = getSessionManager().getPreformattedMessage().getCc_suffix_iso();
            ccp.setDefaultCountryUsingNameCode(defaultCountrycode);
            ccp.resetToDefaultCountry();
        }catch (Exception e){}

        spinner_videoType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                videoType=items[position];

                String newString = getSessionManager().getVideoType()[position].getSms();

                String output=newString.replace("[NAME]", edt_name.getText().toString()).replace("[Subject]", videoType)
                        .replace("[VIDEO_LINK]", edtIntroUrl.getText().toString()).replace("[Your Name]", edt_your_name.getText().toString());

                edt_desc.setText(output);

                String emailString = getSessionManager().getVideoType()[position].getEmailBody();

                String emailOutput=emailString.replace("[NAME]", edt_name.getText().toString()).replace("[Subject]", videoType)
                        .replace("[VIDEO_LINK]", edtIntroUrl.getText().toString()).replace("[Your Name]", edt_your_name.getText().toString());

                edtEmailMsg.setText(emailOutput);

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

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
                                .replace("[VIDEO_LINK]", edtIntroUrl.getText().toString()).replace("[Your Name]", edt_your_name.getText().toString());

                        edt_desc.setText(output);

                        String emailString = getSessionManager().getVideoType()[i].getEmailBody();

                        String emailOutput=emailString.replace("[NAME]", edt_name.getText().toString()).replace("[Subject]", videoType)
                                .replace("[VIDEO_LINK]", edtIntroUrl.getText().toString()).replace("[Your Name]", edt_your_name.getText().toString());

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
                                .replace("[VIDEO_LINK]", edtIntroUrl.getText().toString()).replace("[Your Name]", edt_your_name.getText().toString());

                        edt_desc.setText(output);

                        String emailString = getSessionManager().getVideoType()[i].getEmailBody();

                        String emailOutput=emailString.replace("[NAME]", edt_name.getText().toString()).replace("[Subject]", videoType)
                                .replace("[VIDEO_LINK]", edtIntroUrl.getText().toString()).replace("[Your Name]", edt_your_name.getText().toString());

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
                }else {
                    if (isdesShown){
                        radioGroup.setVisibility(View.VISIBLE);

                    }
                }
            }
        });

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

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton rb=(RadioButton)findViewById(checkedId);
                if (rb==radioSMS && isdesShown){
                    edt_desc.setVisibility(View.VISIBLE);
                    edtEmailMsg.setVisibility(View.GONE);
                }

                if (rb==radioEmail && isdesShown){
                    edtEmailMsg.setVisibility(View.VISIBLE);
                    edt_desc.setVisibility(View.GONE);
                }



            }
        });


        String introLink = getSessionManager().getIntroLink();


        edtIntroUrl.setText(introLink);

        if(introLink.isEmpty()){
            txtIntroTitle.setText("Add Video URL:");
        } else {
            txtIntroTitle.setText("Current Video URL:");
        }

    }

    @Override
    protected void initListener() {

        mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
            }
        });

        defaultShareLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
            }
        });

        contactLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
            }
        });

        linearBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        btnIntro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String vimeoLink = edtIntroUrl.getText().toString();
                if(vimeoLink.isEmpty()){
                    showToast("Please enter video url.");
                } else {
                    getSessionManager().setIntroLink(vimeoLink);

                    defaultShareLayout.setVisibility(View.GONE);
                    contactLayout.setVisibility(View.VISIBLE);

//                    startActivity(
//                            new Intent(context, ShareDetailsActivity.class)
//                                    .putExtra("videoSubject", "Introduction")
//                                    .putExtra("vimeoLink", vimeoLink)
//                    );
//                    overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                }
            }
        });


        findViewById(R.id.backImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                defaultShareLayout.setVisibility(View.VISIBLE);
                contactLayout.setVisibility(View.GONE);

            }
        });

        findViewById(R.id.defaultMessageDownImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (edt_email.getText().toString().isEmpty()){
                    radioGroup.setVisibility(View.GONE);
                    if (isdesShown){
                        edt_desc.setVisibility(View.GONE);
                        edtEmailMsg.setVisibility(View.GONE);
                        isdesShown=false;
                    }else {
                        isdesShown=true;
                        edt_desc.setVisibility(View.VISIBLE);
                        edtEmailMsg.setVisibility(View.GONE);
                    }

                }else {
                    radioGroup.setVisibility(View.VISIBLE);
                    if (isdesShown){
                        edt_desc.setVisibility(View.GONE);
                        edtEmailMsg.setVisibility(View.GONE);
                        radioGroup.setVisibility(View.GONE);
                        isdesShown=false;
                    }else {
                        isdesShown=true;

                        radioGroup.getCheckedRadioButtonId();
                        Log.e("edtEmailMsg", "onClick: "+radioSMS.isChecked()+"  email "+radioEmail.isChecked() );

                        if (radioSMS.isChecked()){
                            edt_desc.setVisibility(View.VISIBLE);
                            edtEmailMsg.setVisibility(View.GONE);
                        }else if (radioEmail.isChecked()){

                            edt_desc.setVisibility(View.GONE);
                            edtEmailMsg.setVisibility(View.VISIBLE);
                        }

                    }

                }



            }
        });

        findViewById(R.id.btn_done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
            }
        });

        findViewById(R.id.btn_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyboard(view);
                if(AppUtils.isEditTextEmpty(edt_name)){
                    showToast(getString(R.string.empty_cname));
                    return;
                }
                if(AppUtils.isEditTextEmpty(edt_your_name)){
                    showToast(getString(R.string.empty_yname));
                    return;
                }

                if(AppUtils.isEditTextEmpty(edt_title)){
                    showToast(getString(R.string.empty_title));
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



                String review = "0";

                if(videoType.equals("Introduction")){
                    review="0";
                } else if(videoType.equals("General")){
                    review="0";
                } else {

                    review="1";
                }

                String smsBody=edt_desc.getText().toString();

                String emailBody="";
                if (edt_email.getText().toString().isEmpty()){
                    emailBody="";
                }else {
                    emailBody=edtEmailMsg.getText().toString();
                }

                if(isInternetAvailable()) {
                    showProgressDialog("");
                    sharePresenter.shareVimeoURL(
                            strPhones, strEmails, videoType.toLowerCase(Locale.ROOT), "1",
                            getSessionManager().getUserType(), edt_your_name.getText().toString(),
                            getSessionManager().getUserId(), getSessionManager().getUserLocationId(),
                            edt_name.getText().toString(), "Android", getSessionManager().getFCMToken(),
                            review, edtIntroUrl.getText().toString().trim(),smsBody,emailBody,edt_title.getText().toString()
                    );


                } else {
                    showToast(getString(R.string.api_error_internet));
                }


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

//        btnWalk.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                String vimeoLink = edtWalkUrl.getText().toString();
//                if(vimeoLink.isEmpty()){
//                    showToast("Please enter video url.");
//                } else {
//                    getSessionManager().setWalkLink(vimeoLink);
//                    startActivity(
//                            new Intent(context, ShareDetailsActivity.class)
//                                    .putExtra("videoSubject", "General")
//                                    .putExtra("vimeoLink", vimeoLink)
//                    );
//                    overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
//                }
//            }
//        });
//
//        btnThank.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                String vimeoLink = edtThankUrl.getText().toString();
//                if(vimeoLink.isEmpty()){
//                    showToast("Please enter video url.");
//                } else {
//                    getSessionManager().setThankLink(vimeoLink);
//                    startActivity(
//                            new Intent(context, ShareDetailsActivity.class)
//                                    .putExtra("videoSubject", "Thank You")
//                                    .putExtra("vimeoLink", vimeoLink)
//                    );
//                    overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
//                }
//            }
//        });

    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
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
