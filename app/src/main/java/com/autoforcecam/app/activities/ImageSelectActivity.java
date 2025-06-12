package com.autoforcecam.app.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.autoforcecam.app.CircleRecycler.CircleRecyclerView;
import com.autoforcecam.app.CircleRecycler.CircularHorizontalMode;
import com.autoforcecam.app.CircleRecycler.ItemViewMode;
import com.autoforcecam.app.R;
import com.autoforcecam.app.adapters.OverlayAdapter;
import com.autoforcecam.app.base.BaseActivity;
import com.autoforcecam.app.responses.Overlays;
import com.autoforcecam.app.utils.AppUtils;
import com.bumptech.glide.Glide;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

/* Created by JSP@nesar */

public class ImageSelectActivity extends BaseActivity {

    private String selectedImagePath = "", picName = "", finalPath = "", orientation = "";
    private RelativeLayout rl_preview;
    private Bitmap selectedImageBitmap;
    private CircleRecyclerView recycler_overlay;
    private ImageView img_selected;
    private ItemViewMode mItemViewMode;
    private LinearLayoutManager linearLayoutManager;
    private OverlayAdapter overlayAdapter;
    private ArrayList<Overlays> selectedOverlayList = new ArrayList<>();
    private ImageView img_actionbar_cross;
    private TextView txt_actionbar_more;


    @Override
    protected int getLayoutView() {
        return R.layout.act_image_select;
    }

    @Override
    protected Context getActivityContext() {
        return ImageSelectActivity.this;
    }

    @Override
    protected void initView() {

        recycler_overlay = findViewById(R.id.recycler_overlay);
        img_selected = findViewById(R.id.img_selected);
        img_actionbar_cross = findViewById(R.id.img_actionbar_cross);
        txt_actionbar_more = findViewById(R.id.txt_actionbar_more);
        rl_preview = findViewById(R.id.rl_preview);

    }

    @Override
    protected void initData() {

        img_actionbar_cross.setVisibility(View.VISIBLE);
        txt_actionbar_more.setVisibility(View.VISIBLE);

        txt_actionbar_more.setText("NEXT");


        linearLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        recycler_overlay.setLayoutManager(linearLayoutManager);
        mItemViewMode = new CircularHorizontalMode();
        recycler_overlay.setViewMode(mItemViewMode);
        recycler_overlay.setNeedCenterForce(false);

        selectedOverlayList = new ArrayList<>();
        overlayAdapter = new OverlayAdapter(context, selectedOverlayList);
        recycler_overlay.setAdapter(overlayAdapter);

        selectedImagePath = getIntent().getStringExtra("selectedImagePath");
        orientation = getIntent().getStringExtra("orientation");

        int screenWidth = getSessionManager().getScreenWidth();

        if(orientation.equalsIgnoreCase("landscape")){
            int height = screenWidth/4;
            height = height * 3;
            rl_preview.getLayoutParams().width = screenWidth;
            rl_preview.getLayoutParams().height = height;
        } else {
            rl_preview.getLayoutParams().width = screenWidth;
            rl_preview.getLayoutParams().height = screenWidth;
        }

        loadOverlaysToAdapter();

    }

    @Override
    protected void initListener() {

        Glide.with(context)
                .load("file://" + selectedImagePath)
                .into(img_selected);

        try {
            URL url = new URL("file://" + selectedImagePath);
           selectedImageBitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            Log.e("loadBitmaps", "selectedImageBitmap: "+selectedImageBitmap );
        } catch(IOException e) {
            System.out.println(e);
        }

       // selectedImageBitmap = ((BitmapDrawable) img_selected.getDrawable()).getBitmap();

//        imageLoader.displayImage("file://" + selectedImagePath, img_selected, options, new ImageLoadingListener() {
//            @Override
//            public void onLoadingStarted(String imageUri, View view) {
//                showProgressDialog("");
//            }
//            @Override
//            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
//                hideProgressDialog();
//                finishWithToast();
//            }
//            @Override
//            public void onLoadingCancelled(String imageUri, View view) {
//                hideProgressDialog();
//                finishWithToast();
//            }
//            @Override
//            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
//                hideProgressDialog();
//                selectedImageBitmap = ((BitmapDrawable) img_selected.getDrawable()).getBitmap();
//            }
//        });

        img_actionbar_cross.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        txt_actionbar_more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new MyAsyncTask().execute();
            }
        });

    }

    private void finishWithToast(){
        showToast(getString(R.string.api_error_internet));
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(context, ImageCaptureActivity.class));
        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
        finishAffinity();
    }

    private void loadOverlaysToAdapter() {

        ArrayList<Overlays> imageOverlayList = getSessionManager().getImageOverlayList();

        selectedOverlayList = new ArrayList<>();

        int position = 0;

        if (orientation.equalsIgnoreCase("landscape")) {

            String defaultOverlayId = getSessionManager().getPicLanOverlayId();

            for (int i = 0; i < imageOverlayList.size(); i++) {
                if (imageOverlayList.get(i).getOrientation().equalsIgnoreCase("landscape")) {
                    selectedOverlayList.add(imageOverlayList.get(i));
                    String overlayId = imageOverlayList.get(i).getId();
                    if(defaultOverlayId.equals(overlayId)){
                        position = selectedOverlayList.size();
                    }
                }
            }

        } else {

            String defaultOverlayId = getSessionManager().getPicPorOverlayId();

            for (int i = 0; i < imageOverlayList.size(); i++) {
                if (imageOverlayList.get(i).getOrientation().equalsIgnoreCase("portrait")) {
                    selectedOverlayList.add(imageOverlayList.get(i));
                    String overlayId = imageOverlayList.get(i).getId();
                    if(defaultOverlayId.equals(overlayId)){
                        position = selectedOverlayList.size();
                    }
                }
            }

        }

        overlayAdapter.updateList(selectedOverlayList);

        int finalPosition = position;

        Handler handler1 = new Handler();
        handler1.postDelayed(new Runnable() {
            @Override
            public void run() {
                recycler_overlay.scrollToPosition(finalPosition - 1);
            }
        }, 1500);

        Handler handler2 = new Handler();
        handler2.postDelayed(new Runnable() {
            @Override
            public void run() {
                recycler_overlay.setNeedCenterForce(true);
            }
        }, 2000);

    }



    // TODO Media Save Methods

    private class MyAsyncTask extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
//            showProgressDialog("");
        }

        @Override
        protected String doInBackground(String... strings) {
            savePicture();
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

          //  hideProgressDialog();

            String image_url = "", resolution = "";

            if(orientation.equalsIgnoreCase("landscape")){

                if(selectedOverlayList.size()!=0){
                    int currentPosition = linearLayoutManager.findFirstVisibleItemPosition();
                    String overlayId = selectedOverlayList.get(currentPosition).getId();
                    getSessionManager().setPicLanOverlayId(overlayId);
                    image_url = selectedOverlayList.get(currentPosition).getImage_url();
                } else {
                    getSessionManager().setPicLanOverlayId("");
                }

                resolution = AppUtils.picLandscapeResolution;

            } else {

                if(selectedOverlayList.size()!=0){
                    int currentPosition = linearLayoutManager.findFirstVisibleItemPosition();
                    String overlayId = selectedOverlayList.get(currentPosition).getId();
                    getSessionManager().setPicPorOverlayId(overlayId);
                    image_url = selectedOverlayList.get(currentPosition).getImage_url();
                } else {
                    getSessionManager().setPicPorOverlayId("");
                }

                resolution = AppUtils.picPortraitResolution;

            }

            startActivity(new Intent(context, ImagePreviewActivity.class)
                    .putExtra("differenceInPosition", 0)
                    .putExtra("isImageSelected", true)
                    .putExtra("capturedImagePath", finalPath)
                    .putExtra("picName", picName)
                    .putExtra("logoImageURL", image_url)
                    .putExtra("resolution", resolution)
                    .putExtra("orientation", orientation)
            );
            overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);

        }
    }

    private void savePicture() {

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        selectedImageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] png = stream.toByteArray();

        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/SalesCAM/Pics";
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        picName = "pic_" + AppUtils.getTimeStamp() + ".png";

        File photo = new File(dir, picName);

        if (photo.exists()) {
            photo.delete();
        }

        try {

            FileOutputStream fos = new FileOutputStream(photo.getPath());
            fos.write(png);
            fos.close();

            finalPath = photo.getPath();
            Log.e("loadBitmaps", "finalPath: "+finalPath );

        } catch (java.io.IOException e) {
            Log.e("loadBitmaps", "IOException: "+e );
        }

    }

}
