package com.autoforcecam.app.interfaces;


import com.autoforcecam.app.base.MvpView;

public interface SendInterface extends MvpView {

    void onSuccess_ShareMedia(String sendType, String message);

}
