package com.gamedevil.gameofhands;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

public class SingleImageClassifier extends AppCompatActivity {

    private static final String TAG = "SingleImageClassifier";
    private static final int SELECT_IMAGE_FROM_GALLERY_CODE = 123;
    private static final int CROP_SIZE = 300;

    private MaterialButton mSelectImageBtn;
    private ImageView mDisplayImageView;
    private ImageView mClassSymbol;
    private MaterialTextView mShowClassificationText;
    private HandShapeDetector mHandShapeDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_classifier);

        mSelectImageBtn = findViewById(R.id.select_img_btn);
        mDisplayImageView = findViewById(R.id.selected_image);
        mShowClassificationText = findViewById(R.id.text_view_prediction_label);
        mClassSymbol = findViewById(R.id.classification_image);

        mHandShapeDetector = new HandShapeDetector(this);

        mSelectImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View pView) {
                askForStoragePermission();
            }
        });

    }

    private void askForStoragePermission() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, SELECT_IMAGE_FROM_GALLERY_CODE);
            } else {
                pickImageFromGallery();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pickImageFromGallery() {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto, SELECT_IMAGE_FROM_GALLERY_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == SELECT_IMAGE_FROM_GALLERY_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImageFromGallery();
            } else {
                Toast.makeText(this, "No Permissions granted!!\nRequired for app usage", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == SELECT_IMAGE_FROM_GALLERY_CODE && data != null) {

            try {
                Uri selectedImage = data.getData();
                String[] filePath = {MediaStore.Images.Media.DATA};
                assert selectedImage != null;
                Cursor c = getContentResolver().query(selectedImage, filePath, null, null, null);
                assert c != null;
                c.moveToFirst();
                int columnIndex = c.getColumnIndex(filePath[0]);
                String picturePath = c.getString(columnIndex);
                c.close();
                Bitmap thumbnail = (BitmapFactory.decodeFile(picturePath));
                Log.d("ImagePath: ", picturePath + "");
                detectImage(thumbnail);
            } catch (Exception exception) {
                Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
            }

        } else {
            Toast.makeText(this, "Error!!", Toast.LENGTH_SHORT).show();
        }
    }

    private void detectImage(Bitmap pBitmap) {
        if (pBitmap == null) {
            Toast.makeText(this, "Error!!", Toast.LENGTH_SHORT).show();
            return;
        }
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(pBitmap, CROP_SIZE, CROP_SIZE, false);
        mDisplayImageView.setImageBitmap(scaledBitmap);
        String label = mHandShapeDetector.classify(pBitmap);
        String handShape;
        switch (label) {
            case "0":
                handShape = "Rock";
                mClassSymbol.setImageResource(R.drawable.rock);
                break;
            case "1":
                handShape = "Paper";
                mClassSymbol.setImageResource(R.drawable.paper);
                break;
            case "2":
                handShape = "Scissor";
                mClassSymbol.setImageResource(R.drawable.scissor);
                break;
            default:
                handShape = "Unknown";
        }
        mShowClassificationText.setText(handShape);
    }
}

