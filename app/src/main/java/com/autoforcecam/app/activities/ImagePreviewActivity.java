package com.autoforcecam.app.activities;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.CameraSelector;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.exifinterface.media.ExifInterface;
import androidx.viewpager.widget.ViewPager;

import com.autoforcecam.app.R;
import com.autoforcecam.app.adapters.ViewPagerAdapter;
import com.autoforcecam.app.base.BaseActivity;
import com.autoforcecam.app.fragments.EditImageFragment;
import com.autoforcecam.app.fragments.FiltersListFragment;
import com.autoforcecam.app.interfaces.EditImageInterface;
import com.autoforcecam.app.interfaces.FiltersListInterface;
import com.autoforcecam.app.interfaces.PermissionsInterface;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.tabs.TabLayout;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.autoforcecam.app.photofilters.imageprocessors.Filter;
import com.autoforcecam.app.photofilters.imageprocessors.subfilters.BrightnessSubFilter;
import com.autoforcecam.app.photofilters.imageprocessors.subfilters.ContrastSubFilter;
import com.autoforcecam.app.photofilters.imageprocessors.subfilters.SaturationSubfilter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

/* Created by JSP@nesar */

public class ImagePreviewActivity extends BaseActivity implements FiltersListInterface, EditImageInterface, PermissionsInterface {

    private Collection<String> permissions = Arrays.asList(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE);

    private ImageLoader imageLoader;
    private DisplayImageOptions options;


    private boolean askPermissionsAgain = true;
    private String picsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/SalesCAM/Pics";
    private String picName = "", capturedImagePath = "", logoImageURL = "", orientation = "portrait", finalPath = "",selectedCameraPosition="";
    private int imageWidth = 0, imageHeight = 0, differenceInPosition = 0;
    private boolean isImageSelected = false;
    private ImageView img_preview, img_overlay, img_actionbar_cross;
    private TextView txt_actionbar_more;
    private Bitmap capturedImageBitmap, overlayBitmap, filteredImage, finalImage;


    private TabLayout tabs;
    private ViewPager viewPager;

    private FiltersListFragment filtersListFragment;
    private EditImageFragment editImageFragment;

    private int brightnessFinal = 0;
    private float saturationFinal = 1.0f;
    private float contrastFinal = 1.0f;

    static {
        System.loadLibrary("NativeImageProcessor");
    }

    @Override
    protected int getLayoutView() {
        return R.layout.act_imagepreview;
    }

    @Override
    protected Context getActivityContext() {
        return ImagePreviewActivity.this;
    }

    @Override
    protected void initView() {


        img_actionbar_cross = findViewById(R.id.img_actionbar_cross);
        txt_actionbar_more = findViewById(R.id.txt_actionbar_more);
        img_preview = findViewById(R.id.img_preview);
        img_overlay = findViewById(R.id.img_overlay);
        tabs = findViewById(R.id.tabs);
        viewPager = findViewById(R.id.viewpager);


        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion > 32){
            permissions = Arrays.asList(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.ACCESS_MEDIA_LOCATION);

        }else {
            permissions = Arrays.asList(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if(askPermissionsAgain) {
            try {
                checkPermissions();
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
    }

    @Override
    protected void initData() {

        txt_actionbar_more.setText("NEXT");
        img_actionbar_cross.setVisibility(View.VISIBLE);
        txt_actionbar_more.setVisibility(View.VISIBLE);

        txt_actionbar_more.setEnabled(false);

        imageLoader = ImageLoader.getInstance();
        imageLoader.init(ImageLoaderConfiguration.createDefault(context));
        options = new DisplayImageOptions.Builder()
                .resetViewBeforeLoading(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .bitmapConfig(Bitmap.Config.RGB_565).build();

        differenceInPosition = getIntent().getIntExtra("differenceInPosition", 0);
        isImageSelected = getIntent().getBooleanExtra("isImageSelected", false);
        capturedImagePath = getIntent().getStringExtra("capturedImagePath");
        picName = getIntent().getStringExtra("picName");
        logoImageURL = getIntent().getStringExtra("logoImageURL");
        orientation = getIntent().getStringExtra("orientation");
        String resolution = getIntent().getStringExtra("resolution");
        selectedCameraPosition=getIntent().getStringExtra("selectedCameraPosition");

        Log.e("initData", "initData: "+capturedImagePath );

        try{
            imageWidth = Integer.parseInt(resolution.split("x")[0]);
            imageHeight = Integer.parseInt(resolution.split("x")[1]);
        } catch (Exception e){}

        setupViewPager();


        if(isInternetAvailable()){

            showProgressDialog("Please wait...");
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    hideProgressDialog();
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            getBitmaps();
                        }
                    }, 1000);

                }
            }, 1000);

        }else {
            finishWithToast();
        }


    }



    @Override
    protected void initListener() {

        img_actionbar_cross.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(isImageSelected){
                    finish();
                    overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                }else {
                    startActivity(new Intent(context, ImageCaptureActivity.class));
                    overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                    finishAffinity();
                }

            }
        });

        txt_actionbar_more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                if(overlayBitmap!=null){
                    Log.e("loadBitmaps", "overlayBitmap: "+overlayBitmap );
                    finalImage = createSingleImageFromMultipleImages(filteredImage, overlayBitmap, true);
                    Log.e("finalImage", "onClick: "+finalImage );
                } else {
                    Log.e("loadBitmaps", "filteredImage: "+filteredImage );
                    finalImage = filteredImage;
                }

                new MyAsyncTask().execute();

            }
        });

    }


    @Override
    public void onBackPressed() {

        if(isImageSelected){
            finish();
            overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
        }else {
            startActivity(new Intent(context, ImageCaptureActivity.class));
            overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
            finishAffinity();
        }

    }

    private void getBitmaps(){


        if(logoImageURL.trim().length()!=0){



//            Glide.with(getApplicationContext())
//                    .asBitmap()
//                    .load("https://www.google.es/images/srpr/logo11w.png")
//                    .into(new CustomTarget<Bitmap>(100,100) {
//                        @Override
//                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
//
//                        }
//
//                        @Override
//                        public void onLoadCleared(@Nullable Drawable placeholder) {
//
//                        }
//
//                    });


            imageLoader.displayImage(logoImageURL, img_overlay, options, new ImageLoadingListener() {
                @Override
                public void onLoadingStarted(String imageUri, View view) {
                       showProgressDialog("");
                }
                @Override
                public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                    hideProgressDialog();
                    finishWithToast();
                }
                @Override
                public void onLoadingCancelled(String imageUri, View view) {
                    hideProgressDialog();
                    finishWithToast();
                }
                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    Log.e("onLoadingComplete", "onLoadingComplete: "+loadedImage );
                    hideProgressDialog();
                    overlayBitmap = loadedImage;
                    loadBitmaps();
                }
            });

//            imageLoader.displayImage(logoImageURL, img_overlay, options, new ImageLoadingListener() {
//                @Override
//                public void onLoadingStarted(String imageUri, View view) {
//                    showProgressDialog("");
//                }
//                @Override
//                public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
//                    hideProgressDialog();
//                    finishWithToast();
//                }
//                @Override
//                public void onLoadingCancelled(String imageUri, View view) {
//                    hideProgressDialog();
//                    finishWithToast();
//                }
//                @Override
//                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
//                    hideProgressDialog();
//                    overlayBitmap = ((BitmapDrawable) img_overlay.getDrawable()).getBitmap();
//                    loadBitmaps();
//                }
//            });

        } else {

            overlayBitmap = null;
            loadBitmaps();

        }

    }

    private void loadBitmaps(){

        try {

            Log.e("loadBitmaps", "isImageSelected: "+isImageSelected );
            if (isImageSelected){
                BitmapFactory.Options options = new BitmapFactory.Options();
                Log.e("loadBitmaps", "options: "+options );
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                capturedImageBitmap =  BitmapFactory.decodeFile(capturedImagePath, options);
                Log.e("loadBitmaps", "capturedImageBitmap: "+capturedImageBitmap );
            }else {

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    capturedImageBitmap =  BitmapFactory.decodeFile(capturedImagePath, options);
                Log.e("loadBitmaps", "capturedImageBitmap: "+capturedImageBitmap );

                Log.e("", "loadBitmaps: "+selectedCameraPosition +" oo "+orientation);
                ExifInterface ei = new ExifInterface(capturedImagePath);
                int orientation1 = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_UNDEFINED);
                Log.e("", "loadBitmaps: "+orientation1 );
                switch(orientation1) {

                    case ExifInterface.ORIENTATION_ROTATE_90:
                        capturedImageBitmap = rotateImage(capturedImageBitmap, 90);
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_180:
                        capturedImageBitmap = rotateImage(capturedImageBitmap, 180);
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_270:
                        capturedImageBitmap = rotateImage(capturedImageBitmap, 270);
                        break;

                    case ExifInterface.ORIENTATION_NORMAL:
                    default:
                        capturedImageBitmap = capturedImageBitmap;
                }
//                if (!orientation.equalsIgnoreCase("landscape") ){
//                    Matrix matrix = new Matrix();
//                    if ( selectedCameraPosition=="front"){
//
//                        // setup rotation degree
//                        matrix.postRotate(270);
//                    }else {
//
//                        // setup rotation degree
//                        matrix.postRotate(90);
//
//                    }
//                    capturedImageBitmap = Bitmap.createBitmap(capturedImageBitmap, 0, 0, capturedImageBitmap.getWidth()-300, capturedImageBitmap.getHeight()-300, matrix, true);
//
//
//
//                }

            }



            if (orientation.equalsIgnoreCase("landscape") && !isImageSelected) {

                int widthBitmap = capturedImageBitmap.getWidth();
                int heightBitmap = capturedImageBitmap.getHeight();
                int width = (heightBitmap / 3) * 4;

                if(width>widthBitmap){

                    int height = (widthBitmap / 4) * 3;

                    int y = heightBitmap - height;

                    capturedImageBitmap = Bitmap.createBitmap(capturedImageBitmap, 0, y, widthBitmap, height);

                } else {

                    int x = getSessionManager().getWidth2() + differenceInPosition;
                    Log.e("loadBitmaps", "loadBitmaps: "+ getSessionManager().getScreenHeight());

                    float multiplier = widthBitmap/getSessionManager().getScreenHeight();
                    x = (int)(x * multiplier);

//                    float dpi = context.getResources().getDisplayMetrics().density;
//                    x = (int) (x * dpi);

                    if ((x + width) > widthBitmap) {
                        x = widthBitmap - width;
                    }

                    capturedImageBitmap = Bitmap.createBitmap(capturedImageBitmap, x, 0, width, heightBitmap);

                }

            }

            capturedImageBitmap = Bitmap.createScaledBitmap(capturedImageBitmap, imageWidth, imageHeight, true);
            capturedImageBitmap = createSingleImageFromMultipleImages(capturedImageBitmap, overlayBitmap, false);
            img_preview.setImageBitmap(capturedImageBitmap);

            try {
                String videoPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/SalesCAM/Pics";
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(videoPath))));
            } catch (Exception e){}

            filtersListFragment.loadFiltersList(capturedImageBitmap);

        }
        catch (Exception e){
            Log.e("initData", "loadBitmaps: "+e+"  " +capturedImagePath);

            showToast(getString(R.string.api_error));
            onBackPressed();

        }

    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    private void finishWithToast(){
        showToast(getString(R.string.api_error_internet));
        onBackPressed();
    }

    private Bitmap createSingleImageFromMultipleImages(Bitmap firstImage, Bitmap secondImage, boolean mergeAll){

        Bitmap result = Bitmap.createBitmap(imageWidth, imageHeight, firstImage.getConfig());
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(firstImage, 0f, 0f, null);
        if(mergeAll){
            canvas.drawBitmap(secondImage, 0f, 0f, null);
        }
        return result;

    }

    private class MyAsyncTask extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog("");
        }

        @RequiresApi(api = Build.VERSION_CODES.R)
        @Override
        protected String doInBackground(String... strings) {
            try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                finalImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();

                File dir = new File(picsPath);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                File photo=new File(dir, picName);

                if (photo.exists()) {
                    photo.delete();
                }

                FileOutputStream fos=new FileOutputStream(photo.getPath());
                fos.write(byteArray);
                fos.close();

                finalPath = photo.getPath();

                return "SUCCESS";

            }
            catch (java.io.IOException e) {
                return "FAILURE";
            }



        }

        @Override
        protected void onPostExecute(String status) {
            super.onPostExecute(status);
            hideProgressDialog();

            if(status.equals("SUCCESS")){

                if(getSessionManager().getPreformattedMessage().getIs_msg_entered().equals("1")){

                    startActivity(new Intent(context, ImageDetailsActivity.class)
                            .putExtra("imageFilePath", finalPath));
                    overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                    finish();

                }else {

                    startActivity(new Intent(context, SavedActivity.class)
                            .putExtra("imageFilePath", finalPath)
                    );
                    overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);

                }

            } else {

                showToast(getString(R.string.api_error));

            }

        }
    }

    private void setupViewPager() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());

        // adding filter list fragment
        filtersListFragment = new FiltersListFragment();
        filtersListFragment.setListener(this);

        // adding edit image fragment
        editImageFragment = new EditImageFragment();
        editImageFragment.setListener(this);

        adapter.addFragment(filtersListFragment, "FILTERS");
        adapter.addFragment(editImageFragment, "EDIT");

        viewPager.setAdapter(adapter);
        tabs.setupWithViewPager(viewPager);

//
//        final Handler handler = new Handler();
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//
//            }
//        }, 2000);


    }

    //TODO Apply Filter Methods

    @Override
    public void onBrightnessChanged(int brightness) {
        Log.e("loadBitmaps", "onBrightnessChanged: " );

        brightnessFinal = brightness;
        Filter myFilter = new Filter();
        myFilter.addSubFilter(new BrightnessSubFilter(brightness));
        img_preview.setImageBitmap(myFilter.processFilter(finalImage.copy(Bitmap.Config.ARGB_8888, true)));

    }

    @Override
    public void onSaturationChanged(float saturation) {
        Log.e("loadBitmaps", "onSaturationChanged: " );

        saturationFinal = saturation;
        Filter myFilter = new Filter();
        myFilter.addSubFilter(new SaturationSubfilter(saturation));
        img_preview.setImageBitmap(myFilter.processFilter(finalImage.copy(Bitmap.Config.ARGB_8888, true)));

    }

    @Override
    public void onContrastChanged(float contrast) {

        Log.e("loadBitmaps", "onContrastChanged: " );
        contrastFinal = contrast;
        Filter myFilter = new Filter();
        myFilter.addSubFilter(new ContrastSubFilter(contrast));
        img_preview.setImageBitmap(myFilter.processFilter(finalImage.copy(Bitmap.Config.ARGB_8888, true)));

    }

    @Override
    public void onEditStarted() {
        Log.e("loadBitmaps", "onEditStarted: " );
    }

    @Override
    public void onEditCompleted() {
        Log.e("loadBitmaps", "onEditCompleted: " );

        // once the editing is done i.e seekbar is drag is completed,
        // apply the values on to filtered image
        final Bitmap bitmap = filteredImage.copy(Bitmap.Config.ARGB_8888, true);

        Filter myFilter = new Filter();
        myFilter.addSubFilter(new BrightnessSubFilter(brightnessFinal));
        myFilter.addSubFilter(new ContrastSubFilter(contrastFinal));
        myFilter.addSubFilter(new SaturationSubfilter(saturationFinal));
        finalImage = myFilter.processFilter(bitmap);

    }

    @Override
    public void onFilterSelected(Filter filter) {

        Log.e("loadBitmaps", "onFilterSelected: " );
        // reset image controls
        resetControls();

        // applying the selected filter
        filteredImage = capturedImageBitmap.copy(Bitmap.Config.ARGB_8888, true);
        // preview filtered image
        img_preview.setImageBitmap(filter.processFilter(filteredImage));

        finalImage = filteredImage.copy(Bitmap.Config.ARGB_8888, true);
        txt_actionbar_more.setEnabled(true);

    }

    private void resetControls() {
        if (editImageFragment != null) {
            editImageFragment.resetControls();
        }
        brightnessFinal = 0;
        saturationFinal = 1.0f;
        contrastFinal = 1.0f;
    }


    private void checkPermissions() throws ExecutionException, InterruptedException {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            askPermissions(permissions, this);
        } else {

        }

    }
    @Override
    public void onPermissionsDenied(boolean isPermanentalyDenied) {

//        askPermissionsAgain = false;
//
//        String message = "";
//
//        if (isPermanentalyDenied) {
//            message = getString(R.string.permanentlyDeniedMessage);
//        } else {
//            if (permissions.size() == 1) {
//                message = getString(R.string.permissionRequired);
//            } else {
//                message = getString(R.string.permissionsRequired);
//            }
//
//        }
//
//        openPermissionsScreen(isPermanentalyDenied, message, this, permissions);

    }

    @Override
    public void onPermissionsCancelled(boolean isSettingsOpened) {

        if(isSettingsOpened){
            askPermissionsAgain = true;
        } else {
            askPermissions(permissions, this);
        }

    }

    @Override
    public void onPermissionsGranted()  {

    }

}
