package com.example.cameraapp;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;

public class Galery extends AppCompatActivity {

    ImageView imageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_galery);
        imageView = findViewById(R.id.imageView);

        Intent intent = getIntent();
        String url = intent.getStringExtra("imageUrl");
        Log.i("Galery",url);
        if(!url.equals(""))
        imageView.setImageBitmap(BitmapFactory.decodeFile(url));

    }

}
