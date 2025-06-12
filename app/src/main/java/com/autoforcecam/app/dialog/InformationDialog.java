package com.autoforcecam.app.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.autoforcecam.app.R;

public class InformationDialog {

    private Dialog dialog;

    public InformationDialog(Context context, String heading, String message) {

        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_information);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);

        TextView txt_heading = dialog.findViewById(R.id.txt_heading);
        TextView txt_message = dialog.findViewById(R.id.txt_message);

        txt_heading.setText(heading);
        txt_message.setText(message);

        ImageView img_close = dialog.findViewById(R.id.img_close);
        img_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

    }

    public void show(){

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
