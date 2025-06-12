package com.autoforcecam.app.interfaces;


import com.autoforcecam.app.base.MvpView;
import com.autoforcecam.app.responses.LoginResponse;

public interface LoginInterface extends MvpView {

    void onSuccess_Login(LoginResponse loginResponse);

}
