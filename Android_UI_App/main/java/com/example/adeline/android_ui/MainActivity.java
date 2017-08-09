package com.example.adeline.android_ui;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.Button;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button linLayHorButton = (Button) findViewById(R.id.mainButton1);
        linLayHorButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LinearLayoutHActivity.class);
                startActivity(intent);
            }
        });


        Button linLayVertButton = (Button) findViewById(R.id.mainButton2);
        linLayVertButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LinearLayoutVActivity.class);
                startActivity(intent);
            }
        });


        Button gridLayoutButton = (Button) findViewById(R.id.mainButton3);
        gridLayoutButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GridLayoutActivity.class);
                startActivity(intent);
            }
        });


        Button relLayoutButton = (Button) findViewById(R.id.mainButton4);
        relLayoutButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RelLayoutActivity.class);
                startActivity(intent);
            }
        });
    }
}
