package com.gamedevil.gameofhands;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.singleClassifierBtn)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View pView) {
                        startActivity(new Intent(MainActivity.this,SingleImageClassifier.class));
                    }
                });
    }
}
