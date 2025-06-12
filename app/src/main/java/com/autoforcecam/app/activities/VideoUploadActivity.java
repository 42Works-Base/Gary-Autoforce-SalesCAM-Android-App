package com.autoforcecam.app.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.autoforcecam.app.R;
import com.autoforcecam.app.base.BaseActivity;
import com.autoforcecam.app.models.UploadModel;

/* Created by JSP@nesar */

public class VideoUploadActivity extends BaseActivity {

    private IntentFilter intentFilter;
    private BroadcastReceiver broadcast_reciever;
    private Button btn_done, btn_quit;
    ImageView thumsImage;
    RelativeLayout progressLayout;
    TextView progress_tv,uplodedText;
    private ProgressBar circular_progress;

    @Override
    protected int getLayoutView() {
        return R.layout.act_video_upload;
    }

    @Override
    protected Context getActivityContext() {
        return VideoUploadActivity.this;
    }

    @Override
    protected void initView() {
        circular_progress = findViewById(R.id.circular_progress);
        progress_tv = findViewById(R.id.progress_tv);
        uplodedText = findViewById(R.id.uplodedText);
        btn_done = findViewById(R.id.btn_done);
        btn_quit = findViewById(R.id.btn_quit);
        thumsImage = findViewById(R.id.thumsUpImage);
        progressLayout = findViewById(R.id.progressLayout);
    }

    @Override
    protected void initData() {
        thumsImage.setVisibility(View.GONE);
        progressLayout.setVisibility(View.VISIBLE);
        initBroadcast();
    }

    @Override
    protected void initListener() {

        btn_done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(context, HomeActivity.class)
                        .putExtra("showOverlayProgress", false)
                );
                overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                finishAffinity();
            }
        });

        btn_quit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UploadModel uploadModel = getSessionManager().getUploadData();
                if(null==uploadModel.getTimeStamp()){
                    finishAffinity();
                    overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                } else {
                    showToast("Please do not quit as your video is now uploading to Vimeo.");
                }
            }
        });

    }

    @Override
    public void onBackPressed() {}

    private void initBroadcast() {

        intentFilter = new IntentFilter();
        intentFilter.addAction("progress");
        intentFilter.addAction("complete");

        if (broadcast_reciever == null) {

            broadcast_reciever = new BroadcastReceiver() {

                @Override
                public void onReceive(Context arg0, Intent intent) {

                    String action = intent.getAction();

                    if (action.equals("progress")) {
                        double progress = intent.getDoubleExtra("progress",0.0);
                        int pr=(int) Math.round(progress);
                        circular_progress.setProgress(pr);
                        progress_tv.setText(pr+"%");

                    }

                    if (action.equals("complete")){
                        progressLayout.setVisibility(View.GONE);
                        thumsImage.setVisibility(View.VISIBLE);
                        uplodedText.setText("Video Uploaded");
                    }
                }

            };

        }

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcast_reciever, intentFilter);

    }

}
