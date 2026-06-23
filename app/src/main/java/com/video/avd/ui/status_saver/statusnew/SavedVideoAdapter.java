package com.video.avd.ui.status_saver.statusnew;

import static com.video.avd.constent.ShareDataKt.VIEW_TYPE;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.video.avd.R;
import com.video.avd.constent.ShareDataKt;
import com.video.avd.ui.status_saver.model.Status;
import com.video.avd.utils.AppUtils;

import java.util.List;

public class SavedVideoAdapter extends RecyclerView.Adapter<SaveditemViewHolder> {

    private Context context;
    private SavedVideoAdapter.SavedVideoClickListener listener;
    private List<Status> savedList;
    public SavedVideoAdapter(List<Status> savedVideoList, SavedVideoClickListener listener) {
        this.savedList = savedVideoList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SaveditemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view;
        view = LayoutInflater.from(context).inflate(R.layout.item_saved, parent, false);
        return new SaveditemViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull final SaveditemViewHolder holder, int position) {
        final Status status = savedList.get(holder.getAdapterPosition()); // Get the saved video at this position

        try {
            if (status.isApi30()) {
                Glide.with(context).load(status.getDocumentFile().getUri()).into(holder.imageView);
            } else {
                Glide.with(context).load(status.getFile()).into(holder.imageView);
            }

            holder.delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppUtils.INSTANCE.firebaseUserAction("deleteButtonCLicked", "SavedVideoAdapter");
                    if (status.isApi30()) {
                        status.getDocumentFile().delete();
                    } else {
                        status.getFile().delete();
                    }
                    int position = holder.getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        savedList.remove(position);
                        notifyItemRemoved(position);
                        ShareDataKt.isFileSave().setValue(true);
                    }
                }
            });


            holder.share.setOnClickListener(v -> {
                AppUtils.INSTANCE.firebaseUserAction("shareButtonClicked", "SavedVideoAdapter");
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
                listener.onStatusVideoClick(savedList, holder.getAdapterPosition(), status);
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
        return savedList.size();
    }



    interface SavedVideoClickListener {
        void onStatusVideoClick(List<Status> list, int position, Status status);

    }
}

