package com.autoforcecam.app.interfaces;

import java.util.concurrent.ExecutionException;

public interface PermissionsInterface {

    void onPermissionsDenied(boolean isPermanentalyDenied);
    void onPermissionsGranted();
    void onPermissionsCancelled(boolean isSettingsOpened);

}
