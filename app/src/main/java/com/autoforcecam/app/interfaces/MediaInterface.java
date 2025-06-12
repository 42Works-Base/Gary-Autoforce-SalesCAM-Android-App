package com.autoforcecam.app.interfaces;


import com.autoforcecam.app.base.MvpView;
import com.autoforcecam.app.responses.OverlayResponse;

public interface MediaInterface extends MvpView {

    void onSuccess_GetOverlays(OverlayResponse overlayResponse);

}
