package com.example.mad_final_prep;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GalleryActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private FirebaseDatabase database;
    private DatabaseReference userImagesRef;
    private FirebaseRecyclerAdapter<ImageModel, GalleryViewHolder> adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gallery);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        BottomNavigationView bnb = findViewById(R.id.bottomNavigation);
        bnb.setSelectedItemId(R.id.bnb_gallery);

        bnb.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.bnb_camera) {
                startActivity(new Intent(getApplicationContext(), CameraActivity.class));
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
                return true;
            }
            else if (item.getItemId() == R.id.bnb_discover) {
                startActivity(new Intent(getApplicationContext(), DiscoverActivity.class));
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
                return true;
            }
            else {
                return false;
            }
        });

        recyclerView = findViewById(R.id.galleryRecyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail().replace(".", "_");
        DatabaseReference imagesRef = FirebaseDatabase.getInstance().getReference().child("Users").child(email);

        FirebaseRecyclerOptions<ImageModel> options =
                new FirebaseRecyclerOptions.Builder<ImageModel>()
                        .setQuery(imagesRef, ImageModel.class)
                        .build();

        adapter = new FirebaseRecyclerAdapter<ImageModel, GalleryViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull GalleryViewHolder holder, int position, @NonNull ImageModel model) {
                Glide.with(GalleryActivity.this)
                        .load(model.getImageUrl())
                        .into(holder.imageView);

                holder.itemView.setOnClickListener(v -> openImageViewActivity(model));
            }

            @NonNull
            @Override
            public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_image_layout, parent, false);
                return new GalleryViewHolder(view);
            }
        };

        recyclerView.setAdapter(adapter);
    }

    private void openImageViewActivity(ImageModel model) {
        Intent intent = new Intent(this, ImageViewActivity.class);
        intent.putExtra("imageName", model.getImageName());
        intent.putExtra("broadcasted", model.isBroadcasted());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            File file = new File(getExternalFilesDir(null), model.getImageName() + ".png");
            try {
                URL url = new URL(model.getImageUrl());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();

                // Download image to memory
                ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];
                int len;
                while ((len = input.read(buffer)) != -1) {
                    byteBuffer.write(buffer, 0, len);
                }
                byte[] imageData = byteBuffer.toByteArray();

                // Save bytes to file
                FileOutputStream output = null;
                try {
                    output = new FileOutputStream(file);
                    output.write(imageData);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (output != null) {
                        try {
                            output.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                Uri imageUri = FileProvider.getUriForFile(GalleryActivity.this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        file);

                handler.post(() -> {
                    intent.putExtra("imageUri", imageUri.toString());
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                });

            } catch (IOException e) {
                e.printStackTrace();
                handler.post(() -> {
                    Toast.makeText(GalleryActivity.this, "Failed to load image", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }
}