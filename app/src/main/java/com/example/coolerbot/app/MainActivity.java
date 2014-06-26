package com.example.coolerbot.app;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements View.OnClickListener {

    Button forwardButton;
    Button leftButton;
    Button rightButton;
    Button backwardButton;
    Button stopButton;
    SabertoothDriver sabertoothDriver;
    StateEstimator stateEstimator;
    Context globalCotext;
    Handler handler;

    private Timer displayUpdate = new Timer();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        globalCotext = this;
        handler = new Handler();

        sabertoothDriver = new SabertoothDriver(this);
        stateEstimator = new StateEstimator(this);

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

        displayUpdate.scheduleAtFixedRate(new UpdateDisplayTask(), 1000, 50);
    }

    @Override
    protected void onResume() {
        sabertoothDriver.resumeDriver();
        stateEstimator.registerListeners();
        super.onResume();
    }

    @Override
    protected void onPause() {
        stateEstimator.unregisterListeners();
        super.onPause();
    }

    @Override
    protected void  onStop() {
        stateEstimator.unregisterListeners();
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
                sabertoothDriver.setForwardMixed((byte)0);
                break;
        }
    }

    private Runnable updateDisplay = new Runnable() {
        public void run() {
            float[] orientation = stateEstimator.getOrientation();
            TextView fusedAzimuth = (TextView) findViewById(R.id.azimuth);
            fusedAzimuth.setText("Azimuth: " + (float) (orientation[0] * 180.0 / 3.143));

            float fusedSpeed = stateEstimator.getSpeed();
            TextView fusedSpeedView = (TextView) findViewById(R.id.speed);
            fusedSpeedView.setText("Fused Speed: " + fusedSpeed);

            float gpsSpeed = stateEstimator.getGpsSpeed();
            TextView gpsSpeedView = (TextView) findViewById(R.id.gpsSpeed);
            gpsSpeedView.setText("GPS Speed: " + gpsSpeed);
        }
    };

    private class UpdateDisplayTask extends TimerTask {
        @Override
        public void run() {
            handler.post(updateDisplay);
        }
    }
}
