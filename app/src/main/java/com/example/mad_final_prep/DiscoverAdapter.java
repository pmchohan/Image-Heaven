package com.example.mad_final_prep;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class DiscoverAdapter extends RecyclerView.Adapter<DiscoverAdapter.ImageViewHolder> {
    private List<StorageReference> imageRefs;
    private Context context;
    private OnItemClickListener listener;

    public DiscoverAdapter(Context context) {
        this.context = context;
        this.imageRefs = new ArrayList<>();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_image_layout, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        StorageReference imageRef = imageRefs.get(position);
        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            Glide.with(context)
                    .load(uri)
                    .into(holder.imageView);
        });
    }

    @Override
    public int getItemCount() {
        return imageRefs.size();
    }

    public void setImageRefs(List<StorageReference> imageRefs) {
        this.imageRefs = imageRefs;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ivImageHolder);
            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onItemClick(imageRefs.get(getAdapterPosition()));
                }
            });
        }
    }

    public interface OnItemClickListener {
        void onItemClick(StorageReference imageRef);
    }
}
