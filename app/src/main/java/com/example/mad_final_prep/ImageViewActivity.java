package com.example.mad_final_prep;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

public class ImageViewActivity extends AppCompatActivity {
    private ImageView imageView, saveDeleteButton, broadcastPrivButton;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String imageName, email;
    private Boolean saved, broadcasted;
    private Bitmap capturedImage;
    private byte[] byteArray;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_image_view);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mAuth = FirebaseAuth.getInstance();
        email = mAuth.getCurrentUser().getEmail();
        String imageUriString = getIntent().getStringExtra("imageUri");
        handleImageUri(imageUriString);
//        byteArray = getIntent().getByteArrayExtra("image");
        String name = getIntent().getStringExtra("imageName");
        if (name.equals("null")) {
            String mailFirstPart = email.split("@")[0];
            imageName = "image_"+mailFirstPart+"_"+System.currentTimeMillis();
            saved = false;
        }
        else {
            imageName = name;
            saved = true;
        }
        broadcasted = getIntent().getBooleanExtra("broadcasted", false);
        saveDeleteButton = findViewById(R.id.save_delete_button);
        broadcastPrivButton = findViewById(R.id.broadcast_priv_button);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storage.setMaxUploadRetryTimeMillis(60 * 1000);
        storage.setMaxOperationRetryTimeMillis(30 * 1000);
        storageRef = storage.getReference();
        // Set button states
        updateButtonSaved();
        updateButtonBroadcasted();

        saveDeleteButton.setOnClickListener(v -> saveOrDeleteImage());
        broadcastPrivButton.setOnClickListener(v -> broadcastOrPrivatizeImage());
    }
    @Override
    public void onBackPressed() {
        // Your custom back button handling code
        Intent intent = new Intent(ImageViewActivity.this, CameraActivity.class);
        startActivity(intent);
        finish();
        super.onBackPressed();
    }
    private void updateButtonSaved() {
        // Set initial button states based on whether the image is saved or broadcasted
        if (saved) {
            saveDeleteButton.setImageResource(R.drawable.ic_delete);
        } else {
            saveDeleteButton.setImageResource(R.drawable.ic_save);
        }
    }
    private void updateButtonBroadcasted() {
        if (broadcasted) {
            broadcastPrivButton.setImageResource(R.drawable.ic_priv);
        } else {
            broadcastPrivButton.setImageResource(R.drawable.ic_broadcast);
        }
    }

    private void saveOrDeleteImage() {
        if (saved) {
            saved = false;
            deleteImage();
            Intent intent = new Intent(ImageViewActivity.this, CameraActivity.class);
            startActivity(intent);
            finish();
        } else {
            saveImage();
            saved = true;
            updateButtonSaved();
        }
    }

    private void broadcastOrPrivatizeImage() {
        saved = false;
        saveDeleteButton.setImageResource(R.drawable.ic_save);
        broadcasted = !broadcasted;
        updateButtonBroadcasted();
    }

    private void saveImage() {
        uploadImageToStorage(byteArray).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String downloadUrl = task.getResult();
                Log.d("Storage", "Download URL: " + downloadUrl);
                HashMap<String, Object> imageData = new HashMap<>();
                imageData.put("email", email);
                imageData.put("imageName", imageName);
                imageData.put("imageUrl", downloadUrl);
                imageData.put("broadcasted", broadcasted);
                // Save image to Firestore
                String user = email.replace(".", "_");
                FirebaseDatabase.getInstance().getReference().child("Users").child(user).child(imageName).setValue(imageData)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Toast.makeText(ImageViewActivity.this, imageName+"Image Data Saved", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(ImageViewActivity.this, "Data Save Failed", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Log.e("Storage", "Error getting download URL", task.getException());
            }
        });
    }

    private void deleteImage() {
        String user = email.replace(".", "_");
        Log.v("FirebaseDB", "User: " + user);
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(user).child(imageName);
        Log.d("FirebaseDB", "Reference set, retrieving image data");

        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String imageUrl = dataSnapshot.child("imageUrl").getValue(String.class);
                    if (imageUrl != null) {
                        Log.d("FirebaseDB", "Image URL found: " + imageUrl);
                        deleteFromStorageAndDatabase(imageUrl, reference);
                    } else {
                        Log.e("FirebaseDB", "Image URL not found in database");
                        // Handle the case where the image URL is not found
                    }
                } else {
                    Log.e("FirebaseDB", "Image data not found in database");
                    // Handle the case where the image data is not found
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("FirebaseDB", "Error reading image data", databaseError.toException());
            }
        });
    }

    private void deleteFromStorageAndDatabase(String imageUrl, DatabaseReference dbReference) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);

        storageRef.delete().addOnSuccessListener(aVoid -> {
            // File deleted successfully from Storage, now delete from Database
            Log.d("Storage", "Image deleted successfully from Storage");
            dbReference.removeValue()
                    .addOnSuccessListener(unused -> {
                        Log.d("FirebaseDB", "Image data deleted from Database");
                        Toast.makeText(ImageViewActivity.this, "Image deleted successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FirebaseDB", "Failed to delete image data from Database", e);
                        Toast.makeText(ImageViewActivity.this, "Failed to delete image data from Database", Toast.LENGTH_SHORT).show();
                    });
        }).addOnFailureListener(exception -> {
            Log.e("Storage", "Error deleting image from Storage", exception);
            Toast.makeText(ImageViewActivity.this, "Failed to delete image from Storage", Toast.LENGTH_SHORT).show();
        });
    }
    private void handleImageUri(String imageUriString) {
        if (imageUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);

            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream != null) {
                    // Create a temporary file to store the image
                    File tempFile = File.createTempFile("temp_image", ".png", getCacheDir());
                    FileOutputStream outputStream = new FileOutputStream(tempFile);

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    inputStream.close();
                    outputStream.close();

                    // Now use the tempFile for your ImageCompressionTask
                    new ImageCompressionTask().execute(tempFile);
                } else {
                    Toast.makeText(this, "Failed to open image stream", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Image URI not found", Toast.LENGTH_SHORT).show();
        }
    }

    private class ImageCompressionTask extends AsyncTask<File, Void, byte[]> {

        @Override
        protected byte[] doInBackground(File... files) {
            File imageFile = files[0];
            capturedImage = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            capturedImage = rotateBitmapIfNeeded(capturedImage);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            capturedImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
            return stream.toByteArray();
        }

        @Override
        protected void onPostExecute(byte[] ba) {
            byteArray = ba;
            imageView = findViewById(R.id.image_view);
            if (imageView != null) {
                imageView.setImageBitmap(BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length));
            }
        }
        private Bitmap rotateBitmapIfNeeded(Bitmap bitmap) {
            if (bitmap.getWidth() > bitmap.getHeight()) {
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                try {
                    Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    bitmap.recycle();
                    return rotatedBitmap;
                } catch (OutOfMemoryError e) {
                    e.printStackTrace();
                    return bitmap;
                }
            }
            return bitmap;
        }
    }
    private Task<String> uploadImageToStorage(byte[] byteArray) {
        String imagePath;
        if (broadcasted) {
            imagePath = "public/" + imageName+".png";
        }
        else {
            imagePath = "private/" + imageName+".png";
        }
        StorageReference imageRef = storageRef.child(imagePath);
        TaskCompletionSource<String> tcs = new TaskCompletionSource<>();

        UploadTask uploadTask = imageRef.putBytes(byteArray);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            // Get the download URL
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                tcs.setResult(downloadUrl);
            }).addOnFailureListener(e -> {
                Log.e("Storage", "Failed to get download URL", e);
                tcs.setException(e);
            });
        }).addOnFailureListener(e -> {
            Log.e("Storage", "Image upload failed", e);
            tcs.setException(e);
        });

        return tcs.getTask();
    }
}