package com.example.imagetotextapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class Scanner extends AppCompatActivity {

    private ImageView capturedImage;
    private TextView resultText;
    private Button snapButton, detectButton;
    private Bitmap imageBitmap;

    private Uri capturedImageUri; // URI to handle the captured image file

    public static final int REQUEST_IMAGE_CAPTURE = 1;
    public static final int PERMISSION_REQUEST_CODE = 101;
    private static final int REQUEST_PICK_IMAGE = 2;
    private static final String TAG = "ScannerActivity";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        // Initializing UI components
        capturedImage = findViewById(R.id.photoImage);
        resultText = findViewById(R.id.detectedText);
        snapButton = findViewById(R.id.takeSnapButton);
        detectButton = findViewById(R.id.detectButton);

        // Set onClickListeners for buttons
        detectButton.setOnClickListener(v -> detectText());
        snapButton.setOnClickListener(v -> {
            if (CheckPermission()) {
                // Show a dialog to choose between Camera and Gallery
                showImageSourceDialog();
            } else {
                RequestPermission();
            }
        });
    }

    // Check if camera permission is granted
    private boolean CheckPermission() {
        boolean cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

        if (cameraPermission) {
            Log.d(TAG, "Camera permission is granted.");
            return true;
        } else {
            Log.d(TAG, "Camera permission is not granted.");
            return false;
        }
    }

    // Request camera permission
    private void RequestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                PERMISSION_REQUEST_CODE);
        Log.d(TAG, "Requesting camera permission.");
    }

    // Handle the result of permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                boolean cameraPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                if (cameraPermission) {
                    Log.d(TAG, "Camera permission granted.");
                    CaptureImage();
                } else {
                    Log.e(TAG, "Camera permission not granted.");
                    Toast.makeText(this, "Camera permission is required to capture images.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    // Capture an image using the camera or pick from the gallery
    private void CaptureImage() {
        // Intent to pick image from gallery
        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickPhotoIntent.setType("image/*");

        // Intent to capture image using the camera
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Create a chooser for picking image or capturing
        Intent chooserIntent = Intent.createChooser(pickPhotoIntent, "Select Image Source");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takePictureIntent});

        startActivityForResult(chooserIntent, REQUEST_PICK_IMAGE);
    }

    // Handle the result of image capture or image pick
    // Handle the result of image capture or image pick
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // Handle image captured using the camera
                if (data != null && data.getExtras() != null) {
                    imageBitmap = (Bitmap) data.getExtras().get("data");
                    if (imageBitmap != null) {
                        capturedImage.setImageBitmap(imageBitmap);
                        Log.d(TAG, "Image captured successfully.");
                    } else {
                        Log.e(TAG, "Image bitmap is null.");
                        Toast.makeText(this, "Failed to capture image. Please try again.", Toast.LENGTH_LONG).show();
                    }
                }
            } else if (requestCode == REQUEST_PICK_IMAGE) {
                // Handle image picked from the gallery
                if (data != null) {
                    Uri selectedImageUri = data.getData();
                    try {
                        imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                        capturedImage.setImageBitmap(imageBitmap);
                        Log.d(TAG, "Image picked from gallery successfully.");
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to load image from gallery: " + e.getMessage());
                        Toast.makeText(this, "Failed to pick image from gallery. Please try again.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.e(TAG, "Image picking intent returned null data.");
                    Toast.makeText(this, "Failed to pick image from gallery. Please try again.", Toast.LENGTH_LONG).show();
                }
            }
        } else {
            Log.e(TAG, "Image capture or selection cancelled.");
            Toast.makeText(this, "Image capture or selection cancelled. Please try again.", Toast.LENGTH_LONG).show();
        }
    }

    // Perform text detection on the captured or selected image
    private void detectText() {
        if (imageBitmap == null) {
            Toast.makeText(this, "No image to detect text from. Please capture an image first.", Toast.LENGTH_LONG).show();
            return;
        }

        // Create an InputImage from the captured bitmap
        InputImage inputImage = InputImage.fromBitmap(imageBitmap, 0);

        // Initialize the TextRecognizer
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        // Process the image to detect text
        recognizer.process(inputImage)
                .addOnSuccessListener(text -> {
                    // Extract detected text and display in the TextView
                    StringBuilder detectedText = new StringBuilder();
                    for (Text.TextBlock block : text.getTextBlocks()) {
                        detectedText.append(block.getText()).append("\n");
                    }
                    resultText.setText(detectedText.toString());
                    Log.d(TAG, "Text detected successfully.");
                    Toast.makeText(this, "Text detection complete.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Text detection failed: " + e.getMessage());
                    Toast.makeText(this, "Failed to detect text. Please try again.", Toast.LENGTH_LONG).show();
                });
    }

    // Method to show a dialog for choosing between Camera and Gallery
    private void showImageSourceDialog() {
        // Options for the dialog
        String[] options = {"Capture Image", "Pick from Gallery"};

        // Create and show the dialog
        new AlertDialog.Builder(this)
                .setTitle("Select Image Source")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Capture Image option selected
                        captureImage();
                    } else {
                        // Pick Image from Gallery option selected
                        pickImageFromGallery();
                    }
                })
                .show();
    }

    // Capture image using camera
    private void captureImage() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);  // Use the specific request code for capturing image
        }
    }

    // Pick an image from the gallery
    private void pickImageFromGallery() {
        @SuppressLint("IntentReset") Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickPhotoIntent.setType("image/*");
        startActivityForResult(pickPhotoIntent, REQUEST_PICK_IMAGE);  // Use the specific request code for picking image
    }

}
