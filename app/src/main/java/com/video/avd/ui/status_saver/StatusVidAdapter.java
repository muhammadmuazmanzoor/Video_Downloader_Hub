package com.video.avd.ui.status_saver;


import static com.video.avd.constent.ShareDataKt.VIEW_TYPE;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.video.avd.R;
import com.video.avd.ui.status_saver.model.Status;
import com.video.avd.ui.status_saver.statusnew.ItemViewHoldernew;

import java.util.List;

import kotlin.Deprecated;


@Deprecated(message = "Deprecated, Use StatusVidAdapterNew")
public class StatusVidAdapter extends RecyclerView.Adapter<ItemViewHoldernew> {

    private List<Status> videoList;
    private Context context;
    private final RelativeLayout container;

    private StatusVideoClickListener listener;


    public void updateData(List<Status> videoList) {
        this.videoList = videoList;
        notifyDataSetChanged();
    }

    public StatusVidAdapter(List<Status> videoList, RelativeLayout container, StatusVideoClickListener listener) {
        this.videoList = videoList;
        this.container = container;
        this.listener = listener;
    }


    @NonNull
    @Override
    public ItemViewHoldernew onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view;
        view = LayoutInflater.from(context).inflate(R.layout.item_status_new, parent, false);
        return new ItemViewHoldernew(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ItemViewHoldernew holder, int position) {
        final Status status = videoList.get(position);
        try {
            if (status.isApi30()) {
                Glide.with(context).load(status.getDocumentFile().getUri()).into(holder.imageView);
            } else {
                Glide.with(context).load(status.getFile()).into(holder.imageView);
            }
            holder.download.setOnClickListener(v -> {
                listener.onsaveClick(status, context);
            });
            holder.share.setOnClickListener(v -> {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/mp4");
                if (status.isApi30()) {
                    shareIntent.putExtra(Intent.EXTRA_STREAM, status.getDocumentFile().getUri());
                } else {
                    shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + status.getFile().getAbsolutePath()));
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share image"));
            });
            LayoutInflater inflater = LayoutInflater.from(context);
            final View view1 = inflater.inflate(R.layout.view_video_full_screen, null);
            holder.rootLayout.setOnClickListener(v -> {
                listener.onStatusVideoClick(videoList, holder.getAdapterPosition(), status);
            });
        } catch (Exception e) {
            // Handle exceptions
        }
    }

    @Override
    public int getItemViewType(int position) {
        return VIEW_TYPE.getValue();
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    interface StatusVideoClickListener {
        void onStatusVideoClick(List<Status> list, int position, Status status);

        void onsaveClick(Status status, Context context);
    }

}
