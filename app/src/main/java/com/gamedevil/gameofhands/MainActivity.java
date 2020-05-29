package com.gamedevil.gameofhands;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int SELECT_IMAGE_FROM_GALLERY_CODE = 123;

    private MaterialButton mSelectImageBtn;
    private ImageView mDisplayImageView;
    private MaterialTextView mShowClassificationText;
    private HandShapeDetector mHandShapeDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSelectImageBtn = findViewById(R.id.select_img_btn);
        mDisplayImageView = findViewById(R.id.selected_image);
        mShowClassificationText = findViewById(R.id.text_view_prediction_label);
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
                pickImageFromGallary();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pickImageFromGallary() {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto, SELECT_IMAGE_FROM_GALLERY_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case SELECT_IMAGE_FROM_GALLERY_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickImageFromGallary();
                } else {
                    Toast.makeText(this, "No Permissions granted!!\nRequired for app usage", Toast.LENGTH_SHORT).show();
                }
                break;
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
        if(pBitmap==null){
            Toast.makeText(this, "Error!!", Toast.LENGTH_SHORT).show();
            return;
        }
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(pBitmap,
                300, 300, false);
        mDisplayImageView.setImageBitmap(scaledBitmap);
        Log.d(TAG,""+pBitmap.getWidth()+" "+pBitmap.getHeight());
       Log.d(TAG, " "+mHandShapeDetector.classify(pBitmap));
    }
}

