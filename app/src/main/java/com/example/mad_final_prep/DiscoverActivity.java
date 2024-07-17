package com.example.mad_final_prep;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

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

public class DiscoverActivity extends AppCompatActivity implements DiscoverAdapter.OnItemClickListener{
    private RecyclerView recyclerView;
    private DiscoverAdapter adapter;
    private FirebaseStorage storage;
    private StorageReference publicStorageRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_discover);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        BottomNavigationView bnb = findViewById(R.id.bottomNavigation);
        bnb.setSelectedItemId(R.id.bnb_discover);

        bnb.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.bnb_camera) {
                startActivity(new Intent(getApplicationContext(), CameraActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            } else if (item.getItemId() == R.id.bnb_gallery) {
                startActivity(new Intent(getApplicationContext(), GalleryActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            } else {
                return false;
            }
        });

        recyclerView = findViewById(R.id.discoverRecyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        adapter = new DiscoverAdapter(this);
        adapter.setOnItemClickListener(this);
        recyclerView.setAdapter(adapter);

        storage = FirebaseStorage.getInstance();
        publicStorageRef = storage.getReference().child("public");

        loadImagesFromStorage();
    }
    private void loadImagesFromStorage() {
        publicStorageRef.listAll().addOnSuccessListener(listResult -> {
            List<StorageReference> imageRefs = listResult.getItems();
            adapter.setImageRefs(imageRefs);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load images: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onItemClick(StorageReference imageRef) {
        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(() -> {
                try {
                    // Create a file to save the downloaded image
                    File file = new File(getExternalFilesDir(null), imageRef.getName() + ".png");
                    URL url = new URL(uri.toString());
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

                    // Get URI for the file
                    Uri imageUri = FileProvider.getUriForFile(DiscoverActivity.this,
                            getApplicationContext().getPackageName() + ".fileprovider",
                            file);

                    handler.post(() -> {
                        Intent intent = new Intent(DiscoverActivity.this, ImageViewActivity.class);
                        intent.putExtra("imageUri", imageUri.toString());
                        intent.putExtra("imageName", imageRef.getName());
                        intent.putExtra("broadcasted", true); // Assuming all public images are broadcasted
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                    handler.post(() -> {
                        Toast.makeText(DiscoverActivity.this, "Failed to open image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });

        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to open image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

}