package com.autoforcecam.app.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.RecyclerView;

import com.autoforcecam.app.R;
import com.autoforcecam.app.interfaces.PlayerInterface;
import com.autoforcecam.app.responses.Overlays;
import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class OverlayAdapter extends RecyclerView.Adapter<OverlayAdapter.ViewHolder> {

    private Context context;
    private LayoutInflater mInflater;
    private ArrayList<Overlays> overlayList;

    private PlayerInterface playerInterface;

    public OverlayAdapter(Context context, ArrayList<Overlays> overlayList) {

        this.context = context;
        this.overlayList = overlayList;
        this.mInflater = LayoutInflater.from(context);


    }

    public OverlayAdapter(Context context, PlayerInterface playerInterface, ArrayList<Overlays> overlayList) {

        this.context = context;
        this.overlayList = overlayList;
        this.playerInterface = playerInterface;
        this.mInflater = LayoutInflater.from(context);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateList(ArrayList<Overlays> overlayList){
        this.overlayList = overlayList;
        Log.e("loadOverlaysToAdapter", "loadOverlaysToAdapte r: "+overlayList.size());

        notifyDataSetChanged();
    }

    @Override
    public OverlayAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.listitem_overlay, parent, false);
        return new OverlayAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final OverlayAdapter.ViewHolder holder, final int position) {

        Log.e("loadOverlaysToAdapter", "loadOverlaysToAdapter: "+overlayList.get(position).getImage_url());


        if(!overlayList.get(position).getImage_url().trim().isEmpty()){

            Glide.with(context)
                    .load(overlayList.get(position).getImage_url())
                    .into(holder.img_overlay);

            holder.img_overlay.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    if(null!=playerInterface){
                        playerInterface.openMediaPlayerOptions();
                    }
                }
            });

        }

    }

    @Override
    public int getItemCount() {
        return overlayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView img_overlay;
        private LinearLayout ll_loading;

        ViewHolder(View itemView) {
            super(itemView);

            img_overlay = itemView.findViewById(R.id.img_overlay);
            ll_loading = itemView.findViewById(R.id.ll_loading);

        }

    }

}
