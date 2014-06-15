package com.example.coolerbot.app;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity implements View.OnClickListener {

    Button forwardButton;
    Button leftButton;
    Button rightButton;
    Button backwardButton;
    Button stopButton;
    SabertoothDriver sabertoothDriver;
    Context globalCotext;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        globalCotext = this;

        sabertoothDriver = new SabertoothDriver(this);

        forwardButton = (Button) findViewById(R.id.forwardButton);
        forwardButton.setOnClickListener(this);
        leftButton = (Button) findViewById(R.id.leftButton);
        leftButton.setOnClickListener(this);
        rightButton = (Button) findViewById(R.id.rightButton);
        rightButton.setOnClickListener(this);
        backwardButton = (Button) findViewById(R.id.backwardButton);
        backwardButton.setOnClickListener(this);
        stopButton = (Button) findViewById(R.id.stopButton);
        stopButton.setOnClickListener(this);

    }

    @Override
    protected void onResume() {
        sabertoothDriver.resumeDriver();
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onResume();
    }

    @Override
    protected void  onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        sabertoothDriver.destoryDriver();
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.forwardButton:
                sabertoothDriver.setForwardMixed((byte)50);
                break;
            case R.id.leftButton:
                sabertoothDriver.setLeftMixed((byte)30);
                break;
            case R.id.rightButton:
                sabertoothDriver.setRightMixed((byte)30);
                break;
            case R.id.backwardButton:
                sabertoothDriver.setBackwardMixed((byte)50);
                break;
            case R.id.stopButton:
                sabertoothDriver.setForwardMixed((byte)00);
                break;
        }
    }
}
