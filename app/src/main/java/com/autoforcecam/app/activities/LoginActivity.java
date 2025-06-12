package com.autoforcecam.app.activities;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import com.autoforcecam.app.R;
import com.autoforcecam.app.base.BaseActivity;
import com.autoforcecam.app.dialog.InformationDialog;
import com.autoforcecam.app.interfaces.LoginInterface;
import com.autoforcecam.app.presenters.LoginPresenter;
import com.autoforcecam.app.responses.DefaultResolution;
import com.autoforcecam.app.responses.LoginResponse;
import com.autoforcecam.app.utils.AppUtils;

/* Created by JSP@nesar */

public class LoginActivity extends BaseActivity implements LoginInterface {

    private EditText edt_email, edt_code;
    private LoginPresenter loginPresenter;

    @Override
    protected int getLayoutView() {
        return R.layout.act_login;
    }

    @Override
    protected Context getActivityContext() {
        return LoginActivity.this;
    }

    @Override
    protected void initView() {

        edt_email = findViewById(R.id.edt_email);
        edt_code = findViewById(R.id.edt_code);

    }

    @Override
    protected void initData() {

        loginPresenter = new LoginPresenter(getAPIInterface());
        loginPresenter.attachView(this);

    }

    @Override
    protected void initListener() {}

    public void checkLoginData(View view){

        hideKeyboard(edt_email);

        if(AppUtils.isEditTextEmpty(edt_email)){
            showToast(getString(R.string.empty_email));
            return;
        }

//        if(!AppUtils.isValidEmail(edt_email)){
//            showToast(getString(R.string.invalid_email));
//            return;
//        }

        if(AppUtils.isEditTextEmpty(edt_code)){
            showToast(getString(R.string.empty_code));
            return;
        }

        if(isInternetAvailable()){

            showProgressDialog("");
            loginPresenter.loginUser(
                    edt_email.getText().toString(),
                    edt_code.getText().toString(),
                    getDeviceID(),
                    getSessionManager().getFCMToken()
            );

        }else {
            showToast(getString(R.string.api_error_internet));
        }

    }

    @Override
    public void onSuccess_Login(LoginResponse loginResponse) {

        hideProgressDialog();

        if(getSessionManager().incrementCounter()){
            getSessionManager().setIncrementCounter(false);
            int counter = getSessionManager().getCounterValue();
            if(counter<5){
                counter++;
                getSessionManager().setCounterValue(counter);
            }
        }

        try{

            String userId = loginResponse.getData().getUser_id();
            String userType = loginResponse.getData().getUserType();
            String userName = loginResponse.getData().getUsername();
            String userCode = loginResponse.getData().getCode();
            String userLocationId = loginResponse.getData().getLocation_id();
            DefaultResolution defaultResolution = loginResponse.getData().getDefaultResolution();

            getSessionManager().setSandBoxKeys(loginResponse.getData().getSandbox_key());
            getSessionManager().setLiveKeys(loginResponse.getData().getLive_key());

            getSessionManager().setUserId(userId);
            getSessionManager().setUserType(userType);
            getSessionManager().setUserName(userName);
            getSessionManager().setUserCode(userCode);
            getSessionManager().setUserLocationId(userLocationId);
            getSessionManager().setDefaultResolution(defaultResolution);
            getSessionManager().setUserLoginStatus(true);

//            startActivity(new Intent(context, HowToUseActivity.class)
//                .putExtra("showFirstInstruction", true)
//            );

            startActivity(new Intent(context, HomeActivity.class)
            .putExtra("showOverlayProgress", true)
            );
            overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            finish();

        }catch (Exception e){

            showToast(getString(R.string.api_error));

        }

    }

    @Override
    public void onUserBlocked(String message) {

        hideProgressDialog();
        new InformationDialog(context, getString(R.string.account_error), message).show();

    }

    @Override
    public void onError(String message) {

        hideProgressDialog();
        showToast(message);

    }

    @Override
    protected void onDestroy() {

        try{ loginPresenter.detachView(); } catch (Exception e){}
        super.onDestroy();

    }

}
