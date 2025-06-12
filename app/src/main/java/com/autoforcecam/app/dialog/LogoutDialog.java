package com.autoforcecam.app.dialog;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.autoforcecam.app.R;
import com.autoforcecam.app.activities.LoginActivity;
import com.autoforcecam.app.utils.SessionManager;

public class LogoutDialog {

    private Dialog dialog;

    public LogoutDialog(AppCompatActivity context) {

        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_logout);
        dialog.setCanceledOnTouchOutside(true);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(true);

//        TextView txt_message = dialog.findViewById(R.id.txt_message);
//
//        try {
//            SessionManager sessionManager = new SessionManager(context);
//            UploadModel uploadModel = sessionManager.getUploadData();
//            if (null == uploadModel.getTimeStamp()) {
//                txt_message.setText("Would you like to Logout?");
//            } else {
//                txt_message.setText("Your video is being uploaded. Logging out of the application would cancel the video upload. Still want to log out?");
//            }
//        } catch (Exception e){}

        Button btn_yes = dialog.findViewById(R.id.btn_yes);
        btn_yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


//                try{ context.stopService(new Intent(context, UploadService.class)); } catch (Exception e){}
//                try{ context.stopService(new Intent(context, CompressUploadService.class)); } catch (Exception e){}

                new SessionManager(context).clearAllData();
                context.startActivity(new Intent(context, LoginActivity.class));
                context.overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                context.finishAffinity();
                dialog.dismiss();
            }

        });

        Button btn_no = dialog.findViewById(R.id.btn_no);
        btn_no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

    }

    public void showDialog(){

        if(dialog!=null){
            if(!dialog.isShowing()) {
                dialog.show();
            }

        }

    }

    public boolean isDialogVisisble(){
        return dialog.isShowing();
    }

    public void hideDialog(){

        if(dialog!=null){
            dialog.dismiss();
        }

    }

}
