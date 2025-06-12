package com.autoforcecam.app.interfaces;


import com.autoforcecam.app.base.MvpView;

public interface MediaUploadInterface extends MvpView {

    void onSuccess_MediaUpload(String postId, String mediaUrl);

}
