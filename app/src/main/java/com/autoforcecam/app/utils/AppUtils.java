package com.autoforcecam.app.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.EditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class AppUtils {

    public static boolean isImageInstructionsShown = false;
    public static boolean isVideoInstructionsShown = false;

    public static String picPortraitResolution = "1200x1200";
    public static String picLandscapeResolution = "1350x1012";
    public static String videoPortraitResolution = "1280x720";

    public static int get5DigitsRandom() {
        Random r = new Random( System.currentTimeMillis() );
        return 10000 + r.nextInt(20000);
    }

    public static boolean isDropBox(Uri uri) {
        return String.valueOf(uri).toLowerCase().contains("content://com.dropbox.");
    }

    public static boolean isGoogleDrive(Uri uri) {
        return String.valueOf(uri).toLowerCase().contains("com.google.android.apps");
    }

    public static boolean isOneDrive(Uri uri) {
        return String.valueOf(uri).toLowerCase().contains("com.microsoft.skydrive.content");
    }

    public static String copyFileToInternalStorage(Context context, Uri uri, String newDirName) {
        Uri returnUri = uri;

        Cursor returnCursor = context.getContentResolver().query(returnUri, new String[]{
                OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE
        }, null, null, null);

        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        String name = (returnCursor.getString(nameIndex));
        String size = (Long.toString(returnCursor.getLong(sizeIndex)));

        String videoPath = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            videoPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
        } else {
            videoPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        }

        File dir = new File(videoPath + "/" + newDirName);
        if (!dir.exists()) {
            dir.mkdir();
        }
        
        File output = new File(videoPath + "/" + newDirName + "/" + name);

        try {

            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(output);
            int read = 0;
            int bufferSize = 1024;
            final byte[] buffers = new byte[bufferSize];
            while ((read = inputStream.read(buffers)) != -1) {
                outputStream.write(buffers, 0, read);
            }

            inputStream.close();
            outputStream.close();

        }
        catch (Exception e) {

            Log.e("Exception", e.getMessage());
        }

        return output.getPath();
    }

    public static boolean isStringEmpty(String str) {

        if (str.trim().length() == 0) {
            return true;
        } else {
            return false;
        }

    }

    public static boolean isEditTextEmpty(EditText editText) {

        if (editText.getText().toString().trim().isEmpty()) {
            return true;
        } else {
            return false;
        }

    }

    public static boolean isValidEmail(EditText editText) {

        String email = editText.getText().toString();

        if (isStringEmpty(email)) {
            return false;
        } else {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
        }

    }

    public static boolean isValidEmail(String strEmail) {


        if (isStringEmpty(strEmail)) {
            return false;
        } else {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(strEmail).matches();
        }

    }

    public static Long getTimeStamp() {

        Long longTimeStamp = System.currentTimeMillis() / 1000;
        return longTimeStamp;

    }

}
