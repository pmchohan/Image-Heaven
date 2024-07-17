package com.example.mad_final_prep;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class GalleryViewHolder extends RecyclerView.ViewHolder {
    public ImageView imageView;

    public GalleryViewHolder(@NonNull View itemView) {
        super(itemView);
        imageView = itemView.findViewById(R.id.ivImageHolder);
    }
}

